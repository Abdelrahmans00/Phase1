## Step 8 optional notes (javax/jakarta + JAAS)

### Namespace alignment

- Current codebase uses `javax.*` imports with `javax:javaee-api:8.0.1`.
- `persistence.xml` and `beans.xml` currently use Jakarta XML namespaces.
- Runtime tests passed on current setup, so no migration was applied to avoid destabilizing teammates' work.

### Recommended follow-up if TA requires strict alignment

Choose one path and apply project-wide:

1. **Stay Java EE 8**: keep `javax.*` code and update XML namespaces/schema to Java EE 8.
2. **Move to Jakarta EE**: migrate imports (`javax` -> `jakarta`) and dependency set accordingly.

### JAAS hardening status

- Existing authorization model is email + role checks in resources/services.
- No JAAS integration was introduced to keep compatibility with the current assignment baseline and team code.
- If TA requires JAAS, add container-managed security (`@RolesAllowed`, security domain config, authenticated principal flow) in a separate hardening branch.
