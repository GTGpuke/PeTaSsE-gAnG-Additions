# CI/CD — PeTaSsE_gAnG_Additions

## Workflows

| Workflow | Fichier | Déclencheur | Étapes |
|----------|---------|-------------|--------|
| CI | `.github/workflows/ci.yml` | push et PR sur `master`, manuel | validation du wrapper, Java 21, Checkstyle sur les trois source sets (zéro warning javac), `test` (JaCoCo), `runGameTest` (sous agent JaCoCo), `jacocoFullReport` + `jacocoCoverageVerification` (100 % des instructions et des branches), `build` (assemble le JAR et vérifie son contenu : ni dépendance embarquée, ni test), `benchmarkLevelZeroGeneration`, upload des JARs et des rapports (Checkstyle, tests, couverture, logs du serveur de test) |
| Release | `.github/workflows/release.yml` | tag `v*.*.*`, manuel | `clean build` (toutes les vérifications ci-dessus), vérification tag = `mod_version`, release GitHub avec JAR et sources, notes extraites du CHANGELOG |
| Dependabot | `.github/dependabot.yml` | mensuel | PR de mise à jour des actions GitHub. Les dépendances Gradle (Fabric, Yarn, Loom) suivent la version de Minecraft et se mettent à jour à la main |

Chaque étape de la CI est une tâche Gradle qui se lance aussi en local : `./gradlew build` enchaîne toutes celles qui
vérifient (Checkstyle, tests, tests en jeu, verrou de couverture, contenu du JAR) ; le rapport fusionné
(`jacocoFullReport`) et le benchmark se lancent à part. La CI ne fait rien de plus, elle le fait sur une machine neuve. Un test, une violation Checkstyle ou un seuil de couverture en échec
fait échouer la tâche, donc le job. Un seul job, pas de matrice : le mod ne vise qu'une version de Minecraft et un JDK.

---

## Créer une release

```bash
# 1. docs/CHANGELOG.md : section [x.y.z] - date
# 2. gradle.properties : mod_version=x.y.z
git add gradle.properties docs/CHANGELOG.md
git commit -m "chore: bump version to x.y.z"
git push origin master
git tag vx.y.z
git push origin vx.y.z
```

Un tag contenant `-` (ex. `v0.9.0-beta.1`) produit une pré-release.

---

## Débugger un job rouge

1. Onglet Actions → job → étape en échec ; les rapports sont dans l'artefact `rapports-<sha>`.
2. Reproduire localement avec la même commande (`./gradlew checkstyleMain`, `./gradlew test`, `./gradlew runGameTest`,
   `./gradlew jacocoCoverageVerification`, `./gradlew build`...).
3. Pour `runGameTest`, le rapport par test est dans `build/junit.xml` et les logs du serveur dans `build/run/gameTest/logs/`.
4. Pour la couverture, le rapport fusionné est `build/reports/jacoco/full/html/index.html` ; pour Checkstyle,
   `build/reports/checkstyle/`.
5. Cache Gradle suspect → Actions → Caches → supprimer.
