#!/usr/bin/env bash
# Script de setup de l'environnement cloud Claude Code (claude.ai/code).
#
# À copier dans : claude.ai/code > environnement > Edit > « Setup script ».
# Il s'exécute en root sur Ubuntu 24.04 avant le lancement de Claude ;
# le résultat est mis en cache (~7 jours), donc les sessions suivantes démarrent vite.
#
# L'image cloud fournit Java 21 ; ce projet a besoin de Java 25.
set -euo pipefail

JDK_PKG=openjdk-25-jdk-headless
JAVA25_HOME=/usr/lib/jvm/java-25-openjdk-amd64

if [ ! -x "$JAVA25_HOME/bin/java" ]; then
  apt-get update -qq || true   # certains dépôts tiers peuvent être bloqués : on continue
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "$JDK_PKG"
fi

# Java 25 par défaut pour tout le système
update-alternatives --set java  "$JAVA25_HOME/bin/java"  || true
update-alternatives --set javac "$JAVA25_HOME/bin/javac" || true
echo "JAVA_HOME=$JAVA25_HOME" >> /etc/environment

# Précharge Maven et les dépendances si le dépôt est déjà cloné (sinon, fait à la 1re commande)
for dir in "${CLAUDE_PROJECT_DIR:-}" "$PWD" /home/user/troc-spring-api; do
  if [ -n "$dir" ] && [ -f "$dir/mvnw" ]; then
    (cd "$dir" && JAVA_HOME="$JAVA25_HOME" sh ./mvnw -q -B dependency:go-offline) || true
    break
  fi
done

java -version
