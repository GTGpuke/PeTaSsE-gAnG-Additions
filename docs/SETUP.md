# Setup — PeTaSsE_gAnG_Additions

## Prérequis

| Outil | Version | Vérification |
|-------|---------|-------------|
| Java JDK | **25** | `java -version` |
| Gradle | **9.3.0+** (optionnel, le wrapper suffit) | `gradle --version` |
| Git | any | `git --version` |
| VS Code | latest | — |

---

## 1. Installer Java 25

### Windows (recommandé : Temurin via winget)
```powershell
winget install EclipseAdoptium.Temurin.25.JDK
```

### Windows (SDKMAN via Git Bash)
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-tem
```

### Vérifier
```bash
java -version
# java version "25" ...
```

### Configurer JAVA_HOME
```powershell
# PowerShell (admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25", "Machine")
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

Alternativement, télécharge le [Forge MDK](https://files.minecraftforge.net) pour
MC 26.1 (Forge 62.0.x) qui contient déjà le wrapper.

---

## 4. Setup VS Code

### Extensions recommandées
- **Extension Pack for Java** (Microsoft)
- **Gradle for Java** (Microsoft)

### Lancer le client Minecraft (méthode recommandée)

Le projet dispose d'une tâche VS Code préconfigurée :

**Ctrl+Shift+B** → sélectionner **runClient**

Ou depuis le terminal (PowerShell) :
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.2"; ./gradlew runClient
```

> **Note FG7 :** Les fichiers `.launch` générés par `genEclipseRuns` ne fonctionnent pas directement dans VS Code (containers classpath Eclipse-only). Toujours passer par Gradle ou la tâche VS Code.

### Régénérer les run configs (optionnel)

```bash
./gradlew genEclipseRuns
```
Les `.launch` sont ignorés par git (`.gitignore`). À régénérer après un `clean`.

### Vérifier l'import Gradle
VS Code devrait automatiquement détecter `build.gradle` et proposer d'importer le projet.
Si ce n'est pas le cas : **Ctrl+Shift+P → Java: Clean Java Language Server Workspace**.

---

## 5. Commandes Gradle utiles

```bash
# Générer les run configs (à faire une fois)
./gradlew genEclipseRuns

# Lancer le client Minecraft avec le mod
./gradlew runClient  # ou via IDE après genEclipseRuns

# Lancer le serveur dédié
./gradlew runServer  # ou via IDE après genEclipseRuns

# Build (produit build/libs/petasse_gang_additions-0.1.0.jar)
./gradlew build

# Tests unitaires
./gradlew test

# Tests in-game (GameTest framework)
./gradlew runGameTestServer

# Génération de données (data generation)
./gradlew runData

# Nettoyer le build
./gradlew clean

# Tout nettoyer et rebuild
./gradlew clean build
```

---

## 6. Installer le mod sur le serveur

1. Build : `./gradlew build`
2. Copie `build/libs/petasse_gang_additions-0.1.0.jar` dans le dossier `mods/` du serveur
3. Le serveur et TOUS les clients doivent avoir le même JAR
4. Redémarre le serveur

---

## Problèmes courants

Voir [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
