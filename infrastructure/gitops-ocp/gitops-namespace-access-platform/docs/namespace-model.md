# Namespace Model

Each application receives a dedicated environment namespace.

For dev:

- `dbank-app-01-dev`
- `dbank-app-02-dev`
- `dbank-app-03-dev`
- `dbank-app-04-dev`
- `dbank-app-05-dev`

Every namespace includes:

- `Namespace`
- `ResourceQuota`
- `LimitRange`
- default-deny ingress and egress `NetworkPolicy`
- same-namespace allow `NetworkPolicy`

Namespace labels identify application, domain, environment, and platform ownership.

