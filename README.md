# COMP4911 Timesheet System

## Overview
This project is a web-based information system to support software project management.  
It allows employees to submit weekly timesheets and allows managers/admins to track project effort, work packages, and generate reports.

Course: COMP4911  
Instructor/Sponsor: Bruce Link  



## Tech Stack

### Frontend
- ASP.NET Core Razor Pages  
- Purpose: Web UI for employees and managers

### Backend
- Jakarta EE REST API (WildFly)  
- EJB service layer planned for future iterations

### Database
- SQL Database 

### Deployment
- OKD / OpenShift (container-based deployment)


## Repository Structure

```text
├── frontend/    # .NET client application
├── backend/     # Jakarta EE backend (EJB + REST)
├── Sql/         # SQL scripts and schema files
├── tests/       # Any test code for front end or back end
├── docs/        # Any documentation for the project
└── README.md
```


## REST Endpoints

- `GET /api/greet` — health check
- `GET /api/projects` — list all projects
- `POST /api/projects` — create a project
- `GET /api/projects/{id}/workpackages` — list work packages for a project
- `GET /api/workpackages` — list all work packages
- `POST /api/workpackages` — create a work package
- `GET /api/earned-value` — earned value report data


## Local Development Setup

### Prerequisites

Install the following tools before proceeding:

| Tool | Version | Purpose |
|------|---------|---------|
| Java JDK | 21+ | Backend compilation and WildFly runtime |
| Maven | 3.9+ | Backend build and WildFly provisioning |
| Docker Desktop | Latest | Runs the MySQL database container |
| .NET SDK | 10.0+ | Frontend (Razor Pages) |
| Git | Latest | Version control |

> WildFly does **not** need to be installed separately — Maven downloads and provisions it automatically via the `wildfly-maven-plugin` in `backend/pom.xml`.


### Step 1: Clone the Repository

```bash
git clone <repo-url>
cd COMP-4911-project
```


### Step 2: Configure the Database Environment

The `sql/.env.example` file contains the shared credentials used by **all squads**. These values must stay in sync with the `<env>` block in `backend/pom.xml`.

```bash
cd sql
cp .env.example .env
```

**Do not change the values.** They are pre-configured:

| Variable | Value | Used By |
|----------|-------|---------|
| `MYSQL_ROOT_PASSWORD` | `root` | Docker (root account) |
| `MYSQL_DATABASE` | `Project_Management` | Docker, WildFly, Frontend |
| `MYSQL_USER` | `project_user` | Docker, WildFly |
| `MYSQL_PASSWORD` | `password` | Docker, WildFly |
| `MYSQL_HOST` | `127.0.0.1` | WildFly, Frontend |
| `MYSQL_PORT` | `3307` | WildFly, Frontend (host-side, mapped to 3306 in container) |

These same values appear in `backend/pom.xml` under the `<env>` block (lines 53-59). If the two ever diverge, WildFly will fail to connect to MySQL.


### Step 3: Start the MySQL Database

Make sure Docker Desktop is running, then:

```bash
cd sql
docker compose up -d
```

Verify the container started and DDL scripts executed:

```bash
docker logs project_manager_mysql
```

You should see `ready for connections` and no SQL errors.

> **If DDL scripts are updated:** the Docker volume must be destroyed first so the database is re-initialized from scratch:
> ```bash
> docker compose down -v
> docker compose up -d
> ```
> This **deletes all existing data** in the local database.


### Step 4: Build and Run the Backend

```bash
cd backend
mvn clean package wildfly:dev
```

Maven will:
1. Compile the Jakarta EE application
2. Download and provision a WildFly server with the `mysql` add-on
3. Deploy `Project.war` and start the server on port `8080`

Verify the backend is running:

```
http://localhost:8080/Project/api/greet
```

You should see a greeting response.


### Step 5: Start the Frontend

In a **separate terminal**:

```bash
cd frontend
dotnet watch
```

The frontend runs at:

```
http://localhost:3000
```

It connects to the backend at the URL configured in `frontend/appsettings.json` (`http://localhost:8080/Project`).


### Running Everything Together (Quick Reference)

```bash
# Terminal 1 — Database (run once, stays running)
cd sql
docker compose up -d

# Terminal 2 — Backend
cd backend
mvn clean package wildfly:dev

# Terminal 3 — Frontend
cd frontend
dotnet watch
```

Open http://localhost:3000 — the frontend should display data from the backend.


### Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `Unable to acquire JDBC Connection` | Wrong credentials or MySQL not running | Verify `sql/.env` matches `pom.xml` `<env>` block; run `docker compose up -d` |
| `Unable to determine Dialect without JDBC metadata` | WildFly can't reach MySQL at all | Ensure the `<env>` block in `pom.xml` has all 5 variables (`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`) |
| `WFLYCTL0211: Cannot resolve expression` | `<env>` block missing from `pom.xml` | Add the `<env>` block inside the `wildfly-maven-plugin` `<configuration>` section |
| `Required services not installed: MySQLDS` | `persistence.xml` references wrong datasource name | Ensure `persistence.xml` uses `java:jboss/datasources/MySQLDS` |
| `Schema-validation: missing column` | Docker volume has stale DDL | Run `docker compose down -v && docker compose up -d` in `sql/` |
| `Cross-Origin Request Blocked` | CORS filter issue | Verify `CorsFilter.java` handles OPTIONS preflight and doesn't duplicate headers |


## Team Rules and Standards

- Use meaningful names and keep code readable
- Never push directly to main
- Prefer small PRs that are easy to review
- Use meaningful variable and class names
- Keep REST endpoints small business logic belong in service/EJB layer

### Branching
- main → stable release branch  
- dev → active development branch  
- Feature branches:
- Always create a feature branch:

  feature/<task-name>

- Examples:

  feature/login  
  feature/timesheet-entry  
  bugfix/api-routing  

- Open a Pull Request (PR) before merging


### Pull Requests
- PR must include a clear description of changes
- Assign all reviewers listed here
- PR should be small and focused
All pull requests must be reviewed by:

- Kaid  @kaid711
- Nate  @NateRolo 
- Lucas @zhapte


### Naming Conventions

Backend (Java)
- Classes: PascalCase  
- Methods: camelCase  
- REST routes: `/api/resource-name`

Frontend (.NET)
- Pages: PascalCase.cshtml  
- PageModels: PascalCase.cshtml.cs  
- Variables: camelCase


### Commit Message Guidelines

Good examples:
- Add hello world endpoint
- Setup Razor Pages frontend
- Fix API base URL config

Bad examples:
- update stuff
- changes
- fix


### Folder Organization Rules

- frontend/ → all .NET Razor UI code
- backend/  → all Jakarta REST/EJB code
- sql/      → DB setup or deployment scripts
- tests/    → unit tests and integration tests for both frontend and backend 
- docs/		→ reports, diagrams, meeting notes 


### Documentation Rules

- Inline code formatting should be used in documentation:
  - Use `REST endpoints` instead of plain text
  - Use `dotnet run` when referencing commands
  - Use `GET /api/hello` when referencing routes
  
- Document all important endpoints in a simple list:

  - `POST /api/auth/login`
  - `GET /api/timesheets`
  - `POST /api/timesheets/submit`

- Add setup instructions directly in the README whenever something changes:
  - New environment variable
  - New required tool
  - New deployment step
  

## Authentication Plan

Planned approach:
- Token-based login (UUID stored in database)
- Token stored in HttpOnly cookie
- Role-based access control (Admin, Employee, Manager)


## Deployment Notes

