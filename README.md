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

**Initialize and populate the database:** with MariaDB running, run the backend once with the `--init-data` argument. It will create:

- **Reference data:** states, channels, request types (all tables populated).
- **Users:** admin, staff, student (each with a known password for testing).
- **Sample requests and history:** 5 requests in different lifecycle states, with history entries (only when the request table is empty).

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--init-data
```

**Test users (identifier / password):**

| Role   | Identifier | Password   | Notes |
|--------|------------|------------|-------|
| ADMIN  | admin      | admin123   |       |
| STAFF  | staff      | staff123   |       |
| STUDENT| student1   | student123 | Has sample requests |
| STUDENT| student2   | student123 | Has sample requests |
| STUDENT| student3   | student123 | Has sample requests |
| STUDENT| student4   | student123 | Has sample requests |
| STUDENT| student5   | student123 | User only (no requests) |

After that, start the backend normally (`mvn spring-boot:run`) and log in at http://localhost:4000/login. The init command is idempotent: you can run it again without duplicating reference data or users; sample requests are created only when there are no requests yet.

**Full DB reset (drop all tables, recreate schema, then init-data):** use the `reset` profile so Hibernate runs with `ddl-auto: create`. Use the same profile as your backend (e.g. `docker` in dev container). If port 9000 is in use, pick another port:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--init-data --server.port=9001" -Dspring-boot.run.profiles=docker,reset
```

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
  - **STUDENT**: Can register new requests and view only their own requests. Cannot classify, assign, attend, or close.
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

**Using a `.env` file (recommended for local development):**

1. Copy `backend/.env.example` to `backend/.env`.
2. In `backend/.env`, set `OPENAI_API_KEY=sk-your-key` and `APP_AI_ENABLED=true`.
3. The "Backend: Spring Boot" task in `.vscode/tasks.json` loads `backend/.env` automatically when starting the backend (if the file exists). No extension required.
4. Keep `.env` out of version control (it is in `.gitignore`).

**When IA is disabled or unavailable:**

- Summary: the API returns a non-LLM fallback summary (request id, state, history count).
- Suggest: the API returns `available: false` and a message; the client can ignore or show the message. Core flows (registration, classification, lifecycle, closure) never depend on AI and work normally.

## Deployment (Railway + Vercel)

The backend is intended to be deployed on **[Railway](https://railway.com/)**, and the frontend on **[Vercel](https://vercel.com/)**.

### Backend on Railway

1. Create a new project on Railway and add a **MySQL** (or MariaDB-compatible) database from the catalog.
2. Add a **service** from this repo: use the **backend** directory as root (or set `RAILWAY_DOCKERFILE_PATH` to `backend/Dockerfile` if deploying from monorepo root).
3. Set **environment variables**:
   - `SPRING_PROFILES_ACTIVE` = `railway`
   - `SPRING_DATASOURCE_URL` = JDBC URL for your MySQL service (e.g. `jdbc:mysql://host:port/db`; build it from the variables Railway provides for the MySQL service, e.g. `MYSQL_URL` is often `mysql://...` — use the same host, user, password, port, and database in `jdbc:mysql://...` form).
   - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` = DB user and password (or use the vars from the Railway MySQL plugin).
   - `JWT_SECRET` = a long random secret (at least 32 characters) for production.
4. Deploy. Railway will build the Dockerfile and run the JAR. Expose the service and note the public URL (e.g. `https://your-app.railway.app`).
5. Run data init once (reference data + admin user): use Railway’s **Run Command** or a one-off deploy with `--init-data` and `SPRING_PROFILES_ACTIVE=railway`, or connect to the DB and run the init logic manually. Default admin: identifier `admin`, password `admin123` (change in production).

### Frontend on Vercel

1. Import the repo in Vercel and set the **Root Directory** to `frontend`.
2. Add **Environment Variable**:
   - `API_URL` = your Railway backend URL including path to API (e.g. `https://your-app.railway.app/api`). The build script injects this into the Angular app.
3. Deploy. Vercel uses `vercel.json`: the build runs `node scripts/set-api-url.js && ng build --configuration=production`, which writes the API base URL into the production environment before building.

The backend allows cross-origin requests (CORS) so the browser can call the API from the Vercel origin.

### Docker (local or self-hosted)

For local development or a single-machine deploy, use Docker Compose as described above: `docker compose up` runs MariaDB, backend (profile `docker`), and frontend (nginx proxying `/api/` to the backend). No Railway or Vercel required.

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
