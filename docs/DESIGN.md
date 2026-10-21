# Direction visuelle CinéBil

La demande du 21 octobre 2026 autorise les affiches et données externes. Les styles, scripts et
polices restent servis localement ; les affiches importées sont également conservées localement.

Skills lus : `find-skills` (Vercel), `frontend-design` (Anthropic) et `web-design-guidelines`
(Vercel). Le catalogue skills.sh et les sources des éditeurs ont été consultés avant installation.

## Intention

Un cinéma de quartier contemporain, où l'affiche du film fait le spectacle. Le choix du film et
la réservation restent immédiatement compréhensibles. Préserver Thymeleaf et les services métier.

## Tokens

- Bleu de salle : `#13283b`, fond et navigation.
- Bleu projeté : `#254d65`, surfaces secondaires.
- Papier : `#f3f0e7`, panneaux de lecture et formulaires.
- Blanc lumière : `#fffdf6`, texte et surfaces.
- Cuivre : `#f0b582`, actions et détails de réservation.
- Sauge : `#abd1bd`, disponibilité et succès.
- Titres : Franklin Gothic / Arial Narrow / sans-serif, grands et resserrés.
- Lecture : Aptos / Segoe UI / sans-serif, sans téléchargement de police.

## Composition

Accueil : navigation compacte, grande image de film avec titre aligné à gauche, puis rail
d'affiches et planning lisible. Catalogue : filtres dans l'URL, affiches en grille, accès à une
fiche détaillée. Réservation : étapes, résumé de séance, quantité et total immédiatement visibles.

```text
Logo                 Films  Séances                          Connexion
┌─────────────────────────────────────────────────────────────────────┐
│       Affiche panoramique / titre du film / réserver                 │
└─────────────────────────────────────────────────────────────────────┘
Cette semaine                         Planning
[affiche] [affiche] [affiche] [affiche] [affiche]
Les prochaines séances : date | film | salle | places | réserver
```

## Relecture avant réalisation

Le précédent écran regroupait des blocs génériques sur fond crème. Remplacer le panneau
« Comment ça marche ? » par un vrai film et sa photographie ; réserver les cartes aux affiches,
et les surfaces claires aux formulaires. Éviter les statistiques décoratives et les animations
de défilement répétées. La seule entrée animée accompagne l'affiche principale ; les autres
mouvements répondent à l'interaction. Aucun carrousel automatique.

## Accessibilité et dégradation

Navigation clavier, focus visible, lien d'évitement, images dimensionnées, contraste, labels,
états vides utiles, pas de contenu masqué sans JavaScript. `prefers-reduced-motion` désactive les
animations. Menus et filtres restent utilisables sans script. Les imports réseau ne bloquent
ni le démarrage ni la consultation des films déjà enregistrés.
