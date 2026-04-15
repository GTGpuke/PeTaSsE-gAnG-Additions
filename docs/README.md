# PeTaSsE_gAnG_Additions — Documentation

Documentation technique complète du mod.

---

## Index

| Fichier | Description |
|---------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture technique, packages, patterns, conventions |
| [SETUP.md](SETUP.md) | Guide d'installation complet (Java, Gradle, VS Code) |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Conventions de code, workflow Git, checklist |
| [TESTING.md](TESTING.md) | Guide des tests unitaires et benchmark |
| [CICD.md](CICD.md) | Pipeline GitHub Actions, créer une release |
| [ITEMS.md](ITEMS.md) | Catalogue des items |
| [BLOCKS.md](BLOCKS.md) | Catalogue des blocs |
| [ENTITIES.md](ENTITIES.md) | Catalogue des entités (template) |
| [DIMENSIONS.md](DIMENSIONS.md) | Catalogue des dimensions |
| [DEPENDENCIES.md](DEPENDENCIES.md) | Plan de dépendances Backrooms |
| [CHANGELOG.md](CHANGELOG.md) | Journal de toutes les modifications |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Résolution des problèmes courants |

---

## Stack technique

- **Minecraft 1.21.1** — Yarn mappings 1.21.1+build.3, Java 21 requis
- **Fabric Loader 0.16.9** — mod loader
- **Fabric API 0.102.0+1.21.1** — API de modding
- **Fabric Loom 1.9** — plugin Gradle
- **Gradle 9.3.0** — build system
- **JUnit 5** — tests unitaires
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
