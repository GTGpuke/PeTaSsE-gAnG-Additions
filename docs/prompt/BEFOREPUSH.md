Avant de push ou de proposer une feature, tu dois TOUJOURS effectuer un audit complet sur le code que tu as généré ou modifié.

Ton objectif : détecter les incohérences, le code mort, les problèmes de performance, les problèmes structurels, les régressions, les oublis de documentation et les problèmes artistiques.

Si des améliorations peuvent être faites SANS casser de features existantes ni réduire la clarté, tu dois les appliquer immédiatement.

Tu dois exécuter les étapes d'audit suivantes sur CHAQUE fichier créé ou modifié, et tu dois garantir qu'AUCUNE régression n'est introduite dans le projet. Tout changement fait pendant l'audit doit préserver les comportements et fonctionnalités existants.

Tu dois aussi vérifier les interactions entre fichiers, les dépendances, les imports et les usages à travers tout le projet pour t'assurer que les modifications ne cassent pas d'autres modules ou features.

Tu dois garder le contexte COMPLET de la feature en cours et des chemins de code liés pendant tout l'audit, et tu dois éviter les refactors qui pourraient introduire des effets de bord.

---

## 1. AUDIT JAVA — Code source du mod

Pour CHAQUE fichier Java créé ou modifié :

### 1.1 Qualité du code
- [ ] Le code compile sans aucun warning (`./gradlew build --warning-mode all`)
- [ ] Aucun import inutilisé
- [ ] Aucun import wildcard (`import x.y.*` interdit)
- [ ] Imports triés dans l'ordre : `java` → `javax` → `net.minecraft` → `net.minecraftforge` → `com.petassegang`
- [ ] Nommage conforme :
  - Classes : `PascalCase` → `GangBadgeItem`
  - Méthodes/variables : `camelCase` → `appendHoverText()`
  - Constantes : `UPPER_SNAKE_CASE` → `GANG_BADGE`
  - Registry names : `snake_case` → `gang_badge`
- [ ] Aucun code mort (méthodes vides, variables inutilisées, blocs commentés, TODO oubliés)
- [ ] Aucun `System.out.println` — utiliser uniquement `ModConstants.LOGGER`
- [ ] Aucun `@SuppressWarnings` non justifié
- [ ] Chaque classe a un rôle unique (Single Responsibility)
- [ ] Aucune duplication de code — extraire dans `util/` si un pattern se répète

### 1.2 Commentaires et messages
**RÈGLE CRITIQUE — Les commentaires doivent TOUJOURS être en français :**
- [ ] Tous les commentaires sont rédigés en français
- [ ] Chaque commentaire commence par une majuscule
- [ ] Chaque commentaire se termine par un point
- [ ] Les commentaires sont concis et utiles (pas de commentaires évidents comme `// Incrémente i`)
- [ ] Les Javadoc des classes et méthodes publiques sont en français

**Messages d'erreur et logs :**
- [ ] Tous les messages d'erreur sont en français
- [ ] Chaque message d'erreur commence par une majuscule
- [ ] Chaque message d'erreur se termine par un point
- [ ] Les messages de log (`LOGGER.info/warn/error`) sont en français
- [ ] Les messages de log incluent le contexte pertinent (quel objet, quelle action)

**Contenu visible en jeu (tooltips, GUI, noms d'items, etc.) :**
- [ ] JAMAIS de texte en dur dans le code — toujours passer par `Component.translatable("clé")`
- [ ] Les clés de traduction sont cohérentes avec la convention : `item.petasse_gang_additions.nom_item`
- [ ] La traduction anglaise existe dans `en_us.json`
- [ ] La traduction française existe dans `fr_fr.json`
- [ ] Les deux traductions sont correctes, naturelles et cohérentes

### 1.3 Performance Minecraft
- [ ] AUCUNE allocation d'objet dans les méthodes appelées chaque tick ou chaque frame :
  - `tick()`, `inventoryTick()`, `animateTick()`
  - `render()`, `appendHoverText()` (si appelé fréquemment)
  - Event handlers appelés souvent (onLivingUpdate, onRenderWorld, etc.)
- [ ] Pas de `new Object()` inutile dans les boucles
- [ ] Utiliser des constantes pré-allouées pour les `Component`, `ResourceLocation`, `ChatFormatting`, etc.
- [ ] Pas d'appels réseau ou I/O bloquants sur le thread principal
- [ ] Lazy initialization avec `Lazy<>` ou `Supplier<>` là où c'est pertinent
- [ ] Les textures ne sont pas chargées manuellement — utiliser le système de resource packs

### 1.4 Architecture Forge
- [ ] DeferredRegister pour TOUS les enregistrements (items, blocks, entities, sounds, tabs, etc.)
- [ ] Aucun registre statique direct
- [ ] Séparation client/serveur stricte :
```bash
  grep -rn "import net.minecraft.client" src/main/java/ --include="*.java" | grep -v "/client/"
```
  Tout import client en dehors du package `client/` doit être protégé par un dist check ou `@OnlyIn(Dist.CLIENT)`
- [ ] Les event handlers sont enregistrés sur le bon bus (MOD bus vs FORGE bus)
- [ ] Les événements sont annotés avec `@SubscribeEvent` correctement
- [ ] Les méthodes de lifecycle (`commonSetup`, `clientSetup`, `gatherData`) sont utilisées correctement

### 1.5 Interactions inter-fichiers
- [ ] Tous les imports vers d'autres classes du projet sont corrects
- [ ] Aucune dépendance circulaire entre packages
- [ ] Les `RegistryObject<>` / `DeferredHolder<>` sont utilisés correctement (.get() au bon moment)
- [ ] Les dépendances optionnelles vers d'autres mods passent par `ModList.get().isLoaded()`
- [ ] La structure des données retournées par les méthodes est cohérente avec le reste du projet
- [ ] Aucune feature existante n'a été altérée involontairement

---

## 2. AUDIT RESSOURCES — Assets et data packs

### 2.1 Modèles JSON
Pour chaque modèle créé ou modifié :
- [ ] Le JSON est valide (pas d'erreurs de syntaxe)
```bash
  find src/main/resources -name "*.json" -exec python3 -c "import json,sys; json.load(open(sys.argv[1])); print(f'OK: {sys.argv[1]}')" {} \;
```
- [ ] Les paths de textures sont corrects et pointent vers des fichiers existants
- [ ] Le parent est correct (`item/generated`, `item/handheld`, `block/cube_all`, etc.)
- [ ] Les noms de fichiers sont en `snake_case`

### 2.2 Textures
Pour chaque texture créée ou modifiée :
- [ ] Format PNG valide
- [ ] Dimensions correctes (16x16 pour items/blocks standards, 64x32 ou 64x64 pour entités)
- [ ] Vérifier avec :
```bash
  find src/main/resources -name "*.png" -exec python3 -c "
  from PIL import Image
  import sys
  img = Image.open(sys.argv[1])
  w, h = img.size
  print(f'{sys.argv[1]}: {w}x{h}')
  if w not in [16,32,64,128,256] or h not in [16,32,64,128,256]:
      print(f'  ⚠️  Dimensions non standard')
  " {} \;
```

### 2.3 Cohérence artistique Minecraft
**C'est un point CRITIQUE — le mod doit s'intégrer visuellement dans Minecraft :**
- [ ] Les textures respectent le style pixel art 16x16 de Minecraft :
  - Palette de couleurs cohérente avec le vanilla (pas de couleurs néon ou fluo sauf si c'est voulu)
  - Contours : utiliser des teintes plus sombres de la même couleur (pas de noir pur #000000)
  - Ombrage : lumière venant du haut-gauche (convention Minecraft)
  - Niveau de détail : proportionnel à la résolution 16x16, pas de détails trop fins qui deviennent du bruit
  - Pas d'anti-aliasing (les pixels doivent être nets)
- [ ] Les modèles 3D (si applicable) sont cohérents avec les proportions Minecraft
- [ ] Les couleurs de rareté utilisées dans les tooltips correspondent aux standards vanilla :
  - COMMON = blanc, UNCOMMON = jaune, RARE = aqua, EPIC = violet
- [ ] Les effets visuels (particules, glow) ne sont pas trop intrusifs
- [ ] Le style général est cohérent avec les textures déjà existantes dans le mod
  - Comparer visuellement avec les textures existantes :
```bash
  ls src/main/resources/assets/petasse_gang_additions/textures/item/
  ls src/main/resources/assets/petasse_gang_additions/textures/block/ 2>/dev/null
```
- [ ] Si le mod a un thème visuel établi, la nouvelle texture le respecte

### 2.4 Fichiers de langue
- [ ] Vérifier que CHAQUE clé utilisée dans le code a sa traduction :
```bash
  # Extraire les clés du code
  grep -rohn 'translatable("[^"]*"' src/main/java/ | sed 's/.*translatable("//;s/".*//' | sort -u > /tmp/keys_code.txt

  # Extraire les clés de en_us.json
  python3 -c "import json; [print(k) for k in json.load(open('src/main/resources/assets/petasse_gang_additions/lang/en_us.json')).keys()]" | sort -u > /tmp/keys_en.txt

  # Extraire les clés de fr_fr.json
  python3 -c "import json; [print(k) for k in json.load(open('src/main/resources/assets/petasse_gang_additions/lang/fr_fr.json')).keys()]" | sort -u > /tmp/keys_fr.txt

  # Trouver les clés manquantes
  echo "=== Clés manquantes dans en_us.json ==="
  comm -23 /tmp/keys_code.txt /tmp/keys_en.txt
  echo "=== Clés manquantes dans fr_fr.json ==="
  comm -23 /tmp/keys_code.txt /tmp/keys_fr.txt
```
- [ ] Aucune clé orpheline (présente dans les JSON mais plus utilisée dans le code)
- [ ] Les traductions sont naturelles (pas du Google Translate)
- [ ] Les noms d'items/blocks sont cohérents en EN et FR

### 2.5 Blockstates et loot tables
- [ ] Chaque block a un blockstate JSON correspondant
- [ ] Chaque block a une loot table dans `data/petasse_gang_additions/loot_tables/blocks/`
- [ ] Les recettes dans `data/petasse_gang_additions/recipes/` sont valides

---

## 3. AUDIT TESTS

### 3.1 Exécution des tests existants
```bash
./gradlew test 2>&1
```
- [ ] TOUS les tests existants passent (BUILD SUCCESSFUL)
- [ ] Aucun test skippé sans justification
- [ ] Le rapport est propre : `build/reports/tests/test/index.html`

### 3.2 Couverture de la feature
La feature ajoutée/modifiée DOIT avoir des tests :
- [ ] **Test unitaire** : chaque nouvelle classe/méthode publique a au moins un test
  - Propriétés d'items (stack size, rareté, isFoil, etc.)
  - Comportements custom (onUse, onHit, tick, etc.)
  - Config (valeurs par défaut, validation)
- [ ] **Test de registre** : chaque nouvel élément enregistré est vérifié
  - L'objet est non-null après enregistrement
  - Le registry name est correct
- [ ] **Test de non-régression** : les tests existants passent toujours
  - Exécuter la suite complète, pas seulement les nouveaux tests
- [ ] **GameTest** (si applicable) : test in-game pour les interactions complexes

### 3.3 Qualité des tests
- [ ] Noms descriptifs : `testGangBadgeHasEpicRarity()`, pas `test1()`
- [ ] Assertions avec messages explicites en français :
```java
  assertEquals(1, badge.getMaxStackSize(), "Le Gang Badge doit avoir un stack size de 1.");
```
- [ ] Chaque test teste UNE chose (pas de tests fourre-tout)
- [ ] Pas de tests triviaux qui passent toujours

### 3.4 Création des tests manquants
Si des tests manquent pour la feature : CRÉE-LES immédiatement.
Pattern à suivre :
```java
package com.petassegang.addons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class NomDeLaFeatureTest {

    @Test
    @DisplayName("Description claire en français.")
    void testNomExplicite() {
        // Arrange
        // Act
        // Assert avec message en français
    }
}
```

---

## 4. AUDIT LINTING & PROPRETÉ

### 4.1 Compilation propre
```bash
./gradlew clean build --warning-mode all 2>&1 | grep -i "warning\|error\|deprecated"
```
- [ ] Zéro warning de compilation
- [ ] Zéro erreur
- [ ] Zéro utilisation d'API dépréciée

### 4.2 Analyse statique
```bash
# Recherche de code suspect
echo "=== System.out/err ==="
grep -rn "System\.out\|System\.err" src/main/java/ --include="*.java"

echo "=== printStackTrace ==="
grep -rn "printStackTrace" src/main/java/ --include="*.java"

echo "=== Imports wildcard ==="
grep -rn "import .*\.\*;" src/main/java/ --include="*.java"

echo "=== Imports inutilisés (heuristique) ==="
for f in $(find src/main/java -name "*.java"); do
  while IFS= read -r line; do
    cls=$(echo "$line" | sed 's/import //;s/;//;s/.*\.//')
    if ! grep -q "$cls" "$f" 2>/dev/null | grep -v "^import" > /dev/null; then
      echo "  Possiblement inutilisé dans $f: $line"
    fi
  done < <(grep "^import " "$f" | grep -v "^\*")
done 2>/dev/null | head -20

echo "=== Annotations @SuppressWarnings ==="
grep -rn "@SuppressWarnings" src/main/java/ --include="*.java"

echo "=== TODO/FIXME/HACK oubliés ==="
grep -rn "TODO\|FIXME\|HACK\|XXX\|TEMP" src/main/java/ --include="*.java"

echo "=== Nombres magiques (constantes non nommées) ==="
grep -rn "[^0-9][0-9][0-9][0-9][^0-9xL\.]" src/main/java/ --include="*.java" | grep -v "test\|Test\|version\|Copyright\|serialVer" | head -10
```
- [ ] Aucun `System.out` / `System.err`
- [ ] Aucun `printStackTrace()` (utiliser le Logger)
- [ ] Aucun import wildcard
- [ ] Aucun TODO/FIXME oublié
- [ ] Les nombres magiques sont extraits en constantes nommées

### 4.3 Formatage
- [ ] Indentation : 4 espaces (pas de tabs)
- [ ] Longueur de ligne : max 120 caractères
- [ ] Braces K&R style (ouvrante sur la même ligne)
- [ ] Un espace avant les accolades ouvrantes
- [ ] Lignes vides entre les méthodes
- [ ] Pas de lignes vides multiples consécutives
- [ ] Pas d'espaces en fin de ligne (trailing whitespace)
```bash
grep -rn " $" src/main/java/ --include="*.java" | head -10
```

### 4.4 JSON valide
```bash
find src/main/resources -name "*.json" | while read f; do
  python3 -c "import json; json.load(open('$f'))" 2>&1 && echo "✅ $f" || echo "❌ $f"
done
```
- [ ] Tous les fichiers JSON sont valides

---

## 5. AUDIT SÉCURITÉ & ROBUSTESSE

### 5.1 Null safety
- [ ] Vérifier les `@Nullable` et `@Nonnull` annotations là où c'est pertinent
- [ ] Aucun `NullPointerException` possible sur les chemins de code principaux
- [ ] Les `RegistryObject.get()` sont appelés uniquement après l'enregistrement (pas dans les constructeurs statiques)

### 5.2 Thread safety
- [ ] Pas de modification d'état partagé depuis les event handlers sans synchronisation
- [ ] Les champs statiques mutables sont `volatile` ou `AtomicReference` si accédés depuis plusieurs threads
- [ ] Pas de lazy init non thread-safe sur des champs partagés

### 5.3 Compatibilité serveur
```bash
# Vérifier que le code client n'est pas chargé côté serveur
echo "=== Imports client en dehors de /client/ ==="
grep -rn "import net.minecraft.client" src/main/java/com/petassegang/addons/ --include="*.java" | grep -v "/client/" | grep -v "compat/"
```
- [ ] Aucun import client dans les classes communes
- [ ] `@OnlyIn(Dist.CLIENT)` ou `DistExecutor.safeRunWhenOn` utilisé correctement
- [ ] Le mod ne crash pas en mode serveur dédié

---

## 6. AUDIT DOCUMENTATION

### 6.1 Mise à jour obligatoire
Vérifie si la feature nécessite des mises à jour dans `/docs` :
```bash
echo "=== Fichiers modifiés récemment (feature en cours) ==="
git diff --name-only HEAD 2>/dev/null || find . -newer build.gradle -name "*.java" -o -name "*.json" | head -30
```

Pour chaque fichier modifié, détermine quel doc doit être mis à jour :
| Fichier modifié | Doc à mettre à jour |
|---|---|
| Nouvel item (`.java` + modèle + texture) | `docs/ITEMS.md` |
| Nouveau block | `docs/BLOCKS.md` |
| Nouvelle entité | `docs/ENTITIES.md` |
| Nouvelle dimension | `docs/DIMENSIONS.md` |
| Changement de build.gradle (nouvelle dépendance) | `docs/ARCHITECTURE.md` + `docs/SETUP.md` |
| Nouveau test ou changement de test | `docs/TESTING.md` |
| Changement CI/CD | `docs/CICD.md` |
| Tout changement | `docs/CHANGELOG.md` |

- [ ] `docs/CHANGELOG.md` est mis à jour avec la feature (format Keep a Changelog)
- [ ] Les fichiers doc pertinents sont à jour
- [ ] Les exemples de code dans la doc correspondent au code réel
- [ ] Les commandes Gradle documentées fonctionnent toujours

### 6.2 README.md racine
**TOUJOURS poser la question :**

> "La feature modifie-t-elle le Quick Start, la structure du projet, les prérequis, la liste des commandes, ou toute information présente dans le README.md racine ? Si oui, mets-le à jour."

Vérifie :
- [ ] Le README reflète l'état actuel du projet
- [ ] La section "Structure du projet" est à jour si de nouveaux dossiers/fichiers ont été ajoutés
- [ ] La version affichée est correcte
- [ ] Les badges CI sont toujours valides

### 6.3 Skills
Si la feature introduit un nouveau pattern ou une nouvelle convention :
- [ ] Le skill `project-conventions` est mis à jour
- [ ] Les skills spécifiques (add-item, add-block, etc.) reflètent les patterns actuels

---

## 7. AUDIT DE NON-RÉGRESSION

### 7.1 Tests complets
```bash
./gradlew clean test 2>&1
```
- [ ] TOUS les tests passent, pas seulement les nouveaux

### 7.2 Build complet
```bash
./gradlew clean build --warning-mode all 2>&1
```
- [ ] Build réussi sans aucun warning

### 7.3 Vérification des features existantes
- [ ] Les items existants ont toujours leurs propriétés correctes
- [ ] Les creative tabs existants contiennent toujours tous leurs items
- [ ] Les traductions existantes sont toujours présentes et correctes
- [ ] Les textures existantes n'ont pas été écrasées ou modifiées
- [ ] Les recettes existantes fonctionnent toujours
- [ ] La config existante est compatible (pas de clés supprimées)

### 7.4 Vérification inter-fichiers
- [ ] Les signatures de méthodes publiques n'ont pas changé de manière incompatible
- [ ] Les interfaces publiques sont préservées
- [ ] Aucun fichier de ressource existant n'a été supprimé par erreur
- [ ] Les dépendances dans mods.toml sont toujours cohérentes

---

## 8. NETTOYAGE DES FICHIERS INUTILES

Avant tout push, s'assurer qu'aucun fichier parasite ne traîne dans le repo ou le répertoire de travail.

### 8.1 Fichiers temporaires et scripts de génération
```bash
# Lister les fichiers non trackés par git
git status --short 2>/dev/null | grep "^?"

# Lister les fichiers présents mais dans .gitignore (fichiers ignorés sur disque)
git ls-files --others --ignored --exclude-standard 2>/dev/null
```
- [ ] Supprimer les scripts de génération one-shot (ex : `generate_texture.py`, `fix_*.sh`, `temp_*.groovy`)
- [ ] Supprimer les fichiers `.bak`, `.tmp`, `.swp`, `*.orig`
- [ ] Supprimer les fichiers de debug ou de test ad-hoc laissés à la racine
- [ ] Supprimer les copies de sauvegarde (ex : `ModItems.java.bak`, `build.gradle.old`)

### 8.2 Resources orphelines
```bash
# Textures sans modèle associé
for f in src/main/resources/assets/petasse_gang_additions/textures/item/*.png; do
  name=$(basename "$f" .png)
  if [ ! -f "src/main/resources/assets/petasse_gang_additions/models/item/$name.json" ]; then
    echo "⚠️  Texture sans modèle : $f"
  fi
done

# Modèles sans texture associée
for f in src/main/resources/assets/petasse_gang_additions/models/item/*.json; do
  name=$(basename "$f" .json)
  if [ ! -f "src/main/resources/assets/petasse_gang_additions/textures/item/$name.png" ]; then
    echo "⚠️  Modèle sans texture : $f"
  fi
done
```
- [ ] Aucune texture orpheline (PNG sans modèle JSON correspondant)
- [ ] Aucun modèle orphelin (JSON sans texture correspondante)
- [ ] Aucune traduction orpheline (clé JSON sans usage dans le code)
- [ ] Aucune loot table orpheline (pour un block/entity supprimé)

### 8.3 Classes Java mortes
```bash
# Classes Java non référencées nulle part (heuristique)
for f in $(find src/main/java -name "*.java"); do
  cls=$(basename "$f" .java)
  count=$(grep -rn "$cls" src/main/java/ --include="*.java" | grep -v "^$f:" | wc -l)
  if [ "$count" -eq 0 ]; then
    echo "⚠️  Classe possiblement inutilisée : $cls ($f)"
  fi
done
```
- [ ] Aucune classe Java non référencée
- [ ] Aucun package vide
- [ ] Aucune interface ou classe abstraite sans implémentation

### 8.4 Dépendances inutiles
- [ ] Vérifier que chaque dépendance dans `build.gradle` est réellement utilisée dans le code
- [ ] Supprimer les dépendances commentées laissées "pour plus tard" sans TODO justifié
- [ ] Aucun repository Maven déclaré mais inutilisé

### 8.5 Règle générale
> **Si un fichier n'est pas nécessaire au build, aux tests, à la documentation ou au runtime — il doit être supprimé.**
> Ne pas garder de fichiers "au cas où" : c'est à ça que sert git (historique des suppressions).

---

## 9. AUDIT PERFORMANCE GLOBALE

### 9.1 Taille du JAR
```bash
./gradlew build -q
ls -lh build/libs/petasse_gang_additions-*.jar
```
- [ ] La taille du JAR est raisonnable (pas de fichiers volumineux inclus par erreur)
- [ ] Pas de dépendances embarquées inutilement

### 9.2 Temps de build
```bash
time ./gradlew build -q
```
- [ ] Le build reste dans un temps raisonnable
- [ ] Pas de tâches Gradle inutiles ajoutées

### 9.3 Impact runtime
- [ ] La feature n'ajoute pas de processing chaque tick si ce n'est pas nécessaire
- [ ] Les event handlers ont des early returns pour les cas non pertinents
- [ ] Pas de logging excessif en production (utiliser `LOGGER.debug` pour le debug, pas `LOGGER.info`)

---

## 10. CHECKLIST FINALE PRÉ-PUSH

Avant de confirmer que c'est prêt à push, vérifie une dernière fois :

### Code
- [ ] `./gradlew clean build --warning-mode all` → BUILD SUCCESSFUL, 0 warning
- [ ] `./gradlew test` → tous les tests passent
- [ ] Aucun `System.out`, `printStackTrace`, import wildcard, code mort
- [ ] Commentaires en français, avec majuscule et point final
- [ ] Messages d'erreur en français, avec majuscule et point final
- [ ] Contenu in-game via `Component.translatable()`, traduit en EN et FR
- [ ] Séparation client/serveur respectée
- [ ] DeferredRegister pour tous les enregistrements
- [ ] Aucune allocation dans les méthodes tick/render

### Ressources
- [ ] Tous les JSON sont valides
- [ ] Toutes les textures sont des PNG 16x16 (ou taille appropriée)
- [ ] Les textures respectent le style Minecraft et la cohérence visuelle du mod
- [ ] Toutes les clés de traduction sont présentes en EN et FR
- [ ] Aucune clé orpheline dans les fichiers de langue

### Tests
- [ ] Nouveaux tests écrits pour la feature
- [ ] Tests existants toujours verts
- [ ] Messages d'assertions en français

### Documentation
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] Fichiers doc pertinents mis à jour
- [ ] README.md racine vérifié — mis à jour si nécessaire
- [ ] Skills mis à jour si nouveau pattern introduit

### Nettoyage
- [ ] Scripts de génération one-shot supprimés
- [ ] Fichiers `.bak`, `.tmp`, `.orig` supprimés
- [ ] Aucune texture/modèle/traduction orphelin
- [ ] Aucune classe Java morte
- [ ] Aucune dépendance inutilisée dans `build.gradle`

### Non-régression
- [ ] Aucune feature existante cassée
- [ ] Aucun fichier existant modifié involontairement
- [ ] Interfaces publiques préservées

---

## 11. RAPPORT PRÉ-PUSH

Après l'audit complet, affiche un rapport synthétique :
```
╔══════════════════════════════════════════════════════════════╗
║              RAPPORT PRÉ-PUSH — PeTaSsE_gAnG_Additions         ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Feature : [nom de la feature]                               ║
║  Version : [version du mod]                                  ║
║  Date    : [date]                                            ║
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  RÉSULTATS                                                   ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  1. Code Java        : ✅ / ⚠️ / ❌  ([détails])            ║
║  2. Ressources       : ✅ / ⚠️ / ❌  ([détails])            ║
║  3. Tests            : ✅ / ⚠️ / ❌  ([X/Y passent])        ║
║  4. Linting          : ✅ / ⚠️ / ❌  ([détails])            ║
║  5. Sécurité         : ✅ / ⚠️ / ❌  ([détails])            ║
║  6. Documentation    : ✅ / ⚠️ / ❌  ([fichiers MAJ])       ║
║  7. Non-régression   : ✅ / ⚠️ / ❌  ([détails])            ║
║  8. Performance      : ✅ / ⚠️ / ❌  ([taille JAR])         ║
║  9. Style artistique : ✅ / ⚠️ / ❌  ([détails])            ║
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  CORRECTIONS APPLIQUÉES                                      ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  1. [correction 1]                                           ║
║  2. [correction 2]                                           ║
║  ...                                                         ║
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  README.md : ✅ À jour / 📝 Mis à jour / ℹ️ Pas de modif   ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  VERDICT : ✅ PRÊT À PUSH / ❌ BLOCAGES RESTANTS            ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

Si le verdict est ❌ : liste les problèmes bloquants et corrige-les.
Si le verdict est ✅ : confirme que le push peut être effectué.

---

## RÈGLES ABSOLUES

1. **Ne JAMAIS ignorer un problème** — tout doit être signalé et corrigé
2. **Corriger AVANT de signaler** — ne pas lister sans agir
3. **Zéro régression** — les features existantes doivent fonctionner identiquement
4. **Commentaires et erreurs en français** — c'est non négociable
5. **Contenu in-game traduit EN + FR** — via le système de langue Minecraft
6. **Textures cohérentes Minecraft** — le style pixel art vanilla doit être respecté
7. **Documentation à jour** — /docs et README reflètent l'état réel
8. **Tests obligatoires** — pas de feature sans test
9. **Build propre** — zéro warning, zéro erreur
10. **Toujours vérifier le README** — poser la question explicitement

Lance l'audit maintenant en commençant par le point 1.