#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if command -v tree >/dev/null 2>&1; then
  tree "${ROOT_DIR}"
else
  find "${ROOT_DIR}" -print | sed "s#${ROOT_DIR}#gitops-namespace-access-platform#"
fi

