#!/usr/bin/env bash
# Hook SessionStart (voir .claude/settings.json).
# Ne fait rien en local ; dans une session cloud, garantit que Java 25 est
# installé et que JAVA_HOME pointe dessus pour toutes les commandes de Claude.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

JAVA25_HOME=/usr/lib/jvm/java-25-openjdk-amd64

# Filet de sécurité si le setup script de l'environnement n'a pas été configuré
if [ ! -x "$JAVA25_HOME/bin/java" ]; then
  bash "${CLAUDE_PROJECT_DIR:-.}/scripts/cloud-setup.sh" >/dev/null 2>&1 || true
fi

if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export JAVA_HOME=$JAVA25_HOME"
    echo "export PATH=$JAVA25_HOME/bin:\$PATH"
  } >> "$CLAUDE_ENV_FILE"
fi

echo "Java pour cette session : $("$JAVA25_HOME/bin/java" -version 2>&1 | grep -v JAVA_TOOL | head -1)"
