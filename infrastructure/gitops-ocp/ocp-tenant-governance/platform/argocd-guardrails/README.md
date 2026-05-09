# Argo CD Guardrails

This folder contains optional AppProject guardrails for application repositories.

The `dbank-dev` AppProject allows application repositories to deploy standard namespaced application resources into their approved dev namespaces. It intentionally does not allow `Role` or `RoleBinding` because RBAC is managed by the platform team in this repository.

