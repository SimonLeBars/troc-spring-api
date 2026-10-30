# troc-api

API REST de troc d'objets entre utilisateurs, fil rouge du cours Spring Boot (4 sessions).
Les recommandations de travail détaillées (git, dépendances, supports de TP, Bruno) sont dans
`.claude/CLAUDE.md` : elles priment sur ce fichier.

## Stack

- Java **25** (obligatoire : `java.version` vaut 25 dans le `pom.xml`)
- Spring Boot 4.1.x, Maven via le wrapper (`./mvnw`, `mvnw.cmd` sous Windows)
- Package racine : `fr.cours.troc`, découpé par domaine (`user`, `item`, `trade`)
- Persistance : Spring Data JDBC + H2. Schéma dans `src/main/resources/schema.sql`.
  - Application : base fichier dans `data/` (ignoré par git), console sur `/h2-console`
  - Tests : profil `test`, base H2 en mémoire (`src/test/resources/application-test.properties`)
- Documentation : springdoc-openapi (séance 5). Swagger UI sur `/swagger-ui.html`, description
  OpenAPI sur `/v3/api-docs` (aussi disponibles sur Render et les previews).

## Commandes

```bash
./mvnw verify            # compile + lance les tests (à exécuter avant chaque commit)
./mvnw test              # tests seuls
./mvnw spring-boot:run   # démarre l'API sur http://localhost:8080
```

Pour tester un endpoint pendant que l'application tourne : `curl -s http://localhost:8080/...`
(ou la collection Bruno dans `bruno/`). Lancer l'application en arrière-plan et l'arrêter une
fois la vérification faite.

## Intégration continue

`.github/workflows/ci.yml` lance `./mvnw verify` sur chaque pull request et chaque push sur
`main`. Une PR ne doit être mergée que si ce check est vert.

## Déploiement (Render)

- `Dockerfile` à la racine : build Maven puis image JRE 25. Le port vient de la variable `PORT`.
- `main` est déployé automatiquement sur Render (https://troc-spring-api.onrender.com, offre gratuite, 512 Mo de RAM, mise en veille
  après 15 min sans requête : la première requête suivante prend ~1 min).
- Chaque pull request obtient sa propre preview (URL visible sur la PR, « View deployment »),
  supprimée au merge ou à la fermeture. Base H2 vide à chaque déploiement.
- Pour tester une preview avec Bruno : environnement `Render`, remplacer `baseUrl` par l'URL
  de la preview.

## Sessions cloud (Claude Code on the web)

L'image cloud fournit Java 21 par défaut. Le script `scripts/cloud-setup.sh` (setup script de
l'environnement) installe Java 25 ; le hook `SessionStart` de `.claude/settings.json`
(`scripts/cloud-session-start.sh`) pointe `JAVA_HOME` dessus. Si `./mvnw` se plaint de la
version de Java, vérifier `java -version` et `echo $JAVA_HOME`.

Dans une session cloud, travailler sur une branche et ouvrir une pull request plutôt que de
pousser sur `main`.
