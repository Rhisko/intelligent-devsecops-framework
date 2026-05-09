#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if command -v kustomize >/dev/null 2>&1; then
  KUSTOMIZE=(kustomize build)
elif command -v kubectl >/dev/null 2>&1; then
  KUSTOMIZE=(kubectl kustomize)
else
  echo "error: kustomize or kubectl is required" >&2
  exit 1
fi

paths=(
  "platform/namespaces/dev"
  "platform/rbac-catalog"
  "platform/access-control/bindings/dev"
  "platform/argocd-guardrails"
  "clusters/ocp-dev"
)

for path in "${paths[@]}"; do
  echo "=== Rendering ${path}"
  "${KUSTOMIZE[@]}" "${ROOT_DIR}/${path}" >/dev/null
done

echo "All Kustomize entrypoints rendered successfully."

