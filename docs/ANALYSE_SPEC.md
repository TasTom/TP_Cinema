# Analyse de la spécification et traçabilité

Source : `Examen_TP_Billetterie_Cinema_Java25.pdf` (9 pages), BUT Informatique 3e année,
R5.08 Qualité du développement.

Ce document transforme le sujet en **exigences vérifiables**, puis relie chaque exigence à
l'implémentation et au test correspondant.

**Réserve issue de la relecture du 2026-10-21** : cette matrice décrit le socle et les parcours
nominaux. Elle ne démontre pas l'absence de défaut sur les cas limites. L'[audit approfondi](AUDIT_2026-10-21.md)
la complète avec les écarts reproduits, leur priorité et les sondes exécutées.

**Après correctifs et refonte** : les huit constats A01–A08 sont clôturés par des vérifications,
les 28 sondes ont rejoint la suite principale. `clean verify` final : 117 tests verts, aucun
ignoré. Les affiches, fiches film, import TMDB/Ghibli et animations restent une extension hors
barème et préservent les DTO REST et la règle de capacité. Voir la recette et les ADR-015 à 017.

---

## 1. Périmètre

### Dans le périmètre (imposé)

- Backend Spring Boot : API REST, persistance JPA, migrations Flyway, validation, sécurité,
  Actuator, OpenAPI.
- MySQL comme base de référence, sans H2.
- Build Maven reproductible avec wrapper versionné, Java 25.
- Tests automatisés exécutés par `.\mvnw.cmd clean verify`.
- README permettant à un tiers de relancer et vérifier le projet.

### Hors périmètre (exclu par le sujet)

- Interface graphique, paiement réel, envoi d'e-mail, inscription d'utilisateur.
- Comptes de démonstration en mémoire (autorisés explicitement).

### Extension décidée par le porteur du projet

- **Frontend Thymeleaf complet** (catalogue, réservation, back-office) : hors barème, isolé dans
  le paquet `ui`, sans modification des contrats d'API ni de la recette A1–A10.

---

## 2. Contraintes techniques et versions

| Contrainte du sujet | Mise en œuvre | Vérification |
|---|---|---|
| Java 25 | `<java.version>25</java.version>` (une seule déclaration, `release` fourni par le parent) | `.\mvnw.cmd -version` → Java 25.0.4.1 |
| Spring Boot 4.1.1 + parent Maven | `spring-boot-starter-parent:4.1.1` | `dependency:tree` |
| Maven Wrapper versionné | Maven 3.9.16, `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd` | `.\mvnw.cmd -version` |
| MySQL, pas H2 | MySQL 9.7 (Docker) en dev et en test Testcontainers | `CinemaIntegrationTest`, `ReservationRepositoryTest` |
| API REST Spring MVC | `spring-boot-starter-webmvc` + contrôleurs `@RestController` | `ReservationControllerMvcTest`, `ApiSecurityMvcTest` |
| Persistance Spring Data JPA | `spring-boot-starter-data-jpa`, repositories | `ReservationRepositoryTest` |
| Migrations Flyway | `spring-boot-starter-flyway` + `flyway-mysql` + `db/migration/V1`, `V2` | démarrage sur base neuve |
| Validation Jakarta | `spring-boot-starter-validation`, annotations sur les DTO | tests 400 |
| Spring Security | `spring-boot-starter-security`, HTTP Basic, BCrypt | `ApiSecurityMvcTest`, `CinemaIntegrationTest` |
| Actuator | `spring-boot-starter-actuator`, exposition `health` seule | `CinemaIntegrationTest` |
| DTO dédiés, entités non exposées | `record` Request/Response par fonctionnalité | revue de code |
| Versions gérées non répétées | seules `springdoc` (imposé 3.1.1) et `java.version` sont explicites | `pom.xml` |
| Pas de processeur d'annotations | Ni Lombok ni MapStruct | `pom.xml` |
| Documentation OpenAPI + Swagger UI | springdoc `3.1.1`, `/v3/api-docs`, `/swagger-ui.html` | `CinemaIntegrationTest` |

Détail complet des versions et de leur compatibilité : [`DECISIONS.md`](./DECISIONS.md), ADR-001.

---

## 3. Modèle métier et règles

| Agrégat | Données | Règles | Implémentation | Test |
|---|---|---|---|---|
| Film | id, titre, genre, dureeMinutes, dateSortie | titre obligatoire ; durée 40–300 | `Film`, `FilmService#creer`, contraintes SQL `ck_films_duree` | `ApiSecurityMvcTest`, contrainte SQL |
| Salle | id, nom, capacite | nom unique ; capacité 20–500 | `Salle`, `SalleService#creer`, `uk_salles_nom`, `ck_salles_capacite` | `ApiSecurityMvcTest`, `CinemaIntegrationTest` |
| Séance | id, film, salle, debut, prixUnitaire | film et salle obligatoires ; prix > 0 | `Seance`, `SeanceService#creer`, `ck_seances_prix`, `DECIMAL(10,2)` | `CinemaIntegrationTest` |
| Réservation | id, code, seance, emailClient, nombrePlaces, statut, creeeLe | 1–8 places ; code unique ; `CONFIRMEE`/`ANNULEE` | `Reservation`, `ReservationService`, `uk_reservations_code`, `ck_reservations_places` | `ReservationServiceTest`, `ReservationControllerMvcTest` |

Le montant total est **calculé côté serveur** : `prixUnitaire × nombrePlaces`, arrondi
`HALF_UP` à 2 décimales, stocké dans `montant_total`.

---

## 4. Invariant de capacité et concurrence

**Énoncé** : pour une séance, la somme des places des réservations `CONFIRMEE` ne dépasse jamais
la capacité de la salle. Une annulation libère les places. La vérification reste correcte sous
concurrence, sur deux threads ou deux instances.

**Mise en œuvre**

1. `@Transactional` sur `ReservationService#creer` : lecture, contrôle et écriture dans une seule
   transaction.
2. `SeanceRepository#findByIdForUpdate` : `@Lock(PESSIMISTIC_WRITE)` → `SELECT … FOR UPDATE` sur la
   ligne `seances`. C'est un **verrou en base**, pas une synchronisation mémoire.
3. La somme des places confirmées est calculée dans la même transaction, après obtention du verrou.
4. En cas de dépassement : `CapaciteInsuffisanteException` → HTTP 409, **aucune écriture**.
5. L'annulation prend le même verrou avant de basculer le statut, pour éviter qu'une annulation
   concurrente ne fausse un calcul de capacité.

**Scénario obligatoire du sujet** (5 places restantes, 2 × 4 places) : implémenté dans
`ReservationConcurrencyTest` — 20 places, 15 déjà réservées, deux requêtes simultanées de
4 places → exactement `201` et `409`, total confirmé `19 ≤ 20`.

Justification du choix : [`DECISIONS.md`](./DECISIONS.md), ADR-004.

---

## 5. Contrat HTTP et matrice de recette

| Méthode et chemin | Accès | Résultat attendu | Implémentation |
|---|---|---|---|
| `GET /api/films` | Public | 200, liste filtrable titre/genre | `FilmController#lister` |
| `GET /api/seances?du=&au=` | Public | 200, séances dans l'intervalle | `SeanceController#lister` |
| `POST /api/programmation/films` | PROGRAMMATEUR, ADMIN | 201 + `Location` | `ProgrammationFilmController` |
| `POST /api/programmation/seances` | PROGRAMMATEUR, ADMIN | 201 ou 400 | `ProgrammationSeanceController` |
| `POST /api/admin/salles` | ADMIN | 201, 400 ou 409 | `SalleAdminController` |
| `POST /api/reservations` | CLIENT (et ADMIN) | 201, 400, 404 ou 409 | `ReservationController#creer` |
| `DELETE /api/reservations/{code}` | CLIENT ou ADMIN | 204 ou 404 | `ReservationController#annuler` |
| `GET /actuator/health` | Public | 200 + état | Actuator |

Endpoints de lecture complémentaires (non exigés, cohérents avec le modèle) :
`GET /api/films/{id}`, `GET /api/seances/{id}`, `GET /api/salles`, `GET /api/salles/{id}`.

### Recette fonctionnelle A1–A10

| ID | Contrôle | Preuve |
|---|---|---|
| A1 | Wrapper + Java 25, `clean verify` réussit | `.\mvnw.cmd -version`, `clean verify` |
| A2 | Base neuve : Flyway migre, Hibernate valide | `V1`/`V2`, `ddl-auto: validate`, démarrage vérifié |
| A3 | Lecture publique anonyme en JSON maîtrisé | `CinemaIntegrationTest#exposeLaLecturePubliqueLaSanteEtLaDocumentation` |
| A4 | 0 ou 9 places → 400 + corps stable | `ReservationControllerMvcTest`, `CinemaIntegrationTest#valideLesPlacesEtRefuseLaSurreservation` |
| A5 | CLIENT ne crée pas de salle, ADMIN oui, anonyme 401 | `ApiSecurityMvcTest`, `CinemaIntegrationTest#appliqueLesAutorisationsDesRoles` |
| A6 | Réservation valide → 201 avec code, statut, quantité, montant | `ReservationControllerMvcTest#creeUneReservationValide`, `CinemaIntegrationTest` |
| A7 | Demande > places restantes → 409 sans écriture | `ReservationServiceTest#refuseUneReservationAuDelaDeLaCapacite`, `CinemaIntegrationTest` |
| A8 | Deux demandes simultanées sans surréservation | `ReservationConcurrencyTest` |
| A9 | Annulation 204 puis places libérées | `ReservationServiceTest#annuleUneReservationEtLibereLesPlaces`, `CinemaIntegrationTest` |
| A10 | Health et OpenAPI accessibles | `CinemaIntegrationTest#exposeLaLecturePubliqueLaSanteEtLaDocumentation` |

---

## 6. Sécurité

| Exigence | Mise en œuvre |
|---|---|
| HTTP Basic | `SecurityConfig`, `httpBasic(Customizer.withDefaults())` |
| Mots de passe jamais en clair dans le code source | BCrypt dans `SecurityConfig`, mais identifiants fictifs en clair dans `templates/login.html` : écart A04 de l'audit |
| BCrypt | `BCryptPasswordEncoder` (facteur 10) |
| Trois comptes documentés | `client` / `client123`, `programmateur` / `prog123`, `admin` / `admin123` |
| CLIENT : créer et annuler une réservation | règles `POST /api/reservations`, `DELETE /api/reservations/**` |
| PROGRAMMATEUR : films et séances | `POST /api/programmation/**` |
| ADMIN : salles + toutes les opérations protégées | `POST /api/admin/**` + `hasAnyRole(..., "ADMIN")` |
| Lectures publiques | `GET /api/films/**`, `/api/seances/**`, `/api/salles/**` |
| Documentation OpenAPI publique | `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` |
| `/actuator/health` public, autres endpoints non exposés | `management.endpoints.web.exposure.include: health` |
| Réponses 401/403 JSON stables | `JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler` |

Note : deux chaînes de filtres coexistent — `@Order(1)` pour l'API REST (sans session, JSON) et
`@Order(2)` pour l'interface Thymeleaf (form login, CSRF, session).

---

## 7. Erreurs

Les erreurs métier des contrôleurs REST passent par `GlobalExceptionHandler`
(`@RestControllerAdvice` limité aux `@RestController`). Les réponses d'erreur contrôlées
n'exposent ni pile ni SQL. Cette centralisation ne couvre pas uniformément les erreurs de
routage et de négociation HTTP ; les écarts A03, A05 et A07 sont détaillés dans l'audit.

| Situation | Statut | Code |
|---|---|---|
| Données invalides (Jakarta Validation) | 400 | `VALIDATION_ECHOUEE` |
| Corps absent ou mal formé | 400 | `REQUETE_MAL_FORMEE` |
| Paramètre manquant ou invalide | 400 | `PARAMETRE_MANQUANT`, `PARAMETRE_INVALIDE` |
| Ressource absente | 404 | `SEANCE_INTROUVABLE`, `FILM_INTROUVABLE`, `RESERVATION_INTROUVABLE`, `SALLE_INTROUVABLE` |
| Capacité insuffisante | 409 | `CAPACITE_INSUFFISANTE` |
| Doublon (nom de salle) | 409 | `SALLE_DEJA_EXISTANTE` |
| Conflit d'intégrité | 409 | `CONFLIT_DONNEES` |
| Authentification absente | 401 | `NON_AUTHENTIFIE` |
| Rôle insuffisant | 403 | `ACCES_REFUSE` |
| Erreur non gérée | 500 | `ERREUR_INTERNE` |

Corps stable : `timestamp`, `status`, `code`, `message`, `path`, `details[]` (optionnel).

---

## 8. Tests demandés par le sujet

| Type | Couverture minimale | Classe(s) |
|---|---|---|
| Service unitaire | montant, capacité insuffisante, annulation | `ReservationServiceTest` |
| MVC | création valide, 400, 401, 403 | `ReservationControllerMvcTest`, `ApiSecurityMvcTest` |
| JPA | recherche et somme des places confirmées | `ReservationRepositoryTest` |
| Intégration MySQL | migration, réservation réelle, transaction | `CinemaIntegrationTest` |
| Concurrence | deux réservations simultanées | `ReservationConcurrencyTest` |

Les tests MySQL utilisent Testcontainers et sont annotés `@Testcontainers(disabledWithoutDocker = true)` :
sans Docker, ils sont ignorés et **les autres tests continuent de s'exécuter** (limite documentée
dans le README, conformément au sujet).

---

## 9. Exploitation et documentation

| Exigence | Livrable |
|---|---|
| `/actuator/health` public, autres endpoints masqués | `application.yml` |
| `/v3/api-docs` et `/swagger-ui.html` | `OpenApiConfig`, springdoc |
| README court : prérequis, MySQL, lancement, comptes, commandes, endpoints, concurrence | [`README.md`](../README.md) |
| JAR vérifié hors IDE | `java -jar target/cinema-billetterie-1.0.0.jar` |
| Pas de secret réel | identifiants de dev par variables d'environnement, hachages BCrypt seuls |
| Pas de `target/`, config IDE, journaux, secrets locaux | [`.gitignore`](../.gitignore) |

---

## 10. Bonus retenu

**Métriques métier Micrometer** (1 point maximum, plafonné) :

- `cinema.reservations{resultat="confirmee"|"annulee"|"refusee"}` — compteur d'opérations.
- `cinema.places{resultat="reservee"|"refusee"}` — distribution des places par opération.

Vérifié dans `ReservationServiceTest` via `SimpleMeterRegistry`. Les deux autres bonus du sujet
(ETag, idempotence) n'ont pas été retenus pour rester dans le temps imparti et éviter du code
non testé.
