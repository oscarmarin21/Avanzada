# Avanzada

Monorepo with **Backend** (Java Spring Boot), **Frontend** (Angular) and **MariaDB**, orchestrated with Docker.

## Requirements

- Docker and Docker Compose
- For optional local development: Java 21 (backend), Node.js 20+ (frontend)

## Ports

| Service   | Port | URL / Usage                 |
|-----------|------|-----------------------------|
| Backend   | 9000 | http://localhost:9000       |
| Frontend  | 4000 | http://localhost:4000       |
| MariaDB   | 3307 | MySQL client (host: localhost, port: 3307) |

## Running with Docker

From the repository root:

```bash
docker compose build
docker compose up
```

- **Frontend**: http://localhost:4000  
- **Backend (health)**: http://localhost:9000/health  

To run in the background: `docker compose up -d`.

## Development with Dev Containers

If you use **VS Code** or **Cursor** with the [Dev Containers](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers) extension, you can open the project inside a container that already includes Java 21, Maven, Node 20 and Angular CLI, with MariaDB running in another container.

1. Install the **Dev Containers** extension.
2. Open the repo folder and when prompted, or from the command palette (**Ctrl/Cmd+Shift+P**), choose **Dev Containers: Reopen in Container**.
3. Wait for the image to build and MariaDB to start (ports 9000, 4000 and 3307 are forwarded to the host).

**Auto-start:** When opening the workspace inside the dev container, two tasks (defined in `.vscode/tasks.json`) run and start the backend and frontend in **two** editor terminals. You do not need to start them manually.

To disable auto-start, remove the `"runOptions": { "runOn": "folderOpen" }` block from each task in `.vscode/tasks.json`. To run them manually: **Terminal > Run Task** and choose "Backend: Spring Boot" or "Frontend: Angular".

## Local development

### Database

You can start only MariaDB:

```bash
docker compose up mariadb -d
```

Connection: host `localhost`, port `3307`, database `avanzada`, user `avanzada`, password `avanzada` (as in `docker-compose.yml`).

**Initialize reference data and admin user:** with MariaDB running, run the backend once with the `--init-data` argument. It will create states, channels, request types, and an admin user (identifier `admin`, password `admin123`), then exit:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--init-data
```

After that, start the backend normally (`mvn spring-boot:run`) and log in at http://localhost:4000/login with `admin` / `admin123`. The command is idempotent: you can run it again without duplicating data.

### Backend

With MariaDB running (Docker or local on 3307):

```bash
cd backend
mvn spring-boot:run
```

Default configuration in `application.yml` uses `localhost:3307`. For tests: `mvn verify`.

### Frontend

```bash
cd frontend
npm ci
npm start
```

Served at http://localhost:4000. The proxy forwards `/api` to `http://localhost:9000`, so the backend must be running for API calls. You must **log in** (e.g. at http://localhost:4000/login) before using the app; the backend requires a valid JWT for all API calls except login and health.

### Authentication and roles

- **Login**: `POST /api/auth/login` with `{ "identifier": "...", "password": "..." }`. Returns a JWT and user info (id, identifier, name, role).
- **Roles** (RF-13):
  - **STUDENT**: Can register new requests and view data. Cannot classify, assign, attend, or close.
  - **STAFF**: Can register, classify, assign, and attend requests. Cannot close.
  - **ADMIN**: Full access including closing requests.
- If no user in the database has a password set, the backend sets the first user’s password to `admin123` and role to ADMIN on startup (see `DevAuthBootstrap`). Use that user’s identifier and `admin123` to log in. In production, set user passwords explicitly (e.g. via DB or future admin API) and set `JWT_SECRET` (min 32 characters).

## Optional: AI integration (RF-09, RF-10, RF-11)

The backend can optionally call an OpenAI-compatible LLM for:

- **Request summary** – `GET /api/requests/{id}/summary`: textual summary of the request and its history.
- **Type/priority suggestion** – `POST /api/ai/suggest` with `{ "description": "..." }`: suggests request type code and priority from the description. The client must confirm or adjust before applying; suggestions are never auto-applied.

**Configuration** (in `backend/src/main/resources/application.yml` and/or environment variables):

| Property / Env var       | Default                          | Description |
|--------------------------|-----------------------------------|-------------|
| `app.ai.enabled`         | `false`                           | Set to `true` to allow AI calls. |
| `OPENAI_API_KEY`         | (empty)                           | API key for the LLM provider. |
| `OPENAI_API_URL`         | `https://api.openai.com/v1/chat/completions` | Endpoint (OpenAI or compatible). |
| `OPENAI_MODEL`           | `gpt-3.5-turbo`                   | Model name. |
| `app.ai.timeout-seconds` | `10`                              | Timeout for LLM requests. |

**When IA is disabled or unavailable:**

- Summary: the API returns a non-LLM fallback summary (request id, state, history count).
- Suggest: the API returns `available: false` and a message; the client can ignore or show the message. Core flows (registration, classification, lifecycle, closure) never depend on AI and work normally.

## Structure

```
Avanzada/
├── .devcontainer/    # Dev Containers config (Dockerfile + compose + devcontainer.json)
├── .vscode/          # tasks.json: Backend/Frontend tasks (optional; may be in .gitignore)
├── .cursor/rules/    # Rules for AI assistants (Java, Angular, Docker)
├── backend/          # Spring Boot 3, Java 21, JPA, MariaDB
├── frontend/         # Angular 18, standalone; styling is Tailwind CSS only (see frontend/DESIGN.md)
├── docker-compose.yml
├── AGENTS.md         # Instructions for AI assistants
└── README.md
```

For more detail (package structure, conventions, validation) see **AGENTS.md** in the project root. For frontend styling and design patterns see **frontend/DESIGN.md**.
