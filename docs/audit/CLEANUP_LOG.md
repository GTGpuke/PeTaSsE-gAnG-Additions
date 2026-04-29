# CLEANUP LOG

## 2026-04-29 - Rangement des documents de suivi

Resume :
- suppression des documents de suivi a la racine du projet
- conservation d'un seul `README.md` a la racine
- creation d'un dossier `docs/audit/` pour les rapports et journaux internes

Fichiers touches :
- deplace : `CLEANUP_LOG.md` -> `docs/audit/CLEANUP_LOG.md`
- deplace : `AUDIT_REPORT.md` -> `docs/audit/AUDIT_REPORT.md`
- deplace : `docs/README.md` -> `docs/INDEX.md`
- cree : `docs/audit/INDEX.md`
- modifie : references dans `README.md`, `.skills/` et `docs/backrooms/to-check/TO CHECK.md`

Justification :
- la racine doit rester lisible et ne contenir que les fichiers d'entree principaux du projet
- `docs/README.md` faisait doublon avec le README racine
- les rapports d'audit et journaux de nettoyage sont des documents internes mieux places sous `docs/audit/`

Risques identifies :
- tres faibles
- changements documentaires uniquement

Reste a faire :
- garder `README.md` comme unique README du projet
- utiliser `docs/INDEX.md` comme index de documentation
- journaliser les prochains nettoyages dans `docs/audit/CLEANUP_LOG.md`

## 2026-04-19 - Batch Level 0 nettoyage sûr

Resume :
- suppression de reliquats internes non utilises dans la couche biome du Level 0
- conservation stricte du comportement actif de generation

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroSurfaceBiome.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLightStageTest.java`

Justification :
- `LevelZeroSurfaceBiome.fromId(...)` n'avait plus aucune reference dans le projet
- `LevelZeroSurfaceBiome.lightInterval(...)` ne participait plus a la logique active de lumiere et n'etait plus utilise que par un test
- le test a ete realigne sur les proprietes encore actives de la trame lumineuse par biome

Risques identifies :
- faibles
- limites a la couche de tests et a une API interne non utilisee

Reste a faire :
- poursuivre le tri des reliquats internes du Level 0
- garder les couches gelees clairement identifiees sans supprimer trop tot ce qui peut servir au pass visuel final

Refuse ou reporte :
- overlays surfaciques reels : reportes
- grosses structures manuelles : reportees
- connexions verticales via petites structures procedurales : abandonnees

## 2026-04-19 - Batch Level 0 nettoyage sûr 2

Resume :
- correction d'une incoherence de signature interne dans `LevelZeroSurfaceBiome`
- suppression d'un import mort dans le generateur du Level 0

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroSurfaceBiome.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroChunkGenerator.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- les constantes d'enum `BASE` et `RED` gardaient encore l'ancien parametre retire dans `LevelZeroSurfaceBiome`, ce qui cassait la compile
- `LevelZeroChunkGenerator` importait encore `LevelZeroVerticalLayout` sans plus l'utiliser

Risques identifies :
- tres faibles
- aucun changement de comportement attendu, seulement remise au propre de la compile et de l'arborescence des imports

Reste a faire :
- poursuivre le tri des reliquats purement internes
- conserver les signatures publiques actives tant qu'elles n'ont pas ete explicitement depreciees ou remplacees

Refuse ou reporte :
- suppression de signatures publiques internes non referencees : reportee tant qu'une passe API dediee n'a pas ete validee

## 2026-04-20 - Batch Level 0 arborescence debug 1

Resume :
- sortie de la palette debug des structures rares hors du package `write`
- separation plus nette entre ecriture normale et visualisation debug

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroStructureDebugPalette.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureWriteStage.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureDebugPalette.java`

Justification :
- `LevelZeroStructureDebugPalette` ne participe pas a l'ecriture normale des chunks ; elle ne fournit que des blocs de visualisation
- la sortir de `write` rend l'arborescence plus lisible et rapproche le code de la separation cible entre logique active et outils de debug

Risques identifies :
- tres faibles
- limites aux imports de la couche debug structures

Deplacements :
- `world/backrooms/level0/write/LevelZeroStructureDebugPalette.java`
  -> `world/backrooms/level0/debug/LevelZeroStructureDebugPalette.java`

Reste a faire :
- appliquer le meme tri aux autres palettes debug encore rangees dans `write`
- continuer la separation `actif / gele / debug` sans toucher a la logique de generation

Refuse ou reporte :
- deplacement des autres palettes debug : reporte au batch suivant pour rester atomique

## 2026-04-20 - Batch Level 0 arborescence debug 2

Resume :
- sortie de la palette debug des petits details muraux hors du package `write`
- renforcement de la separation entre rendu normal et outils de visualisation

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroWallPropDebugPalette.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroWallPropWriteStage.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroWallPropDebugPalette.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroWallPropDebugPalette` ne sert qu'a choisir des blocs de visualisation pour les prises et interrupteurs debug
- la garder dans `write` melangeait la couche d'ecriture active avec une aide purement visuelle

Risques identifies :
- tres faibles
- limites aux imports du path debug des fixtures murales

Deplacements :
- `world/backrooms/level0/write/LevelZeroWallPropDebugPalette.java`
  -> `world/backrooms/level0/debug/LevelZeroWallPropDebugPalette.java`

Reste a faire :
- finir le tri des palettes debug restantes encore dans `write`
- poursuivre ensuite le rangement des couches gelees dans une arborescence plus lisible

Refuse ou reporte :
- deplacement de la palette debug des surface details : reporte au batch suivant pour rester atomique

## 2026-04-20 - Batch Level 0 arborescence debug 3

Resume :
- sortie de la palette debug des details surfaciques hors du package `write`
- cloture du premier tri des palettes de visualisation du Level 0

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroSurfaceDetailDebugPalette.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroSurfaceDetailWriteStage.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroSurfaceDetailDebugPalette.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroSurfaceDetailDebugPalette` ne contient aucun comportement de generation ; elle ne sert qu'a peindre les patches connectes en mode debug
- ce deplacement termine un groupe coherent de classes de visualisation qui n'avaient plus leur place dans `write`

Risques identifies :
- tres faibles
- limites aux imports de la couche debug des details surfaciques

Deplacements :
- `world/backrooms/level0/write/LevelZeroSurfaceDetailDebugPalette.java`
  -> `world/backrooms/level0/debug/LevelZeroSurfaceDetailDebugPalette.java`

Reste a faire :
- trier ensuite les classes `write` encore provisoires ou gelees
- proposer un rangement lisible des couches `surface details` gelees sans casser leur conservation

Refuse ou reporte :
- deplacement des write stages eux-memes : reporte tant qu'on n'a pas fini le tri des auxiliaires debug

## 2026-04-20 - Batch Level 0 arborescence debug 4

Resume :
- sortie du write stage purement debug des structures rares hors du package `write`
- maintien de l'ecriture normale dans `write`, avec import mis a jour

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroStructureWriteStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroBlockWriter.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroStructureWriteStageTest.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroStructureWriteStage` ne fait qu'afficher une couche semantique debug ; il n'ecrit aucune vraie structure gameplay
- le garder dans `write` entretenait une confusion entre pipeline d'ecriture normal et rendu de diagnostic

Risques identifies :
- faibles
- limites aux imports du writer et au test dedie de cette etape debug

Deplacements :
- `world/backrooms/level0/write/LevelZeroStructureWriteStage.java`
  -> `world/backrooms/level0/debug/LevelZeroStructureWriteStage.java`

Reste a faire :
- appliquer le meme tri a `LevelZeroSurfaceDetailWriteStage` si on veut pousser la separation debug plus loin
- garder `LevelZeroWallPropWriteStage` dans `write` tant que la couche murale melange encore rendu reel et fixtures debug

Refuse ou reporte :
- deplacement de `LevelZeroWallPropWriteStage` : reporte car le stage porte encore une partie active du rendu mural

## 2026-04-20 - Batch Level 0 arborescence debug 5

Resume :
- sortie du write stage purement debug des surface details hors du package `write`
- confirmation que la couche de details surfaciques gelees reste isolee du pipeline normal

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroSurfaceDetailWriteStage.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroSurfaceDetailWriteStageTest.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroSurfaceDetailWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroSurfaceDetailWriteStage` n'est plus branche dans `LevelZeroBlockWriter` et n'existe plus que comme rendu debug d'une couche volontairement gelee
- le garder dans `write` entretenait une confusion entre etape potentielle du pipeline et outil de visualisation de developpement

Risques identifies :
- tres faibles
- limites au test dedie et aux imports de cette etape debug

Deplacements :
- `world/backrooms/level0/write/LevelZeroSurfaceDetailWriteStage.java`
  -> `world/backrooms/level0/debug/LevelZeroSurfaceDetailWriteStage.java`

Reste a faire :
- s'attaquer ensuite au prochain vrai palier de rangement des classes actives
- garder `LevelZeroWallPropWriteStage` dans `write` tant qu'il melange encore rendu reel et fixtures debug

Refuse ou reporte :
- deplacement de `LevelZeroWallPropWriteStage` : reporte car comportement encore partiellement actif

## 2026-04-20 - Batch Level 0 arborescence stage 1

Resume :
- debut de separation du sous-ensemble regional de `stage`
- deplacement de l'etape legacy de layout regional vers un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/region/LevelZeroLegacyRegionLayoutStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroRegionGrid.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroRegionLayoutTest.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyRegionLayoutStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- cette classe est purement regionale et constitue un bon premier candidat pour commencer a structurer `stage` sans toucher a la logique
- elle reste fonctionnellement identique ; seul son emplacement evolue pour rendre l'arborescence plus lisible

Risques identifies :
- tres faibles
- limites aux imports du grid regional et du test dedie

Deplacements :
- `world/backrooms/level0/stage/LevelZeroLegacyRegionLayoutStage.java`
  -> `world/backrooms/level0/stage/region/LevelZeroLegacyRegionLayoutStage.java`

Reste a faire :
- poursuivre par l'etape regionale de walkability si cette premiere extraction reste stable
- separer ensuite plus proprement les autres familles de stages par responsabilite

Refuse ou reporte :
- deplacement simultane de plusieurs classes regionales : reporte pour rester dans un batch atomique

## 2026-04-20 - Batch Level 0 arborescence stage 2

Resume :
- deplacement de l'etape legacy de walkability regionale vers le meme sous-package `stage/region`
- completion du premier duo de stages regionaux sans changement de logique

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/region/LevelZeroLegacyRegionWalkabilityStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroRegionGrid.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroRegionLayoutTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLegacyLayoutPipelineTest.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyRegionWalkabilityStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- cette classe est la contrepartie naturelle de `LevelZeroLegacyRegionLayoutStage`
- les regrouper dans `stage/region` rend la partie regionale du pipeline plus lisible sans toucher a son comportement

Risques identifies :
- tres faibles
- limites aux imports du grid regional et des tests qui instancient l'etape de walkability

Deplacements :
- `world/backrooms/level0/stage/LevelZeroLegacyRegionWalkabilityStage.java`
  -> `world/backrooms/level0/stage/region/LevelZeroLegacyRegionWalkabilityStage.java`

Reste a faire :
- choisir ensuite une autre famille de stages a isoler proprement
- probablement `light` ou `biome`, toujours en petits batchs

Refuse ou reporte :
- refonte de l'interface `LevelZeroRegionStage` : reportee car ce n'est plus du simple rangement

## 2026-04-20 - Batch Level 0 arborescence stage 3

Resume :
- debut de separation de la famille `light`
- deplacement de `LevelZeroLightStage` vers un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/light/LevelZeroLightStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLightStageTest.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLightStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroLightStage` est un bloc metier autonome et bien isole, bon candidat pour commencer la famille `light`
- le comportement reste strictement identique ; seul l'emplacement et les imports changent

Risques identifies :
- tres faibles
- limites aux imports de la pipeline legacy et au test dedie a la lumiere

Deplacements :
- `world/backrooms/level0/stage/LevelZeroLightStage.java`
  -> `world/backrooms/level0/stage/light/LevelZeroLightStage.java`

Reste a faire :
- choisir si l'on deplace ensuite les auxiliaires de pipeline lies a la lumiere ou si l'on ouvre la famille `biome`
- continuer a garder `LevelZeroLegacyLayoutPipeline` a sa place tant qu'il orchestre plusieurs familles

Refuse ou reporte :
- deplacement de `LevelZeroLegacyLayoutPipeline` : reporte car il reste volontairement transversal

## 2026-04-20 - Batch Level 0 arborescence stage 4

Resume :
- ouverture de la famille `biome`
- deplacement de `LevelZeroSurfaceBiomeStage` vers un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/biome/LevelZeroSurfaceBiomeStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroSurfaceBiomeStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroSurfaceBiomeStage` est un bloc metier simple et autonome, ideal pour commencer la famille `biome`
- aucun comportement n'est modifie ; seul l'emplacement et les imports changent

Risques identifies :
- tres faibles
- limites aux imports de la pipeline legacy

Deplacements :
- `world/backrooms/level0/stage/LevelZeroSurfaceBiomeStage.java`
  -> `world/backrooms/level0/stage/biome/LevelZeroSurfaceBiomeStage.java`

Reste a faire :
- choisir si l'on continue la famille `biome` ou si l'on revient plus tard sur les stages transversaux
- garder la pipeline legacy comme point d'orchestration stable

Refuse ou reporte :
- deplacement simultane de plusieurs stages `biome` : reporte pour rester atomique

## 2026-04-20 - Batch Level 0 arborescence stage 5

Resume :
- ouverture de la famille `geometry`
- deplacement de `LevelZeroLegacyGeometryStage` vers un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/geometry/LevelZeroLegacyGeometryStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroGeometryStageTest.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyGeometryStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroLegacyGeometryStage` est un bloc metier autonome qui porte uniquement la couche de micro-anomalies geometriques
- le deplacer rend l'arborescence plus lisible sans toucher aux probabilites ni aux regles appliquees

Risques identifies :
- tres faibles
- limites aux imports de la pipeline legacy et du test dedie

Deplacements :
- `world/backrooms/level0/stage/LevelZeroLegacyGeometryStage.java`
  -> `world/backrooms/level0/stage/geometry/LevelZeroLegacyGeometryStage.java`

Reste a faire :
- faire la meme chose pour `LevelZeroLegacyMicroPatternStage` si on veut completer le sous-ensemble geometry/detail
- garder les classes transversales a leur place tant qu'elles orchestrent plusieurs familles

Refuse ou reporte :
- deplacement simultane de `LevelZeroLegacyMicroPatternStage` : reporte pour rester dans un batch atomique

## Batch 2026-04-20 - Stage geometry/detail 2

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`
- `src/test/java/com/petassegang/addons/**`

Resume :
- completion du sous-ensemble `geometry/detail`
- deplacement de `LevelZeroLegacyMicroPatternStage` vers un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/geometry/LevelZeroLegacyMicroPatternStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroMicroPatternStageTest.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroLegacyMicroPatternStage` appartient au meme sous-domaine metier que `LevelZeroLegacyGeometryStage`
- le deplacer rend l'arborescence plus lisible sans toucher a la projection 3x3 ni a la logique des motifs

Risques identifies :
- tres faibles
- limites aux imports de la pipeline legacy et du test dedie

Deplacements :
- `world/backrooms/level0/stage/LevelZeroLegacyMicroPatternStage.java`
  -> `world/backrooms/level0/stage/geometry/LevelZeroLegacyMicroPatternStage.java`

## Batch 2026-04-20 - Stage topology

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`
- `src/test/java/com/petassegang/addons/**`

Resume :
- ouverture de la famille `topology`
- deplacement conjoint de `LevelZeroLargeRoomStage` et `LevelZeroLegacyTopologyStage`

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/topology/LevelZeroLargeRoomStage.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/topology/LevelZeroLegacyTopologyStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroTopologyStageTest.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- ces deux classes forment deja un duo metier coherent dans la pipeline legacy
- les regrouper clarifie la lecture sans changer le marquage `largeRoom` ni la derivation de topologie

Risques identifies :
- tres faibles
- limites aux imports de la pipeline legacy et du test dedie

Deplacements :
- `world/backrooms/level0/stage/LevelZeroLargeRoomStage.java`
  -> `world/backrooms/level0/stage/topology/LevelZeroLargeRoomStage.java`
- `world/backrooms/level0/stage/LevelZeroLegacyTopologyStage.java`
  -> `world/backrooms/level0/stage/topology/LevelZeroLegacyTopologyStage.java`

## Batch 2026-04-20 - Stage region 3

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`

Resume :
- rangement de `LevelZeroSectorCacheKeyStage` dans la famille regionale

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/region/LevelZeroSectorCacheKeyStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- cette classe ne porte qu'une cle de cache de secteur
- son role est regional et elle n'est utilisee que par la pipeline legacy

Risques identifies :
- tres faibles
- limites a l'import dans la pipeline

Deplacements :
- `world/backrooms/level0/stage/LevelZeroSectorCacheKeyStage.java`
  -> `world/backrooms/level0/stage/region/LevelZeroSectorCacheKeyStage.java`

## Batch 2026-04-20 - Package docs

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`

Resume :
- ajout de `package-info.java` aux familles `region` et `topology`

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/region/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/topology/package-info.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- ces sous-packages existent maintenant comme vraies familles metier
- les documenter directement dans l'arborescence aide a garder le rangement lisible sans toucher au code actif

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Optimisation structurelle sure du writer

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- suppression d'ecritures d'air redondantes dans le stage `interior`
- petite simplification de l'initialisation de colonne dans `foundation`

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroInteriorWriteStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroFoundationWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `writeColumnSample()` evitait deja les colonnes traversables dans `interior`, mais `writeChunkColumn()` reecrivait encore de l'air sur toute la hauteur interieure
- le chunk sort vide sur cette tranche avant ecriture, donc reecrire explicitement de l'air sur les colonnes walkable etait redondant
- `Arrays.fill()` remplace une boucle equivalente dans `foundation` sans changer le contenu final

Risques identifies :
- tres faibles ; le comportement reste identique tant que l'interieur des colonnes traversables est bien vide avant le stage `interior`, ce qui est le contrat actuel du writer

Deplacements :
- aucun

## Batch 2026-04-20 - Profilage fin du writer Level 0

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- ajout d'un decorateur de profiling pour mesurer chaque write stage du Level 0 separement

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroProfiledWriteStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroBlockWriter.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- les logs precedents montraient que le cout principal etait dans `write_chunk`
- il fallait distinguer fondation, sol, interieur, plafond, props et lumieres sans changer le moindre bloc pose
- le decorateur garde exactement les memes delegates et ne fait qu'ajouter des sections de monitoring

Risques identifies :
- nuls en mode normal ; le monitor reste opt-in
- faibles en mode debug perf, avec un surcout volontaire de mesure

Deplacements :
- aucun

## Batch 2026-04-20 - Rangement docs dimension

Scope :
- `docs/**`

Resume :
- deplacement du template de `dimension_type` multi-layer hors du dossier `docs/backrooms` vers un dossier `docs/dimensions`

Fichiers touches :
- deplace : `docs/backrooms/backrooms-level0-dimension-type-multilayer.json`
  -> `docs/dimensions/backrooms-level0-dimension-type-multilayer.json`
- modifie : `docs/DIMENSIONS.md`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- ce fichier documente un format de `dimension_type`, pas une etape de roadmap ou une spec interne du pipeline Backrooms
- le garder dans `docs/backrooms` melangeait la doc de gameplay/generation avec la doc de dimension

Risques identifies :
- nuls ; c'est un document de reference non charge par le jeu

Deplacements :
- `docs/backrooms/backrooms-level0-dimension-type-multilayer.json`
  -> `docs/dimensions/backrooms-level0-dimension-type-multilayer.json`

## Batch 2026-04-20 - Monitoring de performance in-game

Scope :
- `src/main/java/com/petassegang/addons/**`
- `build.gradle`

Resume :
- ajout d'un moniteur de performance opt-in avec logs periodiques, synthese F3 et sections profilees autour de la generation du Level 0

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/debug/performance/ModPerformanceMonitor.java`
- cree : `src/main/java/com/petassegang/addons/client/debug/performance/ClientPerformanceMonitorHook.java`
- modifie : `src/main/java/com/petassegang/addons/config/ModConfig.java`
- modifie : `src/main/java/com/petassegang/addons/PeTaSsEgAnGAdditionsMod.java`
- modifie : `src/main/java/com/petassegang/addons/PeTaSsEgAnGAdditionsClientMod.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroChunkGenerator.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroLayout.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroRegionGrid.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroBlockWriter.java`
- modifie : `build.gradle`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- donner un moyen concret de reperer les hotspots, les appels trop frequents et l'evolution CPU/RAM directement en jeu
- garder ce systeme entierement opt-in pour ne pas introduire de regression en usage normal
- profiler en priorite les gros points d'entree du Level 0 plutot que d'instrumenter agressivement les boucles fines

Risques identifies :
- faible sur la perf normale car tout est desactive par defaut
- charge supplementaire limitee quand le mode est actif ; ce mode est justement destine au diagnostic
- le GPU n'est pas mesurable proprement via l'API Java/Fabric seule, donc le suivi client utilise les FPS comme proxy de rendu

Deplacements :
- aucun

## Batch 2026-04-20 - Arborescence writer profiling

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- extraction du decorateur de profiling du writer dans un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/profiling/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/profiling/LevelZeroProfiledWriteStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroBlockWriter.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroProfiledWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le decorateur de profiling ne participe pas a la logique d'ecriture elle-meme ; il ajoute seulement des scopes de mesure
- le sortir du package racine `write` clarifie la difference entre pipeline actif et instrumentation optionnelle

Risques identifies :
- tres faibles
- limites aux imports du writer et au package du decorateur

Deplacements :
- `world/backrooms/level0/write/LevelZeroProfiledWriteStage.java`
  -> `world/backrooms/level0/write/profiling/LevelZeroProfiledWriteStage.java`

## Batch 2026-04-20 - Arborescence writer structures

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`
- `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/**`
- `src/test/java/com/petassegang/addons/**`

Resume :
- extraction de la couche semantique des structures rares dans un sous-package dedie du writer

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/LevelZeroStructureResolver.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/LevelZeroStructureProfile.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/LevelZeroStructureKind.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/LevelZeroStructureCellRole.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/LevelZeroStructureGameplayPointKind.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroResolvedColumn.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroResolvedColumnResolver.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroStructureDebugPalette.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroStructureWriteStage.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLightWriteStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroStructureResolverTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroStructureWriteStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroWallPropWriteStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/LevelZeroPerformanceCheck.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureResolver.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureProfile.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureKind.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureCellRole.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureGameplayPointKind.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- cette famille porte une couche semantique autonome, distincte des stages d'ecriture purs et du reste des details muraux
- la sortir du package racine `write` clarifie immediatement ce qui releve de la resolution semantique des structures rares

Risques identifies :
- faibles
- limites aux imports du resolveur de colonnes, du debug structures et des tests relies a cette couche

Deplacements :
- `world/backrooms/level0/write/LevelZeroStructureResolver.java`
  -> `world/backrooms/level0/write/structure/LevelZeroStructureResolver.java`
- `world/backrooms/level0/write/LevelZeroStructureProfile.java`
  -> `world/backrooms/level0/write/structure/LevelZeroStructureProfile.java`
- `world/backrooms/level0/write/LevelZeroStructureKind.java`
  -> `world/backrooms/level0/write/structure/LevelZeroStructureKind.java`
- `world/backrooms/level0/write/LevelZeroStructureCellRole.java`
  -> `world/backrooms/level0/write/structure/LevelZeroStructureCellRole.java`
- `world/backrooms/level0/write/LevelZeroStructureGameplayPointKind.java`
  -> `world/backrooms/level0/write/structure/LevelZeroStructureGameplayPointKind.java`

## Batch 2026-04-20 - Arborescence layout sector

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/**`
- `src/main/java/com/petassegang/addons/world/backrooms/level0/**`
- `src/test/java/com/petassegang/addons/**`

Resume :
- extraction du sous-ensemble sectoriel du layout historique dans un sous-package dedie

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/sector/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/sector/LevelZeroSectorData.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/sector/LevelZeroSectorGenerator.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/sector/LevelZeroSectorWalkabilitySampler.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/sector/LevelZeroSectorRoomKind.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroLayout.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroCellState.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroChunkSlice.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroRegionGrid.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroRegionWalkability.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroWalkabilitySampler.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroCellEvaluation.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroLegacyLayoutPipeline.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/light/LevelZeroLightStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/region/LevelZeroLegacyRegionWalkabilityStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/structure/LevelZeroStructureResolver.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroCellStateTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLayoutTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLegacyLayoutPipelineTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLightStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroLightWriteStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroRegionLayoutTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroSectorGeneratorTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroStructureResolverTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroStructureWriteStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroSurfaceDetailResolverTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroTopologyStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroWallPropWriteStageTest.java`
- modifie : `src/test/java/com/petassegang/addons/LevelZeroPerformanceCheck.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroSectorData.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroSectorGenerator.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroSectorWalkabilitySampler.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroSectorRoomKind.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le layout historique melangeait encore les briques sectorielles avec les structures de layout plus generales
- les regrouper sous `layout/sector` rend plus lisible ce qui releve du cache de secteurs et de la source de walkability brute

Risques identifies :
- faibles
- limites aux imports du layout, des stages qui relisent `roomKind` et des tests relies a cette semantique

Deplacements :
- `world/backrooms/level0/layout/LevelZeroSectorData.java`
  -> `world/backrooms/level0/layout/sector/LevelZeroSectorData.java`
- `world/backrooms/level0/layout/LevelZeroSectorGenerator.java`
  -> `world/backrooms/level0/layout/sector/LevelZeroSectorGenerator.java`
- `world/backrooms/level0/layout/LevelZeroSectorWalkabilitySampler.java`
  -> `world/backrooms/level0/layout/sector/LevelZeroSectorWalkabilitySampler.java`
- `world/backrooms/level0/layout/LevelZeroSectorRoomKind.java`
  -> `world/backrooms/level0/layout/sector/LevelZeroSectorRoomKind.java`

## Batch 2026-04-20 - Optimisation sure foundation writer

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`
- `src/test/java/com/petassegang/addons/**`

Resume :
- reduction des lectures repetitives dans le stage `foundation`
- ajout d'un test dedie pour verrouiller l'ecriture des couches fixes legacy

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroFoundationWriteStage.java`
- cree : `src/test/java/com/petassegang/addons/BackroomsLevelZeroFoundationWriteStageTest.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `foundation` reste le hotspot principal du writer dans les logs de performance
- cette passe ne change aucune coordonnee ni aucun bloc ecrit ; elle met seulement en cache les etats invariants du stage et reduit les appels repetes a `verticalSlice()`
- le nouveau test verrouille explicitement la bedrock basse et le sous-sol technique legacy

Risques identifies :
- tres faibles
- limites au stage `foundation`, avec un test unitaire qui couvre la traduction minimale attendue

Deplacements :
- aucun

## Batch 2026-04-20 - Suppression du relicat mort LevelZeroLayoutStages

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/**`

Resume :
- suppression d'un ancien helper de mini-stages qui n'etait plus utilise nulle part

Fichiers touches :
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/LevelZeroLayoutStages.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- `LevelZeroLayoutStages` n'avait plus aucune reference dans le code actif, les tests, les docs ou le build
- ses responsabilites historiques sont aujourd'hui deja couvertes par les stages explicites et la facade regionale
- le garder donnait un faux signal d'API encore valide alors qu'il etait devenu un relicat

Risques identifies :
- nuls a tres faibles ; toute reference oubliee aurait casse la compile immediatement

Deplacements :
- aucun

## Batch 2026-04-20 - Nettoyage assets orphelins des anciens wall props

Scope :
- `src/main/resources/assets/petasse_gang_additions/**`

Resume :
- suppression d'anciens assets de wall props qui n'etaient plus references nulle part

Fichiers touches :
- supprime : `src/main/resources/assets/petasse_gang_additions/models/block/level_zero_wood_trim_east.json`
- supprime : `src/main/resources/assets/petasse_gang_additions/models/block/level_zero_wood_trim_north.json`
- supprime : `src/main/resources/assets/petasse_gang_additions/models/block/level_zero_wood_trim_south.json`
- supprime : `src/main/resources/assets/petasse_gang_additions/models/block/level_zero_wood_trim_west.json`
- supprime : `src/main/resources/assets/petasse_gang_additions/textures/block/level_zero_wood_trim.png`
- supprime : `src/main/resources/assets/petasse_gang_additions/textures/block/level_zero_switch.png`
- supprime : `src/main/resources/assets/petasse_gang_additions/textures/block/level_zero_outlet.png`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- la variante `wood_trim` a ete abandonnee et `LevelZeroBaseboardStyle` ne garde plus que `NONE` et `WHITE`
- les textures `level_zero_switch.png` et `level_zero_outlet.png` ne sont plus utilisees depuis la separation `fixture_plate` / `switch_detail` / `outlet_detail`
- aucun de ces assets n'etait encore reference par le code, les modeles JSON actifs ou la documentation

Risques identifies :
- tres faibles ; si un vieux chemin de resource oublie reapparaissait, le chargement de modele casserait immediatement au demarrage, ce qui sera couvert par la verification de compile/tests puis par le prochain run client

Deplacements :
- aucun

## Batch 2026-04-20 - Nettoyage ressources overlays gelees

Scope :
- `src/main/resources/assets/petasse_gang_additions/models/block/**`

Resume :
- suppression des deux modeles JSON d'overlay mural devenus orphelins

Fichiers touches :
- supprime : `src/main/resources/assets/petasse_gang_additions/models/block/level_zero_wallpaper_damp_overlay.json`
- supprime : `src/main/resources/assets/petasse_gang_additions/models/block/level_zero_wallpaper_dirt_overlay.json`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le runtime client du wallpaper ne bake plus ces overlays
- les modeles ressources n'etaient plus references nulle part

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Simplification wallpaper overlays gelees

Scope :
- `src/main/java/com/petassegang/addons/client/model/backrooms/**`

Resume :
- retrait du plumbing runtime des overlays muraux `surface details` dans le renderer du wallpaper

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/client/model/backrooms/LevelZeroWallpaperModelHandler.java`
- modifie : `src/main/java/com/petassegang/addons/client/model/backrooms/LevelZeroWallpaperBakedModel.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- la couche `surface details` est gelee et le resolver mural renvoie aujourd'hui toujours `NONE`
- garder ce plumbing dans le runtime client ajoutait de la complexite sans effet visuel reel
- le TODO reste present pour rappeler ou rebrancher les overlays si la couche revient

Risques identifies :
- faibles
- limites au rendu client du wallpaper, sans impact sur la generation ni sur les details muraux actifs

Deplacements :
- aucun

## Batch 2026-04-20 - Simplification resolved column

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- suppression du helper redondant `LevelZeroResolvedColumn.walkable()`

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroResolvedColumn.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroLightWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le helper ne faisait que relayer `material().walkable()`
- il n'etait utilise qu'a un seul endroit et alourdissait inutilement l'API de la colonne resolue

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Clarification couches gelees

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- clarification du statut des couches conservees mais gelees (`surface details`)
- clarification du statut de fondation semantique du `structure resolver`

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureResolver.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroSurfaceDetailProfile.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroConnectedDetailVariant.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroSurfaceDetailResolver.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- il ne restait plus de code mort evident dans ces sous-ensembles
- la bonne action etait donc de rendre leur statut explicite plutot que de supprimer une base encore utile

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Nettoyage structure resolver

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`
- `src/test/java/com/petassegang/addons/**`

Resume :
- suppression du parametre `largeRoom` devenu mort dans `LevelZeroStructureResolver`

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroStructureResolver.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroResolvedColumnResolver.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroStructureResolverTest.java`
- modifie : `src/test/java/com/petassegang/addons/LevelZeroPerformanceCheck.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le resolver n'utilisait plus du tout `largeRoom`
- garder ce parametre donnait un faux signal sur la dependance reelle de la couche structures

Risques identifies :
- faibles
- limites aux appels du resolver et aux tests associes

Deplacements :
- aucun

## Batch 2026-04-20 - Nettoyage wall props mort

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- suppression des deux helpers `writeBaseboard(...)` devenus vides dans le writer des `wall props`

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroWallPropWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- les plinthes ne sont plus ecrites comme blocs depuis longtemps
- les deux helpers prives ne faisaient plus rien et ne servaient qu'a porter un TODO

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Nettoyage surface details debug mort

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/**`
- `src/main/java/com/petassegang/addons/config/**`
- `src/test/java/com/petassegang/addons/**`
- `build.gradle`

Resume :
- suppression du vieux rendu debug des `surface details`, plus branche nulle part dans le runtime
- suppression de son toggle de config et des restes Gradle associes

Fichiers touches :
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroSurfaceDetailDebugPalette.java`
- supprime : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroSurfaceDetailWriteStage.java`
- supprime : `src/test/java/com/petassegang/addons/BackroomsLevelZeroSurfaceDetailWriteStageTest.java`
- modifie : `src/main/java/com/petassegang/addons/config/ModConfig.java`
- modifie : `src/test/java/com/petassegang/addons/BackroomsLevelZeroDebugConfigTest.java`
- modifie : `build.gradle`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le stage debug des `surface details` n'etait plus branche dans `LevelZeroBlockWriter`
- son toggle de config ne pilotait plus aucun comportement runtime
- la couche `surface details` utile reste conservee via `LevelZeroSurfaceDetailResolver`, mais le vieux rendu debug etait devenu mort

Risques identifies :
- faibles
- limites au debug supprime et a ses restes de configuration

Deplacements :
- aucun

## Batch 2026-04-20 - Commentaires stage transversaux

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`

Resume :
- enrichissement des commentaires des types transversaux du package racine `stage`

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroCellContext.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroRegionContext.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroCellStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroRegionStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/LevelZeroCellEvaluation.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- ces types servent de contrat commun entre plusieurs sous-familles du pipeline
- les commenter plus precisement aide a comprendre les frontieres sans toucher a la logique

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Commentaires stage racine

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`
- `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/**`
- `src/main/java/com/petassegang/addons/world/backrooms/level0/write/**`

Resume :
- ajout d'un `package-info.java` au package racine `stage`
- harmonisation de quelques commentaires restants autour de `column sample`

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroStructureWriteStage.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/LevelZeroBlockWriter.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- le package racine `stage` avait encore un role implicite, non documente
- quelques commentaires utilisaient encore un vocabulaire mixte peu propre

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Commentaires packages

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/**`

Resume :
- harmonisation des `package-info.java` en francais
- petite relecture d'un commentaire de stage debug provisoire

Fichiers touches :
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/coord/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/biome/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/geometry/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/light/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/region/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/topology/package-info.java`
- modifie : `src/main/java/com/petassegang/addons/world/backrooms/level0/debug/LevelZeroSurfaceDetailWriteStage.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- les commentaires doivent rester coherents avec l'arborescence nettoyee
- le francais doit etre uniforme et explicite sur les zones touchees

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Package docs 3

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/**`

Resume :
- ajout de `package-info.java` aux packages centraux `coord`, `layout` et `write`

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/coord/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/layout/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/write/package-info.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- ces packages portent les briques principales de la generation et de l'ecriture
- les documenter aide a lire l'arborescence sans modifier une seule classe active

Risques identifies :
- nuls

Deplacements :
- aucun

## Batch 2026-04-20 - Package docs 2

Scope :
- `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/**`

Resume :
- ajout de `package-info.java` aux familles `biome`, `light` et `geometry`

Fichiers touches :
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/biome/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/light/package-info.java`
- cree : `src/main/java/com/petassegang/addons/world/backrooms/level0/stage/geometry/package-info.java`
- modifie : `docs/audit/CLEANUP_LOG.md`

Justification :
- ces sous-packages existent deja comme familles metier stables
- les documenter directement dans l'arborescence facilite la lecture sans toucher au code actif

Risques identifies :
- nuls

Deplacements :
- aucun
## 2026-04-20 - Batch doc backrooms TODOs

- Relecture croisee de `docs/backrooms/backrooms-level0-roadmap.md` et
  `docs/backrooms/backrooms-level0-pipeline-v6.md` contre l'etat reel du code.
- Mise a jour de la section `Etat actuel resume` de la roadmap pour refleter :
  pipeline explicite, separation region/chunk, multi-layer actif, writer dedie
  et couches runtime deja branchees.
- Ajout de `TODO` explicites utiles plus tard dans la roadmap pour :
  structures manuelles, connexions verticales controlees, biomes par layer plus
  declaratifs, dumps debug et consolidation du monitor de performance.
- Ajout dans la spec pipeline d'une note de conformite avec l'implementation
  actuelle et de `TODO` explicites pour :
  connexions verticales via grosses structures manuelles,
  bornes verticales revalidees selon les contraintes Minecraft reelles,
  branchement futur des prefabs manuels et regles anti-collage.
- Aucun changement de code runtime ; documentation uniquement.
## 2026-04-20 - Batch skills conformité projet

- Relecture des skills du projet dans `.skills/`.
- Mise a jour de `.skills/project-conventions/SKILL.md` pour refleter plus
  fidelement l'arborescence actuelle du Level 0 (`coord`, `layout/sector`,
  `stage/*`, `write/profiling`, `write/structure`, `debug`).
- Ajout dans ce skill des garde-fous backrooms deja utilises dans le projet :
  relecture roadmap/pipeline/TO CHECK et journalisation dans `docs/audit/CLEANUP_LOG.md`.
- Mise a jour de `.skills/pre-push-audit/SKILL.md` pour l'adapter au workflow
  reel du projet :
  shell Windows/PowerShell,
  commandes Gradle fiables (`compileTestJava --stacktrace`, `test --stacktrace`,
  `runClient`),
  garde-fous backrooms et non-regression.
- Aucun changement de code runtime ; documentation/outillage uniquement.
## 2026-04-20 - Batch commentaires architecture Level 0

- Ajout d'un `package-info.java` racine pour `world/backrooms/level0` avec :
  resume global de la pipeline,
  ordre de lecture conseille,
  definition explicite de `Legacy` comme coeur stable de generation.
- Renforcement des Javadocs d'entree sur :
  `LevelZeroChunkGenerator`,
  `LevelZeroLayout`,
  `LevelZeroRegionGrid`,
  `LevelZeroSectorGenerator`,
  `LevelZeroLegacyLayoutPipeline`.
- Clarification explicite de la hierarchie `secteur -> region -> chunk` dans les
  `package-info.java` de `layout`, `layout/sector`, `stage` et `stage/region`.
- Ajout d'un commentaire sur `SECTOR_COLS / SECTOR_ROWS` pour expliquer leur
  origine historique `1920x1080 / 8`.
- Aucun changement de logique runtime ; commentaires et structure documentaire
  uniquement.
## 2026-04-20 - Batch commentaires layout vers writer Level 0

- Renforcement des commentaires/Javadocs sur la chaine
  `RegionLayout -> ChunkSlice -> ResolvedColumn -> BlockWriter`.
- Fichiers clarifies :
  `LevelZeroBlockWriter`,
  `LevelZeroResolvedColumnResolver`,
  `LevelZeroChunkSlice`,
  `LevelZeroRegionLayout`,
  `LevelZeroRegionLayoutBuilder`,
  `LevelZeroRegionWalkability`,
  `write/package-info.java`.
- Objectif : rendre plus evident le passage de la logique cellule-par-cellule
  vers la resolution de colonnes puis l'ecriture bloc par bloc.
- Aucun changement de logique runtime ; commentaires uniquement.
## 2026-04-20 - Batch commentaires socle Level 0

- Ajout d'un `package-info.java` pour `level0/noise`.
- Renforcement des Javadocs des types socles suivants :
  `LevelZeroSurfaceBiome`,
  `LevelZeroCoords`,
  `LevelZeroLayerStackLayout`,
  `LevelZeroVerticalSlice`,
  `LevelZeroSeedResolver`,
  `StageRandom`,
  `LevelZeroCellState`,
  `LevelZeroCellTag`,
  `LevelZeroCellTopology`.
- Objectif : rendre plus auto-explicites les notions de biome visuel, coordonnees,
  verticale canonique, derive de seeds et etat semantique de cellule.
- Aucun changement de logique runtime ; commentaires uniquement.
## 2026-04-20 - Batch commentaires stages metier Level 0

- Renforcement des Javadocs de plusieurs stages metier afin qu'un lecteur
  comprenne mieux leur role exact sans lire toute l'implementation.
- Fichiers clarifies :
  `LevelZeroLightStage`,
  `LevelZeroSurfaceBiomeStage`,
  `LevelZeroLargeRoomStage`,
  `LevelZeroLegacyTopologyStage`,
  `LevelZeroLegacyRegionWalkabilityStage`,
  `LevelZeroWalkabilitySampler`.
- Objectif : mieux expliciter la difference entre candidature lumineuse,
  marquage de grandes rooms, topologie fine et verite brute de walkability.
- Aucun changement de logique runtime ; commentaires uniquement.
## 2026-04-20 - Batch finition commentaires entree layout

- Ajustement du Javadoc de `LevelZeroRegionGrid` pour clarifier que la mention
  de la spec v6 renvoie a une direction d'architecture, mais que la facade est
  bien une piece active du runtime actuel.
- Ajout de commentaires explicatifs sur les constantes historiques de
  `LevelZeroLayout` :
  densite du maillage, nombre de mini-labyrinthes, collision stop, quantites de
  salles et modulo historique des neons.
- Aucun changement de logique runtime ; commentaires uniquement.
## 2026-04-20 - Batch commentaires satellites layout write

- Renforcement des commentaires/Javadocs sur plusieurs helpers et resolvers
  satellites du `layout` et du `write`.
- Fichiers clarifies :
  `LevelZeroWallPropResolver`,
  `LevelZeroSurfaceDetailResolver`,
  `LevelZeroStructureResolver`,
  `LevelZeroBlockPalette`,
  `LevelZeroColumnMaterial`,
  `LevelZeroWriteStage`,
  `LevelZeroCellConnections`,
  `LevelZeroCellMicroPattern`,
  `LevelZeroGeometryMask`,
  `LevelZeroChunkCellWindow`.
- Objectif : faire en sorte que meme les petits fichiers satellites indiquent
  clairement s'ils portent une decision metier, une fondation future ou un
  simple contrat utilitaire.
- Aucun changement de logique runtime ; commentaires uniquement.
