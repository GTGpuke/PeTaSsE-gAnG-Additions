# Entités — PeTaSsE_gAnG_Additions

## Portail Backrooms (`backrooms_portal`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `backrooms/portal/entity/BackroomsPortalEntity.java` |
| Type | `BackroomsPortalEntityType.INSTANCE` (MISC, 0.1 x 0.1, tracking 8, rafraîchi toutes les 20 ticks) |
| Renderer | `backrooms/portal/client/entity/BackroomsPortalEntityRenderer.java` : rien quand le rendu distant est actif (le contenu passe par le stencil), cadre sombre sinon |
| Données synchronisées | orientation source et destination (`PortalFacing`), dimension et position d'arrivée, taille, visible |
| Données serveur | téléportable, porte propriétaire (dimension + position) |
| Création | par une `BackroomsDoorBlockEntity` (portail d'entrée devant la porte, portail de retour au point d'arrivée) ; `BackroomsPortalEntity.builder(world)…build()` pour un portail libre |
| Cycle de vie | ne bouge jamais, ne peut pas être frappé ; toutes les 100 ticks, un portail dont la porte a disparu (chunk de la porte chargé) se retire seul |

Le détail du système (géométrie, téléportation, synchronisation, rendu) est dans [PORTALS.md](PORTALS.md).

Commande de dev (environnement de développement seulement) :

```mcfunction
/backrooms portal list
/backrooms portal clear
```

Aucun mob custom pour l'instant.
