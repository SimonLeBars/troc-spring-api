# Initialiser le projet avec Spring Initializr

> À faire avant le TP 1. Ce support n'a pas de partie expérimentation : il prépare le projet commun à tous les TP.

## Prérequis techniques

| Outil | Version | Où le trouver |
|---|---|---|
| JDK (Java) | **25** | [Eclipse Temurin (Adoptium)](https://adoptium.net/temurin/releases/?version=25), ou directement depuis IntelliJ (voir ci-dessous) |
| IntelliJ IDEA | récente (2025.2 ou plus) | [jetbrains.com/idea](https://www.jetbrains.com/idea/download/) — la version **Community** suffit ; la version **Ultimate** est gratuite pour les étudiants ([licence éducation](https://www.jetbrains.com/community/education/#students)) |
| Git | récente | [git-scm.com](https://git-scm.com/downloads) |
| Bruno | récente | [usebruno.com/downloads](https://www.usebruno.com/downloads) — client HTTP pour tester les routes, à partir du TP 1 |

Maven n'a **pas** besoin d'être installé : le projet que vous allez générer à l'étape 1 contiendra un *wrapper* (`mvnw` / `mvnw.cmd`), ajouté par Spring Initializr, qui télécharge automatiquement la bonne version de Maven au premier lancement.

### Installer le JDK 25 depuis IntelliJ

Si vous ne voulez pas installer le JDK à la main : `File > Project Structure > Project > SDK > Add SDK > Download JDK...`, choisissez la version **25** et le fournisseur **Eclipse Temurin**. IntelliJ le télécharge et le configure pour le projet.

⚠️ Dans ce cas, le JDK n'est connu que d'IntelliJ : `mvnw` lancé depuis un terminal ne le trouvera pas forcément. Lancez alors l'application depuis IntelliJ (bouton ▶ à côté de la méthode `main`, ou fenêtre **Maven** à droite), ou faites pointer `JAVA_HOME` vers le dossier où IntelliJ l'a téléchargé (par défaut `~/.jdks/`, soit `C:\Users\<vous>\.jdks\` sous Windows).

### Vérifier l'installation

Dans un terminal :

```bash
java -version   # doit afficher 25.x
git --version
```

Si `java -version` affiche une autre version (17, 21...), un ancien JDK est prioritaire dans votre `PATH` : définissez la variable d'environnement `JAVA_HOME` vers le dossier du JDK 25 et placez `%JAVA_HOME%\bin` (Windows) ou `$JAVA_HOME/bin` (Linux / macOS) en tête du `PATH`. L'installeur Temurin pour Windows propose de le faire pour vous (options *Set JAVA_HOME* et *Add to PATH*).

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
| Spring Boot | **4.1.x** (la plus récente proposée, par exemple 4.1.1 ; évitez les SNAPSHOT/M/RC) |
| Group | `fr.cours` |
| Artifact | `troc-api` |
| Name | `troc-api` |
| Description | API de troc d'objets entre utilisateurs |
| Package name | `fr.cours.troc` |
| Packaging | Jar |
| Java | 25 |
| Dependencies | **Spring Web** uniquement |

⚠️ Ne rajoutez pas d'autres dépendances (base de données, tests, sécurité...) pour l'instant — on les ajoutera au fur et à mesure des TP, quand elles auront du sens. Un `pom.xml` qui ne contient que ce dont on a besoin est plus facile à comprendre.

## Étape 2 — Repérer la structure du projet

Le projet généré contient une classe de démarrage dans le package racine `fr.cours.troc`. Renommez-la `TrocApplication` si l'assistant l'a appelée autrement (par exemple `TrocApiApplication`) : clic droit sur la classe > `Refactor > Rename`.

```
troc-api/
├── pom.xml                                  ← dépendances (Spring Web)
└── src/
    ├── main/java/fr/cours/troc/
    │   └── TrocApplication.java             ← point d'entrée de l'application
    ├── main/resources/application.properties ← configuration (TP 3)
    └── test/java/fr/cours/troc/             ← tests (TP 8)
```

Dans ce projet, le code est rangé **par fonctionnalité** : tout ce qui concerne les utilisateurs ira dans un package `fr.cours.troc.user`, tout ce qui concerne les objets échangés dans `fr.cours.troc.item`, etc. Créez toujours ces packages **sous** `fr.cours.troc` : vous verrez au TP 1 pourquoi Spring en a besoin.

## Étape 3 — Vérifier que tout fonctionne

Dans un terminal, à la racine du projet :

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows (PowerShell ou invite de commandes)
.\mvnw.cmd spring-boot:run
```

Sous Windows, le préfixe `.\` est indispensable dans PowerShell, le terminal par défaut (y compris dans IntelliJ) : sans lui, PowerShell ne cherche pas la commande dans le dossier courant et répond que `mvnw.cmd` n'est pas reconnu.

Vous devriez voir les logs Spring Boot démarrer, avec une ligne du type `Tomcat started on port 8080`. Arrêtez avec `Ctrl+C`. Si ça compile et démarre sans erreur, votre projet est prêt — vous n'avez pas encore d'endpoint (ça vient au TP 1), c'est normal de ne voir qu'une page d'erreur si vous ouvrez `localhost:8080` dans un navigateur.

⚠️ **L'application s'arrête toute seule juste après le démarrage, sans erreur ?** Il manque la dépendance **Spring Web** : sans serveur web, Spring démarre, n'a rien à faire, et le programme se termine. Vérifiez que le `pom.xml` contient bien :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Si ce n'est pas le cas, ajoutez-la, rechargez Maven dans IntelliJ (icône **Load Maven Changes**, ou fenêtre **Maven** > **Reload**), puis relancez.

## Pour aller plus loin

- [Documentation officielle Spring Initializr](https://docs.spring.io/initializr/docs/current/reference/html/)
- [Guide "Building an Application with Spring Boot"](https://spring.io/guides/gs/spring-boot)
