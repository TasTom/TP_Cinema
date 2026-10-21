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

| Question | Choix retenu |
|---|---|
| Périmètre du frontend Thymeleaf | **Complet** : catalogue public, réservation cliente, back-office Admin/Programmateur |
| Stratégie de concurrence | **Verrou pessimiste** `SELECT … FOR UPDATE` |
| Bonus | **Métriques Micrometer** (places réservées / refusées) |
| Langue de la documentation | **Français** (vocabulaire métier conservé : Film, Salle, Seance, Reservation) |
| JDK de build | **JDK 25 local** `C:\Users\tomta\.jdks\ms-25.0.4.1` |
| MySQL de développement | **Docker `mysql:9.7`** (dernière LTS) |
| Initialisation Git | **Oui**, commits par étapes |

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

## Prompt 5 et suivants

À compléter à chaque nouvel échange.
