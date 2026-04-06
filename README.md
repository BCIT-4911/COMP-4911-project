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
|- frontend/    # .NET client application
|- backend/     # Jakarta EE backend (EJB + REST)
|- sql/         # SQL scripts and schema files
|- tests/       # Any test code for front end or back end
|- docs/        # Any documentation for the project
`- README.md
```


## REST Endpoints

All backend routes are exposed under `/Project/api` in local development.

### Health

- `GET /api/greet` - simple backend health check

### Authentication

- `POST /api/auth/login` - authenticate with `empId` and password
- `GET /api/auth/can-access-approver-dashboard` - check approver dashboard access for the current user
- `GET /api/auth/direct-reports` - list direct report employee IDs for the current user

### Employees

- `GET /api/employees` - list employees
- `GET /api/employees/{id}` - get one employee
- `POST /api/employees` - create an employee
- `PUT /api/employees/{id}` - update an employee
- `POST /api/employees/employee-self-update-password` - update the logged-in employee password
- `DELETE /api/employees/{id}` - delete an employee

### Labor Grades

- `GET /api/labor-grades` - list labor grades
- `GET /api/labor-grades/{id}` - get one labor grade
- `POST /api/labor-grades` - create a labor grade
- `PUT /api/labor-grades/{id}` - update a labor grade
- `DELETE /api/labor-grades/{id}` - delete a labor grade
- `GET /api/labor-grades/report?projectId=&wpId=&employeeId=&weekEnding=` - labor report

### Projects

- `GET /api/projects` - list projects visible to the current user
- `GET /api/projects/{id}` - get one project
- `POST /api/projects` - create a project
- `PUT /api/projects/{id}` - update a project
- `DELETE /api/projects/{id}` - delete a project
- `PUT /api/projects/{id}/close` - close a project
- `PUT /api/projects/{id}/open` - reopen a project
- `POST /api/projects/{id}/workpackages` - create a work package under a project
- `GET /api/projects/{id}/workpackages` - list work packages for a project
- `POST /api/projects/{id}/employees/{empId}?role=` - assign an employee to a project
- `DELETE /api/projects/{id}/employees/{empId}` - remove an employee from a project
- `GET /api/projects/{id}/employees` - list employees assigned to a project
- `GET /api/projects/{id}/weekly-report` - weekly project report
- `GET /api/projects/{id}/report` - plain-text project report

### Work Packages

- `GET /api/workpackages` - list work packages
- `GET /api/workpackages/{id}` - get one work package
- `POST /api/workpackages` - create a work package
- `PUT /api/workpackages/{id}` - update a work package
- `DELETE /api/workpackages/{id}` - delete a work package
- `POST /api/workpackages/{id}/employees/{empId}?role=` - assign an employee to a work package
- `DELETE /api/workpackages/{id}/employees/{empId}` - remove an employee from a work package
- `GET /api/workpackages/{id}/employees` - list employees assigned to a work package
- `PUT /api/workpackages/{id}/close` - close a work package
- `PUT /api/workpackages/{id}/open` - reopen a work package
- `PUT /api/workpackages/{id}/etc` - update estimated time to completion
- `GET /api/workpackages/{id}/children` - list child work packages
- `GET /api/workpackages/{id}/parent` - get parent work package
- `GET /api/workpackages/{id}/report` - plain-text work package report
- `GET /api/workpackages/chargeable` - list chargeable work packages for the current user

### Timesheets

- `GET /api/timesheets` - list timesheets, optionally filtered by `empId`, `approverId`, and `status`
- `GET /api/timesheets/{id}` - get one timesheet
- `POST /api/timesheets` - create a timesheet
- `PUT /api/timesheets/{id}` - update a timesheet
- `PUT /api/timesheets/{id}/submit` - submit a timesheet
- `PUT /api/timesheets/{id}/approve` - approve a timesheet
- `PUT /api/timesheets/{id}/return` - return a timesheet for changes
- `DELETE /api/timesheets/{id}` - delete a timesheet

### Earned Value

- `GET /api/earned-value?parentWpId=&asOf=` - weekly earned value report for a control account
- `GET /api/earned-value/projects/{projectId}/monthly-report?asOf=` - monthly earned value report for a project
- `GET /api/earned-value/workpackages/{wpId}/monthly-performance?asOf=` - monthly performance report for one work package

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

> WildFly does **not** need to be installed separately. Maven downloads and provisions it automatically via the `wildfly-maven-plugin` in `backend/pom.xml`.


### Step 1: Clone the Repository

```bash
git clone https://github.com/BCIT-4911/COMP-4911-project.git
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
# Terminal 1 - Database (run once, stays running)
cd sql
docker compose up -d

# Terminal 2 - Backend
cd backend
mvn clean package wildfly:dev

# Terminal 3 - Frontend
cd frontend
dotnet watch
```

Open http://localhost:3000 - the frontend should display data from the backend.


### Seeded users

After the database and backend are running on a fresh local database, the following base users are seeded by `backend/src/main/java/com/corejsf/seed/EmptyDbSeeder.java`. Passwords are BCrypt-hashed in the database, and **all seeded passwords are `password`**:

| emp_id | First name | Last name  | System role        | Supervisor   |
|--------|------------|------------|--------------------|--------------|
| 1      | Wile       | Coyote     | ADMIN              | N/A          |
| 2      | Elmer      | Fudd       | OPERATIONS_MANAGER | N/A         |
| 3      | Road       | Runner     | HR                 | Elmer Fudd   |
| 4      | Bugs       | Bunny      | EMPLOYEE           | Elmer Fudd   |
| 5      | Daffy      | Duck       | EMPLOYEE           | Bugs Bunny   |
| 6      | Tweety     | Bird       | EMPLOYEE           | Bugs Bunny   |
| 7      | Sylvester  | Cat        | EMPLOYEE           | Bugs Bunny   |
| 8      | Marvin     | Martian    | EMPLOYEE           | Elmer Fudd   |

These eight users are the deterministic base accounts used by the backend tests and default demo flow.

The seeder also creates additional demo data for labor and earned-value reporting, including extra employees such as `Marcus Aurelius`, `Elena Fisher`, `James Holden`, and `Sarah Connor`. Those extra records support report screenshots and test/demo scenarios, but the eight users above are the main accounts to use for local login.

Use `empId` and `password` to log in. Example API payload:

```json
{
  "empId": 1,
  "password": "password"
}
```


### Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `Unable to acquire JDBC Connection` | Wrong credentials or MySQL not running | Verify `sql/.env` matches `pom.xml` `<env>` block; run `docker compose up -d` |
| `Unable to determine Dialect without JDBC metadata` | WildFly can't reach MySQL at all | Ensure the `<env>` block in `pom.xml` has all 5 variables (`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`) |
| `WFLYCTL0211: Cannot resolve expression` | `<env>` block missing from `pom.xml` | Add the `<env>` block inside the `wildfly-maven-plugin` `<configuration>` section |
| `Required services not installed: MySQLDS` | `persistence.xml` references wrong datasource name | Ensure `persistence.xml` uses `java:jboss/datasources/MySQLDS` |
| `Schema-validation: missing column` | Docker volume has stale DDL | Run `docker compose down -v && docker compose up -d` in `sql/` |
| `Cross-Origin Request Blocked` | CORS filter issue | Verify `CorsFilter.java` handles OPTIONS preflight and doesn't duplicate headers |


## Running Tests

Source lives in `tests/`; `backend/pom.xml` points `<testSourceDirectory>` at that folder. **JUnit 5** everywhere; API tests use **REST Assured**. Target URL is controlled by JVM properties `api.baseUri` and `api.basePath`, which Surefire sets from Maven properties (defaults: `http://localhost:8080` and `/Project/api`). See `tests/com/corejsf/TestConfig.java`.

### Unit tests (no server)

`RebacServiceTest`, `JwtUtilTest`, `PasswordHashTest` - pure Java, no HTTP.

```bash
cd backend
mvn test -Dtest=RebacServiceTest,JwtUtilTest,PasswordHashTest
```

### API integration tests (HTTP)

Need a running backend and MySQL with data compatible with `EmptyDbSeeder` (Looney Tunes users, password `password`). Start the stack as in [Step 3](#step-3-start-the-mysql-database) and [Step 4](#step-4-build-and-run-the-backend). If the DB is dirty, reset it (`docker compose down -v && docker compose up -d` in `sql/`) and restart WildFly so the seeder runs.

**After code changes**, redeploy before testing (e.g. `mvn package -DskipTests` while `wildfly:dev` is running, or restart WildFly).

| Class | Focus |
|-------|--------|
| `HealthCheckTest` | `GET /greet` with JWT |
| `AuthEndpointTest` | Login, approver dashboard, direct reports |
| `AuthFilterIntegrationTest` | Missing/invalid Bearer -> 401 |
| `EmployeeResourceTest` | Employees list/get/create |
| `LaborGradeResourceTest` | Labor grades list/get |
| `ProjectResourceTest` | Projects CRUD, close/open, assignments, report, RBAC |
| `WorkPackageResourceTest` | Work packages CRUD, hierarchy, report |
| `TimesheetResourceTest` | Draft -> submit -> approve/return -> delete |
| `ProjectAndWorkPackageRebacIntegrationTest` | Project/WP assignment and open/close ReBAC |

**Commands**

```bash
# Local WildFly (default Surefire properties)
cd backend
mvn test
```

```bash
# Remote OKD (ROOT.war: base path /api). Profile avoids broken Windows parsing of unquoted https:// -D flags.
cd backend
mvn test -Premote-okd
```

Profile `remote-okd` in `backend/pom.xml` points at the squad host; change the profile or Maven properties for another URL.

**Manual overrides** (quote each `-D` in PowerShell):

```powershell
cd backend
mvn test "-Dapi.baseUri=https://example.com" "-Dapi.basePath=/api"
```

| Property | When to use |
|----------|-------------|
| `api.seedOpsEmpId` | First login uses this emp id (default `1`). Set if your OPS user is not id 1. |
| `api.relaxedTls` | `true` to force relaxed HTTPS trust (HTTPS targets already use relaxed TLS in `TestConfig`). |

**Subset**

```bash
cd backend
mvn test -Dtest=AuthFilterIntegrationTest,WorkPackageResourceTest
```




## Folder Organization Rules

- frontend/ - all .NET Razor UI code
- backend/ - all Jakarta REST/EJB code
- sql/ - DB setup or deployment scripts
- tests/ - unit tests and integration tests for both frontend and backend
- docs/ - reports, diagrams, meeting notes

## Authentication

Authentication is already implemented in the backend using JWT bearer tokens.

- Login endpoint: `POST /api/auth/login`
- Login request body: `empId` and `password`
- Login response: a signed JWT returned in the response body
- Protected endpoints: require `Authorization: Bearer <token>`
- Unprotected routes: `POST /api/auth/login` and `OPTIONS` requests

### JWT contents

The generated JWT currently includes:

- `empId`
- `systemRole`
- `firstName`
- `lastName`
- `exp` (expiration timestamp)

### Token behavior

- Tokens are signed with HMAC-SHA256
- Token lifetime is currently 8 hours
- The signing secret is read from the `JWT_SECRET` environment variable
- If `JWT_SECRET` is not set, the backend falls back to a development default secret

### Authorization model

After authentication, the backend applies role-based and ReBAC-style authorization checks. In practice, access rules depend on both the user's system role and their relationship to the target employee, project, work package, or timesheet.

### Example authenticated request

```http
Authorization: Bearer <token>
```


## Deployment Notes

This project is deployed to OKD with separate frontend and backend delivery flows.

- `frontend` is built as a Docker image and deployed to OKD
- `backend` is deployed to OKD using the Helm-based setup together with the WildFly/Maven build from `backend/pom.xml`

### Deployment links

- Frontend: `http://frontend4911-liul-labs.apps.okd4.infoteach.ca/`
- Backend: `https://backend-liul-labs.apps.okd4.infoteach.ca/`


