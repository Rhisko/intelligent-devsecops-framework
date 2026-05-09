# Namespace Model

Each application receives a dedicated namespace per environment.

For `dbank-app-01`:

- `dbank-app-01-dev`
- `dbank-app-01-staging`
- `dbank-app-01-prod`

The same pattern is used for `dbank-app-02` through `dbank-app-05`.

Every namespace includes:

- `Namespace`
- `ResourceQuota`
- `LimitRange`
- default-deny ingress and egress `NetworkPolicy`
- same-namespace allow `NetworkPolicy`

Namespace labels identify application, domain, environment, and platform ownership.
