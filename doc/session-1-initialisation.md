# Session 1 — Initialiser le projet avec Spring Initializr

## Pourquoi Spring Initializr ?

Un projet Spring Boot a besoin d'une configuration Maven précise (parent POM, plugin de build, dépendances gérées). Spring Initializr génère tout ça pour vous en quelques clics, plutôt que d'écrire un `pom.xml` à la main et risquer des erreurs de configuration avant même d'avoir commencé à coder.

## Étape 1 — Générer le projet

Vous avez deux façons équivalentes de faire ; utilisez celle qui vous met le plus à l'aise.

### Option A — Depuis IntelliJ (recommandé)

1. `File > New > Project...`
2. Choisissez **Spring Boot** dans la liste de gauche (IntelliJ intègre directement start.spring.io).
3. Remplissez les champs avec les valeurs ci-dessous.
4. Sur l'écran des dépendances, ne cochez que **Spring Web**.
5. Terminez l'assistant — IntelliJ télécharge et ouvre le projet.

### Option B — Depuis le site [start.spring.io](https://start.spring.io)

1. Remplissez le formulaire avec les valeurs ci-dessous.
2. Cliquez sur **Generate** : un fichier `.zip` est téléchargé.
3. Décompressez-le et ouvrez le dossier dans IntelliJ (`File > Open`).

### Valeurs à utiliser

| Champ | Valeur |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | dernière version stable (évitez les SNAPSHOT/M/RC) |
| Group | `com.example` |
| Artifact | `troc-api` |
| Name | `troc-api` |
| Description | API de troc d'objets entre utilisateurs |
| Package name | `com.example.troc` |
| Packaging | Jar |
| Java | 25 |
| Dependencies | **Spring Web** uniquement |

⚠️ Ne rajoutez pas d'autres dépendances (base de données, tests, sécurité...) pour l'instant — on les ajoutera au fur et à mesure des prochaines sessions, quand elles auront du sens. Un `pom.xml` qui ne contient que ce dont on a besoin est plus facile à comprendre.

## Étape 2 — Vérifier que tout fonctionne

Dans un terminal, à la racine du projet :

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Vous devriez voir les logs Spring Boot démarrer, avec une ligne du type `Tomcat started on port 8080`. Arrêtez avec `Ctrl+C`. Si ça compile et démarre sans erreur, votre projet est prêt — vous n'avez pas encore d'endpoint (ça vient dans la suite du TP), c'est normal de ne voir qu'une page d'erreur si vous ouvrez `localhost:8080` dans un navigateur.

## Pour aller plus loin

- [Documentation officielle Spring Initializr](https://docs.spring.io/initializr/docs/current/reference/html/)
- [Guide "Building an Application with Spring Boot"](https://spring.io/guides/gs/spring-boot)
