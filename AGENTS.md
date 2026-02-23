# Avanzada – Instructions for the AI agent

This document provides context for the AI agent to work on the **Avanzada** monorepo: Backend (Java Spring Boot), Frontend (Angular) and MariaDB, orchestrated with Docker.

## Project standards (always apply)

See **`.cursor/rules/project-standards.mdc`** for the full rule (always applied). Summary:

1. **Documentation**: Keep `README.md`, `AGENTS.md`, and `.cursor/rules/` up to date with the repo. Update them when changing structure, ports, or behaviour.
2. **English**: Code, comments, and documentation (including this file when editing) in English.
3. **Clean code & SOLID**: Single responsibility, dependency injection, thin controllers, services for logic; avoid duplication and keep names and functions clear.

## Project structure

```
Avanzada/
├── backend/                 # Java Spring Boot 3.x (Java 21)
│   ├── src/main/java/com/avanzada/
│   │   ├── AvanzadaApplication.java
│   │   ├── config/           # SecurityConfig, JwtUtil, JwtProperties
│   │   ├── controller/      # @RestController (incl. AuthController)
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # JPA repositories
│   │   └── security/        # JWT filter, UserDetails
│   ├── src/main/resources/
│   │   └── application.yml  # Profiles: default (local), docker
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                # Angular 18, standalone components, Tailwind CSS + Flowbite
│   ├── public/i18n/         # i18n JSON (es.json base, en.json)
│   ├── src/app/             # Components, routes, config
│   ├── tailwind.config.js   # Theme (colors, etc.) and content paths
│   ├── DESIGN.md            # Design system and Tailwind patterns
│   ├── angular.json         # serve port 4000, proxy to backend
│   ├── proxy.conf.json      # /api -> http://localhost:9000 (dev)
│   ├── nginx.conf           # /api/ -> backend:9000 (Docker)
│   ├── package.json
│   └── Dockerfile
├── .devcontainer/           # Dev Containers: devcontainer.json, Dockerfile, docker-compose
├── .vscode/                 # tasks.json (Backend/Frontend auto-run on open); may be in .gitignore
├── docker-compose.yml       # mariadb, backend, frontend
├── .cursor/rules/           # Rules by context (Java, Angular, Docker)
├── AGENTS.md                # This file
└── README.md
```

Note: `.vscode/` and `.cursor/` may be in `.gitignore`; they are still part of the development flow (Dev Containers, tasks, AI rules).

## Ports

| Service   | Host port | Usage |
|-----------|-----------|-------|
| Backend   | 9000      | Spring Boot API |
| Frontend  | 4000      | Angular (dev) or nginx (Docker) |
| MariaDB   | 3307      | Host access (3306 inside Docker network) |

Inside Docker, the backend connects to MariaDB at `mariadb:3306`. The frontend in Docker calls the backend at `http://backend:9000` via nginx proxy (`/api/`).

## How to run

- **Full Docker**: from repo root:
  - `docker compose build`
  - `docker compose up` (or `docker compose up -d`)
  - Frontend: http://localhost:4000 — Backend: http://localhost:9000 — Health: http://localhost:9000/health
- **Local backend**: default profile in `application.yml` (DB at `localhost:3307`). Ensure MariaDB is up (e.g. `docker compose up mariadb -d`) and run the Spring Boot app (IDE or `mvn spring-boot:run`). To seed reference data and an admin user (identifier `admin`, password `admin123`), run once: `mvn spring-boot:run -Dspring-boot.run.arguments=--init-data`; the process will init and exit. To fully reset the DB (drop tables, recreate, then init-data), use profiles `docker,reset` and `--init-data` (see README).
- **Local frontend**: `cd frontend && npm ci && npm start` (port 4000, proxy `/api` → `http://localhost:9000`).
- **Dev Containers**: with the Dev Containers extension, open the repo in container; `.devcontainer/` includes the `dev` service with Java 21, Maven, Node and Angular CLI. Ports 9000, 4000 and 3307 are forwarded to the host. Optionally, `.vscode/tasks.json` defines "Backend: Spring Boot" and "Frontend: Angular" tasks that can run on folder open (runOn: folderOpen).

## Conventions and where to edit

- **Backend (Java)**  
  - Base package: `com.avanzada`.  
  - Config: `backend/src/main/resources/application.yml`. Profiles: `default` (local), `docker` (compose, host `mariadb:3306`), `railway` (Railway deploy, env `SPRING_DATASOURCE_*` or `DATABASE_URL`).  
  - New endpoints: controllers in `backend/src/main/java/com/avanzada/controller/`.  
  - JPA entities in `entity/`, repositories in `repository/`.  
  - Validation: `spring-boot-starter-validation`; use DTOs and `@Valid` where applicable.  
  - See `.cursor/rules/` for Java/Spring code standards.

- **Frontend (Angular)**  
  - Project: `frontend`, component prefix `app`.  
  - Angular 18 with standalone components.  
  - **Styling**: Tailwind CSS and Flowbite. No component or global CSS files; use Tailwind + Flowbite utility/component classes. Theme in `tailwind.config.js`, patterns and Flowbite usage in `frontend/DESIGN.md`. Interactive components are initialised via `initFlowbite()` in `AppComponent`. Do not add `styleUrl` or new `.css` files.  
  - **i18n**: Runtime translations with `@ngx-translate/core` and `@ngx-translate/http-loader`. Default language is Spanish (`es`); English (`en`) is supported. Translation files: `frontend/public/i18n/es.json` (base), `frontend/public/i18n/en.json`. Config in `app.config.ts` (`provideTranslateService`). In components that need translations, import `TranslateModule` and use the `translate` pipe (e.g. `{{ 'key' | translate }}`) or inject `TranslateService` and call `use('es'|'en')` to switch language.  
  - API calls: use relative path `/api/...` so that in dev (proxy) and Docker (nginx) they target the backend.  
  - Serve/proxy config: `angular.json` (port 4000) and `proxy.conf.json`.  
  - See `.cursor/rules/` for Angular/TypeScript patterns.

- **Docker**  
  - `docker-compose.yml`: services `mariadb`, `backend`, `frontend`. Backend depends on MariaDB (healthcheck).  
  - Backend: Eclipse Temurin 21 image, port 9000, profile `docker`.  
  - Frontend: build with Node, run with nginx on port 80 (mapped 4000:80).  
  - Backend env vars in compose: `SPRING_PROFILES_ACTIVE`, `MARIADB_*` (DB, user, password).  
  - See `.cursor/rules/` for Dockerfile and compose.

## Security and roles (RF-13)

- **Authentication**: JWT. Login via `POST /api/auth/login` with body `{ "identifier": "...", "password": "..." }`. Response includes `token` and `user` (id, identifier, name, role). All other `/api/**` endpoints require `Authorization: Bearer <token>`. Public: `/health`, `POST /api/auth/login`.
- **Roles** (stored in `app_user.role`, included in JWT):
  - **STUDENT**: Register requests (POST /api/requests); view **only their own** requests (list filtered by requestedBy; GET detail/history/summary return 403 if not the requester). Cannot classify, assign, attend, or close.
  - **STAFF**: Register requests; view all requests; classify, assign, attend (lifecycle except close). Cannot close requests.
  - **ADMIN**: Full access including close (POST /api/requests/{id}/close).
- **Backend**: `@PreAuthorize` on `RequestController` (e.g. `hasRole('ADMIN')` for close, `hasAnyRole('STAFF','ADMIN')` for classify/assign/attend, `hasAnyRole('STUDENT','STAFF','ADMIN')` for create). User id for audit comes from JWT (SecurityContext), not headers.
- **Frontend**: `AuthService` (login, logout, token in sessionStorage); HTTP interceptor adds Bearer token; auth guard redirects unauthenticated users to `/login`. UI hides or disables actions by role (e.g. Close only for ADMIN, New request only when `canRegister()`).
- **Config**: `app.jwt.secret` (min 32 bytes for HS256), `app.jwt.expiration-seconds`. In production set `JWT_SECRET` env var. If no user has a password set, dev bootstrap sets the first user’s password to `admin123` and role to ADMIN (see `DevAuthBootstrap`).

## Deployment (Railway + Vercel)

- **Backend (Railway)**: Deploy from repo with root or Dockerfile at `backend/`. Use profile `railway`; set `SPRING_PROFILES_ACTIVE=railway`, `SPRING_DATASOURCE_URL` (JDBC URL from Railway MySQL), `JWT_SECRET`. See README "Deployment (Railway + Vercel)".
- **Frontend (Vercel)**: Root directory `frontend`. Set env `API_URL` to backend API base (e.g. `https://your-app.railway.app/api`). Build uses `scripts/set-api-url.js` to inject API_URL into `environment.prod.ts`. CORS is enabled on the backend for the frontend origin.

## Validation and testing

- **Backend**: `cd backend && mvn verify` (includes tests). To only compile: `mvn compile`.
- **Frontend**: `cd frontend && npm run build` (production build). Tests: `npm test`.
- **Docker**: after code changes, rebuild with `docker compose build [backend|frontend]` then `docker compose up`.

## Reference plan

The monorepo design and ports follow the plan in `.cursor/plans/monorepo_java_angular_docker_7943f96b.plan.md`. For extending services (more ports, new containers) refer to that plan and keep this documentation aligned.

## Summary for the agent

1. **Structure**: Backend in `backend/`, frontend in `frontend/`, orchestration at root.  
2. **Ports**: 9000 backend, 4000 frontend, 3307 MariaDB (host).  
3. **Config**: Backend by profiles in `application.yml`; in Docker use profile `docker` and host `mariadb`.  
4. **API from frontend**: Dev and Docker use relative `/api/` (proxy/nginx). Production (Vercel) uses `environment.apiUrl` set at build from `API_URL` (e.g. Railway backend + `/api`).  
5. **Additional rules**: Apply the rules in `.cursor/rules/` according to the file type being edited.  
6. **Dev Containers**: If the user works inside the dev container, the backend must use profile `docker` (connection to `mariadb:3306`). The tasks in `.vscode/tasks.json` already set `SPRING_PROFILES_ACTIVE=docker` for the backend.
