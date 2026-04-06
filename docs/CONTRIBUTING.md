# Contributing — PétasseGang Addons

Guide de développement pour les membres de la gang.

---

## Workflow Git

```
main          ← production stable, tagged releases
develop       ← intégration ongoing
feature/xxx   ← branche par feature
fix/xxx       ← branche par bugfix
```

### Branches
```bash
git checkout develop
git checkout -b feature/my-awesome-item
# ... code ...
git push origin feature/my-awesome-item
# Ouvre une PR vers develop
```

### Commits
Format : `type(scope): message`

| Type | Usage |
|------|-------|
| `feat` | Nouveau contenu (item, block, entité…) |
| `fix` | Correction de bug |
| `refactor` | Refactoring sans changement fonctionnel |
| `docs` | Documentation seulement |
| `test` | Ajout/modification de tests |
| `chore` | Gradle, CI, tooling |

Exemples :
```
feat(item): add Gang Badge with EPIC rarity and foil effect
fix(config): enableGangBadge default was inverted
docs(items): add Gang Badge to ITEMS.md
test(item): add GangBadgeItem tooltip assertions
```

---

## Checklist avant de push

- [ ] `./gradlew build` passe sans warning ni erreur
- [ ] `./gradlew test` — tous les tests verts
- [ ] Nouveau contenu = nouveau test dans `src/test/`
- [ ] `docs/ITEMS.md` (ou BLOCKS.md, etc.) mis à jour
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] Lang keys ajoutées en `en_us.json` ET `fr_fr.json`
- [ ] Modèle JSON + texture PNG présents

---

## Style de code

| Règle | Détail |
|-------|--------|
| Encodage | UTF-8 partout |
| Indentation | 4 espaces (pas de tabs) |
| Longueur de ligne | 120 chars max |
| Imports | Pas de wildcard (`import java.util.*` interdit) |
| Javadoc | Classes publiques + méthodes publiques non triviales |
| Commentaires | En anglais |
| Logs | Via `ModConstants.LOGGER` uniquement |
| Nulls | Jamais sans commentaire justifiant pourquoi c'est safe |

---

## Utiliser les Skills Claude Code

Les skills sous `/.skills/` sont des guides autonomes pour Claude Code.
Au lieu de décrire manuellement ce que tu veux, utilise la phrase magique :

```
# Ajouter un item
"Ajoute un item épée en diamant custom nommé GangSword"

# Ajouter un bloc
"Crée un bloc de minerai custom appelé gangite_ore"

# Ajouter une recette
"Ajoute une recette craft pour le Gang Badge"
```

Claude Code lit automatiquement le skill approprié et suit les étapes.

---

## Ajout de contenu — règles non négociables

1. **Un RegistryObject par ligne** dans les fichiers `init/`
2. **Zéro allocation dans les hot-paths** (render, tick) — utilise des champs `static final`
3. **Séparation client/serveur** — tout code `@OnlyIn(CLIENT)` dans `client/`
4. **Tests** — chaque classe `item/`, `block/`, `entity/` a sa classe de test correspondante
5. **CHANGELOG** — toujours mettre à jour avant de tag une version
