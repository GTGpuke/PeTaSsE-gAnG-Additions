# Items — GTG's Backrooms

Catalogue des items du mod, tous enregistrés dans `core/registry/ModItems.java`. L'onglet créatif *GTG's Backrooms*
(`ModCreativeTab.ENTRIES`) les liste tous, la baguette de portail en tête. Le bloc portail n'a pas d'item : la baguette le
pose.

---

## Baguette de portail (`portal_wand`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `core/portal/item/PortalWandItem` ; écran `core/portal/client/wand/PortalWandScreen` (client) |
| Stack max | 1 |
| Rareté | Uncommon |
| Composant | `gtgs_backrooms:portal_settings` (`PortalSettings`), réglages par défaut à la création : `width` et `height` de 1 à 8, `transmits_light`, `double_sided`, `one_way`, `fleeting_exit`, `exit`, `exit_scatter` (0 à 64), `flush`, `upside_down`, `placement` (`look`, `standing`, `floor`, `ceiling`), `destination` (identifiant d'une dimension, absent pour la destination automatique), `mark` (sortie fixée : `dimension`, `pos`, `facing`) ; affichés dans l'info-bulle (réglages, puis l'usage) |
| Onglet | *GTG's Backrooms*, en tête : c'est le seul moyen de créer un portail |
| Usage dans le vide | ouvre un écran qui règle chacun des réglages : largeur et hauteur (1 à 8), lumière des blocs, faces, retour (aller-retour, aller simple, sortie fugace), sens de sortie, arrivée au hasard (0 à 64 blocs), plan (au milieu ou au bord du bloc), arrivée (droite ou tête en bas), pose (selon le regard, debout, au sol, au plafond), destination (automatique ou une dimension du serveur), sortie fixée (« Fixer la sortie ici » retient l'endroit où l'on se tient ; un second clic l'efface) ; ils partent au serveur à la fermeture de l'écran, par Terminer comme par Échap, s'ils ont changé (payload `portal_wand_settings`) |
| Usage sur un bloc | pose un bloc portail réglé ainsi, comme vanilla pose un bloc (face avant vers le joueur, couché en regardant le sol ou le plafond, ou selon la pose réglée, refusé si le cadre traverserait un bloc ; derrière le plan, un bloc ne gêne pas) ; la baguette ne s'use pas, en survie comme en créatif |
| Usage sur un portail | accroupi (le portail ne se vise qu'ainsi), clic droit : la baguette prend ses réglages, et le joueur le voit au-dessus de sa barre d'objets ; on en pose un pareil, ou on le casse (clic gauche, il ne rend rien) pour le refaire autrement |
| Son | à chaque pose, un portail qui s'ouvre (`ModSounds.PORTAL_WAND_PLACE`, sous-titre « Un portail s'ouvre ») |
| Texture | `textures/item/portal_wand.png`, modèle `item/handheld` |

---

## Blocs sous forme d'item

Chaque bloc de [BLOCKS.md](BLOCKS.md) possède un `BlockItem` du même identifiant : `level_zero_wallpaper`,
`level_zero_wallpaper_aged`, `level_zero_wallpaper_adaptive`, `level_zero_damp_carpet`, `level_zero_damp_carpet_aged`,
`level_zero_ceiling_tile`, `level_zero_fluorescent_light` (onglet *GTG's Backrooms*). Le bloc portail (`backrooms_portal`)
n'en a pas : la baguette le pose, et cassé il ne rend rien.

---

## Ajouter un item

1. Sa classe, s'il en a une, dans le paquet de son domaine (tableau « Où ranger quoi » de [ARCHITECTURE.md](ARCHITECTURE.md)) :
   `backrooms/<niveau>/item/` pour un objet d'un niveau, `core/<système>/item/` pour un objet d'un système du socle.
2. Champ `static final` dans `core/registry/ModItems` + entrée dans `ModCreativeTab.ENTRIES`.
3. `models/item/<id>.json` (parent `item/generated` ou `item/handheld`) + texture 16x16.
4. Clé `item.gtgs_backrooms.<id>` en EN et FR.
5. Un test du comportement (pas de la réflexion) dans le paquet miroir de `src/test/java/` ou `src/gametest/java/`.
