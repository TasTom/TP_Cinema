# CinéBil — Billetterie de cinéma

Application de billetterie : API REST Spring Boot, persistance MySQL, migrations Flyway,
sécurité HTTP Basic, vérification de capacité sous concurrence, et interface Thymeleaf sobre.

Projet réalisé dans le cadre de l'épreuve **R5.08 Qualité du développement — BUT Informatique 3e année**
(sujet `Examen_TP_Billetterie_Cinema_Java25.pdf`).

---

## 1. Prérequis

| Outil | Version attendue | Vérification |
|---|---|---|
| JDK | **25 LTS** | `java -version` |
| Maven | fourni par le wrapper **3.9.16** | `.\mvnw.cmd -version` |
| Docker | 20+ avec Compose | `docker compose version` |

> **Important — JDK** : le projet compile et s'exécute en Java 25. Si le `java` par défaut de la
> machine n'est pas 25, définissez `JAVA_HOME` avant toute commande :
>
> ```powershell
> $env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1"
> ```
>
> `.\mvnw.cmd -version` doit alors afficher `Java version: 25.x`.
> Maven, la compilation, les tests et `java -jar` doivent tous utiliser ce même JDK 25.

Aucune installation de Maven n'est nécessaire : le wrapper télécharge la version 3.9.16 au premier
appel.

---

## 2. Démarrer MySQL

```powershell
docker compose up -d
docker compose ps          # attendre l'état healthy
```

Le service `cinema-mysql` expose MySQL 9.7 sur `localhost:3306` avec :

| Paramètre | Valeur par défaut |
|---|---|
| Base | `cinema` |
| Utilisateur | `cinema` |
| Mot de passe | `cinema` |

Ces valeurs sont **de développement**. Elles peuvent être surchargées par
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`,
`MYSQL_ROOT_PASSWORD`, `MYSQL_PORT`.

Pour repartir d'une base vide :

```powershell
docker compose down -v
docker compose up -d
```

Au démarrage de l'application, Flyway applique `V1__schema_initial.sql` puis
`V2__donnees_demonstration.sql`, et Hibernate valide le schéma (`ddl-auto=validate`).
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
normalement. Dans ce cas, les points de recette dépendant de MySQL doivent être validés
manuellement avec `docker compose up -d` puis les commandes `curl` de [`docs/API.md`](docs/API.md).

---

## 4. Lancer l'application

### Avec Maven

```powershell
.\mvnw.cmd spring-boot:run
```

### Avec le JAR (vérification hors IDE)

```powershell
.\mvnw.cmd clean package
java -jar target\cinema-billetterie-1.0.0.jar
```

L'application écoute sur <http://localhost:8080> (`SERVER_PORT` pour changer).

| Ressource | URL |
|---|---|
| Accueil | <http://localhost:8080/> |
| Catalogue | <http://localhost:8080/films> |
| Planning | <http://localhost:8080/seances> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Santé | <http://localhost:8080/actuator/health> |

---

## 5. Comptes de démonstration

Authentification **HTTP Basic** pour l'API, formulaire de connexion pour l'interface.
Les mots de passe sont stockés **hachés en BCrypt** ; aucun mot de passe en clair n'existe dans les
sources.

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
| GET | `/api/seances?du=&au=` | 200, séances dans l'intervalle (bornes incluses) |
| GET | `/api/seances/{id}` | 200 ou 404 |
| GET | `/api/salles`, `/api/salles/{id}` | 200 |
| GET | `/actuator/health` | 200 `{"status":"UP"}` |
| GET | `/v3/api-docs`, `/swagger-ui.html` | Documentation OpenAPI |

### Protégés

| Méthode | Chemin | Rôles | Résultat |
|---|---|---|---|
| POST | `/api/programmation/films` | `PROGRAMMATEUR`, `ADMIN` | 201 + `Location` |
| POST | `/api/programmation/seances` | `PROGRAMMATEUR`, `ADMIN` | 201 ou 400 |
| POST | `/api/admin/salles` | `ADMIN` | 201, 400 ou 409 |
| POST | `/api/reservations` | `CLIENT`, `ADMIN` | 201, 400, 404 ou 409 |
| DELETE | `/api/reservations/{code}` | `CLIENT`, `ADMIN` | 204 ou 404 |

Exemple :

```powershell
curl.exe -u client:client123 -H "Content-Type: application/json" `
  -d '{"seanceId":1,"emailClient":"lea@example.test","nombrePlaces":3}' `
  http://localhost:8080/api/reservations
```

> Sous PowerShell, préférez un fichier de corps (`--data-binary "@payload.json"`) : l'échappement
> des guillemets en ligne de commande peut produire un JSON invalide.

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

| Classe | Type | Couverture |
|---|---|---|
| `ReservationServiceTest` | unitaire (Mockito) | montant, capacité insuffisante, annulation idempotente, validation, métriques |
| `ReservationControllerMvcTest` | MVC | 201, 400 (0 et 9 places), 401, 403, 409, 204 |
| `ApiSecurityMvcTest` | MVC + sécurité | lecture publique, 401, 403, 201 `ADMIN`, 201 `PROGRAMMATEUR`, 409 doublon |
| `ReservationRepositoryTest` | JPA + MySQL | somme des places confirmées, libération après annulation |
| `CinemaIntegrationTest` | intégration MySQL | migrations, réservation réelle, sécurité, filtres, OpenAPI, health |
| `ReservationConcurrencyTest` | concurrence | deux réservations simultanées sans surréservation |

```powershell
.\mvnw.cmd test                                  # tous les tests
.\mvnw.cmd test -Dtest=ReservationServiceTest    # une classe
```

---

## 9. Choix techniques

| Sujet | Décision |
|---|---|
| Architecture | paquets par fonctionnalité (`film`, `salle`, `seance`, `reservation`) |
| DTO | `record` immuables ; entités JPA jamais renvoyées |
| Processeurs d'annotations | aucun (ni Lombok ni MapStruct) → aucun risque avec Java 25 |
| Schéma | 100 % Flyway, `ddl-auto=validate` |
| Erreurs | `@RestControllerAdvice` + codes stables, sans trace de pile ni SQL |
| Sécurité | deux chaînes de filtres : API stateless JSON, interface à session |
| Métriques | Micrometer : `cinema.reservations`, `cinema.places` (bonus) |
| Frontend | extension hors barème, isolée dans `ui`, aucune règle métier dupliquée |

Documentation complémentaire :

- [`docs/ANALYSE_SPEC.md`](docs/ANALYSE_SPEC.md) — analyse du sujet et traçabilité A1–A10
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — couches, flux, diagramme de concurrence
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — décisions d'architecture (ADR)
- [`docs/API.md`](docs/API.md) — contrat HTTP et catalogue d'erreurs
- [`docs/RECETTE.md`](docs/RECETTE.md) — procès-verbal des contrôles A1–A10 exécutés
- [`docs/PROMPTS.md`](docs/PROMPTS.md) — journal des prompts et des décisions

---

## 10. Limites connues

- **Docker requis** pour les tests MySQL. Sans Docker, ces classes sont ignorées et signalées
  comme telles par surefire ; les autres tests restent verts.
- **MySQL 9.7** est plus récent que la dernière version vérifiée par Flyway (9.4) : un avertissement
  `Using MySQL 9.7 which is newer than the version Flyway has been verified with` apparaît au
  démarrage. Il est informatif : les migrations s'appliquent et `ddl-auto=validate` passe.
- **Comptes en mémoire** : adaptés à l'épreuve, pas à une production multi-instances.
- **Interface Thymeleaf** : hors périmètre du sujet, donc non notée, mais fonctionnelle et testée
  manuellement (parcours client, back-office, validation, 403).
- **Pas de pagination** sur les listes : volumes de démonstration.
