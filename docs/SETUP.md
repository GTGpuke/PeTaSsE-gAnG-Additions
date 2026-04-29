# Setup — PeTaSsE_gAnG_Additions

## Prérequis

| Outil | Version | Vérification |
|-------|---------|-------------|
| Java JDK | **21** | `java -version` |
| Gradle | **9.3.0+** (optionnel, le wrapper suffit) | `gradle --version` |
| Git | any | `git --version` |
| VS Code | latest | — |

---

## 1. Installer Java 21

### Windows (recommandé : Temurin via winget)
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

### Windows (SDKMAN via Git Bash)
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
```

### Vérifier
```bash
java -version
# openjdk version "21" ...
```

### Configurer JAVA_HOME
```powershell
# PowerShell (admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21", "Machine")
```

---

## 2. Cloner le repo

```bash
git clone https://github.com/PetasseGang/petasse_gang_additions.git
cd petasse_gang_additions
```

---

## 3. Télécharger le Gradle Wrapper JAR

Le fichier binaire `gradle/wrapper/gradle-wrapper.jar` n'est pas inclus dans le repo
(voir `.gitignore`). Pour le générer :

```bash
# Option A : si Gradle 9.3 est installé globalement
gradle wrapper --gradle-version 9.3.0

# Option B : copier le JAR depuis une installation Gradle locale
# %GRADLE_HOME%\lib\plugins\gradle-wrapper-*.jar → gradle/wrapper/gradle-wrapper.jar
```

---

## 4. Setup VS Code

### Extensions recommandées
- **Extension Pack for Java** (Microsoft)
- **Gradle for Java** (Microsoft)

### Lancer le client Minecraft (méthode recommandée)

Le projet dispose d'une tâche VS Code préconfigurée :

**Ctrl+Shift+B** → sélectionner **runClient**

Ou depuis le terminal :
```bash
./gradlew runClient
```

> **Note encodage Windows :** Si le chemin du projet contient un caractère accentué (ex : `Développement`),
> la propriété `-Dfile.encoding=COMPAT` dans `gradle.properties` est nécessaire pour que le worker
> de test Gradle trouve les classes. Cette propriété est déjà présente dans le repo.

### Vérifier l'import Gradle
VS Code devrait automatiquement détecter `build.gradle` et proposer d'importer le projet.
Si ce n'est pas le cas : **Ctrl+Shift+P → Java: Clean Java Language Server Workspace**.

---

## 5. Commandes Gradle utiles

```bash
# Lancer le client Minecraft avec le mod
./gradlew runClient

# Lancer le serveur dédié
./gradlew runServer

# Build (produit build/libs/petasse_gang_additions-0.6.0.jar)
./gradlew build

# Tests unitaires
./gradlew test

# Benchmark de performance Level 0
./gradlew benchmarkLevelZeroGeneration

# Lancer le client avec le monitor de performance debug
./gradlew runClient -PdebugPerformanceMonitor=true

# Monitor de performance avec logs plus fréquents
./gradlew runClient -PdebugPerformanceMonitor=true -PperformanceLogIntervalSeconds=5

# Nettoyer le build
./gradlew clean

# Tout nettoyer et rebuild
./gradlew clean build
```

---

## 6. Installer le mod sur le serveur

1. Build : `./gradlew build`
2. Copie `build/libs/petasse_gang_additions-0.6.0.jar` dans le dossier `mods/` du serveur
3. Copie également `fabric-api-*.jar` et `fabric-loader-*.jar` si non présents
4. Le serveur et TOUS les clients doivent avoir le même JAR
5. Redémarre le serveur

---

## Problèmes courants

Voir [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
