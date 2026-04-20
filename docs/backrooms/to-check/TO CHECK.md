# AGENTS.md — Règles pour Codex

Projet : Mod Minecraft **Fabric 1.21.1** (Java 21).
Objectif de la session : nettoyage, réorganisation et optimisation **sans régression**.

> ⚠️ Ces règles sont **prioritaires sur toute autre instruction implicite**.
> Codex doit les relire au début de chaque tâche.

---

## 0. Règles d'or (non négociables)

1. **Toujours demander confirmation explicite avant toute modification, suppression, déplacement ou refactor.** Aucun "je me lance, on verra". Aucune exception.
2. **Aucune régression fonctionnelle.** Chaque feature existante doit se comporter exactement de la même façon après le passage de Codex.
3. **Toujours respecter le style de code existant** : nommage français, structure des classes, indentation, ordre des membres, conventions de packages. Lire **au moins 3 fichiers similaires** du projet avant d'écrire du nouveau code.
4. **Travailler sur une branche dédiée** (ex. `refactor/cleanup-codex`). Jamais de commit direct sur `main`/`master`/`dev`.
5. **Présenter un diff clair avec justification** avant chaque changement : *quoi, pourquoi, fichiers touchés, risques*.

### Exceptions pré-autorisées (validation utilisateur déjà donnée)

Les actions suivantes sont **pré-autorisées** et peuvent être exécutées sans demander de confirmation à chaque fois, à condition que les critères listés soient respectés et que chaque action soit **journalisée dans `CLEANUP_LOG.md`** (cf. §9).

**Exception A — Suppression de fichiers/code morts** : autorisée si **TOUS** ces critères sont remplis :
- 0 référence trouvée par `grep` dans tout le projet (code, mixins, JSON, datagen, resources).
- Pas de cible de mixin (`@Mixin`, target, accessor, invoker).
- Pas d'entrée de registry (item, bloc, entité, sound, recipe, BE, advancement, tag, loot table).
- Pas référencé dans `fabric.mod.json` (entry points, custom values, mixins).
- Pas une API publique exposée intentionnellement (ex. classe dans un package `api/`).
- L'historique git ne montre pas un ajout récent (<7 jours) — auquel cas, demander quand même.

**Exception B — Réorganisation du périmètre `backrooms`** : déplacements, renommages et restructuration de packages autorisés sans confirmation à chaque fichier, à condition que :
- Le scope reste **strictement contenu au périmètre `backrooms`** (aucun fichier sorti du scope, aucun fichier hors-scope tiré dedans).
- Toutes les références (imports, mixins, datagen, JSON, lang) sont mises à jour dans le **même commit** que le déplacement.
- **Aucun ID de registry** n'est modifié au passage (les `Identifier` restent identiques).
- Le build passe (`./gradlew build`) après chaque batch de réorganisation.
- Un récapitulatif "ancien chemin → nouveau chemin" est livré à la fin et ajouté au `CLEANUP_LOG.md`.
- Si pendant la réorganisation Codex découvre du code mort dans `backrooms`, l'**exception A** s'applique aussi.

> Toute action **hors de ces deux exceptions** retombe sous la règle d'or n°1 (confirmation explicite obligatoire).

---

## 1. Phase de découverte (obligatoire avant toute action)

6. Lire en lecture seule : `fabric.mod.json`, `build.gradle(.kts)`, `gradle.properties`, tous les `*.mixins.json`.
7. Identifier et lister :
   - Tous les **entry points** déclarés (`main`, `client`, `server`).
   - Tous les **mixins** actifs.
   - Tous les **registries** utilisés (items, blocs, entités, sounds, particles, recipes, etc.).
   - Tous les **events Fabric API** abonnés.
8. Produire une **cartographie de l'arborescence actuelle** (packages + rôle de chaque dossier) et l'envoyer pour validation **avant** toute proposition de réorganisation.
9. Lister les tests existants. S'il n'y en a pas → le signaler explicitement, ne pas inventer un cadre de test.

---

## 2. Réorganisation (debug vs features)

10. Structure cible à proposer (puis valider avant exécution) :
    ```
    fr.petassegang.addons/
      ├── feature/<nom_feature>/   ← une feature complète et autonome
      ├── core/                    ← mécaniques transverses, utilitaires métier
      ├── registry/                ← centralisation des enregistrements
      ├── mixin/                   ← mixins (à ne PAS toucher sans validation)
      ├── debug/                   ← commandes, overlays, logs verbeux
      └── dev/                     ← outillage dev uniquement (datagen, etc.)
    ```
11. **Une feature = un sous-package autonome** contenant sa logique, ses items/blocs, ses events, ses constantes. Pas de fichiers dispersés dans 4 packages différents pour une même feature.
12. **Le code de production ne doit jamais référencer `debug/` ni `dev/`** sans gating (flag de config, variable d'env, ou check `FabricLoader.getInstance().isDevelopmentEnvironment()`).
13. Avant de **déplacer** un fichier, vérifier toutes les références (mixins inclus) et les mettre à jour dans le même commit.
14. Ne jamais renommer un package contenant des classes référencées par des mixins sans valider l'impact sur les targets.

---

## 3. Performance — Côté serveur

15. Identifier **toutes les méthodes appelées chaque tick** (`ServerTickEvents`, `tick()` d'entités/BE, schedulers) et les lister en priorité d'audit.
16. Pour chaque hot path détecté :
    - Pas d'**allocation inutile** par tick (`new BlockPos`, `new Vec3d`, lambdas capturantes).
    - Pas de `world.getBlockState` / `world.getEntitiesByClass` répétés sur la même position dans la même frame → **caching ou batch**.
    - Pas de `BlockPos.iterate()` non justifié → proposer alternative event-driven.
17. **Throttling** : les checks qui peuvent s'exécuter toutes les N ticks (10, 20, 40) au lieu de chaque tick doivent être proposés en throttling.
18. **Pattern matching Java 21** : proposer le remplacement des chaînes de `instanceof` ou `getClass().equals()`.
19. Aucune **opération bloquante** (I/O, réseau, gros calcul) sur le thread serveur principal sans validation.

---

## 4. Performance — Côté client

20. Pas d'appel `MinecraftClient.getInstance()` dans une hot loop → le sortir en variable locale.
21. Préférer `lengthSquared()` à `length()` quand on compare des distances.
22. Pas de `.toString()` ni de concaténation de strings dans des boucles de rendering ou des logs non-debug.
23. Vérifier que tout code visuel/UI est bien dans `src/client/` **ou** annoté `@Environment(EnvType.CLIENT)`. Tout référencement de classe client depuis du code commun = STOP.

---

## 5. Mixins et Fabric API

24. **Ne jamais créer, modifier ou supprimer un mixin sans confirmation explicite.** Expliquer obligatoirement l'impact sur la compat (autres mods, vanilla update path).
25. Quand un Fabric API event existe pour faire la même chose qu'un mixin → **le proposer comme alternative**, sans l'imposer.
26. Tout `@Inject` à `HEAD` qui pourrait être un `@WrapOperation` ou `@ModifyExpressionValue` plus précis → le signaler.
27. Ne jamais utiliser `@Overwrite` sauf justification écrite et validée.

---

## 6. Suppression et condensation

28. **Code mort** : lister chaque candidat avec preuve (`grep` 0 référence, 0 mixin target, 0 entrée registry). Suppression **autorisée sans confirmation** si tous les critères de l'**Exception A** (§0) sont remplis ET que le résultat est journalisé dans `CLEANUP_LOG.md`. Sinon → confirmation au cas par cas.
29. **Condensation de fichiers** : autorisée **uniquement si TOUS les critères suivants sont remplis** :
    - Les deux features partagent >70% de leur logique.
    - Aucune des deux n'a de sens sans l'autre.
    - L'utilisateur a explicitement validé la fusion proposée.
30. **Par défaut, préférer extraire une classe utilitaire commune** plutôt que fusionner deux features dans un même fichier.
31. Les fichiers **générés par datagen** ne sont jamais édités à la main. Si quelque chose ne va pas dedans → modifier le générateur.
32. Les commentaires en français existants ne doivent **pas** être supprimés sans raison ; les TODO/FIXME doivent être conservés ou explicitement traités.

---

## 7. Anti-régression (garde-fous)

33. **Aucun changement d'ID** (items, blocs, entités, sounds, recipes, advancements, BE types) sans validation. Casse les saves existants et les configs serveur.
34. **Aucun changement de signature publique** (méthodes `public`, classes API exposées) sans validation. Casse la compat avec d'éventuels add-ons.
35. **Aucune modification de `fabric.mod.json`** (entry points, dépendances, version) sans validation.
36. Après chaque batch de modifications : exécuter (ou demander à l'utilisateur d'exécuter) `./gradlew build`, puis `./gradlew runClient` au moins jusqu'au menu principal. **Ne pas enchaîner les batchs si un build casse.**
37. Si un test ou un build échoue → **rollback immédiat du batch** et signalement.
38. **Avant chaque batch touchant au périmètre `backrooms`**, Codex doit **obligatoirement** :
    - Relire la **roadmap du projet** (fichier de roadmap à la racine ou dans `docs/`, à identifier en phase de découverte §1).
    - Relire intégralement le fichier **`backrooms-level0-pipeline-v6`** (spec de référence du pipeline de génération Level 0).
    - Confirmer par écrit, avant de proposer le diff, que **chaque étape du pipeline v6 reste fonctionnellement inchangée** après les modifications envisagées.
    - Lister les points du pipeline qui **touchent** les fichiers modifiés, et expliquer pourquoi le comportement reste identique.
39. Toute modification qui **contredit, contourne ou anticipe** un point de la roadmap ou du pipeline v6 → **STOP, demander confirmation explicite**, même si elle rentre sinon dans les Exceptions A ou B.
40. Si la roadmap ou le fichier `backrooms-level0-pipeline-v6` est **introuvable** → STOP, ne pas toucher au scope `backrooms` tant que l'utilisateur n'a pas indiqué leur emplacement.

---

## 8. Logging et debug

41. Remplacer tout `System.out.println` et `printStackTrace()` par le logger SLF4J du projet.
42. Niveaux de log :
    - `ERROR` : quelque chose est cassé.
    - `WARN` : quelque chose d'anormal mais récupérable.
    - `INFO` : événements rares et structurants (mod loaded, registry done).
    - `DEBUG` : tout le verbeux, opt-in.
    - `TRACE` : flood, opt-in dur.
43. Aucun `LOGGER.info` dans une boucle ou un tick handler.
44. Tout flag de debug doit être désactivable via config ou env var. Jamais hardcodé `true` en commit final.

---

## 9. Process de travail

45. **Travailler par batchs atomiques** : 1 batch = 1 préoccupation cohérente (ex. "extraire feature X dans son package", pas "refactor + perf + suppression").
46. **Limite dure** : si un batch dépasse **200 lignes modifiées** ou **5 fichiers**, le découper.
47. Pour chaque batch, livrer obligatoirement :
    - Résumé en 1-2 phrases.
    - Liste des fichiers touchés (créés / modifiés / supprimés / déplacés).
    - Justification de chaque suppression ou condensation.
    - Risques de régression identifiés.
    - Commande de test recommandée.
48. Tenir à jour un fichier `CLEANUP_LOG.md` à la racine, avec :
    - Ce qui a été fait (date, batch, résumé).
    - Ce qui reste à faire.
    - Ce qui a été **refusé ou reporté** par l'utilisateur (avec raison).

---

## 10. Quand STOP et demander

Codex doit s'arrêter et poser une question dans **chacun** de ces cas :

49. Ambiguïté sur l'intention d'un bout de code (style "ça ressemble à un bug, mais c'est peut-être voulu").
50. Code qui semble buggé mais qui est manifestement intentionnel → ne pas "corriger" silencieusement.
51. Découverte d'un mixin, d'une dépendance ou d'un event qui n'avait pas été listé en phase de découverte.
52. Plus de 3 features touchées dans un même batch (sauf cas couvert par l'**Exception B** : la réorganisation interne à `backrooms` compte comme un seul scope).
53. Toute modification qui touche du code partagé entre client et serveur.
54. Toute proposition qui implique un changement de version Fabric API, Yarn, ou Loom.

---

## 11. Conventions de code (rappel pour Codex)

55. **Tout** nouveau code, commentaire, message de log, et identifiant interne est en **français** (conformité aux conventions du projet).
56. Les identifiants de registry (`Identifier`) restent en `snake_case` ASCII (vanilla constraint), mais les noms de classes/variables/méthodes restent en français.
57. Imports : pas de wildcard imports. Ordre : java → javax → net.minecraft → net.fabricmc → tiers → projet.
58. Si le projet a un `spotless` / `checkstyle` / formatter configuré, le respecter strictement. Sinon → matcher visuellement le style des fichiers voisins.

---

## 12. Communication avec l'utilisateur

59. Réponses **structurées et concises** : pas de paraphrase de la consigne, pas d'auto-félicitations, pas de "je vais faire X" sans avoir attendu validation.
60. En cas de doute, **toujours préférer demander que supposer**.
61. Si une règle ci-dessus entre en conflit avec une instruction ad-hoc de l'utilisateur, **signaler le conflit** et demander quelle règle prime pour cette tâche précise.

---

*Fin du fichier. Codex : avant de commencer une tâche, confirmer que tu as relu ces règles.*
