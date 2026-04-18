# Backrooms Level 0 - Roadmap d'implementation progressive

## But

Refondre le Level 0 de maniere plus professionnelle, en s'appuyant sur la pipeline cible de
`backrooms-level0-pipeline-v6.md`, sans perdre le rendu actuel du generateur monocouche.

Cette roadmap sert de plan de travail incrementable : chaque phase doit etre validable avant de
passer a la suivante.

## Ce qu'il faut absolument preserver de l'existant

- Le ressenti du layout actuel base sur la traduction du script Python de reference.
- La grille logique `3x3` qui donne aujourd'hui les proportions du labyrinthe.
- Le melange `mini-mazes + stamping de rooms`, deja present dans `LevelZeroLayout`.
- Le plafond bas, les neons reguliers et la lecture visuelle immediate du Level 0.
- Les biomes cosmetiques qui changent la palette sans casser la topologie.
- L'optimisation visuelle `wallpaper visible / bedrock invisible` dans les murs.
- Les garde-fous deja presents : invariants de layout, benchmark, cache de layout.

## Etat actuel resume

Le code actuel est simple, monolithique, mais deja sain sur plusieurs points :

- `LevelZeroLayout` porte la logique principale du layout et reste deterministe.
- `LevelZeroChunkGenerator` ecrit directement un chunk monocouche exploitable.
- `LevelZeroSurfaceBiome` est decouple de la topologie.
- Le benchmark `benchmarkLevelZeroGeneration` tourne et donne une baseline tres legere sur la
  logique actuelle.

En revanche, la spec v6 est beaucoup plus ambitieuse que l'implementation actuelle :

- pas encore de pipeline par etapes explicites ;
- pas encore de representation intermediaire taggee ;
- pas encore de separation nette `region cachee / extraction chunk` ;
- pas encore de multi-layer ;
- pas encore de systeme de structures/details/lumieres en modules distincts.

## Ordre recommande

Le plus important est de ne pas faire un "big bang refactor". On garde d'abord le resultat actuel,
puis on remplace les briques une par une.

### Phase 0 - Baseline et verrouillage du rendu actuel

Objectif : figer ce qu'on aime avant de toucher a l'architecture.

Sous-taches :

- Conserver un point d'entree monocouche fonctionnel equivalent a l'actuel.
- Ajouter un document de reference visuelle avec captures de chunks/rooms que tu valides.
- Ajouter une notion de "compat mode" ou "legacy layout" pour comparer avant/apres.
- Stabiliser la commande de validation locale (`tests + benchmark + runClient`).

Validation :

- Le rendu in-game sur quelques seeds de reference est juge conforme.
- Le benchmark actuel reste utilisable comme point de comparaison.
- On sait lister precisement ce qui est "intouchable" dans le feeling actuel.

### Phase 1 - Reorganisation des fichiers sans changement de comportement

Objectif : rendre le code du Level 0 lisible avant d'ajouter des features.

Sous-taches :

- Deplacer la logique du Level 0 vers une arborescence pipeline plus claire.
- Extraire les helpers du `ChunkGenerator` vers des classes dediees.
- Isoler les constantes verticales, spatiales et de seed dans des fichiers distincts.
- Garder exactement le meme resultat de generation.

Arborescence cible conseillee :

```text
world/backrooms/level0/
|- LevelZeroChunkGenerator.java
|- LevelZeroSettings.java
|- coord/
|  |- LevelZeroCoords.java
|  `- LevelZeroVerticalLayout.java
|- noise/
|  `- StageRandom.java
|- layout/
|  |- LevelZeroLayout.java              // facade legacy temporaire
|  |- LevelZeroRegionLayout.java
|  `- LevelZeroChunkSlice.java
|- stage/
|  |- LevelZeroStage.java
|  |- maze/...
|  |- biome/...
|  |- light/...
|  |- detail/...
|  `- structure/...
`- write/
   |- LevelZeroBlockPalette.java
   `- LevelZeroBlockWriter.java
```

Validation :

- Aucune regression visuelle visible.
- Aucune regression des invariants deja testes.
- Les responsabilites deviennent lisibles par dossier.

### Phase 2 - Canon de coordonnees et RNG de pipeline

Objectif : preparer la spec v6 sans encore changer fortement le rendu.

Sous-taches :

- Introduire `StageRandom` conforme a la spec.
- Centraliser toutes les conversions bloc/chunk/cellule/region.
- Remplacer les seeds et mix locaux eparpilles par une API unique.
- Ajouter des tests de determinisme plus stricts.

Validation :

- Meme seed + memes coordonnees = meme resultat a 100 %.
- Aucun `new Random(...)` sauvage dans le pipeline.
- Les conversions negatives et les bords de chunk sont couvertes par tests.

### Phase 3 - Region cachee et extraction par chunk

Objectif : transformer l'existant en vraie pipeline regionale, mais encore avec le rendu actuel.

Sous-taches :

- Introduire `RegionGrid` et `ChunkGrid`.
- Faire calculer le layout macro une fois par region, puis extraire la portion chunk.
- Rebrancher l'algo actuel de maze/rooms comme premiere implementation de la region.
- Conserver la generation monocouche et la palette actuelle.

Validation :

- Le rendu reste tres proche de l'actuel.
- Le cache ne depend plus d'une classe monolithique opaque.
- Les bordures entre chunks restent coherentes.

### Phase 4 - Representation intermediaire taggee

Objectif : ne plus raisonner seulement en `walkable / non-walkable`.

Sous-taches :

- Introduire `CellTag`, `WallType` et les metadonnees minimales.
- Mapper l'ancien layout sur ces tags sans enrichissement agressif au debut.
- Ecrire un `BlockWriter` base sur la grille taggee.
- Garder la logique actuelle comme premiere traduction des tags vers les blocs.

Validation :

- Meme forme generale du labyrinthe.
- Le writer produit le meme type de couloirs, murs, sols et plafonds.
- Les tags peuvent etre debugges visuellement.

### Phase 5 - Etapes explicites 1 a 4 de la spec

Objectif : faire emerger la vraie pipeline en conservant la priorite au rendu valide.

Sous-taches :

- Etape 1 : maze generation.
- Etape 2 : noise geometry.
- Etape 3 : grandes zones.
- Etape 4 : biomes visuels.

Strategie recommandee :

TODO MAJEUR - A reprendre sur la forme :

- Le branchement technique de la micro-geometrie bloc-par-bloc est en place,
  mais sa forme visuelle actuelle ne doit pas etre consideree comme definitive.
- Revoir plus tard la forme exacte des motifs `1x1` visibles en jeu :
  renfoncements, demi-murs, alcoves, murs decales et etranglements ponctuels.
- Revalider en priorite le ressenti visuel global pour conserver le feeling
  actuel du Level 0, avec des anomalies rares mais credibles.

- Commencer par brancher l'ancien algo comme implementation de l'etape 1.
- Ajouter l'etape 2 avec un impact faible et controle.
- Regle generale : sauf contre-indication explicite, les noises et details
  geometriques doivent etre derives a l'echelle du bloc `1x1`, pas a
  l'echelle de la cellule `3x3`.
- Interpreter `noise geometry` comme une couche d'anomalies locales rares
  au-dessus d'une topologie globalement `3x3`, et non comme une deformation
  massive de toute la grille.
- Preserver les couloirs `3x3` comme langage principal du niveau.
- Faire porter les anomalies geometriques finales a l'echelle du bloc
  individuel a l'interieur d'une cellule logique `3x3`.
- Les decisions peuvent rester rares et pilotees au niveau cellule/region,
  mais ce sont des exceptions assumees ; par defaut, leur traduction finale
  doit produire des details `1x1` exploitables par le writer.
- Introduire seulement a de rares endroits des elements du type :
  murs decales, demi-murs, renfoncements, alcoves, etranglements a 1 bloc.
- Garder les etranglements `1-wide` tres ponctuels et exceptionnels.
- Integrer l'etape 3 seulement apres validation visuelle des salles actuelles.
- Ne refaire la logique des biomes qu'apres stabilisation de la topologie.

Validation :

- Chaque etape peut etre activee/desactivee en debug.
- On peut comparer avant/apres par seed fixe.
- Les grandes pieces que tu aimes dans l'existant sont toujours presentes.

### Phase 6 - Lumieres, details et structures

Objectif : enrichir sans casser la lecture du niveau.

Sous-taches :

- Etape 5 : placement des neons comme vrai module.
- Etape 6 : details decoratifs legers.
- Etape 7 : structures prefabriquees simples et rares.
- Instrumentation de perf par etape.

Validation :

- Les neons gardent leur role central dans l'ambiance.
- Pas d'explosion du nombre de sources lumineuses.
- Le niveau reste lisible et ne devient pas "bruite" visuellement.

### Phase 7 - Multi-layer et geographie verticale

Objectif : passer du monocouche actuel au vrai Level 0 multi-layer de la spec.

Sous-taches :

- Introduire le schema vertical canonique.
- Adapter le `DimensionType` (`min_y`, `height`, bedrock inter-layer).
- Generer 3 a 5 layers seedes independamment.
- Ajouter les premiers ancrages verticaux sans structures complexes.

Validation :

- Chaque layer est deterministe.
- Les ecarts verticaux respectent exactement la spec.
- Les connexions verticales tombent sur du traversable des deux cotes.

### Phase 8 - Production et nettoyage final

Objectif : finir la transition et supprimer la dette temporaire.

Sous-taches :

- Supprimer les chemins legacy devenus inutiles.
- Completer la doc technique du nouveau pipeline.
- Ajouter des tests de non-regression plus forts.
- Ajouter un dump debug de grille/tag/biome si utile.

Validation :

- Le pipeline finale est lisible, testable et documentee.
- Le mode legacy n'est retire qu'une fois la nouvelle version validee visuellement.

## Ce que je recommande de faire en premier, concretement

Ordre de mise en oeuvre immediat :

1. Phase 0 : verrouiller les references visuelles et la baseline.
2. Phase 1 : reorganisation du package `level0` sans changer le comportement.
3. Phase 2 : `StageRandom` + coordonnees canoniques + tests.
4. Phase 3 : cache regional propre avec extraction par chunk.

Je conseille de ne pas attaquer le multi-layer tout de suite. Si on change en meme temps la
topologie, le writer, les lumieres et la verticale, on ne saura plus ce qui a casse le rendu que
tu aimes.

## Point d'attention important

Le benchmark actuel est utile, mais il mesure surtout la logique de layout et de sondes de murs du
systeme actuel. Il faut le garder comme baseline, puis ajouter plus tard un benchmark oriente
"pipeline par etape" au lieu de le remplacer immediatement.

TODO FINAL - Revalidation complete apres fin du dev :

- Revalider tout le Level 0 une fois la refonte terminee, et pas seulement
  les sous-parties au fil de l'eau.
- Refaire une passe complete `baseline / noise geometry / debug visuel /
  rendu final` sur plusieurs seeds fixes.
- Reconfirmer que le ressenti global reste conforme a ce qu'on aime dans
  l'existant avant de considerer la pipeline comme finalisee.
