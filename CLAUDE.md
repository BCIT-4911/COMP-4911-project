# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Database (run once, stays running)
```bash
cd sql && docker compose up -d
```
If DDL scripts changed, destroy and recreate the volume:
```bash
cd sql && docker compose down -v && docker compose up -d
```

### Backend
```bash
cd backend && mvn clean wildfly:dev -DskipTests
```
WildFly is provisioned automatically by Maven — no separate install needed. Runs on `http://localhost:8080`.

### Frontend
```bash
cd frontend && dotnet watch
```
Runs on `http://localhost:3000`, connects to backend via `ApiBaseUrl` in `frontend/appsettings.json`.

### Health check
`GET http://localhost:8080/Project/api/greet`

## Architecture

### Backend (Jakarta EE / WildFly)
Package root: `backend/src/main/java/`

Two package namespaces exist — newer code lives under `com.corejsf`, older controllers under `ca.bcit.infosys`:

| Package | Purpose |
|---------|---------|
| `com.corejsf` | `RestApplication` (`@ApplicationPath("/api")`), `CorsFilter` |
| `com.corejsf/api/` | Newer JAX-RS resources (e.g. `EarnedValueResource`) |
| `com.corejsf/Entity/` | JPA entities (Employee, Project, WorkPackage, Timesheet, etc.) |
| `com.corejsf/Service/` | Business logic / EJB services |
| `com.corejsf/Repo/` | JPA repositories |
| `com.corejsf/DTO/` | Data transfer objects |
| `com.corejsf/Pojo/` | Plain POJOs |
| `ca.bcit.infosys/project/` | ProjectController, ProjectValidation |
| `ca.bcit.infosys/workpackage/` | WorkPackageController, WorkPackageValidation |
| `ca.bcit.infosys/timesheet/` | TimesheetController, TimesheetValidation |
| `ca.bcit.infosys/laborgrade/` | LaborGradeController |

JPA persistence unit: `project-management-pu`, datasource: `java:jboss/datasources/MySQLDS`.
Schema is managed **exclusively via DDL scripts** in `sql/init_scripts/` — Hibernate schema generation is set to `none`.

New REST resources should go in `com/corejsf/api/`, entities in `com/corejsf/Entity/`, services in `com/corejsf/Service/`.

### Frontend (ASP.NET Core Razor Pages)
- `frontend/Pages/` — Razor Pages: `Index`, `Projects`, `WorkPackages`, `Timesheets/Index`, `EarnedValue/Index`
- `frontend/model/` — shared C# model/DTO classes used by page models
- `frontend/Pages/Shared/_Layout.cshtml` — shared layout
- Backend URL configured in `frontend/appsettings.json` → `ApiBaseUrl`

### Database
MySQL runs in Docker on **port 3307** (host) mapped to 3306 in the container.
Credentials in `sql/.env` (copied from `sql/.env.example`) must match the `<env>` block in `backend/pom.xml`.

| Variable | Value |
|----------|-------|
| `MYSQL_DATABASE` | `Project_Management` |
| `MYSQL_USER` | `project_user` |
| `MYSQL_PASSWORD` | `password` |
| `MYSQL_HOST` | `127.0.0.1` |
| `MYSQL_PORT` | `3307` |

## Naming Conventions

**Backend (Java):** Classes → PascalCase, methods → camelCase, REST routes → `/api/resource-name`

**Frontend (.NET):** Pages → `PascalCase.cshtml`, PageModels → `PascalCase.cshtml.cs`, variables → camelCase

## Branching

- `main` → stable release
- `dev` → active development
- Feature branches: `feature/<task-name>` (e.g. `feature/login`, `bugfix/api-routing`)
- Never push directly to `main`; open a PR targeting `dev` or `main`
