# Reproduire les sondes de l'audit

Ces fichiers sont des **sondes d'audit**, hors de `src/test` : ils ne modifient pas le build
principal. Certaines assertions expriment un comportement attendu qui n'est pas encore satisfait.
Un échec de ces assertions est donc prévu sur le code audité.

- [Rapport et conclusions](../../AUDIT_2026-10-21.md)
- [Cas limites MySQL et HTTP](AuditCasLimitesTest.java) : 17 cas, dont 10 en échec lors de l'audit.
- [Sécurité MVC](AuditSecuriteMvcTest.java) : 11 cas réussis.
- [Observations et résultats enregistrés](resultats.json).

Les deux fichiers de test utilisent uniquement les dépendances déjà présentes. Les tests
MySQL démarrent leur propre conteneur `mysql:9.7`, puis effacent seulement ses données.
Le second compte CLIENT de la sonde existe uniquement dans son contexte Spring de test.

Depuis la racine du dépôt, avec Java 25 et Docker accessibles :

```powershell
$env:JAVA_HOME = "C:\Users\tomta\.jdks\ms-25.0.4.1" # adapter à votre installation
$cinemaRoot = (Get-Location).Path
$cinemaCopie = Join-Path $cinemaRoot '.tools\audit-reproduction'
if (Test-Path -LiteralPath $cinemaCopie) {
    throw 'Choisissez un nouveau dossier de copie : ne pas écraser une précédente reproduction.'
}
New-Item -ItemType Directory -Path $cinemaCopie -Force | Out-Null
foreach ($cinemaItem in @('src', '.mvn', 'pom.xml', 'mvnw', 'mvnw.cmd')) {
    Copy-Item -LiteralPath (Join-Path $cinemaRoot $cinemaItem) -Destination $cinemaCopie -Recurse
}
$cinemaSondes = Join-Path $cinemaRoot 'docs\audit\2026-10-21'
Copy-Item -LiteralPath (Join-Path $cinemaSondes 'AuditCasLimitesTest.java') `
    -Destination (Join-Path $cinemaCopie 'src\test\java\fr\butinfo\cinema')
Copy-Item -LiteralPath (Join-Path $cinemaSondes 'AuditSecuriteMvcTest.java') `
    -Destination (Join-Path $cinemaCopie 'src\test\java\fr\butinfo\cinema')
Push-Location $cinemaCopie
try {
    .\mvnw.cmd test '-Dtest=AuditCasLimitesTest,AuditSecuriteMvcTest'
} finally {
    Pop-Location
}
```

Sur l'état audité, les résultats cumulés sont **28 cas : 18 réussites, 10 échecs d'assertion**,
sans erreur d'exécution ni test ignoré. L'audit a exécuté ces cas en trois commandes, 15 puis
11 puis 2 cas ; le bloc ci-dessus les regroupe. Le bloc de reproduction a été contrôlé
syntaxiquement, mais cette variante groupée n'a pas été réexécutée.

Si Docker est inaccessible, les cas MySQL peuvent être ignorés : cela ne reproduit pas les
constats concernés. Les cas de verrouillage réduisent le délai d'attente à six secondes uniquement
pour les connexions du test. Les journaux et rapports Surefire se trouvent dans le `target` de
la copie ; ils ne doivent pas être ajoutés au dépôt.
