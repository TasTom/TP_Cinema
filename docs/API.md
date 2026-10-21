# Contrat d'API

Base : `http://localhost:8080`. Toutes les réponses sont en `application/json`.
Documentation interactive : `GET /swagger-ui.html` — spécification brute : `GET /v3/api-docs`.

Authentification des routes protégées : **HTTP Basic**.

| Compte | Mot de passe | Rôles |
|---|---|---|
| `client` | `client123` | `CLIENT` |
| `programmateur` | `prog123` | `PROGRAMMATEUR` |
| `admin` | `admin123` | `ADMIN` |

---

## 1. Routes publiques

### `GET /api/films`

Liste les films, triés par titre. Filtres optionnels et cumulables.

| Paramètre | Type | Description |
|---|---|---|
| `titre` | chaîne | Recherche partielle, insensible à la casse |
| `genre` | chaîne | Correspondance exacte, insensible à la casse |

```bash
curl -s "http://localhost:8080/api/films"
curl -s "http://localhost:8080/api/films?titre=incept"
curl -s "http://localhost:8080/api/films?genre=Drame"
```

```json
[
  {
    "id": 2,
    "titre": "Inception",
    "genre": "Science-fiction",
    "dureeMinutes": 148,
    "dateSortie": "2010-07-16"
  }
]
```

### `GET /api/films/{id}`

`200` avec le film, `404` `FILM_INTROUVABLE` s'il n'existe pas.

### `GET /api/seances`

Séances triées par date de début, avec capacité et places restantes calculées.

| Paramètre | Type | Description |
|---|---|---|
| `du` | date ISO (`yyyy-MM-dd`) | Début d'intervalle, inclus |
| `au` | date ISO (`yyyy-MM-dd`) | Fin d'intervalle, inclus |

```bash
curl -s "http://localhost:8080/api/seances?du=2026-11-01&au=2026-11-30"
```

```json
[
  {
    "id": 1,
    "filmId": 1,
    "filmTitre": "Le Voyage de Chihiro",
    "salleId": 1,
    "salleNom": "Salle Lumiere",
    "debut": "2026-11-05T20:00:00",
    "prixUnitaire": 9.90,
    "capaciteSalle": 120,
    "placesRestantes": 120
  }
]
```

Si `du > au` : `400` `INTERVALLE_INVALIDE`.

### `GET /api/seances/{id}`

`200` avec la séance, `404` `SEANCE_INTROUVABLE`.

### `GET /api/salles` et `GET /api/salles/{id}`

Lecture publique des salles (`id`, `nom`, `capacite`).

### `GET /actuator/health`

```json
{"status": "UP"}
```

Seul endpoint Actuator exposé.

### Documentation

| Chemin | Contenu |
|---|---|
| `GET /v3/api-docs` | Spécification OpenAPI 3 (JSON) |
| `GET /swagger-ui.html` | Interface Swagger UI (redirection vers l'index) |

---

## 2. Routes protégées

### `POST /api/programmation/films` — PROGRAMMATEUR ou ADMIN

```bash
curl -s -u programmateur:prog123 \
  -H "Content-Type: application/json" \
  -d '{"titre":"Dune","genre":"Science-fiction","dureeMinutes":155,"dateSortie":"2021-09-15"}' \
  http://localhost:8080/api/programmation/films
```

```json
{"id": 4, "titre": "Dune", "genre": "Science-fiction", "dureeMinutes": 155, "dateSortie": "2021-09-15"}
```

`201 Created` + `Location: /api/films/4`.
Erreurs : `400` `VALIDATION_ECHOUEE` (titre vide, durée hors 40–300), `401`, `403`.

### `POST /api/programmation/seances` — PROGRAMMATEUR ou ADMIN

```bash
curl -s -u programmateur:prog123 \
  -H "Content-Type: application/json" \
  -d '{"filmId":1,"salleId":1,"debut":"2026-12-02T20:00:00","prixUnitaire":10.50}' \
  http://localhost:8080/api/programmation/seances
```

`201 Created` + `Location: /api/seances/4`.
Erreurs : `400` `VALIDATION_ECHOUEE` (film ou salle manquant, date manquante, prix ≤ 0),
`400` `FILM_INTROUVABLE` / `SALLE_INTROUVABLE`, `401`, `403`.

### `POST /api/admin/salles` — ADMIN

```bash
curl -s -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"nom":"Salle Tati","capacite":150}' \
  http://localhost:8080/api/admin/salles
```

`201 Created` + `Location: /api/salles/4`.
Erreurs : `400` `VALIDATION_ECHOUEE` (nom vide, capacité hors 20–500),
`409` `SALLE_DEJA_EXISTANTE`, `401`, `403`.

### `POST /api/reservations` — CLIENT ou ADMIN

```bash
curl -s -u client:client123 \
  -H "Content-Type: application/json" \
  -d '{"seanceId":1,"emailClient":"lea@example.test","nombrePlaces":3}' \
  http://localhost:8080/api/reservations
```

```json
{
  "id": 1,
  "code": "RES-70574FA58E",
  "seanceId": 1,
  "filmTitre": "Le Voyage de Chihiro",
  "salleNom": "Salle Lumiere",
  "debut": "2026-11-05T20:00:00",
  "emailClient": "lea@example.test",
  "nombrePlaces": 3,
  "statut": "CONFIRMEE",
  "montantTotal": 29.70,
  "creeeLe": "2026-10-21T12:30:07.457732"
}
```

`201 Created`. Le **montant total est calculé côté serveur** : `prixUnitaire × nombrePlaces`.

| Cas | Statut | Code |
|---|---|---|
| `nombrePlaces` hors 1–8, e-mail invalide, champ manquant | 400 | `VALIDATION_ECHOUEE` |
| Séance inexistante | 404 | `SEANCE_INTROUVABLE` |
| Places restantes insuffisantes | 409 | `CAPACITE_INSUFFISANTE` |
| Non authentifié | 401 | `NON_AUTHENTIFIE` |
| Rôle `PROGRAMMATEUR` | 403 | `ACCES_REFUSE` |

### `DELETE /api/reservations/{code}` — CLIENT ou ADMIN

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -u client:client123 -X DELETE \
  http://localhost:8080/api/reservations/RES-70574FA58E
```

`204 No Content`. Les places sont immédiatement libérées.
Idempotent : une seconde annulation renvoie aussi `204`.
Code inconnu : `404` `RESERVATION_INTROUVABLE`.

---

## 3. Catalogue d'erreurs

Corps unique pour toutes les erreurs d'API :

```json
{
  "timestamp": "2026-10-21T12:25:15.209341200Z",
  "status": 409,
  "code": "CAPACITE_INSUFFISANTE",
  "message": "Il ne reste que 2 places pour cette séance.",
  "path": "/api/reservations",
  "details": []
}
```

`details` n'apparaît que pour les erreurs de validation :

```json
{
  "timestamp": "2026-10-21T12:30:14.899627300Z",
  "status": 400,
  "code": "VALIDATION_ECHOUEE",
  "message": "Les données fournies sont invalides.",
  "path": "/api/reservations",
  "details": [
    {"champ": "nombrePlaces", "message": "Le nombre de places doit être compris entre 1 et 8."}
  ]
}
```

| Code | Statut | Origine |
|---|---|---|
| `VALIDATION_ECHOUEE` | 400 | Jakarta Validation sur un DTO ou un paramètre |
| `REQUETE_MAL_FORMEE` | 400 | Corps absent ou JSON invalide |
| `PARAMETRE_MANQUANT` | 400 | Paramètre de requête requis absent |
| `PARAMETRE_INVALIDE` | 400 | Paramètre de requête non convertible |
| `INTERVALLE_INVALIDE` | 400 | `du` postérieur à `au` |
| `FILM_INTROUVABLE` | 400 ou 404 | Film inexistant (404 en lecture, 400 en création de séance) |
| `SALLE_INTROUVABLE` | 400 ou 404 | Salle inexistante |
| `SEANCE_INTROUVABLE` | 404 | Séance inexistante |
| `RESERVATION_INTROUVABLE` | 404 | Code de réservation inconnu |
| `RESSOURCE_INTROUVABLE` | 404 | Chemin inconnu |
| `CAPACITE_INSUFFISANTE` | 409 | Places restantes insuffisantes |
| `SALLE_DEJA_EXISTANTE` | 409 | Nom de salle déjà utilisé |
| `CONFLIT_DONNEES` | 409 | Violation de contrainte non traduite |
| `NON_AUTHENTIFIE` | 401 | Authentification absente ou invalide |
| `ACCES_REFUSE` | 403 | Rôle insuffisant |
| `METHODE_NON_AUTORISEE` | 405 | Méthode HTTP inutilisable sur la ressource |
| `ERREUR_INTERNE` | 500 | Erreur non gérée |

Aucun message d'erreur ne contient de trace de pile ni de détail SQL.

---

## 4. Métriques métier (Micrometer)

Non exposées publiquement : `management.endpoints.web.exposure.include: health`.
Consultables en interne via un `MeterRegistry` (tests, injection de dépendance) ou en élargissant
temporairement l'exposition dans un profil de diagnostic.

| Métrique | Type | Tags | Sens |
|---|---|---|---|
| `cinema.reservations` | compteur | `resultat=confirmee\|annulee\|refusee` | Nombre d'opérations par issue |
| `cinema.places` | distribution | `resultat=reservee\|refusee` | Places par opération |

---

## 5. Interface web (extension hors barème)

| Page | Chemin | Accès |
|---|---|---|
| Accueil | `/` | Public |
| Catalogue | `/films` | Public |
| Planning | `/seances` | Public |
| Réservation | `/reservation/{seanceId}` | `CLIENT`, `ADMIN` |
| Confirmation | `/reservation/confirmation` | `CLIENT`, `ADMIN` |
| Connexion | `/login` | Public |
| Administration | `/admin` | `PROGRAMMATEUR`, `ADMIN` |
| Gestion des films | `/admin/films` | `PROGRAMMATEUR`, `ADMIN` |
| Gestion des séances | `/admin/seances` | `PROGRAMMATEUR`, `ADMIN` |
| Gestion des salles | `/admin/salles` | `ADMIN` |
| Toutes les réservations | `/admin/reservations` | `ADMIN` |

L'interface utilise les mêmes services que l'API et ne modifie aucun contrat REST.
