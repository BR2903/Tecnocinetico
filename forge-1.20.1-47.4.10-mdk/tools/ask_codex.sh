#!/usr/bin/env bash
# Encargo de MODELOS/ARTE a Codex desde Claude Code, en el chat de Codex que ya usa el usuario.
#
#   bash tools/ask_codex.sh <archivo_con_el_encargo.md> [archivo_respuesta.txt]
#
# - Reanuda la sesión de Codex del proyecto (CODEX_SESSION) y le manda el encargo por stdin.
# - La última respuesta de Codex queda en el archivo de respuesta; el registro completo, al lado (.log).
# - Codex trabaja en la raíz del repo con sandbox workspace-write y sin pedir aprobaciones.
# - Reparto (ver AGENTS.md §3c): Codex = modelos, texturas y vistas previas; Claude = código y lógica.
#   Codex NO hace commit: Claude revisa (check_models, check_zfight, compilación, pruebas) y sube.
set -euo pipefail

CODEX_SESSION="${CODEX_SESSION:-01a1c80d-c127-7890-a278-de32d1e10058}"
BRIEF="$1"
OUT="${2:-${BRIEF%.*}.respuesta.txt}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

cd "$ROOT"
codex exec resume "$CODEX_SESSION" - -o "$OUT" < "$BRIEF" > "${OUT%.*}.log" 2>&1
echo "respuesta de Codex en: $OUT"
