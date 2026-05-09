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
  "bootstrap/argocd-applications"
  "platform/namespaces/dev"
  "platform/namespaces/staging"
  "platform/namespaces/prod"
  "platform/namespace-access/dev"
  "platform/namespace-access/staging"
  "platform/namespace-access/prod"
  "platform/rbac-catalog"
  "platform/access-control/groups"
  "platform/access-control/bindings/dev"
  "platform/access-control/bindings/staging"
  "platform/access-control/bindings/prod"
  "platform/argocd-guardrails"
  "clusters/ocp-dev"
  "clusters/ocp-staging"
  "clusters/ocp-prod"
)

for path in "${paths[@]}"; do
  echo "=== Rendering ${path}"
  "${KUSTOMIZE[@]}" "${ROOT_DIR}/${path}" >/dev/null
done

echo "All Kustomize entrypoints rendered successfully."
