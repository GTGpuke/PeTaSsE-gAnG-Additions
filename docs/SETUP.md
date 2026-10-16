# Setup — PeTaSsE_gAnG_Additions

## Prérequis

| Outil | Version | Vérification |
|-------|---------|--------------|
| Java JDK | **21** (Temurin recommandé) | `java -version` |
| Git | récent | `git --version` |
| Gradle | fourni par le wrapper (9.8.1) | — |

Un seul JDK suffit : Gradle tourne sur Java 21 et compile le mod avec la toolchain Java 21.
Si `JAVA_HOME` pointe sur un autre JDK, Gradle détecte automatiquement un JDK 21 installé
(Eclipse Adoptium, registre Windows, `~/.jdks`) ; sinon installer Temurin 21.

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

---

## Cloner et lancer

```bash
git clone https://github.com/GTGpuke/PeTaSsE-gAnG-Additions.git
cd PeTaSsE-gAnG-Additions
./gradlew runClient
```

Le premier lancement télécharge Minecraft, les mappings et Fabric (quelques minutes).

---

## VS Code

Extensions : *Extension Pack for Java*, *Gradle for Java*. Les tâches `.vscode/tasks.json`
(`runClient`, `runServer`, `build`, `test`, `runGameTest`) forcent `JAVA_HOME` sur Temurin 21 ;
adapter le chemin si nécessaire. **Ctrl+Shift+B** lance `runClient`.

---

## Commandes Gradle

```bash
./gradlew runClient                                   # client de dev
./gradlew runServer                                   # serveur dédié (run/server)
./gradlew test                                        # tests + couverture JaCoCo
./gradlew runGameTest                                 # tests en jeu (serveur headless)
./gradlew checkstyleMain checkstyleTest checkstyleGametest  # conventions du code (Checkstyle)
./gradlew benchmarkLevelZeroGeneration                # benchmark de génération
./gradlew build                                       # JAR : build/libs/petasse_gang_additions-<version>.jar
./gradlew clean build --warning-mode all              # build propre, doit être sans warning

# Toggles de dev (passés en -D au client et au benchmark)
./gradlew runClient -PdebugPerformanceMonitor=true -PperformanceLogIntervalSeconds=5
./gradlew runClient -PlevelZeroDebugMicroGeometry=true -PlevelZeroDebugWallProps=true
./gradlew runClient -PlevelZeroNoiseGeometry=false
./gradlew runClient -PportalFeature=true
```

> **Windows + chemin accentué** (`Développement`) : `-Dfile.encoding=COMPAT` dans `gradle.properties`
> est nécessaire pour que le worker de test trouve ses classes. Déjà présent.

---

## Installer le mod sur le serveur

1. `./gradlew build`
2. Copier `build/libs/petasse_gang_additions-<version>.jar` dans `mods/` du serveur et de chaque client.
3. Fabric Loader ≥ 0.16.9 et Fabric API pour 1.21.1 doivent être présents.
4. Redémarrer.

---

## Problèmes courants

Voir [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
