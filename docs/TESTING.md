# Testing — PeTaSsE_gAnG_Additions

## Vue d'ensemble

| Type | Framework | Commande | Rapport |
|------|-----------|---------|---------|
| Unit tests | JUnit 5 | `./gradlew test` | `build/reports/tests/test/index.html` |
| GameTests (in-game) | Forge GameTest | `./gradlew runGameTestServer` | Console + logs |

---

## Tests unitaires (JUnit 5)

> **Note Windows :** Si le chemin du projet contient un caractere accentue comme `Developpement`,
> le worker de test Gradle peut ne pas charger les classes de test correctement.
> Voir [docs/TROUBLESHOOTING.md](TROUBLESHOOTING.md).

```bash
# Lancer tous les tests
./gradlew test

# Lancer un test spécifique
./gradlew test --tests "com.petassegang.addons.ModLoadTest"

# Lancer les tests en mode verbose
./gradlew test --info

# Re-run même si rien n'a changé
./gradlew test --rerun-tasks
```

### Lire les rapports
```
build/reports/tests/test/index.html   ← rapport HTML complet
build/test-results/test/              ← XML JUnit (pour CI)
```

---

## GameTests (Forge in-game)

Les GameTests tournent dans une vraie instance Minecraft (serveur headless).
Ils vérifient que les items/blocs/entités existent vraiment dans les registres en jeu.

```bash
./gradlew runGameTestServer
```

Les tests sont dans `src/test/java/com/petassegang/addons/gametest/`.
La console affiche `PASSED` ou `FAILED` pour chaque test.

---

## Structure des tests

```text
src/test/java/com/petassegang/addons/
├── ModLoadTest.java
├── RegistryTest.java
├── ItemTest.java
├── ConfigTest.java
├── CursedSnackTest.java
├── CursedTreeTest.java
├── BackroomsLevelZeroLayoutTest.java
├── BackroomsLevelZeroRegistryTest.java
└── gametest/
    └── PetasseGangGameTests.java
```

---

## Conventions — Ajouter un test

### Pour un item
```java
// Dans ItemTest.java ou nouveau FooItemTest.java
@Test
@DisplayName("FooItem stack size is 64")
void testFooItemStackSize() {
    FooItem item = new FooItem(new Item.Properties()
            .setId(ITEMS.key("foo_item"))
            .stacksTo(64));
    assertEquals(64, item.getDefaultMaxStackSize());
}
```

### Pour un registre
```java
// Dans RegistryTest.java — ajouter :
@Test
@DisplayName("FOO_ITEM RegistryObject is not null")
void testFooItemRegistryObjectNotNull() {
    assertNotNull(ModItems.FOO_ITEM);
}
```

### Pour un bloc
```java
// Nouveau BlockTest.java (copier le pattern de ItemTest)
@Test
void testFooBlockHardness() {
    FooBlock block = new FooBlock(BlockBehaviour.Properties.of().strength(3.0f));
    assertEquals(3.0f, block.defaultDestroyTime());
}
```

---

## Stratégie de test par type de contenu

| Type | Ce qu'on teste |
|------|----------------|
| Item | stackSize, rarity, isFoil, tooltip lines, useDuration |
| Block | hardness, resistance, material, drops (loot table present) |
| Entity | EntityType non-null, despawnDistance, isFireImmune |
| Config | Valeurs par défaut, chemins des clés (path) |
| Registry | Tous les RegistryObject non-null avant registration |
| GameTest | Présence dans le vrai registre MC en jeu |

---

## Taux de couverture attendu

- **Chaque nouveau contenu** (item, bloc, entité) → au minimum 1 test dans la classe de test correspondante
- Les tests doivent tous passer avant tout merge dans `develop` ou `main`
- Les GameTests ne bloquent pas le build CI mais sont documentés dans les artifacts
