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
