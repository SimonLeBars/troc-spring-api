# Image de déploiement (Render et ses previews de pull request).
# Étape 1 : construire le jar avec Maven. Étape 2 : l'exécuter avec un JRE seul (image plus légère).

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Dépendances d'abord : cette couche reste en cache tant que le pom.xml ne change pas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sh ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
# Les tests sont déjà joués par la CI GitHub Actions
RUN sh ./mvnw -B -ntp package -DskipTests && cp target/*.jar app.jar


FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# L'offre gratuite de Render n'a que 512 Mo de RAM : on borne la JVM en conséquence
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

# Render impose le port via la variable PORT ; 8080 si on lance l'image ailleurs
EXPOSE 8080
CMD ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar app.jar"]
