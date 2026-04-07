---
name: pre-push-audit
description: "Audit pré-push obligatoire avant tout commit final, push ou déclaration de feature terminée. Utilise ce skill AUTOMATIQUEMENT dès que l'utilisateur dit 'push', 'c'est bon', 'c'est fini', 'c'est prêt', 'commit', 'merge', 'on push', 'terminé', 'fini', 'done', 'prêt à push', 'vérifie avant de push', 'audit', 'check'. Ne JAMAIS déclarer une feature prête sans avoir exécuté ce skill en entier."
---

# Audit pré-push — PeTaSsE_gAnG_Additions

## INSTRUCTIONS D'EXÉCUTION — LIS CECI EN PREMIER

**Ce skill est un audit SÉQUENTIEL. Tu dois le suivre étape par étape, dans l'ordre, sans en sauter aucune.**

### Règles d'exécution OBLIGATOIRES :

1. **Exécute CHAQUE commande bash listée** — ne simule pas le résultat, lance réellement la commande et montre la sortie.
2. **Après chaque commande, analyse la sortie** et coche la case :
   - `✅` si le check passe
   - `❌` si le check échoue — dans ce cas, CORRIGE immédiatement avant de passer au point suivant
   - `⚠️` si tu ne peux pas vérifier (explique pourquoi)
3. **Ne résume PAS les résultats** — montre la sortie réelle de chaque commande.
4. **Ne saute AUCUNE étape** — même si tu penses que "c'est bon". Exécute quand même.
5. **Ne dis JAMAIS "tout semble correct" sans preuve** — chaque ✅ doit être appuyé par la sortie d'une commande ou une vérification concrète.
6. **Si une correction est nécessaire** : corrige → ré-exécute la commande → confirme que c'est ✅ → continue.
7. **À la fin, affiche le rapport complet** avec le verdict.

### Workflow :
```
Pour chaque section de l'audit :
  1. Annonce la section ("## Audit X — Nom")
  2. Pour chaque point de la section :
     a. Exécute la commande ou vérifie le fichier
     b. Montre la sortie
     c. Marque ✅ / ❌ / ⚠️
     d. Si ❌ : corrige → ré-vérifie → marque ✅
  3. Résumé de la section (X/Y points passent)
```

---

## ÉTAPE 1 — Compilation et build

Exécute ces commandes UNE PAR UNE et montre chaque sortie :

```bash
echo "========== ÉTAPE 1.1 — Clean build =========="
./gradlew clean build --warning-mode all 2>&1
```

Vérifie dans la sortie :
- ✅/❌ BUILD SUCCESSFUL ?
- ✅/❌ Zéro warning ? (cherche "warning" dans la sortie)
- ✅/❌ Zéro erreur ?
- ✅/❌ Zéro API dépréciée ? (cherche "deprecated")

Si ❌ sur l'un de ces points : corrige le code, ré-exécute, confirme ✅.

```bash
echo "========== ÉTAPE 1.2 — Vérification du JAR =========="
ls -lh build/libs/petasse_gang_additions-*.jar
echo "--- Contenu du JAR ---"
jar tf build/libs/petasse_gang_additions-*.jar | head -40
```

Vérifie :
- ✅/❌ Le JAR existe et a une taille > 10KB ?
- ✅/❌ Contient les classes dans `com/petassegang/addons/` ?
- ✅/❌ Contient `META-INF/mods.toml` ?
- ✅/❌ Contient les assets dans `assets/petasse_gang_additions/` ?

---

## ÉTAPE 2 — Analyse statique du code

Exécute CHAQUE commande et analyse la sortie :

```bash
echo "========== ÉTAPE 2.1 — System.out / System.err =========="
grep -rn "System\.out\|System\.err" src/main/java/ --include="*.java" || echo "✅ Aucun System.out/err trouvé."
```

```bash
echo "========== ÉTAPE 2.2 — printStackTrace =========="
grep -rn "printStackTrace" src/main/java/ --include="*.java" || echo "✅ Aucun printStackTrace trouvé."
```

```bash
echo "========== ÉTAPE 2.3 — Imports wildcard =========="
grep -rn "import .*\.\*;" src/main/java/ --include="*.java" || echo "✅ Aucun import wildcard trouvé."
```

```bash
echo "========== ÉTAPE 2.4 — TODO/FIXME/HACK oubliés =========="
grep -rn "TODO\|FIXME\|HACK\|XXX\|TEMP" src/main/java/ --include="*.java" || echo "✅ Aucun TODO/FIXME oublié."
```

```bash
echo "========== ÉTAPE 2.5 — @SuppressWarnings =========="
grep -rn "@SuppressWarnings" src/main/java/ --include="*.java" || echo "✅ Aucun @SuppressWarnings."
```

```bash
echo "========== ÉTAPE 2.6 — Trailing whitespace =========="
grep -rn " $" src/main/java/ --include="*.java" | head -5 || echo "✅ Aucun trailing whitespace."
```

```bash
echo "========== ÉTAPE 2.7 — Imports client hors package client/ =========="
grep -rn "import net.minecraft.client" src/main/java/com/petassegang/addons/ --include="*.java" | grep -v "/client/" | grep -v "compat/" || echo "✅ Aucun import client dans les classes communes."
```

Pour chaque résultat non vide : CORRIGE immédiatement → ré-exécute → confirme ✅.

---

## ÉTAPE 3 — Commentaires et messages

```bash
echo "========== ÉTAPE 3.1 — Commentaires en français =========="
echo "Affichage de tous les commentaires pour vérification manuelle :"
grep -rn "^\s*//" src/main/java/com/petassegang/addons/ --include="*.java" | head -30
```

Vérifie CHAQUE commentaire affiché :
- ✅/❌ En français ?
- ✅/❌ Commence par une majuscule ?
- ✅/❌ Finit par un point ?

```bash
echo "========== ÉTAPE 3.2 — Messages de log =========="
grep -rn "LOGGER\.\(info\|warn\|error\|debug\)" src/main/java/ --include="*.java" | head -20
```

Vérifie CHAQUE message de log :
- ✅/❌ En français ?
- ✅/❌ Majuscule + point ?
- ✅/❌ Contexte pertinent dans le message ?

```bash
echo "========== ÉTAPE 3.3 — Textes in-game non traduits (texte en dur) =========="
grep -rn 'new TextComponent\|Component.literal' src/main/java/ --include="*.java" | grep -v "test\|Test" || echo "✅ Aucun texte en dur trouvé."
```

Si des `Component.literal()` sont trouvés dans le code principal (pas les tests) : remplace par `Component.translatable()` → ajoute les clés dans les fichiers de langue.

---

## ÉTAPE 4 — Fichiers de langue et traductions

```bash
echo "========== ÉTAPE 4.1 — Extraction des clés utilisées dans le code =========="
grep -rohn 'translatable("[^"]*"' src/main/java/ --include="*.java" | sed 's/.*translatable("//;s/".*//' | sort -u > /tmp/keys_code.txt
cat /tmp/keys_code.txt
```

```bash
echo "========== ÉTAPE 4.2 — Clés dans en_us.json =========="
python3 -c "import json; [print(k) for k in sorted(json.load(open('src/main/resources/assets/petasse_gang_additions/lang/en_us.json')).keys())]" > /tmp/keys_en.txt
cat /tmp/keys_en.txt
```

```bash
echo "========== ÉTAPE 4.3 — Clés dans fr_fr.json =========="
python3 -c "import json; [print(k) for k in sorted(json.load(open('src/main/resources/assets/petasse_gang_additions/lang/fr_fr.json')).keys())]" > /tmp/keys_fr.txt
cat /tmp/keys_fr.txt
```

```bash
echo "========== ÉTAPE 4.4 — Clés manquantes =========="
echo "--- Manquantes dans en_us.json ---"
comm -23 /tmp/keys_code.txt /tmp/keys_en.txt
echo "--- Manquantes dans fr_fr.json ---"
comm -23 /tmp/keys_code.txt /tmp/keys_fr.txt
echo "--- Clés orphelines dans en_us.json (plus utilisées) ---"
comm -23 /tmp/keys_en.txt /tmp/keys_code.txt
echo "--- Clés orphelines dans fr_fr.json (plus utilisées) ---"
comm -23 /tmp/keys_fr.txt /tmp/keys_code.txt
```

- ✅/❌ Aucune clé manquante en EN ?
- ✅/❌ Aucune clé manquante en FR ?
- ✅/❌ Aucune clé orpheline ?

Si ❌ : ajoute les clés manquantes, supprime les orphelines → ré-exécute → confirme ✅.

---

## ÉTAPE 5 — Ressources (JSON, textures, modèles)

```bash
echo "========== ÉTAPE 5.1 — Validation de tous les JSON =========="
find src/main/resources -name "*.json" | while read f; do
  python3 -c "import json; json.load(open('$f'))" 2>&1 && echo "✅ $f" || echo "❌ $f"
done
```

```bash
echo "========== ÉTAPE 5.2 — Vérification des textures =========="
find src/main/resources -name "*.png" | while read f; do
  python3 -c "
from PIL import Image
import sys
try:
    img = Image.open('$f')
    w, h = img.size
    status = '✅' if w in [16,32,64,128,256] and h in [16,32,64,128,256] else '⚠️  Dimensions non standard'
    print(f'{status} $f: {w}x{h}')
except Exception as e:
    print(f'❌ $f: {e}')
" 2>/dev/null || echo "⚠️  PIL non dispo, vérification manuelle de $f avec file"
  file "$f"
done
```

```bash
echo "========== ÉTAPE 5.3 — Textures sans modèle associé =========="
for f in src/main/resources/assets/petasse_gang_additions/textures/item/*.png 2>/dev/null; do
  name=$(basename "$f" .png)
  [ ! -f "src/main/resources/assets/petasse_gang_additions/models/item/$name.json" ] && echo "⚠️  Texture orpheline : $f"
done || echo "✅ Aucune texture orpheline."
echo "--- Modèles sans texture ---"
for f in src/main/resources/assets/petasse_gang_additions/models/item/*.json 2>/dev/null; do
  name=$(basename "$f" .json)
  [ ! -f "src/main/resources/assets/petasse_gang_additions/textures/item/$name.png" ] && echo "⚠️  Modèle orphelin : $f"
done || echo "✅ Aucun modèle orphelin."
```

### Cohérence artistique Minecraft
Pour chaque NOUVELLE texture, vérifie visuellement (ouvre le fichier ou décris-le) :
- ✅/❌ Style pixel art 16x16 Minecraft ?
- ✅/❌ Pas de couleurs néon/fluo incohérentes ?
- ✅/❌ Ombrage haut-gauche (convention Minecraft) ?
- ✅/❌ Contours en teintes sombres (pas de noir pur #000000) ?
- ✅/❌ Pas d'anti-aliasing (pixels nets) ?
- ✅/❌ Cohérent avec les textures existantes du mod ?

---

## ÉTAPE 6 — Tests

```bash
echo "========== ÉTAPE 6.1 — Exécution des tests =========="
./gradlew test 2>&1
```

- ✅/❌ BUILD SUCCESSFUL ?
- ✅/❌ Tous les tests passent ? (aucun FAILED)
- ✅/❌ Aucun test skippé ?

```bash
echo "========== ÉTAPE 6.2 — Liste des fichiers de test =========="
find src/test -name "*.java" -type f 2>/dev/null
```

```bash
echo "========== ÉTAPE 6.3 — Fichiers source SANS test correspondant =========="
for f in $(find src/main/java/com/petassegang/addons -name "*.java" ! -name "ModConstants.java" ! -name "*Mod.java" -type f); do
  cls=$(basename "$f" .java)
  if ! find src/test -name "${cls}Test.java" -type f 2>/dev/null | grep -q .; then
    echo "⚠️  Pas de test pour : $cls"
  fi
done
```

Si des classes n'ont pas de test : CRÉE les tests manquants → ré-exécute `./gradlew test` → confirme ✅.

Vérifie aussi la qualité des tests :
```bash
echo "========== ÉTAPE 6.4 — Messages d'assertions en français =========="
grep -rn "assert\|Assert" src/test/java/ --include="*.java" | grep -v "import\|package" | head -20
```
- ✅/❌ Les assertions ont des messages en français ?

---

## ÉTAPE 7 — Nettoyage des fichiers inutiles

```bash
echo "========== ÉTAPE 7.1 — Fichiers temporaires =========="
find . -name "*.bak" -o -name "*.tmp" -o -name "*.swp" -o -name "*.orig" -o -name "*~" 2>/dev/null | grep -v ".gradle" | grep -v "build/"
```

```bash
echo "========== ÉTAPE 7.2 — Scripts de génération one-shot =========="
find . -maxdepth 2 -name "generate_*.py" -o -name "fix_*.sh" -o -name "temp_*" -o -name "debug_*" 2>/dev/null | grep -v ".gradle" | grep -v "build/" | grep -v "node_modules"
```

```bash
echo "========== ÉTAPE 7.3 — Classes Java potentiellement mortes =========="
for f in $(find src/main/java -name "*.java" -type f); do
  cls=$(basename "$f" .java)
  count=$(grep -rn "$cls" src/main/java/ --include="*.java" | grep -v "^$f:" | wc -l)
  if [ "$count" -eq 0 ]; then
    echo "⚠️  Classe potentiellement inutilisée : $cls ($f)"
  fi
done
```

Supprime tout ce qui doit l'être → confirme ✅.

---

## ÉTAPE 8 — Documentation

```bash
echo "========== ÉTAPE 8.1 — Fichiers modifiés (feature en cours) =========="
git diff --name-only HEAD 2>/dev/null || find . -newer build.gradle -name "*.java" -o -name "*.json" 2>/dev/null | grep -v build/ | grep -v .gradle/ | head -30
```

Détermine quels docs doivent être mis à jour :
| Fichier modifié | Doc à mettre à jour |
|---|---|
| Nouvel item | `docs/ITEMS.md` |
| Nouveau block | `docs/BLOCKS.md` |
| Nouvelle entité | `docs/ENTITIES.md` |
| Nouvelle dimension | `docs/DIMENSIONS.md` |
| Changement build.gradle | `docs/ARCHITECTURE.md` + `docs/SETUP.md` |
| Changement de test | `docs/TESTING.md` |
| Changement CI/CD | `docs/CICD.md` |
| **TOUT changement** | `docs/CHANGELOG.md` |

- ✅/❌ `docs/CHANGELOG.md` est mis à jour ?
- ✅/❌ Les docs pertinentes sont à jour ?

### Vérification du README
**POSE-TOI EXPLICITEMENT LA QUESTION :**
> "Est-ce que cette feature modifie le Quick Start, la structure du projet, les prérequis, les commandes, ou toute info du README.md racine ?"

```bash
echo "========== ÉTAPE 8.2 — Contenu actuel du README =========="
head -50 README.md
```

- ✅/❌ README.md reflète l'état actuel du projet ?
- Si mise à jour nécessaire → fais-la → confirme ✅

---

## ÉTAPE 9 — Non-régression finale

```bash
echo "========== ÉTAPE 9.1 — Clean build final =========="
./gradlew clean build --warning-mode all 2>&1
```

```bash
echo "========== ÉTAPE 9.2 — Tests finaux =========="
./gradlew test 2>&1
```

- ✅/❌ Build : BUILD SUCCESSFUL, 0 warning ?
- ✅/❌ Tests : tous passent ?

```bash
echo "========== ÉTAPE 9.3 — Vérification des fichiers existants =========="
echo "--- Items enregistrés ---"
grep -rn "DeferredRegister\|RegistryObject\|DeferredHolder" src/main/java/com/petassegang/addons/init/ --include="*.java"
echo "--- Traductions existantes ---"
python3 -c "import json; d=json.load(open('src/main/resources/assets/petasse_gang_additions/lang/en_us.json')); print(f'{len(d)} clés EN'); d=json.load(open('src/main/resources/assets/petasse_gang_additions/lang/fr_fr.json')); print(f'{len(d)} clés FR')"
echo "--- Textures existantes ---"
find src/main/resources/assets/petasse_gang_additions/textures -name "*.png" -type f
```

- ✅/❌ Tous les éléments existants sont toujours enregistrés ?
- ✅/❌ Aucune traduction supprimée ?
- ✅/❌ Aucune texture supprimée ?
- ✅/❌ La config existante est compatible ?

---

## ÉTAPE 10 — Rapport final

**Après avoir complété TOUTES les étapes précédentes, affiche ce rapport :**

Compte le nombre total de ✅, ❌ corrigés, et ⚠️ de tout l'audit.

```
╔══════════════════════════════════════════════════════════════╗
║              RAPPORT PRÉ-PUSH — PeTaSsE_gAnG_Additions         ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  Feature : [nom de la feature]                               ║
║  Version : [version dans gradle.properties]                  ║
║  Date    : [date du jour]                                    ║
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  RÉSULTATS                                                   ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  1. Build & JAR       : ✅ / ❌  [détails]                   ║
║  2. Analyse statique  : ✅ / ❌  [détails]                   ║
║  3. Commentaires & FR : ✅ / ❌  [détails]                   ║
║  4. Traductions       : ✅ / ❌  [X clés EN, X clés FR]      ║
║  5. Ressources        : ✅ / ❌  [détails]                   ║
║  6. Tests             : ✅ / ❌  [X/Y passent]               ║
║  7. Nettoyage         : ✅ / ❌  [détails]                   ║
║  8. Documentation     : ✅ / ❌  [fichiers MAJ]              ║
║  9. Non-régression    : ✅ / ❌  [détails]                   ║
║                                                              ║
╠══════════════════════════════════════════════════════════════╣
║  CORRECTIONS APPLIQUÉES : [nombre]                           ║
╠══════════════════════════════════════════════════════════════╣
║  [liste des corrections]                                     ║
╠══════════════════════════════════════════════════════════════╣
║  README.md : ✅ À jour / 📝 Mis à jour / ℹ️ Pas nécessaire ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  VERDICT : ✅ PRÊT À PUSH / ❌ BLOCAGES RESTANTS            ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
```

**Si VERDICT = ❌ :** liste les blocages, corrige-les, ré-exécute les étapes concernées, et refais le rapport.
**Si VERDICT = ✅ :** confirme "Le push peut être effectué."