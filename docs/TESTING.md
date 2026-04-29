# Testing — PeTaSsE_gAnG_Additions

## Vue d'ensemble

| Type | Framework | Commande | Rapport |
|------|-----------|---------|---------|
| Tests unitaires | JUnit 5 | `./gradlew test` | `build/reports/tests/test/index.html` |
| Benchmark perf Level 0 | JavaExec local | `./gradlew benchmarkLevelZeroGeneration` | Console |
| Monitor perf runtime | Client dev opt-in | `./gradlew runClient -PdebugPerformanceMonitor=true` | Logs + debug HUD |

---

## Tests unitaires (JUnit 5)

> **Note Bootstrap :** Les tests qui instancient des objets Minecraft (items, blocs, block entities)
> nécessitent que le moteur Minecraft soit initialisé via `SharedConstants.createGameVersion()` +
> `Bootstrap.bootstrap()` dans un `@BeforeAll`. Sans ça, ils échouent avec
> `ExceptionInInitializerError` dans `DataComponentTypes` ou `SoundEvents`.
> Les tests de constantes pures (ModLoadTest, ConfigTest) fonctionnent sans bootstrap.

> **Note encodage Windows :** Si le chemin du projet contient un caractère accentué (ex : `Développement`),
> la propriété `-Dfile.encoding=COMPAT` dans `gradle.properties` est obligatoire pour que le worker
> Gradle trouve les classes de test. Cette propriété est déjà présente dans le repo.

```bash
# Lancer tous les tests
./gradlew test

# Lancer un test spécifique
./gradlew test --tests "com.petassegang.addons.ModLoadTest"

# Lancer les tests en mode verbose
./gradlew test --info

# Re-run même si rien n'a changé
./gradlew test --rerun
```

### Lire les rapports
```
build/reports/tests/test/index.html   ← rapport HTML complet
build/test-results/test/              ← XML JUnit (pour CI)
```

---

## Benchmark de performance — Level 0

Le Level 0 empile beaucoup de logique de génération et de rendu adaptatif. Avant
de superposer plusieurs couches de génération, on garde un benchmark local
déterministe pour vérifier qu'une révision reste dans la même range de coût.

```bash
# Lancer le benchmark local
./gradlew benchmarkLevelZeroGeneration

# Lancer avec un budget maximal par chunk
./gradlew benchmarkLevelZeroGeneration -PlevelZeroPerfBudgetMsPerChunk=0.350
```

Le benchmark :
- utilise toujours la même liste de seeds ;
- scanne la même zone de chunks ;
- affiche le temps moyen total, le temps moyen par chunk et des compteurs
  utiles (`wallColumns`, `exposedColumns`, `mixedColumns`, `faceSamples`,
  structures, lights, roles de cellules et points gameplay) ;
- échoue si le budget `levelZeroPerfBudgetMsPerChunk` est dépassé.

Cette vérification sert surtout à détecter :
- une régression silencieuse entre deux révisions du générateur ;
- une explosion du nombre de murs mixtes ;
- une hausse anormale du nombre de sondes nécessaires pour le papier peint adaptatif.

---

## Structure des tests

```text
src/test/java/com/petassegang/addons/
├── ModLoadTest.java                       ← constantes MOD_ID, LOGGER (pas de bootstrap)
├── RegistryTest.java                      ← champs de registre non-null
├── ItemTest.java                          ← propriétés GangBadgeItem (besoin bootstrap)
├── ConfigTest.java                        ← constantes ModConfig (pas de bootstrap)
├── CursedSnackTest.java                   ← propriétés CursedSnackItem (besoin bootstrap)
├── CursedTreeTest.java                    ← registre blocs Arbre Maudit (besoin bootstrap)
├── BackroomsLevelZeroLayoutTest.java      ← invariants layout Level 0 (partiel sans bootstrap)
├── BackroomsLevelZeroRegistryTest.java    ← registre Level 0 (besoin bootstrap)
└── gametest/
    └── PetasseGangGameTests.java          ← stub @Deprecated (Forge GameTest non applicable)
```

---

## Conventions — Ajouter un test

### Pour un item (sans bootstrap — constantes seulement)
```java
@Test
@DisplayName("FooItem stack size is 64")
void testFooItemStackSize() {
    // Tester uniquement les propriétés qui ne nécessitent pas le moteur MC
    assertEquals(64, FooItem.DEFAULT_MAX_COUNT, "La taille de pile doit être 64.");
}
```

### Pour un registre (champ non-null)
```java
// Dans RegistryTest.java — ajouter :
@Test
@DisplayName("FOO_ITEM n'est pas null")
void testFooItemNotNull() {
    assertNotNull(ModItems.FOO_ITEM, "Le champ FOO_ITEM doit être non-null.");
}
```

### Pour un item avec bootstrap
```java
@BeforeAll
static void bootstrapMinecraft() {
    SharedConstants.createGameVersion();
    Bootstrap.bootstrap();
}

@Test
@DisplayName("FooItem stack size is 64")
void testFooItemStackSize() {
    FooItem item = new FooItem(new Item.Settings().maxCount(64));
    assertEquals(64, item.getMaxCount(), "La taille de pile doit être 64.");
}
```

---

## Stratégie de test par type de contenu

| Type | Ce qu'on teste |
|------|----------------|
| Item | maxCount, rarity, hasGlint, tooltip lines, useDuration |
| Block | hardness, resistance, drops (loot table présente) |
| Entity | EntityType non-null, despawnDistance |
| Config | Valeurs par défaut des constantes |
| Registry | Tous les champs de registre non-null avant enregistrement |

---

## Taux de couverture attendu

- **Chaque nouveau contenu** (item, bloc, entité) → au minimum 1 test dans la classe de test correspondante
- Les tests purement Java (pas de bootstrap Minecraft) doivent tous passer avant tout merge dans `develop` ou `main`
- Les tests nécessitant Bootstrap sont tolérés en failure avec `ignoreFailures = true` jusqu'à ajout du setup `@BeforeAll`
