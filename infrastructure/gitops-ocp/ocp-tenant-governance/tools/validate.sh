#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"${ROOT_DIR}/tools/render.sh"

echo "=== Rendering every kustomization.yaml"
while IFS= read -r kustomization; do
  dir="$(dirname "${kustomization}")"
  if command -v kustomize >/dev/null 2>&1; then
    kustomize build "${dir}" >/dev/null
  else
    kubectl kustomize "${dir}" >/dev/null
  fi
done < <(find "${ROOT_DIR}" -name kustomization.yaml -type f | sort)

echo "Validation completed."

