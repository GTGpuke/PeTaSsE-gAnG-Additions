# GTG's Backrooms — Documentation

## Index

| Fichier | Description |
|---------|-------------|
| [GUIDE.md](GUIDE.md) | Guide de conception et de développement : règles d'or, état réel, architecture cible du rendu et de l'audio, méthode clean room, feuille de route, cohabitation avec les packs de shaders, compatibilité, licence |
| [AUDIT.md](AUDIT.md) | Audit complet d'octobre 2026 : constats, décisions, mesures de performance |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Packages, pipeline de génération, rendu client, cycle de vie, conventions |
| [SETUP.md](SETUP.md) | Installation (Java 21, wrapper Gradle, VS Code), commandes |
| [TESTING.md](TESTING.md) | Tests unitaires, empreintes de génération, GameTest, benchmark |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Workflow Git, commits, checklist, style |
| [CICD.md](CICD.md) | GitHub Actions, créer une release |
| [ITEMS.md](ITEMS.md) | Catalogue des items |
| [BLOCKS.md](BLOCKS.md) | Catalogue des blocs |
| [DIMENSIONS.md](DIMENSIONS.md) | Dimension Level 0 : géographie, génération, accès |
| [ENTITIES.md](ENTITIES.md) | Entités (entity portail) |
| [PORTALS.md](PORTALS.md) | Portails : porte, géométrie du passage, téléportation, synchronisation des chunks et des entités, rendu stencil et portails imbriqués, interaction à travers le cadre, limites |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Problèmes courants |
| [CHANGELOG.md](CHANGELOG.md) | Journal des versions |
| [BACKROOMS_CONTENT_RULES.md](BACKROOMS_CONTENT_RULES.md) | Règles de contenu et direction artistique des Backrooms |
| `backrooms/` | Spec du générateur du Level 0 (pipeline v6), sa roadmap et le gabarit des structures manuelles |
| `prompt/` | Prompts d'onboarding (`NOKNOWLEDGE.md`), d'audit pré-push (`BEFOREPUSH.md`) et de vérification finale (`FINALCHECK.md`) pour les assistants IA |

## Stack

Minecraft 1.21.1 · Yarn 1.21.1+build.3 · Fabric Loader 0.19.5 · Fabric API 0.116.17 · Fabric Loom 1.17.21 ·
Gradle 9.8.1 · Java 21 · JUnit 5.11 + fabric-loader-junit · JaCoCo · GitHub Actions

## Skills (`.skills/`)

Guides pour assistants IA : `add-item`, `add-block`, `add-entity`, `add-dimension`, `add-recipe`, `add-sound`,
`add-creative-tab`, `add-dependency`, `project-conventions`, `pre-push-audit`.
