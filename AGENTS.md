# AGENTS.md — Mémoire du projet CinéBil

> **À lire avant toute intervention.** Ce fichier est la mémoire persistante du projet : état,
> règles non négociables, conventions, pièges déjà rencontrés et prochaines étapes.
> Il est lu par tout agent (humain ou IA) qui reprend le dépôt.
>
> Mise à jour : **2026-10-21**. À actualiser à chaque décision structurante ou changement d'état.

**Référence unique pour tous les assistants.** Les consignes communes sont fusionnées ici.
Le fichier [`.github/copilot-instructions.md`](.github/copilot-instructions.md) sert uniquement
de point d'entrée vers ce document ; ne pas y dupliquer les règles. Modifier les consignes ici,
quel que soit l'assistant utilisé.

---

## 1. Identité du projet

| Élément            | Valeur                                                                                                     |
| -------------------- | ---------------------------------------------------------------------------------------------------------- |
| Nom                  | **CinéBil** — billetterie de cinéma                                                               |
| Origine              | Épreuve **R5.08 Qualité du développement**, BUT Informatique 3e année                            |
| Sujet de référence | [`Examen_TP_Billetterie_Cinema_Java25.pdf`](./Examen_TP_Billetterie_Cinema_Java25.pdf)                    |
| Livrable exigé      | dépôt Maven complet, `.\mvnw.cmd clean verify` vert, JAR lançable hors IDE, README                    |
| Nature               | **Backend imposé** + **frontend Thymeleaf ajouté hors barème** (choix du porteur du projet) |
| Dépôt Git          | `main` ; consulter `git status` pour l'état de travail courant                                        |

**Statut actuel : refonte et import externe livrés, constats A01–A08 corrigés.**
117 tests verts sans test ignoré, puis recette Chrome bureau/mobile. Consulter la recette et
l'audit pour les preuves et les limites de production ; ne pas annoncer une sécurité exhaustive.

---

## 2. Règles non négociables — contraintes du sujet et choix du projet

Ces règles restent obligatoires pour ce projet. Distinguer les obligations du PDF des choix
d'implémentation figés ici : le sujet autorise aussi le verrou optimiste avec gestion du conflit,
et rend Lombok/MapStruct facultatifs. Le projet conserve ses choix documentés.

1. **Java 25** pour compiler, tester et exécuter. Une seule déclaration : `java.version=25`
   (le parent Spring Boot fournit `release`). **Jamais** de `source`/`target` contradictoires.
2. **Spring Boot 4.1.1** avec `spring-boot-starter-parent` conservé.
3. **Maven Wrapper versionné** (3.9.16). Le build IDE ne prouve rien : `.\mvnw.cmd clean verify` fait foi.
4. **MySQL** comme base de référence. **H2 interdit**, même pour les tests : Testcontainers MySQL.
5. **`ddl-auto=validate`**. Schéma créé **exclusivement** par Flyway. Aucune création automatique.
6. **DTO dédiés** aux entrées et sorties. Une entité JPA ne sort **jamais** d'un contrôleur.
7. **Aucune version répétée** pour les dépendances gérées par le parent (seule exception : springdoc 3.1.1, imposé).
8. **Contrôleurs fins** : la règle de capacité n'y figure pas. Les services portent le métier et les transactions.
9. **Concurrence en base** : choix du projet, verrou pessimiste `SELECT … FOR UPDATE` (ADR-004).
   Une synchronisation uniquement en mémoire est **refusée** par le sujet.
10. **HTTP Basic + BCrypt**. Aucun mot de passe en clair dans les sources.
11. **Endpoints Actuator** : seul `/actuator/health` est exposé.
12. **Pas de dépendance ajoutée « au cas où »** : toute dépendance doit répondre à un besoin implémenté et testé.

---

## 3. Versions figées (vérifiées compatibles)

| Composant                   | Version                     | Note                                               |
| --------------------------- | --------------------------- | -------------------------------------------------- |
| Java                        | 25.0.4.1 LTS                | JDK local : `C:\Users\tomta\.jdks\ms-25.0.4.1`   |
| Spring Boot                 | 4.1.1                       | impose Java 17–26                                 |
| Spring Framework / Security | 7.0.9 / 7.1.1               | gérés par le BOM                                 |
| Hibernate                   | 7.4.5.Final                 | géré par le BOM                                  |
| Flyway / Connector-J        | 12.4.0 / 9.7.0              | gérés par le BOM                                 |
| Testcontainers              | 2.0.5                       | géré par le BOM                                  |
| springdoc-openapi           | 3.1.1                       | ⚠️ version explicitement imposée                |
| Maven Wrapper               | 3.9.16                      | 4.0.0-rc-7 existe mais reste RC → ne pas utiliser |
| MySQL                       | image `mysql:9.7` (9.7.2) | LTS                                                |

---

## 4. Commandes de référence

```powershell
# Toujours définir JAVA_HOME : le `java` du PATH est un JDK 23
$env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1"

docker compose up -d                      # MySQL 9.7 sur localhost:3306
.\mvnw.cmd clean verify                   # build + tests + JAR
& "$env:JAVA_HOME\bin\java.exe" -jar target\cinema-billetterie-1.0.0.jar
.\mvnw.cmd spring-boot:run                # alternative
```

Comptes de démonstration : `client/client123` (CLIENT), `programmateur/prog123` (PROGRAMMATEUR),
`admin/admin123` (ADMIN).

---

## 5. Carte de l'architecture

Paquets par **fonctionnalité**, pas par couche technique.

```text
fr.butinfo.cinema
├── CinemaApplication
├── config/         SecurityConfig (2 chaînes), OpenApiConfig
├── common/error/   ApiError, MetierException + 4 sous-classes, GlobalExceptionHandler
├── film/           Film, FilmRepository, FilmService, api/{FilmController, ProgrammationFilmController}
├── salle/          Salle, SalleRepository, SalleService, api/{SalleController, SalleAdminController}
├── seance/         Seance, SeanceRepository, SeanceService, api/{SeanceController, ProgrammationSeanceController}
├── reservation/    Reservation, StatutReservation, ReservationRepository, ReservationService,
│                   ReservationMetrics, api/ReservationController
├── security/       JsonAuthenticationEntryPoint, JsonAccessDeniedHandler
└── ui/             contrôleurs Thymeleaf + form/ (extension hors barème)
```

Règles de dépendance : `api → service → repository`. Le paquet `ui` dépend des services, jamais
des repositories. Les entités ne franchissent pas la frontière du service.

**Écart constaté lors de l'audit** : `FilmService` et `SalleService` renvoient actuellement des
entités à leurs appelants ; la règle interne de frontière de service ci-dessus n'est donc pas
encore satisfaite. Les contrôleurs REST les convertissent en DTO avant la réponse HTTP,
conformément à l'exigence de sortie du PDF. Ne pas confondre ces deux frontières.

Fichiers clés :

| Sujet                  | Fichier                                                                                                      |
| ---------------------- | ------------------------------------------------------------------------------------------------------------ |
| Invariant de capacité | [ReservationService.java](src/main/java/fr/butinfo/cinema/reservation/ReservationService.java)                |
| Verrou pessimiste      | [SeanceRepository.java](src/main/java/fr/butinfo/cinema/seance/SeanceRepository.java) (`findByIdForUpdate`) |
| Schéma                | [V1__schema_initial.sql](src/main/resources/db/migration/V1__schema_initial.sql)                              |
| Sécurité             | [SecurityConfig.java](src/main/java/fr/butinfo/cinema/config/SecurityConfig.java)                             |
| Erreurs                | [GlobalExceptionHandler.java](src/main/java/fr/butinfo/cinema/common/error/GlobalExceptionHandler.java)       |
| Configuration          | [application.yml](src/main/resources/application.yml)                                                         |

---

## 6. Conventions du dépôt

### Code

- **Langue métier en français** : `Film`, `Salle`, `Seance`, `Reservation`, `StatutReservation`,
  `CreerReservationCommand`, `ReservationResult`. Rester cohérent : ne pas mélanger avec de l'anglais.
- **DTO = `record` immuables** (`*Request`, `*Response`, `*Command`, `*Result`). Aucune entité exposée.
- **Pas de Lombok, pas de MapStruct** : aucun processeur d'annotations. Ne pas en ajouter.
- Constructeurs explicites, injection par constructeur, champs `private final`.
- Validations à deux niveaux : annotations Jakarta sur les DTO **et** garde défensive dans le service.
- Erreurs : lever une sous-classe de `MetierException` avec un `code` stable ; ne jamais renvoyer de
  trace de pile ni de détail SQL.
- Pas de commentaire qui paraphrase le code ; seulement l'intention non évidente.

### Interface Thymeleaf

- Aucune règle métier dans `ui` : appeler les mêmes services que l'API.
- CSS unique `static/css/app.css`, JS local progressif `static/js/app.js`, fontes système.
- Exception autorisée le 21 octobre : données de films TMDB/Ghibli et aperçus d'affiches externes.
  Pages publiques : images importées locales, jamais de jeton côté navigateur (ADR-016).
- Répertoire `data/affiches/` persistant, ignoré par Git ; sauvegarder avec MySQL.
- CSRF obligatoire sur chaque formulaire : `<input th:replace="~{fragments/layout :: csrf}">`.
- Formats français centralisés dans `FormattingHelper` (bean `@format`).

### Tests

- Noms de méthodes **en français descriptif** : `creeUneReservationValide`, `refuseUneReservationAuDelaDeLaCapacite`.
- `ReservationServiceTest` : Mockito, jamais de base.
- Tests MySQL : `@Testcontainers(disabledWithoutDocker = true)` **obligatoire**, sinon le build
  échoue sur un poste sans Docker.
- `UiMvcTest` : vrais templates et filtres de sécurité, services simulés ; aucune base.
- Assertions AssertJ ; `assertThatThrownBy(...).isInstanceOfSatisfying(...)` pour vérifier un `code`.

### Vérification avant de conclure

```powershell
$env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1"
.\mvnw.cmd clean verify
```

- Critère de fin : `BUILD SUCCESS`, aucun échec ni erreur ; le nombre de tests évolue avec le projet.
- Distinguer les tests exécutés des tests ignorés, notamment lorsque Docker est inaccessible.
  Un build vert avec des tests MySQL ignorés ne revalide pas les migrations ni la concurrence.
- Ne jamais annoncer un résultat non exécuté. Consigner la commande, la date, le résultat
  et les éventuelles limites dans `docs/RECETTE.md`.

### Git

- Convention **Conventional Commits** en français : `build:`, `feat(persistence):`, `feat(api):`,
  `feat(ui):`, `test:`, `docs:`.
- Un commit par étape cohérente ; ne jamais committer `target/`, `.idea/`, journaux ou secrets.

---

## 7. Pièges déjà rencontrés — ne pas les revivre

| Piège                                              | Symptôme                                                                         | Solution retenue                                                                                                                                                                                                        |
| --------------------------------------------------- | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Jackson 3**                                 | `No qualifying bean of type com.fasterxml.jackson.databind.ObjectMapper`        | Injecter `tools.jackson.databind.ObjectMapper` (Boot 4 utilise Jackson 3)                                                                                                                                             |
| **Noms de starters Boot 4**                   | `spring-boot-starter-web` introuvable / obsolète                               | Utiliser `-webmvc`, `-flyway`, `-webmvc-test`, `-data-jpa-test`, `-security-test`                                                                                                                             |
| **Annotations de test déplacées**           | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` inexistant | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase` |
| **`@ImportAutoConfiguration`**              | import `org.springframework.context.annotation` introuvable                     | `org.springframework.boot.autoconfigure.ImportAutoConfiguration`                                                                                                                                                      |
| **`MySQLContainer` non générique**        | `type MySQLContainer does not take parameters`                                  | `new MySQLContainer("mysql:9.7")` sans `<>`                                                                                                                                                                         |
| **`ClientHttpResponse.bodyTo`**             | méthode inexistante en Spring 7                                                  | Utiliser `retrieve().toEntity(...)` et **capturer `RestClientResponseException`** pour les 4xx attendus                                                                                                       |
| **`TestRestTemplate`**                      | package déplacé en Boot 4                                                       | Préférer `RestClient` + `@LocalServerPort`                                                                                                                                                                        |
| **Fuseau horaire**                            | séance 20:00 stockée → 21:00 renvoyée                                         | `connectionTimeZone=LOCAL` + `hibernate.type.java_time_use_direct_jdbc=true`                                                                                                                                        |
| **`JAVA_HOME`**                             | `java -version` = 23, build incohérent                                         | Définir `JAVA_HOME` avant Maven et utiliser explicitement `& "$env:JAVA_HOME\bin\java.exe"` pour le JAR : `JAVA_HOME` ne modifie pas le `PATH` |
| **Client JSON en PowerShell**                 | `REQUETE_MAL_FORMEE` sur un `curl -d` inline                                  | Écrire le JSON dans un fichier et utiliser `--data-binary "@fichier.json"`                                                                                                                                           |
| **Warning Flyway / MySQL 9.7**                | « newer than the version Flyway has been verified with »                        | Informatif : migrations et `validate` passent, documenté dans le README                                                                                                                                              |
| **Warning Mockito**                           | auto-attachement de l'inline-mock-maker                                           | Sans impact aujourd'hui, ne pas passer du temps dessus                                                                                                                                                                  |
| **Wrapper Windows / cache `.m2` ordinaire** | `Indexation impossible dans un tableau Null` avant Maven                        | Vérifier que `Target` existe avant `Target[0]` dans `mvnw.cmd` (ADR-013)                                                                                                                                         |
| **Pages d'erreur Thymeleaf**                  | `estAdmin or estProgrammateur` échoue sur des attributs absents                | Réinitialiser les attributs communs dans `UiExceptionHandler` : le modèle est reconstruit lors du traitement d'une exception                                                                                        |

---

## 8. État vérifié (2026-10-21)

`clean verify` : **BUILD SUCCESS — 117 tests, 0 échec, 0 erreur, 0 ignoré**.
MySQL Testcontainers, migrations V1–V4 et concurrence exécutés ; JAR Java 25 lancé hors IDE
sur http://localhost:8080/. PID dans `.tools/cinema.pid`, journaux dans `logs/` : vérifier le
processus réel avant de l'arrêter, ces numéros sont éphémères.

| Classe | Tests |
|---|---|
| `AuditCasLimitesTest` | 17 |
| `AuditSecuriteMvcTest` | 11 |
| `AffichesServiceTest` | 11 |
| `CatalogueExterneServiceTest` | 6 |
| `ImportFilmIntegrationTest` | 2 |
| `ImportFilmServiceTest` | 1 |
| `CinemaIntegrationTest` | 6 |
| `ReservationConcurrencyTest` | 1 |
| `ReservationControllerMvcTest` | 8 |
| `ReservationRepositoryTest` | 1 |
| `ReservationServiceTest` | 5 |
| `ApiSecurityMvcTest` | 9 |
| `UiMvcTest` | 39 |

Audit initial : 28 sondes, 10 échecs archivés dans `docs/audit/2026-10-21/`.
Les 28 sondes sont désormais intégrées et vertes ; A08 vérifié par un vrai SELECT authentifié.
Ne pas réécrire les résultats historiques de l'audit. Les limitations de production subsistent.

Recette Chrome : six films Ghibli importés avec 12 médias locaux ; Chihiro enrichit le film
existant. Pages bureau et mobile 390/320 px, pas de débordement sur les pages vérifiées,
pas d'erreur JavaScript ; réservation réelle de deux places puis annulation et restitution.
TMDB vérifié par HTTP simulé, pas de jeton personnel pour une recette en direct.
Script reproductible : `scripts/recette_ui.py`, détails dans `docs/RECETTE.md`.

L'utilisateur a défini `TMDB_API_TOKEN` dans son propre terminal (valeur fournie masquée).
Cette variable n'est pas transmise à l'environnement de l'agent. Redémarrage depuis ce même
terminal avec `scripts/lancer.ps1` ; `-Verifier` contrôle Java/JAR/processus sans redémarrer.
Ne pas demander ni consigner le jeton. Le script PowerShell utilise UTF-8 avec BOM pour PS 5.

---

### Pièges ajoutés avec la refonte

- `th:replace` est évalué avant `th:each` sur le même élément : placer l'itération dans un
  `th:block` extérieur pour transmettre une variable non nulle au fragment.
- Les captures de page entière ne chargent pas tous les `loading="lazy"` : déclencher leur
  chargement avant de conclure sur les images du rendu.
- Ghibli fournit des années, TMDB des dates et des durées via le détail. Ne pas inventer de
  date, durée ou séance pour compenser une donnée externe absente.

## 9. Documentation

| Fichier                                     | Rôle                                                        |
| ------------------------------------------- | ------------------------------------------------------------ |
| [README.md](README.md)                       | Mode d'emploi complet pour un tiers                          |
| [docs/ANALYSE_SPEC.md](docs/ANALYSE_SPEC.md) | Analyse du sujet + traçabilité exigence → code → test    |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Couches, flux, diagramme de séquence de la concurrence      |
| [docs/DECISIONS.md](docs/DECISIONS.md)       | ADR, alternatives écartées et leurs motifs                 |
| [docs/API.md](docs/API.md)                   | Contrat HTTP, exemples `curl`, catalogue d'erreurs         |
| [docs/RECETTE.md](docs/RECETTE.md)           | Procès-verbal des contrôles réellement exécutés         |
| [docs/AUDIT_2026-10-21.md](docs/AUDIT_2026-10-21.md) | Audit PDF, sécurité, cas limites, constats à corriger et reproductions |
| [docs/PROMPTS.md](docs/PROMPTS.md)           | **Journal des prompts du porteur du projet**, verbatim |

### Règle de consignation (demande explicite du porteur du projet)

> « Tout doit être documenté/consigné ainsi que mes prompts que je t'envoie. »

1. **Chaque nouveau prompt** est ajouté dans `docs/PROMPTS.md`, verbatim, avant ou pendant sa mise en œuvre.
2. **Chaque décision structurante** devient un ADR dans `docs/DECISIONS.md` (avec les alternatives écartées).
3. **Chaque vérification exécutée** est consignée dans `docs/RECETTE.md` avec le résultat observé.
4. **Ce fichier** est mis à jour si l'état, les règles ou les pièges changent.

---

## 10. Périmètre et limites assumées

- Le sujet **exclut** l'interface graphique, le paiement, l'e-mail et l'inscription. Le frontend
  Thymeleaf est une **extension non notée** : ne jamais le présenter comme une exigence du sujet.
- Comptes en mémoire : adaptés à l'épreuve, pas à une production multi-instances.
- Bonus retenu : **Micrometer uniquement**. ETag et idempotence ne sont pas implémentés — ne pas
  prétendre le contraire.
- Pas de pagination sur les listes.

---

## 11. Prochaines étapes possibles

**A01–A08 corrigés et vérifiés.** Refonte visuelle, fiches film et import externe livrés.
La configuration d'un jeton personnel TMDB reste une action locale du porteur du projet.

Étape réalisée le 2026-10-21 : **tests d'interface Thymeleaf**, avec `UiMvcTest` (MockMvc,
`@WithMockUser`, vrais templates et sécurité). Les consignes sont centralisées dans ce fichier.

Pistes restantes, non commencées ; les bonus ETag et idempotence sont des options à réévaluer
au regard du choix Micrometer déjà retenu (ADR-010) :

1. **ETag / contrôle de version HTTP** sur une ressource modifiable (bonus).
2. **Idempotence de création de réservation** avec clé fournie par le client (bonus).
3. **Pagination et filtrage avancé** du catalogue (`Pageable`).
4. **Externalisation des comptes** vers une table `utilisateurs` (production).
