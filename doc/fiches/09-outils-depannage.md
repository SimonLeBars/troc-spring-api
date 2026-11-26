# 🧰 Fiche 9 — Outils et dépannage

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

## 📦 Maven Wrapper · `TP 0`

Pas besoin d'installer Maven : `mvnw` télécharge la bonne version au premier lancement.

| Commande | Rôle |
|---|---|
| `./mvnw spring-boot:run` | démarrer l'API (`Tomcat started on port 8080`) · arrêter : `Ctrl+C` |
| `./mvnw test` | lancer les tests (`TP 9`) |
| `./mvnw verify` | compiler + tester : ✅ avant chaque commit |
| `./mvnw package` | fabriquer `target/troc-api-0.0.1-SNAPSHOT.jar` (`TP 3`) |
| `./mvnw clean` | supprimer `target/` (compilation bizarre, fichier fantôme) |
| `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` | passer un argument (`TP 3`) |

⚠️ **Windows / PowerShell :** `.\mvnw.cmd …` (le `.\` est obligatoire)

**🔄 Après une modification du `pom.xml` :** IntelliJ > icône **Load Maven Changes** (ou fenêtre **Maven** > **Reload**).

| Dépendance | TP |
|---|:-:|
| `spring-boot-starter-webmvc` | 0 |
| `springdoc-openapi-starter-webmvc-ui` (avec `<version>`) | Annexe 1 |
| `spring-boot-starter-validation` | 2, 3 (bonus) |
| `spring-boot-starter-security` · `-security-oauth2-resource-server` | 5 |
| `spring-boot-starter-data-jdbc` · `h2` · `spring-boot-h2console` | 6 |
| `spring-boot-starter-aspectj` | 8 |
| `spring-boot-starter-webmvc-test` (`test`) | 9 (déjà là) |
| `spring-boot-starter-security-test` (`test`) | 9 |

💡 `spring-boot-starter-*` : pas de `<version>`, Spring Boot la choisit · une bibliothèque externe (springdoc) : à nous de choisir une version compatible

## ☕ Java · `TP 0`

```bash
java -version          # 25.x attendu
echo $JAVA_HOME        # PowerShell : $env:JAVA_HOME
```

| Symptôme | 🛠️ Solution |
|---|---|
| `java -version` affiche 17 ou 21 | `JAVA_HOME` vers le JDK 25, `%JAVA_HOME%\bin` **en tête** du `PATH` |
| `release version 25 not supported` | Maven tourne avec un ancien JDK : même solution |
| JDK téléchargé par IntelliJ, `mvnw` ne le trouve pas | `JAVA_HOME` vers `C:\Users\<vous>\.jdks\…`, ou lancer depuis IntelliJ |
| IntelliJ souligne tout en rouge | **File > Project Structure > Project > SDK** : 25 |

## 🐶 Bruno · `TP 1`

| Élément | Où |
|---|---|
| Collection | dossier `bruno/` du projet (fichiers `.bru`, versionnés avec le code) |
| Environnement | en haut à droite : `Local` (`baseUrl` = `http://localhost:8080`) · `Render` |
| Variable | `{{baseUrl}}/items` · `{{token}}` (`TP 5`) |
| Dossiers | `Utilisateurs`, `Objets`, `Trocs` · `Expérimentation` (routes temporaires) |

```
meta {
  name: Créer objet (Alice)
  type: http
  seq: 1                         ← l'ordre dans le dossier
}

post {
  url: {{baseUrl}}/items
  body: json
  auth: none
}
```

- ✅ lire le **code** de la réponse (en haut), pas seulement le JSON · onglet **Headers** pour `Location`
- ⚠️ requête `POST` / `PUT` : `Content-Type: application/json` (sinon `415`)
- 💡 `curl -i http://localhost:8080/users` : la même chose en ligne de commande, `-i` affiche le code

## 🗄️ Console H2 · `TP 6`

<http://localhost:8080/h2-console> · **JDBC URL** : copier **exactement** `spring.datasource.url` · **User Name** `sa` · mot de passe vide

```sql
SELECT * FROM users;
SELECT * FROM items ORDER BY owner_id, name;
SELECT * FROM trades;
```

| Symptôme | 🛠️ Solution |
|---|---|
| table inexistante, base vide | l'URL saisie n'est pas celle de l'application |
| `404` sur `/h2-console` | `spring-boot-h2console` absent · `spring.h2.console.enabled=true` oublié |
| « Database may be already in use » | une 2ᵉ instance tourne : l'arrêter |
| `401` ou page blanche (`TP 6`) | ouvrir `/h2-console/**`, et `frameOptions(… sameOrigin())` |

🧹 **Repartir d'une base propre :** arrêter l'application → supprimer `data/` → relancer.

## 📘 Swagger UI · `Annexe 1`

| URL | Contenu |
|---|---|
| <http://localhost:8080/swagger-ui.html> | page web : toutes les routes, **Try it out** |
| <http://localhost:8080/v3/api-docs> | la description OpenAPI brute (JSON) : générer un client, importer dans Bruno |

- 💡 `@Operation(summary = "…")` pour les règles qui ne se devinent pas
- 🔒 `TP 5` : ouvrir `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` · bouton **Authorize** pour le jeton

## ⌨️ IntelliJ · `TP 0`

Tous les raccourcis (Windows / Linux et macOS) : [fiche 10 — Raccourcis IntelliJ](10-raccourcis-intellij.md).

## 🐞 Déboguer · `TP 8`

| Outil | Usage |
|---|---|
| 🔴 point d'arrêt | clic dans la marge, lancer en **Debug** (icône insecte) |
| 🧭 **Debugger > Frames** | la pile d'appels (proxies : `CglibAopProxy`, `TransactionInterceptor`) |
| 📜 `logging.level.<package>=DEBUG` | `org.springframework.jdbc.core.JdbcTemplate` (SQL), `org.springframework.security` (refus) |
| 🔎 la console | lire la **dernière** ligne `Caused by:` d'une trace : c'est la vraie cause |

⌨️ raccourcis du débogueur : [fiche 10](10-raccourcis-intellij.md).

## 🌿 Git · `TP 0`

| Commande | Rôle |
|---|---|
| `git status` · `git diff` | ce qui a changé |
| `git restore <fichier>` | annuler les modifications d'un fichier (après une expérimentation) |
| `git add …` · `git commit -m "…"` | enregistrer une étape (✅ tests au vert) |
| `.gitignore` | `target/`, `.idea`, **`data/`** (`TP 6`) · ❌ jamais de secret versionné (`TP 5`) |

## 🗺️ Diagnostic : quel code, quelle piste ?

| Reçu | 🔎 Piste | Fiche |
|---|---|:-:|
| l'application s'arrête juste après le démarrage | `spring-boot-starter-webmvc` absent | 9 |
| ne démarre pas, `APPLICATION FAILED TO START` | lire le bloc **Description** / **Action** | 1, 5 |
| `Port 8080 was already in use` | une autre instance tourne | 3 |
| `400` | JSON mal formé · `/items/abc` · `int` absent · `@RequestParam` manquant | 2, 4 |
| `401` | jeton absent, expiré, modifié · route non ouverte · `/error` fermé | 8 |
| `403` | pas le destinataire (normal) · CSRF actif | 2, 8 |
| `404` | chemin mal recopié · préfixe en double · ressource inconnue | 2 |
| `405` | bon chemin, mauvais verbe | 2 |
| `409` | règle métier (normal) | 2, 4 |
| `415` | `Content-Type: application/json` manquant | 2 |
| `500` | **toujours un bug** : lire la trace dans la console | 2, 5 |
| message d'erreur absent du JSON | `spring.web.error.include-message=always` | 2 |
| modification sans effet | application pas relancée · `schema.sql` : supprimer `data/` | 3, 5 |
| « ça marche chez lui, pas chez moi » | `./mvnw clean verify` · recharger Maven · versions de Java | 9 |

## 🔗 Pour aller plus loin

[Maven Wrapper](https://maven.apache.org/wrapper/) · [Bruno — Docs](https://docs.usebruno.com/) · [H2 — Fonctionnalités](https://h2database.com/html/features.html) · [springdoc](https://springdoc.org/) · [IntelliJ — Debugger](https://www.jetbrains.com/help/idea/debugging-code.html)
