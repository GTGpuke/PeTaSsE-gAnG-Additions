# Level 0 - Checklist visuelle avant full release

Cette checklist regroupe tout ce qui vaut le coup d'etre re-verifie visuellement en jeu avant de considerer la nouvelle generation du Level 0 comme stable pour une release complete.

Le but n'est pas seulement de verifier que "ca marche", mais aussi que :
- l'immersion tient
- le layout n'a pas derive
- les nouveaux systemes cohabitent proprement
- rien de subtil ne casse la lecture du niveau

## 1. Topologie generale

- Verifier que le labyrinthe garde bien son langage visuel habituel : couloirs, angles, T-junctions, crossroads, dead ends.
- Verifier que les grandes pieces restent des grandes pieces et ne donnent pas l'impression d'avoir remplace les couloirs.
- Verifier que la hausse de frequence des grandes pieces reste acceptable visuellement.
- Verifier qu'il n'y a pas de zones qui donnent l'impression d'un carve "trop ouvert" ou "trop propre".
- Verifier qu'un meme layer ne donne pas l'impression d'etre trop repetitif sur de longues distances.

## 2. Multi-layer

- Verifier que chaque layer a bien un layout different.
- Verifier que les layers ne donnent pas l'impression d'etre des copies decalees les uns des autres.
- Verifier que la hauteur des couches fonctionne bien :
  - sol
  - volume d'air
  - plafond
  - separation pleine entre layers
- Verifier qu'il n'y a pas de fuite visuelle ou sonore entre layers.
- Verifier qu'aucun element mural ou lumineux ne se retrouve pose au mauvais Y a cause du multi-layer.

## 3. Biomes verticaux

- Verifier qu'un biome peut effectivement changer d'un layer a l'autre.
- Verifier qu'un biome restreint a certains layers n'apparait pas sur un layer interdit.
- Verifier que les transitions de biomes restent credibles visuellement.
- Verifier que les palettes de sol et de murs sont bien coherentes avec le biome attendu.
- Verifier que le biome rouge reste assez rare et ne pollue pas le ressenti global du Level 0.

## 4. Eclairage des couloirs normaux

- Verifier que les neons des couloirs gardent un espacement credible.
- Verifier qu'il reste bien au moins une cellule logique 3x3 vide entre deux zones lumineuses quand c'est l'intention visuelle recherchee.
- Verifier qu'il n'y a plus de cas choquants de neons "colles" entre deux grilles 3x3.
- Verifier que les patterns normaux restent plus sobres que ceux des grandes pieces.
- Verifier que l'eclairage normal n'est pas casse aux frontieres de biomes.
- Verifier que l'eclairage normal n'est pas casse aux frontieres entre cellules ROOM_LARGE et couloirs classiques.

## 5. Eclairage des grandes pieces

- Verifier que les grandes pieces utilisent bien une logique specifique distincte des couloirs.
- Verifier que les patterns des grandes pieces restent lisibles :
  - lignes
  - diagonales
  - blackout rare
- Verifier que les grandes pieces sombres restent tres rares et marquantes.
- Verifier que les 3x3 lighted des grandes pieces ne se collent pas d'une maniere visuellement moche.
- Verifier que les patterns ne sont pas trop "casses" localement.
- Verifier que les `PILLAR_ROOM` gardent une logique lumineuse plus aeree que les grandes rooms standard.

## 6. Micro-geometrie

- Verifier que les offsets, recess, alcoves, half-walls et pinch restent des surprises rares.
- Verifier que les couloirs ne sont pas trop "bruites".
- Verifier que les dead ends restent majoritairement normaux.
- Verifier que les angles restent lisibles et ne ressemblent pas a des erreurs de generation.
- Verifier qu'aucune micro-variation ne donne l'impression de casser la circulation du joueur.

## 7. Plinthes

- Verifier que les plinthes sont visibles sans remplacer le mur.
- Verifier qu'elles restent bien legerement devant le mur, sans z-fighting.
- Verifier qu'il n'y a plus de pixel vide visible aux intersections.
- Verifier qu'elles suivent bien les murs visibles et qu'elles ne "fuient" pas sur des faces absurdes.
- Verifier leur lisibilite avec plusieurs papiers peints et plusieurs biomes.

## 8. Interrupteurs et prises

- Verifier qu'ils sont bien a la bonne hauteur sur le mur.
- Verifier que leur taille reste acceptable en situation reelle.
- Verifier que leur petit relief 3D tient sous plusieurs angles de camera.
- Verifier que toutes les faces visibles sont correctement texturees.
- Verifier qu'ils ne flottent pas trop hors du mur et qu'ils ne sont pas trop enfonces.
- Verifier qu'ils restent assez rares pour etre des details, pas des motifs repetitifs.

## 9. Cohabitation des details muraux

- Verifier que plinthes, prises et interrupteurs cohabitent proprement.
- Verifier qu'ils ne se chevauchent pas visuellement.
- Verifier qu'ils ne donnent pas un effet "mur surcharge".
- Verifier qu'ils restent cohérents dans les grandes pieces comme dans les couloirs.

## 10. Chunk borders et couture

- Verifier les raccords aux frontieres de chunks.
- Verifier qu'un pattern lumineux ne saute pas brutalement d'un chunk a l'autre.
- Verifier que les plinthes restent propres aux raccords de chunks.
- Verifier que les biomes cosmetiques ne montrent pas de couture visible.
- Verifier que les grandes pieces ne changent pas de comportement lumineux au passage d'un chunk.

## 11. Determinisme pratique

- Regenerer la meme seed plusieurs fois et verifier que le resultat visuel est identique.
- Verifier qu'un relog ou redemarrage client ne change pas les details poses.
- Verifier que deux zones explorees a des moments differents gardent le meme rendu.

## 12. Performance ressentie

- Verifier qu'il n'y a pas de stutter inhabituel pendant l'exploration.
- Verifier que le multi-layer n'introduit pas de ralentissement visible au chargement.
- Verifier que les details muraux ne semblent pas poser de souci de rendu.

## 13. A garder pour la passe finale

- Revoir une derniere fois le spacing exact des neons dans les couloirs.
- Revoir une derniere fois le spacing exact des neons dans les grandes pieces.
- Decider si la frequence actuelle des grandes pieces est la bonne.
- Reevaluer plus tard les overlays surfaciques si le projet y revient.
- Reprendre plus tard les grosses structures manuelles et les futures connexions verticales uniquement via ces structures controlees.

## 14. Signaux d'alerte avant release

Si l'un de ces points apparait souvent, il vaut mieux retarder la release de la nouvelle generation :

- neons clairement colles entre cellules 3x3
- grandes pieces qui ressemblent a des bugs de carve
- details muraux qui flottent ou z-fightent
- coutures visibles entre chunks
- biomes qui apparaissent sur des layers interdits
- deux layers qui se ressemblent trop
- repetition trop evidente d'un meme motif lumineux ou decoratif
