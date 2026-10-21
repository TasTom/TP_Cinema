# CinéBil — Billetterie de cinéma

Application de billetterie : API REST Spring Boot, persistance MySQL, migrations Flyway,
sécurité HTTP Basic, vérification de capacité sous concurrence, et interface Thymeleaf avec affiches, fiches film et navigation adaptée au mobile.

Projet réalisé dans le cadre de l'épreuve **R5.08 Qualité du développement — BUT Informatique 3e année**
(sujet `Examen_TP_Billetterie_Cinema_Java25.pdf`).

L'interface Thymeleaf est une **extension hors barème**. Versions du projet : **Java 25**,
**Spring Boot 4.1.1**, **Maven 3.9.16**, **MySQL 9.7** et **springdoc 3.1.1**.

**État de validation** : `clean verify` vert, **117 tests, aucun échec ni test ignoré**.
Les constats A01–A08 de l'[audit du 21 octobre](docs/AUDIT_2026-10-21.md) sont corrigés et
les sondes intégrées aux régressions. Chrome a vérifié le rendu bureau/mobile et une réservation
réelle suivie de son annulation. Les limites de production restent décrites dans l'audit.

![Accueil CinéBil](docs/visuels/accueil.png)

---

## 1. Prérequis

| Outil | Version attendue | Vérification |
|---|---|---|
| JDK | **25 LTS** | `& "$env:JAVA_HOME\bin\java.exe" -version` |
| Maven | fourni par le wrapper **3.9.16** | `.\mvnw.cmd -version` |
| Docker | Moteur accessible avec le plugin Compose | `docker info`, `docker compose version` |

> **Important — JDK** : le projet compile et s'exécute en Java 25. Définissez `JAVA_HOME` vers
> votre JDK 25 avant les commandes suivantes, même si le `java` du `PATH` est déjà en version 25 :
>
> ```powershell
> $env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1" # adapter à votre installation
> & "$env:JAVA_HOME\bin\java.exe" -version
> ```
>
> `.\mvnw.cmd -version` doit alors afficher `Java version: 25.x`.
> Maven, la compilation, les tests et `java -jar` doivent tous utiliser ce même JDK 25.
> Définir `JAVA_HOME` ne change pas le `java` du `PATH` : la commande de lancement ci-dessous
> utilise donc explicitement le binaire de `JAVA_HOME`.

Aucune installation de Maven n'est nécessaire : le wrapper télécharge la version 3.9.16 au premier
appel.

---

## 2. Démarrer MySQL

```powershell
docker compose up -d --wait --wait-timeout 120
docker compose ps          # le service mysql doit être healthy
```

Le service Compose `mysql` (conteneur `cinema-mysql`) expose MySQL 9.7 sur `localhost:3306` avec :

| Paramètre | Valeur par défaut |
|---|---|
| Base | `cinema` |
| Utilisateur | `cinema` |
| Mot de passe | `cinema` |

Ces valeurs sont **de développement**. Les variables se répartissent entre deux configurations :

| Cible | Variables |
|---|---|
| Application Spring Boot | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`, `SERVER_PORT`, `THYMELEAF_CACHE` |
| Conteneur MySQL | `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MYSQL_PORT` |

Les paramètres `DB_*` doivent correspondre à ceux du conteneur. Modifier `MYSQL_PORT` ne modifie
pas automatiquement `DB_URL`. Le contrôle de santé Compose utilise `MYSQL_USER`, `MYSQL_PASSWORD` et `MYSQL_DATABASE`
pour exécuter un `SELECT 1` authentifié ; il suit donc leur configuration.

Pour arrêter MySQL tout en conservant ses données : `docker compose down`.

Pour repartir d'une base vide (**supprime toutes les données du volume du projet**) :

```powershell
docker compose down -v
docker compose up -d --wait --wait-timeout 120
```

Au démarrage de l'application, Flyway applique `V1__schema_initial.sql` puis
`V2__donnees_demonstration.sql`, puis V3 (précision du montant) et V4 (métadonnées des films).
Hibernate valide ensuite le schéma (`ddl-auto=validate`).
Aucune création automatique de table n'est effectuée par Hibernate.

---

## 3. Construire et vérifier

```powershell
.\mvnw.cmd clean verify
```

Cette commande unique :

1. compile en `release 25` ;
2. exécute tous les tests (unitaires, MVC, sécurité, JPA, intégration MySQL, concurrence) ;
3. produit `target/cinema-billetterie-1.0.0.jar`.

Les tests MySQL utilisent **Testcontainers** : si Docker n'est pas disponible, ces classes sont
**ignorées** (`@Testcontainers(disabledWithoutDocker = true)`) et les autres tests s'exécutent
normalement. Un build vert avec des tests ignorés ne valide pas les migrations ni la concurrence.
Rendre Docker accessible, puis relancer `clean verify` pour obtenir la validation complète.
Les conteneurs de test sont distincts de la base de développement lancée par Compose.

---

## 4. Lancer l'application

### Avec Maven

```powershell
.\mvnw.cmd spring-boot:run
```

### Avec le JAR (vérification hors IDE)

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target\cinema-billetterie-1.0.0.jar
```

Utiliser le JAR produit par `clean verify` à l'étape 3, avec MySQL démarré. Choisir un seul mode
de lancement. `Ctrl+C` arrête l'application lancée au premier plan ; l'arrêter avant de relancer
une instance sur le même port ou de reconstruire le JAR sous Windows.

L'application écoute sur <http://localhost:8080> (`SERVER_PORT` pour changer).

| Ressource | URL |
|---|---|
| Accueil | <http://localhost:8080/> |
| Catalogue | <http://localhost:8080/films> |
| Planning | <http://localhost:8080/seances> |
| Connexion | <http://localhost:8080/login> |
| Administration | <http://localhost:8080/admin> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Santé | <http://localhost:8080/actuator/health> |

---

## 5. Comptes de démonstration

Authentification **HTTP Basic** pour l'API, formulaire de connexion pour l'interface.
Les comptes en mémoire sont configurés avec des **hachages BCrypt** dans `SecurityConfig`.
Leurs mots de passe fictifs sont volontairement affichés ci-dessous et sur la page de connexion
pour la démonstration.

| Compte | Mot de passe | Rôles | Droits |
|---|---|---|---|
| `client` | `client123` | `CLIENT` | Créer et annuler une réservation |
| `programmateur` | `prog123` | `PROGRAMMATEUR` | Créer des films et des séances |
| `admin` | `admin123` | `ADMIN` | Créer des salles et accéder à toutes les opérations protégées |

Ces comptes sont **fictifs** et destinés à la démonstration locale. Toute mise en production
imposerait un `UserDetailsService` adossé à une base et des secrets externalisés.

---

## 6. Endpoints

### Publics

| Méthode | Chemin | Résultat |
|---|---|---|
| GET | `/api/films` | 200, liste filtrable par `titre` et `genre` |
| GET | `/api/films/{id}` | 200 ou 404 |
| GET | `/api/seances` | 200, filtres optionnels `du` et `au` au format `yyyy-MM-dd`, bornes incluses ; 400 si invalides |
| GET | `/api/seances/{id}` | 200 ou 404 |
| GET | `/api/salles` | 200, liste des salles |
| GET | `/api/salles/{id}` | 200 ou 404 |
| GET | `/actuator/health` | 200 `{"status":"UP"}` lorsque l'application et sa base sont disponibles |
| GET | `/v3/api-docs`, `/swagger-ui.html` | Documentation OpenAPI |

### Protégés

| Méthode | Chemin | Rôles | Résultat |
|---|---|---|---|
| POST | `/api/programmation/films` | `PROGRAMMATEUR`, `ADMIN` | 201 + `Location` |
| POST | `/api/programmation/seances` | `PROGRAMMATEUR`, `ADMIN` | 201 ou 400 |
| POST | `/api/admin/salles` | `ADMIN` | 201, 400 ou 409 |
| POST | `/api/reservations` | `CLIENT`, `ADMIN` | 201, 400, 404 ou 409 |
| DELETE | `/api/reservations/{code}` | `CLIENT`, `ADMIN` | 204 ou 404 |

Ces routes renvoient aussi **401** sans authentification valide et **403** si le rôle est insuffisant.

Exemple de réservation (crée réellement 3 places si la séance 1 existe et dispose de la capacité) :

```powershell
New-Item -ItemType Directory -Path .tools -Force | Out-Null
@'
{"seanceId":1,"emailClient":"lea@example.test","nombrePlaces":3}
'@ | Set-Content -Encoding Ascii .tools\reservation-exemple.json

curl.exe -u client:client123 -H "Content-Type: application/json" `
  --data-binary "@.tools/reservation-exemple.json" `
  http://localhost:8080/api/reservations
```

Le fichier évite les problèmes de guillemets JSON sous PowerShell. L'exemple contient uniquement
des caractères ASCII ; pour un autre corps contenant des accents, utiliser un fichier UTF-8 sans BOM.

Contrat détaillé, catalogue d'erreurs et exemples : [`docs/API.md`](docs/API.md).

---

## 7. Décision de concurrence

**Stratégie retenue : verrou pessimiste en base (`SELECT … FOR UPDATE`) dans une transaction.**

- `ReservationService#creer` est `@Transactional`.
- `SeanceRepository#findByIdForUpdate` porte `@Lock(LockModeType.PESSIMISTIC_WRITE)` : la ligne de
  la séance est verrouillée pour la durée de la transaction.
- La somme des places `CONFIRMEE` est lue **après** obtention du verrou, dans la même transaction.
- Si les places restantes sont insuffisantes : `CapaciteInsuffisanteException` → HTTP **409**,
  transaction annulée, **aucune écriture partielle**.
- L'annulation prend le même verrou avant de changer le statut.

### Pourquoi ce choix

Le sujet impose une stratégie fondée sur la base et refuse une synchronisation mémoire. Un
`synchronized` ne protège qu'une JVM : deux instances applicatives le contourneraient. Le verrou
pessimiste sérialise les opérations **d'une même séance** tout en laissant les autres séances
indépendantes. Le verrou optimiste (`@Version`) a été écarté : il exige une boucle de reprise dans
tous les appelants et rend la sémantique du 409 moins directe.

### Preuve automatisée

`ReservationConcurrencyTest` : capacité 20, 15 places déjà confirmées, deux requêtes simultanées de
4 places, lancées derrière une `CyclicBarrier` sur deux threads.

Résultat attendu et vérifié :

- statuts exactement `{201, 409}` ;
- total des places confirmées = **19** (jamais 23) ;
- aucune réservation partielle en base.

Détail complet : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §4,
[`docs/DECISIONS.md`](docs/DECISIONS.md) ADR-004.

---

## 8. Tests

Dernière validation complète le **2026-10-21** : **`clean verify` — BUILD SUCCESS,
117 tests, 0 échec, 0 erreur, 0 ignoré**. MySQL et concurrence réellement exécutés.
Preuves, lancement du JAR et recette Chrome : [`docs/RECETTE.md`](docs/RECETTE.md).

| Classe | Type | Couverture |
|---|---|---|
| `ReservationServiceTest` | unitaire (Mockito) | montant, capacité insuffisante, annulation idempotente, validation, métriques |
| `ReservationControllerMvcTest` | MVC | 201, 400 (0 et 9 places), 401, 403, 409, 204 |
| `ApiSecurityMvcTest` | MVC + sécurité | lecture publique, 401, 403, 201 `ADMIN`, 201 `PROGRAMMATEUR`, 409 doublon |
| `UiMvcTest` | MVC + Thymeleaf + sécurité | 39 cas : pages publiques et administration, réservation et confirmation, validation, erreurs HTML, rôles, CSRF |
| `ReservationRepositoryTest` | JPA + MySQL | somme des places confirmées, libération après annulation |
| `CinemaIntegrationTest` | intégration MySQL | migrations, réservation réelle, sécurité, filtres, OpenAPI, health |
| `ReservationConcurrencyTest` | concurrence | deux réservations simultanées sans surréservation |
| `AuditCasLimitesTest`, `AuditSecuriteMvcTest` | MySQL + sécurité | 28 régressions : argent, dates, verrous, transactions, erreurs et sécurité |
| `CatalogueExterneServiceTest`, `AffichesServiceTest` | HTTP simulé | recherche TMDB, Ghibli, cache, panne, tailles, formats et URL autorisées |
| `ImportFilmIntegrationTest` | MySQL | enrichissement sans doublon ni séance inventée, durée invalide |
| `ImportFilmServiceTest` | unitaire | collision d'import : rollback et erreur métier sans détail SQL |

```powershell
.\mvnw.cmd test                                  # tous les tests
.\mvnw.cmd test '-Dtest=ReservationServiceTest'   # une classe
```

---

## 9. Choix techniques

| Sujet | Décision |
|---|---|
| Architecture | paquets par fonctionnalité (`film`, `salle`, `seance`, `reservation`) |
| DTO | entrées et sorties API en `record` immuables ; aucune entité JPA sérialisée par les contrôleurs REST |
| Processeurs d'annotations | aucun (ni Lombok ni MapStruct) → aucun risque avec Java 25 |
| Schéma | 100 % Flyway, `ddl-auto=validate` |
| Erreurs | `@RestControllerAdvice` + codes stables, sans trace de pile ni SQL |
| Sécurité | deux chaînes de filtres : API stateless JSON, interface à session |
| Métriques | Micrometer : `cinema.reservations`, `cinema.places` (bonus) |
| Frontend | extension hors barème, isolée dans `ui`, aucune règle métier dupliquée |

Documentation complémentaire :

- [`AGENTS.md`](AGENTS.md) — **mémoire du projet** : règles, conventions, pièges, état, prochaines étapes
- [`.github/copilot-instructions.md`](.github/copilot-instructions.md) — point d'entrée Copilot vers `AGENTS.md`, sans duplication des règles
- [`docs/ANALYSE_SPEC.md`](docs/ANALYSE_SPEC.md) — analyse du sujet et traçabilité A1–A10
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — couches, flux, diagramme de concurrence
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — décisions d'architecture (ADR)
- [`docs/API.md`](docs/API.md) — contrat HTTP et catalogue d'erreurs
- [`docs/RECETTE.md`](docs/RECETTE.md) — procès-verbal des contrôles A1–A10 exécutés
- [`docs/AUDIT_2026-10-21.md`](docs/AUDIT_2026-10-21.md) — audit approfondi, écarts et preuves de reproduction
- [`docs/PROMPTS.md`](docs/PROMPTS.md) — journal des prompts et des décisions

---

## 10. Limites connues

- **Docker requis** pour les tests MySQL. Sans Docker, ces classes sont ignorées et signalées
  comme telles par surefire ; les autres tests restent verts.
- **MySQL 9.7** est plus récent que la dernière version vérifiée par Flyway (9.4) : un avertissement
  `Using MySQL 9.7 which is newer than the version Flyway has been verified with` apparaît au
  démarrage. Il est informatif : les migrations s'appliquent et `ddl-auto=validate` passe.
- **Comptes en mémoire** : adaptés à l'épreuve, pas à une production multi-instances.
- **Interface Thymeleaf** : hors périmètre du sujet, donc non notée ; parcours serveur couverts
  par 39 tests MVC avec rendu Thymeleaf et une recette Chrome/Playwright distincte du build Maven.
- **Pas de pagination** sur les listes : volumes de démonstration.


## 11. Films externes et affiches

Connectez-vous avec un compte programmateur ou administrateur, puis ouvrez
**Administration → Films → Importer un film et ses affiches** (`/admin/catalogue`).
La collection Ghibli fonctionne sans clé ; la recherche utilise les titres français connus.
L'import enrichit aussi un film existant de même titre et ne crée aucune séance.
Les synopsis Ghibli sont fournis en anglais. Une année seule reste une année, sans date inventée.

Pour un catalogue généraliste, sélectionnez **TMDB** après avoir configuré votre
[jeton de lecture API TMDB](https://developer.themoviedb.org/docs/getting-started).
Définissez-le dans le terminal qui lance l'application, puis redémarrez le JAR :

```powershell
$env:TMDB_API_TOKEN = "votre-jeton-de-lecture" # valeur personnelle, ne pas versionner
& "$env:JAVA_HOME\bin\java.exe" -jar target\cinema-billetterie-1.0.0.jar
```

Le jeton reste côté serveur. Sans configuration, TMDB affiche une explication et Ghibli reste
utilisable. Recherche et détail TMDB sont testés avec des réponses simulées ; aucun jeton réel
n'a été fourni pour une recette TMDB en direct. L'import Ghibli et ses téléchargements ont été
vérifiés en direct sur six films dans la base locale (ces imports ne sont pas des données Flyway).

Sous Windows, si l'application tourne déjà, utilisez **le même terminal** que celui où vous avez
défini le jeton, puis exécutez `./scripts/lancer.ps1`. Le script contrôle Java 25, démarre MySQL,
vérifie le programme qui occupe le port, relance le JAR du projet et contrôle sa santé.
Il transmet l'environnement sans afficher ni écrire le jeton. `./scripts/lancer.ps1 -Verifier`
effectue uniquement les contrôles préalables. Une variable `$env:...` définie dans un autre
terminal ne modifie pas l'environnement d'un processus déjà lancé.

Les images sont conservées dans `data/affiches/` (ou `CINEMA_MEDIA_DIRECTORY`). Ce répertoire
doit être accessible en écriture, conservé entre lancements et sauvegardé avec la base.
Il est ignoré par Git. Une panne du fournisseur ne bloque pas les films déjà importés.
Styles, scripts, fontes système et logo d'attribution sont locaux. Les aperçus de l'administration
utilisent le CDN d'images autorisé ; les pages publiques utilisent les fichiers importés.

Les appels distants sont bornés : connexion 4 s, lecture 8 s, JSON 2 Mo, image 5 Mo, pas de
redirection et formats JPEG/PNG/WebP seulement. L'attribution TMDB figure en pied de page.
Pour un usage commercial, vérifier les [conditions TMDB](https://developer.themoviedb.org/docs/faq).

## 12. Recette visuelle optionnelle

Chrome et le paquet Python `playwright` sont nécessaires uniquement à cette recette, pas au JAR.
Le script lit ses identifiants depuis l'environnement, vérifie les pages, le menu mobile, les
images et les largeurs 1440, 390 et 320 px ; captures et JSON sont écrits dans `.tools/visuels/`.

```powershell
$env:CINEMA_QA_USERNAME = "admin"
$env:CINEMA_QA_PASSWORD = "admin123" # compte de démonstration local
python scripts/recette_ui.py
```

Le mode par défaut ne modifie pas les données métier. Options explicites : `--importer` pour
les six films Ghibli et `--reserver-et-annuler` pour créer puis annuler sa réservation de recette.
`CINEMA_QA_URL` permet de changer l'URL cible (défaut `http://localhost:8080`).
Ne viser qu'une instance de test autorisée. [Charte et choix de design](docs/DESIGN.md).
