# Journal des prompts et des décisions

Ce fichier consigne **tous les prompts envoyés par le porteur du projet**, dans leur ordre
chronologique, ainsi que les décisions prises en réponse. Il sert de mémoire de conception :
il permet de reconstituer *pourquoi* le code est dans cet état.

> Convention : les prompts sont reproduits **verbatim** (fautes et majuscules incluses).
> Les réponses sont résumées ; les décisions structurantes sont détaillées dans
> [`DECISIONS.md`](./DECISIONS.md).

---

## Prompt 1 — Cadrage initial

> L'objectif de ce projet est de concevoir une application autour du cinéma avec un service API,
> backend , ainsi qu'un frontend sobre et optimisé via notamment Thymeleaf, les spécifications sont
> à validé avec moi, analyse en détail le document
> @Examen_TP_Billetterie_Cinema_Java25.pdf
>
> Tout doit être documenté/consigné ainsi que mes prompts que je t'envoie, Utilsise correctement les
> dernières versions LTS et vérifie leur compatibilité entre eux, si tu as le moindre doute pose moi
> une question via l'outil ask dont tu disposes

**Actions menées**

1. Extraction et lecture intégrale des 9 pages du PDF (`Examen_TP_Billetterie_Cinema_Java25.pdf`).
2. Vérification de l'environnement local : Java 23 par défaut, JDK 25 LTS présent dans
   `C:\Users\tomta\.jdks\ms-25.0.4.1`, Maven absent du `PATH`, Docker 29.8.2 opérationnel.
3. Vérification des versions sur Maven Central et Docker Hub :
   Spring Boot `4.1.1`, springdoc `3.1.1`, Maven `3.9.16`, Testcontainers `2.0.5`,
   Flyway `12.4.0`, Hibernate `7.4.5.Final`, Spring Security `7.1.1`, image `mysql:9.7`.
4. Vérification de compatibilité : Spring Boot 4.1.1 exige Java 17 à 26 → Java 25 est supporté.
5. Restitution d'une analyse structurée (périmètre, modèle métier, contrat HTTP, concurrence,
   recette A1–A10, risques, plan de documentation).
6. Question posée via l'outil `ask` sur les 7 points structurants.

**Réponse apportée** : synthèse du sujet + tableau de versions vérifiées + architecture cible +
risques/parades + plan de documentation, puis questionnaire de validation.

---

## Prompt 2 — Exigences de qualité

> Le développement ce fais via spécification,
>
> - Architecture propre et profesionnel
> - Dernière technologie et récente

**Décisions induites**

- Développement **spécification d'abord** : le PDF fait foi, chaque exigence est traçable vers du
  code et/ou un test (voir [`ANALYSE_SPEC.md`](./ANALYSE_SPEC.md)).
- Architecture en couches **organisée par fonctionnalité** (`film`, `salle`, `seance`,
  `reservation`, `security`, `common/error`, `config`, `ui`), contrôleurs fins, services porteurs
  des règles, DTO `record` immuables, entités jamais exposées.
- Aucune dépendance ajoutée « par réflexe » : chaque dépendance du `pom.xml` répond à un besoin
  implémenté (règle de qualité du sujet).
- Pas de Lombok ni MapStruct : supprime tout risque de processeur d'annotations avec Java 25.
- Versions les plus récentes **stables** compatibles entre elles, jamais de version répétée quand
  le parent Spring Boot la gère.

---

## Prompt 3 — Validation des choix structurants (réponses au questionnaire)

| Question                          | Choix retenu                                                                                |
| --------------------------------- | ------------------------------------------------------------------------------------------- |
| Périmètre du frontend Thymeleaf | **Complet** : catalogue public, réservation cliente, back-office Admin/Programmateur |
| Stratégie de concurrence         | **Verrou pessimiste** `SELECT … FOR UPDATE`                                        |
| Bonus                             | **Métriques Micrometer** (places réservées / refusées)                            |
| Langue de la documentation        | **Français** (vocabulaire métier conservé : Film, Salle, Seance, Reservation)      |
| JDK de build                      | **JDK 25 local** `C:\Users\tomta\.jdks\ms-25.0.4.1`                                 |
| MySQL de développement           | **Docker `mysql:9.7`** (dernière LTS)                                              |
| Initialisation Git                | **Oui**, commits par étapes                                                          |

**Conséquences**

- Le frontend est explicitement documenté comme **extension hors barème** : le sujet PDF l'exclut,
  il est donc isolé dans le paquet `ui` et ne modifie aucun contrat d'API.
- Le verrou pessimiste est implémenté dans `SeanceRepository#findByIdForUpdate` et justifié dans
  le README et [`DECISIONS.md`](./DECISIONS.md).
- Les métriques `cinema.reservations` et `cinema.places` sont testées dans
  `ReservationServiceTest`.

---

## Prompt 4 — Contrôle de la consignation

> tu as oublié en cours de route de tout consigner en mémoire ?

**Constat** : exact. Le code et les vérifications avaient avancé, la documentation promesse
(`README`, `ANALYSE_SPEC`, `ARCHITECTURE`, `DECISIONS`, `PROMPTS`, `API`) n'existait pas encore.

**Actions correctives**

1. Création de [`docs/PROMPTS.md`](./PROMPTS.md) (ce fichier) avec l'historique complet.
2. Création de [`docs/ANALYSE_SPEC.md`](./ANALYSE_SPEC.md) : matrice de traçabilité exigence → code → test.
3. Création de [`docs/ARCHITECTURE.md`](./ARCHITECTURE.md) : couches, flux, diagramme de concurrence.
4. Création de [`docs/DECISIONS.md`](./DECISIONS.md) : décisions d'architecture (ADR).
5. Création de [`docs/API.md`](./API.md) : contrat HTTP, exemples `curl`, catalogue d'erreurs.
6. Création de [`README.md`](../README.md) : démarrage, comptes, commandes, limites.
7. Création de [`docs/RECETTE.md`](./RECETTE.md) : procès-verbal des contrôles A1–A10 réellement exécutés.

**Règle adoptée pour la suite** : toute nouvelle demande est ajoutée ici avant ou pendant sa
mise en œuvre, et toute décision structurante est reportée dans `DECISIONS.md`.

---

## Prompt 5 — Fichier de mémoire du projet

> tu as créer le fichier de mémoire du projet ?

**Constat** : non. La documentation existait (`README`, `ANALYSE_SPEC`, `ARCHITECTURE`,
`DECISIONS`, `API`, `RECETTE`, `PROMPTS`) mais aucun **fichier de mémoire** lisible en premier par
un agent ou un tiers n'avait été créé.

**Actions**

1. [`AGENTS.md`](../AGENTS.md) à la racine : mémoire canonique du projet.
   - identité et statut ;
   - 12 règles non négociables issues du sujet ;
   - tableau des versions figées ;
   - commandes de référence (avec `JAVA_HOME`) ;
   - carte de l'architecture et fichiers clés ;
   - conventions code / tests / Git ;
   - **tableau des pièges déjà rencontrés** avec leur solution ;
   - état vérifié, index documentaire, limites assumées, prochaines étapes.
2. [`.github/copilot-instructions.md`](../.github/copilot-instructions.md) : version courte
   chargée automatiquement par Copilot, qui renvoie vers `AGENTS.md`.
3. Mise à jour de l'index documentaire du README.

**Règle historique, remplacée par la fusion du prompt 6** : `AGENTS.md` est la source de vérité ; `copilot-instructions.md` ne contient que les
règles bloquantes, pour éviter toute divergence entre les deux fichiers.

---

## Prompt 6 — Fusion des consignes et poursuite du projet

**Date** : 2026-10-21

```text
continue le projet et fussione AGENTS.md et copilot-instructions.md
Je pense que c'est une bonne chose ?
```

**Mise en œuvre**

- Fusionner les consignes dans `AGENTS.md`, référence unique ; conserver
  `.github/copilot-instructions.md` comme point d'entrée renvoyant vers ce fichier.
- Préserver les modifications locales déjà présentes dans `AGENTS.md`, notamment la suppression
  du trailer d'attribution Copilot obligatoire.
- Poursuivre par des tests MVC de l'interface Thymeleaf : rendu réel des pages, réservation,
  validation, erreurs métier, rôles et protection CSRF. Ce choix renforce l'extension existante
  et conserve le périmètre du bonus Micrometer (ADR-010).
- Consigner les décisions et les résultats effectivement observés dans les documents dédiés.

**Corrections découvertes pendant la validation**

- Le wrapper Windows indexait une propriété `Target` nulle pour un cache Maven ordinaire :
  contrôle préalable ajouté, versions conservées (ADR-013).
- Les pages d'erreur Thymeleaf ne recevaient plus les attributs communs de navigation après
  résolution d'une exception. `UiExceptionHandler` réutilise maintenant leur initialisation,
  avec tests de non-régression pour un client connecté et un visiteur anonyme.

## Prompt 7 — Audit approfondi par rapport au sujet

**Date** : 2026-10-21

```text
par rapport à Examen_TP_Billetterie_Cinema_Java25.pdf
Reprends fais une analyse approfondies regarde les edges du projets, la sécurité etc...
```

**Mise en œuvre** : relire le PDF original, confronter les exigences au code et aux tests,
examiner les cas limites, la sécurité, la concurrence et l'exploitation, reproduire les défauts
sur des données isolées et produire un audit priorisé avec preuves et limites.

**Résultat** : PDF de 9 pages relu, build isolé de référence réussi (64 tests), puis 28 sondes
distinctes exécutées (18 réussites, 10 échecs d'assertion, aucune erreur d'exécution ni aucun
test ignoré). Huit constats priorisés et les limites de production sont consignés dans
`AUDIT_2026-10-21.md`. Les sondes et résultats sont archivés dans `docs/audit/2026-10-21/`.
La mémoire et la documentation ne présentent plus la recette nominale comme une conformité
exhaustive. Aucun correctif applicatif appliqué pendant cette analyse.

---

## Prompt 8 — Enrichissement et refonte de l'application

**Date** : 2026-10-21

```text
Ensuite continue l'application, vas le plus loin possible, affiche, animation du site, meilleurs visuels, récupération des films via api externe, meilleur charte graphique/ meilleur UI/UX via des skills/mcp que tu peux chercher via le skill : find-skills
```

**Mise en œuvre** : découvrir les skills de design demandés, corriger les risques reproduits,
enrichir le catalogue via une API externe, refondre l'interface et ses parcours avec affiches
et animations accessibles, puis vérifier le build et le rendu de l'application lancée.

### Réponses sur le choix du catalogue — verbatim

```text
je pensais à une API tvmaze ?
```

```text
y'a pas un équivelent qui fais les films ?
```

**Décision** : TVmaze étant consacré aux séries, retenir TMDB pour les films généralistes
(jeton personnel côté serveur), avec Ghibli sans clé pour un catalogue immédiatement utilisable.
Les affiches sont conservées localement ; une importation ne crée pas de séance.

**Résultat livré** : charte bleu de salle et cuivre, accueil panoramique, cartes d'affiches,
fiches film, planning, formulaires et menu mobile ; Ghibli utilisable en direct, TMDB prêt pour
un jeton serveur. Correctifs A01–A08 intégrés, 117 tests verts, JAR lancé et recette Chrome
bureau/mobile. Skills installés et appliqués : find-skills, frontend-design, web-design-guidelines.
Le README, la mémoire, les ADR et le procès-verbal ont été mis à jour.

## Prompt 9 — Configuration locale TMDB

```text
j'ai fais la commande :  $env:TMDB_API_TOKEN = "eyJhbGc*****
```

**Mise en œuvre** : conserver la valeur masquée telle que fournie, ne pas demander le jeton.
La variable définie dans le terminal de l'utilisateur n'est pas présente dans l'environnement
de l'agent ni dans le JAR déjà lancé. Ajouter `scripts/lancer.ps1` pour un redémarrage depuis
ce même terminal, avec héritage de l'environnement et sans afficher/enregistrer le secret.

## Prompt 10 — Redémarrage du projet

```text
redémarre le projet ?
```

**Contexte constaté** : l'instance en écoute sur le port 8080 exécutait encore le JAR construit
à 15:22, donc **antérieur** aux évolutions de l'après-midi (V3/V4, catalogue Ghibli/TMDB,
nouvelle interface, correctifs A01–A08). Le redémarrage devait recharger ces sources.

**Mise en œuvre** :

1. Arrêt de l'instance obsolète identifiée par sa ligne de commande, pas par son nom.
2. Reconstruction depuis les sources courantes : `.\mvnw.cmd clean package -DskipTests`
   → **BUILD SUCCESS**, JAR régénéré.
3. Redémarrage via le lanceur officiel [`scripts/lancer.ps1`](../scripts/lancer.ps1), et non par un
   `java -jar` manuel : le script vérifie JDK 25 et le JAR, identifie le processus propriétaire du
   port, démarre MySQL avec `--wait`, écrit `.tools/cinema.pid` et redirige les journaux vers `logs/`.
4. Contrôles après démarrage :

| Chemin                                             | Statut              |
| -------------------------------------------------- | ------------------- |
| `/actuator/health`                               | 200                 |
| `/`, `/films`, `/films/5`, `/seances`      | 200                 |
| `/js/app.js`                                     | 200                 |
| `/api/films`, `/api/seances`, `/v3/api-docs` | 200                 |
| `/admin` anonyme                                 | 302 vers `/login` |

Flyway : 4 migrations validées, schéma déjà en version 4, aucune migration à appliquer.
Huit films sont servis, dont les cinq entrées Ghibli issues de `V4`.

**Limite signalée** : le lanceur affiche `Jeton TMDB transmis au serveur : False`. Le jeton défini
dans le terminal de l'utilisateur n'est pas hérité par l'environnement de l'agent. Pour activer
l'import TMDB, exécuter `.\scripts\lancer.ps1` **depuis le terminal où `TMDB_API_TOKEN` est défini** :
le script arrête l'instance courante (PID suivi) et la relance avec la variable héritée.

**Défaut mineur relevé, non corrigé sans accord** : `/favicon.svg` répond 302 vers `/login`, car la
chaîne de sécurité de l'interface autorise `/favicon.ico` mais pas `/favicon.svg`.
