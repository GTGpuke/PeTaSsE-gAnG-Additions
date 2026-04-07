# PeTaSsE_gAnG_Additions — Documentation

Documentation technique complète du mod.

---

## Index

| Fichier | Description |
|---------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture technique, packages, patterns, conventions |
| [SETUP.md](SETUP.md) | Guide d'installation complet (Java, Gradle, VS Code) |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Conventions de code, workflow Git, checklist |
| [TESTING.md](TESTING.md) | Guide des tests unitaires et GameTests |
| [CICD.md](CICD.md) | Pipeline GitHub Actions, créer une release |
| [ITEMS.md](ITEMS.md) | Catalogue des items (Gang Badge documenté) |
| [BLOCKS.md](BLOCKS.md) | Catalogue des blocs (template) |
| [ENTITIES.md](ENTITIES.md) | Catalogue des entités (template) |
| [DIMENSIONS.md](DIMENSIONS.md) | Catalogue des dimensions (template) |
| [CHANGELOG.md](CHANGELOG.md) | Journal de toutes les modifications |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Résolution des problèmes courants |

---

## Stack technique

- **Minecraft 26.1** — fully deobfuscated, Java 25 requis
- **Forge 62.0.x** — mod loader
- **Gradle 9.3.0** — build system
- **JUnit 5** — tests unitaires
- **Forge GameTest** — tests in-game
- **GitHub Actions** — CI/CD

---

## Skills Claude Code

Les fichiers sous `/.skills/` permettent à Claude Code d'ajouter du contenu automatiquement.

| Skill | Usage |
|-------|-------|
| `add-item` | Ajouter un item, outil, arme, consommable |
| `add-block` | Ajouter un bloc |
| `add-entity` | Ajouter un mob/entité |
| `add-dimension` | Ajouter une dimension |
| `add-recipe` | Ajouter une recette de craft |
| `add-sound` | Ajouter un son |
| `add-creative-tab` | Ajouter/modifier un onglet créatif |
| `project-conventions` | Rappel des conventions du projet |

**Utilisation :** dis simplement à Claude Code ce que tu veux, par exemple :
> "Ajoute un item épée custom appelée GangSword"
> "Crée un bloc de minerai gangite_ore avec une texture violette"
