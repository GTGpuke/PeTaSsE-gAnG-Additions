Avant de push ou de proposer une feature, tu dois TOUJOURS effectuer un audit complet sur le code que tu as généré ou modifié.

Ton objectif : détecter les incohérences, le code mort, les problèmes de performance, les problèmes structurels, les régressions, les oublis de documentation et les problèmes artistiques.

Si des améliorations peuvent être faites SANS casser de features existantes ni réduire la clarté, tu dois les appliquer immédiatement.

Tu dois exécuter les étapes d'audit suivantes sur CHAQUE fichier créé ou modifié, et tu dois garantir qu'AUCUNE régression n'est introduite dans le projet.

---

## 1. AUDIT JAVA — Code source du mod

Pour CHAQUE fichier Java créé ou modifié :

### 1.1 Qualité du code
- [ ] Le code compile sans aucun warning (`./gradlew build --warning-mode all`)
- [ ] Aucun import inutilisé
- [ ] Aucun import wildcard (`import x.y.*` interdit)
- [ ] Imports triés dans l'ordre : `java` → `javax` → `net.minecraft` → `net.fabricmc` → `com.petassegang`
- [ ] Nommage conforme :
  - Classes : `PascalCase` → `GangBadgeItem`
  - Méthodes/variables : `camelCase` → `appendTooltip()`
  - Constantes : `UPPER_SNAKE_CASE` → `GANG_BADGE`
  - Registry names : `snake_case` → `gang_badge`
- [ ] Aucun code mort (méthodes vides, variables inutilisées, blocs commentés, TODO oubliés)
- [ ] Aucun `System.out.println` — utiliser uniquement `ModConstants.LOGGER`
- [ ] Aucun `@SuppressWarnings` non justifié

### 1.2 Commentaires et messages
**RÈGLE CRITIQUE — Les commentaires doivent TOUJOURS être en français :**
- [ ] Tous les commentaires sont rédigés en français
- [ ] Chaque commentaire commence par une majuscule
- [ ] Chaque commentaire se termine par un point
- [ ] Les messages de log (`LOGGER.info/warn/error`) sont en français

**Contenu visible en jeu (tooltips, GUI, noms d'items, etc.) :**
- [ ] JAMAIS de texte en dur — toujours `Text.translatable("clé")`
- [ ] La traduction anglaise existe dans `en_us.json`
- [ ] La traduction française existe dans `fr_fr.json`

### 1.3 Performance Minecraft
- [ ] AUCUNE allocation d'objet dans les méthodes appelées chaque tick ou chaque frame
- [ ] Pas de `new Object()` inutile dans les boucles
- [ ] Utiliser des constantes pré-allouées pour les `Text`, `Identifier`, `Formatting`, etc.

### 1.4 Architecture Fabric
- [ ] `Registry.register()` pour TOUS les enregistrements (items, blocks, entities, sounds, tabs, etc.)
- [ ] Aucun registre statique direct sans passer par `Registries.*`
- [ ] Séparation client/serveur stricte :
```bash
  grep -rn "import net.minecraft.client" src/main/java/ --include="*.java" | grep -v "/client/"
```
  Tout import client en dehors du package `client/` doit être protégé par `@Environment(EnvType.CLIENT)`
- [ ] Entrypoints déclarés dans `fabric.mod.json`
- [ ] `ClientModInitializer` séparé pour le code client

### 1.5 Pièges Fabric/Yarn
- [ ] `Identifier.of(ns, path)` pas `Identifier.fromNamespaceAndPath()`
- [ ] `Item.Settings` pas `Item.Properties`, pas de `.setId()`
- [ ] `hasGlint()` pas `isFoil()`
- [ ] `getMaxCount()` pas `getDefaultMaxStackSize()`
- [ ] `finishUsing()` pas `finishUsingItem()`
- [ ] `Text.translatable()` pas `Component.translatable()`
- [ ] `TypedActionResult` pas `InteractionResultHolder`
- [ ] `net.minecraft.item.Item` (Yarn) pas `net.minecraft.world.item.Item`

---

## 2. AUDIT RESSOURCES — Assets et data packs

### 2.1 Modèles JSON
- [ ] Le JSON est valide
- [ ] Les paths de textures sont corrects et pointent vers des fichiers existants
- [ ] Le parent est correct (`item/generated`, `item/handheld`, `block/cube_all`, etc.)

### 2.2 Textures
- [ ] Format PNG valide
- [ ] Dimensions correctes (16×16 pour les items et blocs standards, 32×32 pour les blocs du Level 0)
- [ ] Style pixel art Minecraft respecté

### 2.3 Worldgen JSON
- [ ] `minecraft:tree` configured_feature utilise `dirt_provider` (pas `below_trunk_provider`)
- [ ] Tous les fichiers JSON sont valides avant push

### 2.4 Fichiers de langue
- [ ] Vérifier que CHAQUE clé `Text.translatable("key")` a sa traduction dans `en_us.json` et `fr_fr.json`
- [ ] Aucune clé orpheline

### 2.5 Blockstates et loot tables
- [ ] Chaque block a un blockstate JSON correspondant
- [ ] Chaque block a une loot table dans `data/petasse_gang_additions/loot_table/blocks/`

---

## 3. AUDIT TESTS

### 3.1 Exécution des tests existants
```bash
./gradlew test 2>&1
```
- [ ] BUILD SUCCESSFUL (avec `ignoreFailures = true`, les failures Bootstrap ne bloquent pas)
- [ ] Les tests purement Java (ModLoadTest, ConfigTest, layout sans Bootstrap) passent

### 3.2 Couverture de la feature
- [ ] Chaque nouvelle classe/méthode publique a au moins un test
- [ ] Chaque nouvel élément enregistré est vérifié non-null
- [ ] Messages d'assertions en français

---

## 4. AUDIT LINTING & PROPRETÉ

```bash
# Analyse statique rapide
echo "=== System.out/err ==="
grep -rn "System\.out\|System\.err" src/main/java/ --include="*.java"

echo "=== printStackTrace ==="
grep -rn "printStackTrace" src/main/java/ --include="*.java"

echo "=== Imports wildcard ==="
grep -rn "import .*\.\*;" src/main/java/ --include="*.java"

echo "=== Imports Forge résiduels ==="
grep -rn "net\.minecraftforge\|net\.minecraft\.world\.item\|net\.minecraft\.world\.level\.block" src/main/java/ --include="*.java"

echo "=== below_trunk_provider dans les JSON ==="
grep -rn "below_trunk_provider" src/main/resources/ --include="*.json"
```
- [ ] Aucun `System.out` / `System.err`
- [ ] Aucun `printStackTrace()`
- [ ] Aucun import wildcard
- [ ] Aucun import Forge résiduel (`net.minecraftforge.*`, `net.minecraft.world.item.*`)
- [ ] Aucun `below_trunk_provider` dans les JSON worldgen

---

## 5. AUDIT SÉCURITÉ & ROBUSTESSE

### 5.1 Thread safety
- [ ] Pas de modification d'état partagé depuis les event handlers sans synchronisation

### 5.2 Compatibilité serveur
```bash
grep -rn "import net.minecraft.client" src/main/java/com/petassegang/addons/ --include="*.java" | grep -v "/client/"
```
- [ ] Aucun import client dans les classes communes

---

## 6. AUDIT DOCUMENTATION

### 6.1 Mise à jour obligatoire
Pour chaque fichier modifié :
| Fichier modifié | Doc à mettre à jour |
|---|---|
| Nouvel item | `docs/ITEMS.md` |
| Nouveau block | `docs/BLOCKS.md` |
| Nouvelle dimension | `docs/DIMENSIONS.md` |
| Changement de build.gradle | `docs/ARCHITECTURE.md` + `docs/SETUP.md` |
| Nouveau test | `docs/TESTING.md` |
| Tout changement | `docs/CHANGELOG.md` |

- [ ] `docs/CHANGELOG.md` est mis à jour
- [ ] README.md racine est à jour si nécessaire

---

## 7. AUDIT NON-RÉGRESSION

```bash
./gradlew clean build --warning-mode all 2>&1
```
- [ ] BUILD SUCCESSFUL sans aucun warning de compilation
- [ ] Les items existants ont toujours leurs propriétés correctes
- [ ] Les creative tabs existants contiennent toujours tous leurs items
- [ ] Les traductions existantes sont toujours présentes et correctes

---

## 8. NETTOYAGE

```bash
git status --short 2>/dev/null | grep "^?"
```
- [ ] Supprimer les scripts de génération one-shot
- [ ] Supprimer les fichiers `.bak`, `.tmp`, `.orig`
- [ ] Aucune texture/modèle/traduction orphelin
- [ ] Aucune classe Java non référencée

---

## 9. CHECKLIST FINALE PRÉ-PUSH

### Code
- [ ] `./gradlew clean build --warning-mode all` → BUILD SUCCESSFUL, 0 warning
- [ ] `./gradlew test` → BUILD SUCCESSFUL (failures Bootstrap tolérées)
- [ ] Aucun `System.out`, `printStackTrace`, import wildcard, code mort
- [ ] Commentaires en français, avec majuscule et point final
- [ ] Contenu in-game via `Text.translatable()`, traduit en EN et FR
- [ ] Séparation client/serveur respectée
- [ ] `Registry.register()` pour tous les enregistrements

### Ressources
- [ ] Tous les JSON sont valides
- [ ] `dirt_provider` dans `minecraft:tree` (pas `below_trunk_provider`)
- [ ] Toutes les clés de traduction présentes en EN et FR

### Documentation
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] README.md racine vérifié

---

## 10. RAPPORT PRÉ-PUSH

```
╔══════════════════════════════════════════════════════════════╗
║           RAPPORT PRÉ-PUSH — PeTaSsE_gAnG_Additions         ║
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
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  README.md : ✅ À jour / 📝 Mis à jour / ℹ️ Pas de modif   ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  VERDICT : ✅ PRÊT À PUSH / ❌ BLOCAGES RESTANTS            ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

---

## RÈGLES ABSOLUES

1. **Ne JAMAIS ignorer un problème** — tout doit être signalé et corrigé
2. **Corriger AVANT de signaler** — ne pas lister sans agir
3. **Zéro régression** — les features existantes doivent fonctionner identiquement
4. **Commentaires et erreurs en français** — c'est non négociable
5. **Contenu in-game traduit EN + FR** — via le système de langue Minecraft
6. **Documentation à jour** — /docs et README reflètent l'état réel
7. **Build propre** — zéro warning, zéro erreur
8. **Pas de `below_trunk_provider`** — utiliser `dirt_provider` en MC 1.21.1
9. **Pas d'imports Forge résiduels** — vérifier `net.minecraftforge`, `net.minecraft.world.item`

Lance l'audit maintenant en commençant par le point 1.
