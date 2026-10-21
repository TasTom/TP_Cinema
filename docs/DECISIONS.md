# Décisions d'architecture (ADR)

Chaque décision est datée, motivée, et accompagnée de ses conséquences et des alternatives
écartées. Format inspiré de « Architecture Decision Record ».

---

## ADR-001 — Versions retenues et compatibilité

**Date** : 2026-10-21
**Statut** : accepté

### Décision

| Composant | Version | Justification |
|---|---|---|
| Java | 25.0.4.1 LTS (Microsoft OpenJDK) | Imposé par le sujet ; LTS ; présent localement |
| Spring Boot | 4.1.1 | Imposé par le sujet |
| Spring Framework | 7.0.9 | Géré par le BOM Boot 4.1.1 |
| Spring Security | 7.1.1 | Géré par le BOM |
| Hibernate | 7.4.5.Final | Géré par le BOM |
| Flyway | 12.4.0 | Géré par le BOM |
| MySQL Connector/J | 9.7.0 | Géré par le BOM |
| Testcontainers | 2.0.5 | Géré par le BOM |
| springdoc-openapi | 3.1.1 | Imposé par le sujet ; POM compatible Boot 4 |
| Maven Wrapper | 3.9.16 | Dernière 3.9.x stable |
| MySQL (image) | `mysql:9.7` (9.7.2) | Dernière LTS de l'image officielle |

### Vérifications effectuées

- `Spring Boot 4.1.1` : « requires at least Java 17 and is compatible with versions up to and
  including Java 26 » → Java 25 supporté.
- Présence effective des starters utilisés dans le dépôt Maven Central en 4.1.1 :
  `spring-boot-starter-webmvc`, `-data-jpa`, `-validation`, `-security`, `-flyway`, `-actuator`,
  `-thymeleaf`, `-test`, `-webmvc-test`, `-data-jpa-test`, `-security-test`,
  `spring-boot-testcontainers`.
- `springdoc-openapi-starter-webmvc-ui:3.1.1` dépend de `spring-boot-tomcat` et `spring-boot-health`,
  ce qui confirme sa cible Boot 4.
- Maven 4.0.0-rc-7 existe mais reste une *release candidate* → écarté au profit de 3.9.16.

### Conséquences

- Aucune version n'est répétée dans les dépendances gérées par le parent, à l'exception de
  `springdoc` (version imposée par le sujet).
- `java.version=25` est la seule déclaration de niveau de langage : le parent en déduit `release 25`,
  sans `source`/`target` contradictoires.

### Point de vigilance

`java` dans le `PATH` de la machine est un JDK 23. Il faut donc définir `JAVA_HOME` sur le JDK 25
avant `.\mvnw.cmd` (documenté dans le README). Le wrapper affiche la version réellement utilisée.

---

## ADR-002 — Architecture par fonctionnalité

**Date** : 2026-10-21
**Statut** : accepté

### Contexte

Le sujet demande une architecture propre avec « contrôleurs fins, services responsables des règles
métier, repositories Spring Data et DTO immuables ».

### Décision

Découpage par **fonctionnalité métier** (`film`, `salle`, `seance`, `reservation`) plutôt que par
couche technique globale (`controllers/`, `services/`, `repositories/`).

### Raisons

- Cohésion : tout ce qui concerne une réservation est dans un seul paquet.
- Lisibilité des dépendances : un paquet ne dépend que des paquets dont il a besoin.
- Évolutivité : ajouter une fonctionnalité n'ajoute pas un fichier dans chacun des paquets globaux.

### Conséquences

- Les classes transverses sont limitées à `common.error`, `config`, `security`.
- Les DTO restent par fonctionnalité (`film/api/dto`), ce qui évite un paquet `dto` fourre-tout.

---

## ADR-003 — DTO en `record` et absence de processeur d'annotations

**Date** : 2026-10-21
**Statut** : accepté

### Contexte

Le sujet signale qu'« un ancien Lombok peut échouer avec Java 25 » et qu'aucun point n'est
attribué pour un processeur d'annotations en soi.

### Décision

- Aucun Lombok, aucun MapStruct.
- Entrées et sorties exposées via des `record` Java (`FilmRequest`, `FilmResponse`,
  `SeanceRequest`, `SeanceResponse`, `SalleRequest`, `SalleResponse`, `ReservationRequest`,
  `ReservationResponse`, `ApiError`, `ChampErreur`).
- Les objets de formulaire Thymeleaf sont de simples POJO mutables (`ui/form`), car Thymeleaf et
  `BindingResult` ont besoin de setters.

### Conséquences

- Zéro configuration de processeur d'annotations dans le `pom.xml`.
- Aucun risque de désynchronisation de sources générées au `clean verify`.
- Plus de code explicite à écrire, mais entièrement lisible et sans magie.

---

## ADR-004 — Concurrence : verrou pessimiste en base

**Date** : 2026-10-21
**Statut** : accepté (choix validé par le porteur du projet)

### Contexte

Le sujet exige que la vérification de capacité reste correcte « lorsque deux requêtes arrivent en
même temps sur deux threads ou deux instances », et précise qu'« une synchronisation seulement en
mémoire n'est pas recevable ». Il faut « une transaction et une stratégie explicite de concurrence
fondée sur la base ».

### Décision

- `@Transactional` sur `ReservationService#creer`.
- `SeanceRepository#findByIdForUpdate` avec `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- La somme des places confirmées est calculée après obtention du verrou, dans la même transaction.
- `ReservationService#annuler` prend le même verrou avant de changer le statut.
- En cas de dépassement : exception métier → HTTP 409, aucune écriture partielle.

### Alternatives écartées

| Alternative | Motif |
|---|---|
| `synchronized`, `ReentrantLock` | Ne protège qu'une JVM, explicitement refusé par le sujet |
| Verrou optimiste `@Version` + reprise | Ajoute une boucle de retry et rend la sémantique 409 moins directe ; le conflit doit être géré dans tous les appelants |
| `SERIALIZABLE` | Portée trop large, dégrade toutes les requêtes |
| Compteur `places_reservees` sur `seances` | Duplique l'information dérivable, risque de désynchronisation |

### Conséquences

- Les opérations d'une même séance sont sérialisées : c'est le comportement voulu.
- Les séances différentes ne se bloquent pas mutuellement.
- Un verrou pessimiste nécessite un index/une clé primaire stable : `seances.id` est la clé primaire.
- Coût : un accès concurrent intense sur **une même séance** voit de l'attente. Acceptable pour
  une billetterie, où le débit par séance est naturellement limité.

### Preuve

`ReservationConcurrencyTest` : capacité 20, 15 places déjà confirmées, deux requêtes simultanées
de 4 places → statuts exactement `{201, 409}`, total confirmé `19`, jamais `23`.

---

## ADR-005 — Sécurité : HTTP Basic, BCrypt, deux chaînes de filtres

**Date** : 2026-10-21
**Statut** : accepté

### Contexte

Le sujet impose HTTP Basic pour l'épreuve, BCrypt, trois comptes de démonstration, et laisse
entendre que l'inscription d'utilisateur est hors périmètre. Le projet ajoute une interface
Thymeleaf qui, elle, a besoin d'un formulaire de connexion et d'une session.

### Décision

- Deux `SecurityFilterChain` ordonnées.
  - `@Order(1)` pour l'API (`/api/**`, OpenAPI, Actuator) : stateless, CSRF désactivé, Basic, erreurs JSON.
  - `@Order(2)` pour l'interface : form login, session, CSRF actif, page `/acces-refuse`.
- Comptes en mémoire (`InMemoryUserDetailsManager`) avec hachages BCrypt.
- Aucun mot de passe en clair dans les sources.

### Conséquences

- L'API reste utilisable par un client non navigateur (`curl`, tests, Postman).
- Les 401/403 de l'API sont des corps JSON stables, pas des redirections HTML.
- Les tests MVC importent la configuration de sécurité réelle : les règles testées sont celles de
  production.

### Limites assumées

- Les comptes en mémoire sont adaptés à la démonstration, pas à la production. Une version
  ultérieure remplacerait `UserDetailsService` par une table `utilisateurs` et un hachage
  configurable.
- Les mots de passe de démonstration sont publics **parce qu'ils sont fictifs** et documentés
  comme tels.

---

## ADR-006 — Flyway plutôt que `ddl-auto: update`

**Date** : 2026-10-21
**Statut** : accepté

### Décision

- Schéma entièrement défini par `V1__schema_initial.sql` et données de démonstration par
  `V2__donnees_demonstration.sql`.
- `spring.jpa.hibernate.ddl-auto=validate`.
- `baseline-on-migrate: false` : une base non vide non versionnée fait échouer le démarrage plutôt
  que de masquer une dérive.

### Conséquences

- La base est reconstructible à l'identique par suppression puis redémarrage.
- Toute divergence entre entités et schéma casse le démarrage : c'est un filet de sécurité.
- Les contraintes métier existent **aussi** en base (unicité, plages, énumérations), pas seulement
  en Java : deux niveaux de défense.

---

## ADR-007 — Fuseau horaire : `connectionTimeZone=LOCAL`

**Date** : 2026-10-21
**Statut** : accepté (correction d'un défaut constaté)

### Problème observé

Avec `serverTimezone=UTC` et la conversion JDBC par défaut, une séance stockée à `20:00:00` en
base était relue `21:00:00` par l'API en heure d'été française (UTC+2), puis `20:00` en heure
d'hiver : décalage incohérent selon la date.

### Décision

```
jdbc:mysql://...&connectionTimeZone=LOCAL
spring.jpa.properties.hibernate.type.java_time_use_direct_jdbc=true
```

### Conséquences

- Une valeur `LocalDateTime` en base est celle qui sort de l'API et s'affiche dans l'interface.
- Aucune conversion implicite entre fuseau du serveur et fuseau applicatif.
- Vérifié de bout en bout : base `20:00:00` → API `2026-11-05T20:00:00` → interface `20:00`.

### Pourquoi pas `TIMESTAMP` + `Instant`

`LocalDateTime` modélise une séance programmée à une heure locale donnée, indépendamment du fuseau
du spectateur. Passer en `Instant` changerait la sémantique métier et la lecture de l'API.

---

## ADR-008 — Frontend Thymeleaf hors barème, isolé

**Date** : 2026-10-21
**Statut** : accepté (choix validé par le porteur du projet)

### Contexte

Le sujet indique explicitement : « Aucune interface graphique […] n'est demandée ». Le porteur du
projet demande néanmoins un frontend sobre et optimisé en Thymeleaf.

### Décision

- Frontend complet implémenté dans le paquet `ui` + `templates/` + `static/css/`.
- Aucune règle métier dupliquée : les contrôleurs d'interface appellent les mêmes services que l'API.
- Aucun endpoint d'API modifié pour les besoins de l'interface.
- Sécurité de l'interface séparée (chaîne `@Order(2)`), ce qui n'altère pas les règles de l'API.

### Conséquences

- La recette A1–A10 reste évaluable sans tenir compte de l'interface.
- Le rendu est 100 % serveur, sans JavaScript applicatif, sans CDN : sobre, rapide, hors ligne.
- CSRF actif sur tous les formulaires.
- Le README précise clairement qu'il s'agit d'une extension non notée.

---

## ADR-009 — Erreurs centralisées et codes stables

**Date** : 2026-10-21
**Statut** : accepté

### Décision

- Exceptions métier spécifiques héritant de `MetierException` (chacune porte son `code`).
- `GlobalExceptionHandler` restreint aux `@RestController` : aucune interception des contrôleurs
  Thymeleaf.
- Codes d'erreur en français, stables et documentés (`VALIDATION_ECHOUEE`, `SEANCE_INTROUVABLE`,
  `CAPACITE_INSUFFISANTE`, `SALLE_DEJA_EXISTANTE`, `NON_AUTHENTIFIE`, `ACCES_REFUSE`, …).
- Corps JSON : `timestamp`, `status`, `code`, `message`, `path`, `details[]` optionnel.
- Aucune trace de pile ni détail SQL dans la réponse ; les erreurs techniques sont journalisées
  côté serveur.

### Conséquences

- Les tests peuvent asserter sur `code` et non sur un message libre.
- Un client peut réagir programmatiquement au `code`.
- Les messages restent compréhensibles pour un utilisateur final.

---

## ADR-010 — Bonus : métriques Micrometer uniquement

**Date** : 2026-10-21
**Statut** : accepté (choix validé par le porteur du projet)

### Décision

Implémenter uniquement le bonus « métrique métier Micrometer », avec :

- `cinema.reservations{resultat="confirmee"|"annulee"|"refusee"}` (compteur) ;
- `cinema.places{resultat="reservee"|"refusee"}` (distribution).

### Raisons

- Le bonus est plafonné à 1 point : trois bonus ne rapportent pas plus qu'un seul.
- Une métrique est peu risquée, entièrement testable sans infrastructure supplémentaire
  (`SimpleMeterRegistry`), contrairement à l'idempotence ou aux ETag qui touchent le contrat HTTP.
- Les bonus non implémentés ne laissent aucun code mort.

### Conséquences

- Les métriques sont vérifiées dans `ReservationServiceTest`.
- Les endpoints Actuator restent limités à `health` : les métriques ne sont pas exposées
  publiquement, conformément au sujet.
