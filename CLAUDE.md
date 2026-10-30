# troc-api

API REST de troc d'objets entre utilisateurs. C'est un projet « fil rouge » pédagogique : il est construit session par session, et chaque session est documentée dans `doc/session-N-<sujet>.md`.

## Stack

- Java **25** (obligatoire : `java.version` vaut 25 dans le `pom.xml`)
- Spring Boot 4.1.x, Maven via le wrapper (`./mvnw`, `mvnw.cmd` sous Windows)
- Package racine : `com.example.troc`
- Base de données prévue : H2 (pas encore ajoutée au `pom.xml`)

## Commandes

```bash
./mvnw verify            # compile + lance les tests (à exécuter avant chaque commit)
./mvnw test              # tests seuls
./mvnw spring-boot:run   # démarre l'API sur http://localhost:8080
```

Pour tester un endpoint pendant que l'application tourne : `curl -s http://localhost:8080/...`.
Lancer l'application en arrière-plan et l'arrêter une fois la vérification faite.

## Règles du projet

- **Dépendances minimales** : n'ajouter une dépendance au `pom.xml` que lorsque la session en cours en a besoin, et expliquer pourquoi dans la doc de session.
- **Documentation de session** : toute évolution notable s'accompagne d'un fichier `doc/session-N-<sujet>.md` (en français), dans le même style que `doc/session-1-initialisation.md` : le « pourquoi » d'abord, puis des étapes numérotées, puis une vérification.
- Code (classes, méthodes, variables) en anglais ; documentation, commentaires de doc et messages de commit en français.
- Messages de commit courts, à l'impératif ou descriptifs, en français (ex. « Ajout de l'entité Objet »).
- Ne jamais pousser directement sur `main` : travailler sur une branche et ouvrir une pull request.

## Sessions cloud (Claude Code on the web)

L'image cloud fournit Java 21 par défaut. Le script `scripts/cloud-setup.sh` installe Java 25 ; le hook `SessionStart` de `.claude/settings.json` (`scripts/cloud-session-start.sh`) pointe `JAVA_HOME` dessus. Si `./mvnw` se plaint de la version de Java, vérifier `java -version` et `echo $JAVA_HOME`.
