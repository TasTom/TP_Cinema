# Procès-verbal de recette

Ce document consigne les contrôles **réellement exécutés**, avec leurs résultats observés.
Il complète la matrice théorique de [`ANALYSE_SPEC.md`](./ANALYSE_SPEC.md) §5.

Environnement d'exécution :

| Élément | Valeur observée |
|---|---|
| Date | 2026-10-21 |
| Système | Windows 11 |
| JDK de build et d'exécution | Microsoft OpenJDK **25.0.4.1 LTS** |
| Maven (wrapper) | **3.9.16** |
| Spring Boot | **4.1.1** (Spring Framework 7.0.9, Security 7.1.1, Hibernate 7.4.5.Final) |
| MySQL | **9.7.2** (conteneur Docker `mysql:9.7`) |
| Docker | 29.8.2, Compose v5.5.1 |

---

## A1 — Environnement Maven et Java 25

```powershell
$env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1"
.\mvnw.cmd -version
```

```text
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Java version: 25.0.4.1, vendor: Microsoft, runtime: C:\Users\tomta\.jdks\ms-25.0.4.1
```

```powershell
.\mvnw.cmd clean verify
```

Résultat : **BUILD SUCCESS**, 30 tests exécutés, 0 échec, 0 erreur, 0 test ignoré,
JAR `target/cinema-billetterie-1.0.0.jar` produit.

Détail des tests :

| Classe | Tests | Résultat |
|---|---|---|
| `CinemaIntegrationTest` | 6 | 0 échec |
| `ReservationConcurrencyTest` | 1 | 0 échec |
| `ReservationControllerMvcTest` | 8 | 0 échec |
| `ReservationRepositoryTest` | 1 | 0 échec |
| `ReservationServiceTest` | 5 | 0 échec |
| `ApiSecurityMvcTest` | 9 | 0 échec |

---

## A2 — Base neuve, Flyway et validation Hibernate

Base supprimée puis recréée vide :

```powershell
docker exec cinema-mysql mysql -uroot -proot -e `
  "DROP DATABASE cinema; CREATE DATABASE cinema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Démarrage du JAR packagé :

```powershell
java -jar target\cinema-billetterie-1.0.0.jar
```

Extraits de journal observés :

```text
FlywayExecutor   : Database: jdbc:mysql://localhost:3306/cinema?...&connectionTimeZone=LOCAL (MySQL 9.7)
DbValidate       : Successfully validated 2 migrations
DbMigrate        : Migrating schema `cinema` to version "1 - schema initial"
DbMigrate        : Migrating schema `cinema` to version "2 - donnees demonstration"
DbMigrate        : Successfully applied 2 migrations to schema `cinema`, now at version v2
Hibernate ORM core version 7.4.5.Final
Started CinemaApplication in 16.964 seconds
```

Hibernate démarre sans erreur de validation : `ddl-auto=validate` est satisfait par le schéma Flyway.
Avertissement informatif présent : Flyway a été validé jusqu'à MySQL 9.4 ; l'application fonctionne
en 9.7 et cela est documenté dans le README.

---

## A3 — Lecture publique

```text
GET /actuator/health  → 200
GET /api/films        → 200
[{"id":2,"titre":"Inception","genre":"Science-fiction","dureeMinutes":148,"dateSortie":"2010-07-16"}, ...]
GET /api/seances      → 200
[{"id":1,"filmId":1,"filmTitre":"Le Voyage de Chihiro","salleId":1,"salleNom":"Salle Lumiere",
  "debut":"2026-11-05T20:00:00","prixUnitaire":9.90,"capaciteSalle":120,"placesRestantes":120}, ...]
```

Aucune donnée technique exposée : ni identifiant de version, ni relation paresseuse, ni champ interne.

---

## A4 — Validation des places

| Corps envoyé | Statut | Corps de réponse |
|---|---|---|
| `nombrePlaces: 0` | **400** | `{"status":400,"code":"VALIDATION_ECHOUEE",...,"details":[{"champ":"nombrePlaces","message":"Le nombre de places doit être compris entre 1 et 8."}]}` |
| `nombrePlaces: 9` | **400** | idem |

Le corps d'erreur est stable et ne contient ni trace de pile ni détail SQL.

---

## A5 — Sécurité et rôles

Création d'une salle (`POST /api/admin/salles`) :

| Appelant | Statut observé |
|---|---|
| anonyme | **401** |
| `client` / `client123` | **403** |
| `admin` / `admin123` | **201** |

Complété par `ApiSecurityMvcTest` : `PROGRAMMATEUR` obtient 201 sur
`POST /api/programmation/films`, `CLIENT` obtient 403, anonyme 401.

Le corps 401 est `{"code":"NON_AUTHENTIFIE"}` et le corps 403 `{"code":"ACCES_REFUSE"}`,
tous deux en JSON, avec `WWW-Authenticate` sur le 401.

---

## A6 — Réservation valide

```powershell
curl.exe -u client:client123 -H "Content-Type: application/json" `
  --data-binary "@resa.json" http://localhost:8080/api/reservations
```

```json
{
  "id": 1,
  "code": "RES-77BDCD7AAD",
  "seanceId": 1,
  "filmTitre": "Le Voyage de Chihiro",
  "salleNom": "Salle Lumiere",
  "debut": "2026-11-05T20:00:00",
  "emailClient": "lea@example.test",
  "nombrePlaces": 3,
  "statut": "CONFIRMEE",
  "montantTotal": 29.70,
  "creeeLe": "2026-10-21T13:22:39.527622"
}
```

`201 Created`, montant calculé côté serveur (`9.90 × 3 = 29.70`).

---

## A7 — Capacité insuffisante

Vérifié par :

- `ReservationServiceTest#refuseUneReservationAuDelaDeLaCapacite` : message
  « Il ne reste que 2 places pour cette séance. », `saveAndFlush` jamais appelé, métrique
  `refusee` incrémentée ;
- `CinemaIntegrationTest#valideLesPlacesEtRefuseLaSurreservation` : après 2 × 8 places sur une salle
  de 20, une demande de 5 renvoie **409** `CAPACITE_INSUFFISANTE` et la somme confirmée reste 16.

---

## A8 — Concurrence

`ReservationConcurrencyTest`, exécuté dans `clean verify` contre MySQL 9.7 réel :

- salle de 20 places, 15 places déjà confirmées (8 + 7) ;
- deux requêtes simultanées de 4 places, synchronisées par `CyclicBarrier` sur 2 threads ;
- statuts observés : exactement **{201, 409}** ;
- total confirmé après coup : **19** (jamais 23) ;
- dernier refus observé côté serveur : `Il ne reste qu'une place pour cette séance.`
  — cohérent avec 20 − 19 = 1.

---

## A9 — Annulation et libération des places

```text
DELETE /api/reservations/RES-77BDCD7AAD  → 204
GET    /api/seances/1                    → "placesRestantes": 120   (retour à la capacité initiale)
```

Complété par `ReservationServiceTest#annuleUneReservationEtLibereLesPlaces` : double annulation
→ une seule métrique `annulee`, statut `ANNULEE` stable (idempotence).

---

## A10 — Exploitation

| Ressource | Résultat |
|---|---|
| `GET /v3/api-docs` | **200** |
| `GET /swagger-ui.html` | 302 vers l'index Swagger (comportement attendu) |
| `GET /swagger-ui/index.html` | **200** |
| `GET /actuator/health` | **200** |
| `GET /actuator/env` | non exposé (seul `health` est publié) |

---

## Vérifications complémentaires de l'interface (hors barème)

Parcours exécutés sur l'application lancée :

| Parcours | Résultat |
|---|---|
| `GET /`, `/films`, `/seances`, `/login` anonymes | 200 |
| Connexion `client` puis `POST /reservation` | 302 vers `/reservation/confirmation`, code affiché, `19,80 €` pour 2 places |
| `nombrePlaces = 9` via formulaire | 200 avec message « Le nombre de places doit être compris entre 1 et 8. » |
| `client` → `/admin` | 403 |
| `admin` → `/admin`, `/admin/films`, `/admin/seances`, `/admin/salles`, `/admin/reservations` | 200 |
| `programmateur` → `/admin/salles`, `/admin/reservations` | 403 |
| `admin` crée un film, une salle et une séance via l'interface | succès, listes mises à jour |
| Affichage des séances | `05/11/2026`, `20:00`, `9,90 €` (formats français corrects) |

---

## Synthèse

| ID | Contrôle | Statut |
|---|---|---|
| A1 | Environnement Maven / Java 25 | ✅ vérifié |
| A2 | Base neuve + Flyway + validation | ✅ vérifié |
| A3 | Lecture publique | ✅ vérifié |
| A4 | Validation 0 / 9 places | ✅ vérifié |
| A5 | Sécurité 401 / 403 / 201 | ✅ vérifié |
| A6 | Réservation 201 avec montant | ✅ vérifié |
| A7 | Capacité 409 sans écriture partielle | ✅ vérifié |
| A8 | Concurrence sans surréservation | ✅ vérifié |
| A9 | Annulation 204 et places libérées | ✅ vérifié |
| A10 | Health et OpenAPI | ✅ vérifié |

---

## Reprise du 2026-10-21 — Fusion des consignes et tests Thymeleaf

### Contrôles préalables et blocages résolus

| Contrôle exécuté | Résultat observé |
|---|---|
| Lecture et comparaison des deux fichiers de consignes | Règles largement dupliquées ; critère figé à 30 tests et ancien trailer Copilot dans la copie |
| Inventaire et diff Git | Modification locale préexistante de `AGENTS.md` : mise en forme et suppression du trailer ; préservée |
| `git status` dans le bac à sable | Refus initial `dubious ownership` ; commandes suivantes avec `git -c safe.directory=C:/Users/tomta/Desktop/Projet_git/TP_Cinema …`, sans modifier la configuration globale |
| `.\mvnw.cmd -version` avec `JAVA_HOME` défini | Échec initial `Indexation impossible dans un tableau Null`, avant le lancement de Maven |
| Même commande après correction de `mvnw.cmd` | Le blocage `Target[0]` disparaît ; accès au binaire refusé dans le bac à sable |
| `.\mvnw.cmd -version` avec accès aux outils locaux | Maven **3.9.16**, Microsoft OpenJDK **25.0.4.1**, Windows 11 |
| `docker version --format '{{.Server.Version}}'` | Accès au moteur refusé dans le bac à sable, puis **29.8.2** avec accès aux outils locaux |

Les contrôles suivants ont été exécutés avec cet accès aux outils locaux et
`JAVA_HOME=C:\Users\tomta\.jdks\ms-25.0.4.1`.

### Tests MVC de l'interface

Commande : `.\mvnw.cmd test -Dtest=UiMvcTest`.

- Premier passage, terminé à 15:37:59 : **33 cas, 3 échecs, 1 erreur**. Deux échecs concernaient
  une attente incorrecte du test sur la redirection relative `/login`, le troisième une valeur
  HTML contenant une entité de caractère. Ces attentes de test ont été corrigées.
- L'erreur révélait un défaut applicatif : une séance introuvable conduisait à une
  `TemplateProcessingException` sur `estAdmin or estProgrammateur`. Le modèle reconstruit
  par le gestionnaire d'exceptions ne contenait plus les attributs communs de navigation.
- Correction : mutualisation de leur initialisation dans `GlobalModelAttributes#ajouterAuModele`
  et appel depuis les deux méthodes de `UiExceptionHandler`.
- Second passage, terminé à **15:39:18** : **34 cas, 0 échec, 0 erreur, 0 ignoré**. Un cas
  supplémentaire vérifie une erreur de paramètre pour un visiteur anonyme.

Couverture exercée avec les vrais templates et la configuration de sécurité :

| Parcours | Résultat |
|---|---|
| Accueil, films, séances et connexion sans authentification | 200, HTML rendu |
| Filtrage des films | Critères transmis au service, film affiché |
| Réservation et administration sans connexion | Redirection vers `/login`, aucun service appelé |
| Formulaire client | E-mail prérempli, jeton CSRF non vide, date et prix français |
| Réservation valide puis confirmation | Redirection, résultat transmis, code et montant affichés |
| Confirmation sans réservation | Redirection vers `/` |
| Réservation de 0 ou 9 places | Erreur du formulaire affichée, aucune création |
| Capacité insuffisante | Erreur métier affichée dans le formulaire |
| Séance supprimée au moment de réserver | Redirection vers `/seances` avec message |
| Séance introuvable pour un client | Page HTML 404, utilisateur et rôles conservés |
| Paramètre de séance invalide sans connexion | Page HTML 400, navigation anonyme rendue |
| Formulaires de réservation, films, séances et salles sans CSRF | 403, aucun service appelé |
| Pages administratives avec le rôle CLIENT | 403, aucun service appelé |
| Salles et réservations avec le rôle PROGRAMMATEUR | 403, aucun service appelé |
| Création de film avec le rôle PROGRAMMATEUR et CSRF valide | Commande transmise, redirection |
| Cinq pages administratives avec le rôle ADMIN | 200, HTML rendu avec données non vides |
| Nom de salle déjà utilisé | Erreur métier affichée, valeurs du formulaire conservées |

### Build complet de référence

```powershell
$env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1"
.\mvnw.cmd clean verify
```

Terminé le **2026-10-21 à 15:40:44 (Europe/Paris)** en **1 min 09 s** :
**BUILD SUCCESS — 64 tests, 0 échec, 0 erreur, 0 ignoré**.

| Classe | Tests | Échecs | Erreurs | Ignorés |
|---|---|---|---|---|
| `CinemaIntegrationTest` | 6 | 0 | 0 | 0 |
| `ApiSecurityMvcTest` | 9 | 0 | 0 | 0 |
| `ReservationControllerMvcTest` | 8 | 0 | 0 | 0 |
| `ReservationServiceTest` | 5 | 0 | 0 | 0 |
| `ReservationRepositoryTest` | 1 | 0 | 0 | 0 |
| `ReservationConcurrencyTest` | 1 | 0 | 0 | 0 |
| `UiMvcTest` | 34 | 0 | 0 | 0 |

Résultats confirmés par les rapports XML Surefire. Les trois suites utilisant MySQL ont démarré
leurs conteneurs `mysql:9.7` ; le test de concurrence a réellement été exécuté. L'avertissement
Flyway/MySQL déjà documenté reste présent. JAR Spring Boot produit :
`target/cinema-billetterie-1.0.0.jar`.

`git diff --check` : aucune erreur d'espacement observée lors de la relecture.
Contrôle des liens Markdown locaux dans `AGENTS.md`, le point d'entrée Copilot, le README
et les trois journaux modifiés : **42 liens contrôlés, 0 cible absente**.

Limites : les nouveaux tests sont des tests serveur, sans navigateur réel. Le JAR a été construit
et reconditionné ; aucun nouveau lancement manuel hors IDE n'a été effectué lors de cette reprise.
La résolution d'un cache Maven sous forme de lien n'a pas été exercée.

---

## Lancement hors IDE du 2026-10-21

Demande : « lance le projet ».

- JAR existant contrôlé : `target/cinema-billetterie-1.0.0.jar`, produit par le build de 15:40:44.
- `docker compose up -d --wait --wait-timeout 120` : conteneur `cinema-mysql` déjà en cours
  d'exécution, état **healthy**, image `mysql:9.7`, port local **3306**.
- Contrôle du port **8080** : aucune écoute avant le lancement. La commande PowerShell de
  recherche d'une écoute inexistante termine avec le code 1, sans erreur Docker.
- JAR lancé avec `C:\Users\tomta\.jdks\ms-25.0.4.1\bin\java.exe -jar
  target\cinema-billetterie-1.0.0.jar`, en arrière-plan, fenêtre masquée, depuis la racine du dépôt.
- PID au lancement : **24868**, conservé dans `.tools/cinema.pid` (ignoré par Git).
- Journaux : `logs/cinema.stdout.log` et `logs/cinema.stderr.log` (ignorés par Git).

Contrôles HTTP locaux observés le **2026-10-21 à 15:50:50 (Europe/Paris)** :

| URL | Résultat |
|---|---|
| `http://localhost:8080/actuator/health` | **200**, `status=UP` |
| `http://localhost:8080/` | **200**, titre `Accueil · CinéBil` |
| `http://localhost:8080/login` | **200** |
| `http://localhost:8080/api/films` | **200**, lecture MySQL effective visible dans le journal |

Application laissée en fonctionnement sur **http://localhost:8080/**. Aucun nouveau build ni
test Maven : le JAR précédemment vérifié est réutilisé sans modification du code.

---

## Audit du README du 2026-10-21

Demande : « le readme est à jour, et correct ? ».

Comparaison effectuée avec `pom.xml`, les propriétés du wrapper, `docker-compose.yml`,
`application.yml`, les contrôleurs REST, `SecurityConfig`, les services et le repository de séance,
les métriques, le test de concurrence, les rapports Surefire et les précédents contrôles de lancement.

Corrections documentaires appliquées :

- versions du projet et nature hors barème du frontend explicitées ;
- commande du JAR utilisant le binaire de `JAVA_HOME` : cette variable seule ne modifie pas le `PATH` ;
- démarrage Compose avec attente de santé et distinction entre service `mysql` et conteneur `cinema-mysql` ;
- distinction entre variables Spring Boot et variables du conteneur ; contrôle de santé actuellement
  lié aux identifiants par défaut signalé ;
- arrêt avec conservation du volume distingué de sa suppression volontaire ;
- tests MySQL ignorés sans Docker distingués d'une validation complète, sans proposer Compose
  comme solution tant que Docker demeure inaccessible ;
- JAR issu du build de référence réutilisé, sans reconstruction redondante ;
- précision sur BCrypt dans la configuration des comptes et publication volontaire des identifiants
  fictifs dans le README et le template de connexion ;
- lecture d'une salle documentée en 200 ou 404 ; filtres de date optionnels et erreurs d'accès précisés ;
- exemple PowerShell JSON par fichier remplaçant le corps en ligne fragile ;
- résultat daté des 64 tests et dernier lancement hors IDE ajoutés ; DTO décrits pour la frontière REST.

Contrôles observés le **2026-10-21 à 16:01:05 (Europe/Paris)** :

| Contrôle | Résultat |
|---|---|
| Analyse des blocs PowerShell avec le parseur PowerShell | **8 blocs, 0 erreur de syntaxe** |
| Désérialisation du JSON de l'exemple | Valide, séance 1, 3 places, e-mail attendu |
| Cibles des liens Markdown locaux du README | **12 liens, 0 cible absente** |
| `git diff --check` | Aucune erreur d'espacement |
| Version Compose exécutée | **5.5.1** |
| Rapports XML Surefire relus | **64 tests, 0 échec, 0 erreur, 0 ignoré**, build de 15:40:44 |

GET via `curl.exe -L` sur les dix URL suivantes : **HTTP 200** pour chacune :
`/`, `/films`, `/seances`, `/login`, `/swagger-ui.html`, `/v3/api-docs`, `/actuator/health`,
`/api/films`, `/api/seances?du=2026-11-01&au=2026-11-30`, `/api/salles`.
La vérification Swagger suit la redirection de sa page d'entrée.

Le format de l'exemple par fichier a été exercé avec `curl.exe --data-binary` et les identifiants
du client de démonstration, en remplaçant volontairement le nombre de places par **0** :
réponse **400 `VALIDATION_ECHOUEE`**, aucune création. Le scénario de réservation valide à
3 places du README n'a pas été rejoué sur les données de développement.

Modifications limitées à la documentation. Aucun nouveau build ni test Maven ; les résultats
datés du dernier build complet ont été contrôlés, sans être présentés comme une nouvelle exécution.

---

## Audit approfondi par rapport au PDF — 2026-10-21

Source : `Examen_TP_Billetterie_Cinema_Java25.pdf`, **9 pages relues intégralement** par extraction
texte avec `pypdf`, mode layout. Confrontation au code, aux migrations, à la sécurité, aux tests
et à la configuration. Rapport détaillé : [`AUDIT_2026-10-21.md`](AUDIT_2026-10-21.md).

### Isolation

Copie créée sous `.tools/audit/project` avec `src`, `.mvn`, `pom.xml`, `mvnw` et `mvnw.cmd`.
Les **87 fichiers initiaux de `src` sont identiques par SHA-256**, à la création et à la fin des
reproductions. Les deux classes de sondes ajoutées dans cette copie sont les seules différences
de ses sources par rapport au projet principal. Aucun code applicatif, POM ou migration principal
n'a été modifié. L'instance lancée et sa base de développement sont conservées.

Toutes les commandes Maven suivantes ont été exécutées dans cette copie, avec
`JAVA_HOME=C:\Users\tomta\.jdks\ms-25.0.4.1`, accès au cache Maven et à Docker, MySQL jetable via
`@ServiceConnection`, sans réutilisation de la base de développement.

| Commande / contrôle | Résultat observé | Fin (Europe/Paris) |
|---|---|---|
| `.\mvnw.cmd clean verify` avant ajout des sondes | **BUILD SUCCESS, 64 tests, 0 échec, 0 erreur, 0 ignoré**, JAR produit, 1 min 38 s | 16:15:47 |
| `.\mvnw.cmd test -Dtest=AuditCasLimitesTest` (première version, 15 cas) | **BUILD FAILURE, 15 tests, 10 échecs d'assertion, 0 erreur, 0 ignoré** | 16:18:47 |
| `.\mvnw.cmd test -Dtest=AuditSecuriteMvcTest` | **BUILD SUCCESS, 11 tests, 0 échec, 0 erreur, 0 ignoré** | 16:21:20 |
| `.\mvnw.cmd test '-Dtest=AuditCasLimitesTest#reutiliseLesPlacesAnnuleesSansLesLibererDeuxFois+appliqueLesContraintesSqlSurUneBaseReelle'` | **BUILD SUCCESS, 2 tests, 0 échec, 0 erreur, 0 ignoré** | 16:23:55 |
| `.\mvnw.cmd test-compile`, sources finales des sondes | **BUILD SUCCESS**, compilation seule, aucun test rejoué | 16:33:09 |

Soit **28 sondes distinctes, 18 réussites et 10 échecs d'assertion**. Les rapports XML ont été
relus et leurs résultats et observations exportés dans
[`audit/2026-10-21/resultats.json`](audit/2026-10-21/resultats.json).

Ces échecs ne sont pas des erreurs d'infrastructure : ils reproduisent les comportements
contraires aux attentes décrites dans le rapport. Les sondes sont archivées hors `src/test`,
avec leur [procédure de reproduction](audit/2026-10-21/README.md). Elles ne sont pas comptées
parmi les 64 tests du projet principal. Le dernier ajustement de la sonde monétaire autorise
le comptage d'une création si une future correction renvoie 201, tout en exigeant zéro écriture
lors du rejet observé ; les assertions de statut et le comportement attendu restent identiques.
Une compilation finale des sondes a été exécutée séparément, sans rejouer leurs tests.

### Autres vérifications réellement exécutées

- `mysqladmin ping` dans `cinema-mysql`, avec un mot de passe volontairement incorrect : message
  **Access denied**, mais **code de sortie 0**. Le healthcheck actuel ne prouve pas l'authentification.
- `GET http://localhost:8080/actuator/health` après les sondes : **UP**. `docker ps` ne montre
  plus que le conteneur MySQL de développement pour l'image `mysql:9.7` : conteneurs de test arrêtés.
- `git ls-files` : wrappers et propriétés versionnés ; `mvnw` en mode **100644**, à rendre exécutable
  pour une utilisation directe sous Unix. Aucun `target`, journal, `.idea` ou `.env` suivi détecté.
- Documentation primaire consultée : Spring Security sur CSRF/Basic et OWASP sur l'autorisation
  par objet. Ces sources étayent les limites de déploiement, sans constituer une preuve d'exploitation.

### Périmètre du bilan

Huit constats A01–A08 restent à corriger. Les vérifications positives portent notamment sur le
scénario de capacité obligatoire, la réutilisation des places après annulation, certaines contraintes
SQL, BCrypt, les rôles, la séparation des sessions API/web, le CSRF de connexion/déconnexion,
l'échappement HTML et le calcul serveur du montant et du statut.

Aucune correction applicative ni nouvelle dépendance intégrée. Pas de scan CVE exhaustif,
de test de charge, de navigateur réel ni de simulation multi-JVM. La copie isolée et les journaux
locaux restent sous `.tools`, ignoré par Git ; seules les sources des sondes, observations et
documentation de l'audit sont destinées au dépôt.

Relecture finale : **69 liens locaux contrôlés, 0 cible absente ; 17 blocs PowerShell analysés,
0 erreur de syntaxe ; `git diff --check` sans erreur**. Le JSON d'observations confirme les
totaux 28/10/0/0. Les deux fichiers Java archivés sont identiques par SHA-256 aux sources des
sondes compilées. La procédure groupée fournie a été analysée syntaxiquement, sans nouvelle
exécution de la totalité des sondes.

---

## Correctifs et refonte — 2026-10-21

Les résultats d'audit ci-dessus sont historiques. Les constats A01–A08 ont ensuite été corrigés
à la demande de poursuivre l'application. Les sondes ont rejoint `src/test` ; leurs anciennes
copies et résultats sont conservés sans réécriture.

### Vérifications intermédiaires

| Commande / contrôle | Résultat observé |
|---|---|
| `mvnw.cmd test -Dtest=AuditCasLimitesTest,AuditSecuriteMvcTest,ReservationServiceTest` | 33 tests verts, 16:45:59 |
| Première exécution de `UiMvcTest` après refonte | 34 cas, 4 erreurs de rendu : itération/remplacement Thymeleaf et format LocalDateTime |
| Correction des fragments puis `UiMvcTest,CatalogueExterneServiceTest,AffichesServiceTest,ImportFilmIntegrationTest` | 55 tests verts, 17:05:43 |
| `mvnw.cmd clean verify`, Java 25 + Docker | 116 tests, 0 échec, 0 erreur, 0 ignoré ; JAR produit, 17:09:34, 2 min 07 s |
| `docker compose up -d --wait --wait-timeout 120` | `cinema-mysql` recréé avec son volume conservé, état healthy |
| Vrai SELECT MySQL avec un mot de passe volontairement incorrect | Access denied, **code 1** ; l'ancien `mysqladmin ping` renvoyait 0 |
| JAR Java 25 lancé hors IDE | Accueil 200, migrations V3/V4 appliquées, santé disponible |

Les avertissements Flyway/MySQL 9.7 et Mockito restent informatifs. Le POM n'a pas été modifié
pour cette refonte : aucune nouvelle dépendance applicative.

### Recette dans Chrome

Le MCP navigateur a échoué avant ouverture d'onglet (`failed to start Node runtime`, chemin
introuvable). Utilisation de Python Playwright avec Chrome installé en mode headless.
Script reproductible ajouté : [`scripts/recette_ui.py`](../scripts/recette_ui.py).
Identifiants injectés par variables d'environnement, pas dans les captures.

- Import réel, via formulaire et CSRF, de Chihiro, Totoro, Château dans le ciel, Mononoké,
  Château ambulant et Kiki. Chihiro enrichit le film 1 existant ; cinq films supplémentaires.
  **12 médias, 3 147 949 octets**, conservés dans `data/affiches/` sans séance ajoutée.
- Accueil, catalogue, séances et administration : HTTP 200, images contrôlées, aucune erreur JS.
- Vérifications à 390 et 320 px : aucun débordement sur les pages contrôlées, menu ouvert puis
  fermé avec Échap ; réduction des mouvements : durée d'animation `0s`.
- Estimation de deux places à 9,90 € : **19,80 €**. Réservation réelle confirmée, puis DELETE
  uniquement du code créé par la recette : **204**, disponibilité revenue à sa valeur initiale.
  La réservation de recette reste en statut ANNULEE pour traçabilité.
- Les captures initiales de page entière omettaient les images paresseuses sous le pli :
  chargement, décodage et passage dans le viewport ajoutés avant capture, puis inspection visuelle.

TMDB : recherche, en-tête Bearer, langue française, détail, absence de jeton et erreurs réseau
testés avec HTTP simulé. **Pas de jeton personnel fourni : aucune recette TMDB en direct.**
Ghibli et le téléchargement des affiches ont été testés en direct. Les imports réels enrichissent
la base de développement ; ils ne sont pas ajoutés à une migration Flyway.

Les constats de production de l'audit (propriété des réservations, comptes, TLS, déploiement et
tests de charge) restent distincts de cette livraison. Aucun scan exhaustif CVE exécuté.

### Construction finale et correction responsive

- `mvnw.cmd clean verify` terminé à **17:21:29**, Java 25, MySQL accessible : **BUILD SUCCESS,
  117 tests, 0 échec, 0 erreur, 0 ignoré**, durée 2 min. Inclut le test du rollback et de la
  traduction métier d'une collision SQL à l'import.
- La recette élargie a détecté un débordement à **320 px** dans `/admin/films`, dû à la largeur
  minimale des enfants de la grille contenant le tableau. `min-width: 0` ajouté aux enfants
  et champs, puis nouvelle construction avec les tests `UiMvcTest` : **39 tests verts**,
  `BUILD SUCCESS` à **17:24:23**, durée 16,569 s. Aucun code Java modifié après le build complet.
- Le JAR ainsi reconstruit est relancé avec Java 25 ; le PID éphémère reste dans `.tools/cinema.pid`.
  Les captures initiales de la refonte ne constituent pas seules la validation finale.

Dernière recette Chrome du JAR final : **réussie**. Les pages accueil, catalogue, séances,
fiche film, formulaire de réservation et gestion des films ne débordent ni à 390 ni à 320 px.
Le tableau d'administration défile dans sa propre zone. Menu/Échap, lien d'évitement au clavier,
images décodées et réduction effective de l'animation du panorama (`0s`) vérifiés.
La consultation publique fonctionne avec les requêtes HTTPS externes bloquées dans Chrome.
**Aucune erreur JavaScript** relevée. Résultats : [JSON](visuels/resultats.json),
[capture publique](visuels/accueil.png), [capture mobile](visuels/accueil-mobile.png).
Santé du service recontrôlée à **17:27:02 : UP** ; le JAR reste lancé sur le port 8080.

Contrôles documentaires : **74 liens locaux valides**, script Python analysé sans erreur,
**18 blocs PowerShell analysés sans erreur** après retrait des marqueurs de citation Markdown,
et `git diff --check` sans erreur. Le premier passage du parseur signalait un `&` dans un bloc
cité à cause des `>` de Markdown : la commande documentée était correcte.

### Jeton défini dans le terminal utilisateur

La présence de `TMDB_API_TOKEN` a été contrôlée dans l'environnement de l'agent, sans lecture
ni affichage de sa valeur : **absent**. Une variable définie dans un autre terminal ne peut pas
être héritée par un JAR déjà lancé. Aucun test TMDB en direct n'est donc annoncé à ce stade.

Ajout de `scripts/lancer.ps1` pour redémarrer le JAR depuis le terminal contenant le jeton,
contrôler l'identité du processus sur le port et conserver la valeur exclusivement dans
l'environnement. Analyse syntaxique PowerShell réussie ; exécution **`-Verifier` réussie**
avec JDK 25, JAR présent et processus du projet identifié. Ce mode n'arrête aucun processus.
Le redémarrage avec le jeton réel reste à exécuter par l'utilisateur depuis son terminal.

## Redémarrage du 2026-10-21 à 17:40

L'instance en écoute exécutait un JAR antérieur aux évolutions de l'après-midi. Redémarrage après
`clean package -DskipTests` (**BUILD SUCCESS**) via `scripts/lancer.ps1` :

- Flyway : **4 migrations validées**, schéma déjà en version 4, aucune migration appliquée ;
- `/actuator/health` → 200 ; `/`, `/films`, `/films/5`, `/seances`, `/js/app.js` → 200 ;
- `/api/films`, `/api/seances`, `/v3/api-docs` → 200 ; `/admin` anonyme → 302 vers `/login` ;
- 8 films servis, dont les 5 entrées Ghibli de `V4` ;
- `Jeton TMDB transmis au serveur : False` (variable absente de l'environnement de l'agent).

Observation non corrigée : `/favicon.svg` répond 302 vers `/login`, la chaîne de sécurité de
l'interface autorisant `/favicon.ico` mais pas `/favicon.svg`.
