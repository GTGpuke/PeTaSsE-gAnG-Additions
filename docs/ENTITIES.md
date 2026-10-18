# Entités — PeTaSsE_gAnG_Additions

## Portail Backrooms (`backrooms_portal`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `backrooms/portal/entity/BackroomsPortalEntity.java` |
| Type | `ModEntities.BACKROOMS_PORTAL` (MISC, 0.1 x 0.1, tracking 8, rafraîchi toutes les 20 ticks) |
| Renderer | `backrooms/portal/client/entity/BackroomsPortalEntityRenderer.java` : rien quand le rendu distant est actif (le contenu passe par le stencil), cadre sombre sinon |
| Données synchronisées | orientation source et destination (`PortalFacing`), dimension et position d'arrivée, taille, visible, ouvert (`teleportable`), lumière transmise |
| Données serveur | bloc portail propriétaire (dimension + position) |
| Création | par une `BackroomsPortalBlockEntity` (selon ses réglages, une ou deux faces dos à dos dans le cadre, autant au point d'arrivée sauf aller simple) ; `BackroomsPortalEntity.builder(world)…build()` pour un portail libre |
| Cycle de vie | ne bouge jamais, ne peut pas être frappé ; toutes les 100 ticks, le registre serveur retire les portails dont le bloc a disparu, ne les tient plus pour siens ou est dans une dimension absente (chunk du bloc chargé) |

Le détail du système (géométrie, téléportation, synchronisation, rendu) est dans [PORTALS.md](PORTALS.md).

Commande de dev (environnement de développement seulement) :

```mcfunction
/backrooms portal list
/backrooms portal clear
```

Aucun mob custom pour l'instant.
