# Instructions Copilot — CinéBil

La mémoire complète du projet est **[`AGENTS.md`](../AGENTS.md)**. Lire ce fichier en premier.
Ci-dessous, uniquement les règles qui font échouer le build ou la notation si elles sont ignorées.

## Contexte

Billetterie de cinéma. Sujet imposé : `Examen_TP_Billetterie_Cinema_Java25.pdf`.
Le frontend Thymeleaf est une **extension hors barème**, pas une exigence du sujet.

## Règles bloquantes

- Java **25** : `JAVA_HOME=C:\Users\tomta\.jdks\ms-25.0.4.1` avant tout `mvnw`. Le `java` du PATH est un JDK 23.
- Spring Boot **4.1.1**, parent conservé. Ne pas ajouter de version sur une dépendance gérée.
- **MySQL uniquement**, jamais H2. Tests MySQL via Testcontainers avec
  `@Testcontainers(disabledWithoutDocker = true)`.
- `ddl-auto=validate` ; le schéma est créé **exclusivement** par Flyway.
- **DTO `record`** obligatoires ; aucune entité JPA renvoyée par un contrôleur.
- **Aucun Lombok, aucun MapStruct.**
- Contrôleurs fins : la règle de capacité vit dans `ReservationService`, protégée par un
  **verrou pessimiste** (`findByIdForUpdate`). Jamais de `synchronized`.
- HTTP Basic + BCrypt ; aucun mot de passe en clair.
- Seul `/actuator/health` est exposé.

## Vérification obligatoire avant de conclure

```powershell
$env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1"
.\mvnw.cmd clean verify
```

Critère de fin : `BUILD SUCCESS`, 30 tests, 0 échec. Ne jamais annoncer un résultat non exécuté.

## Pièges spécifiques à ce projet

| Piège | Solution |
|---|---|
| Jackson 3 | injecter `tools.jackson.databind.ObjectMapper`, pas `com.fasterxml.*` |
| `WebMvcTest` en Boot 4 | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `MySQLContainer` | classe non générique : `new MySQLContainer("mysql:9.7")` |
| `ClientHttpResponse.bodyTo` | inexistant : `retrieve().toEntity(...)` + capturer `RestClientResponseException` |
| Fuseau | `connectionTimeZone=LOCAL` et `java_time_use_direct_jdbc=true` |
| JSON via PowerShell | écrire un fichier et utiliser `--data-binary "@fichier.json"` |

## Consignation — demande explicite du porteur du projet

1. Chaque prompt reçu est ajouté **verbatim** dans [`docs/PROMPTS.md`](../docs/PROMPTS.md).
2. Chaque décision structurante devient un ADR dans [`docs/DECISIONS.md`](../docs/DECISIONS.md).
3. Chaque vérification exécutée est datée dans [`docs/RECETTE.md`](../docs/RECETTE.md).
4. `AGENTS.md` est mis à jour si l'état du projet change.

## Style

- Domaine et messages en **français** (`Film`, `Salle`, `Seance`, `Reservation`).
- Tests nommés en français descriptif ; AssertJ.
- Commits en Conventional Commits français + trailer
  `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.
- Commentaires rares : uniquement l'intention non évidente.
