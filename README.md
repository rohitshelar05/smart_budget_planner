# Smart Budget Planner & Spending Analyzer

Java 17 · Spring Boot 3.3 · Spring Data JPA · MySQL · HTML/CSS/vanilla JS · Chart.js

A single deployable app: Spring Boot serves the REST API under `/api` and the dashboard from the same origin (no CORS setup needed).

## Features
- Income and expense tracking with full CRUD, validation and clear error messages
- Filters: type, category, date range and text search
- Monthly category budgets with **80% (warning)** and **100% (exceeded)** alerts
- Cash-flow chart (last 6 months), category breakdown doughnut, savings rate
- Money is `BigDecimal` / `DECIMAL(15,2)`; dates are `LocalDate`
- No credentials in the repository: the database login comes from environment variables or a git-ignored config file

## Project layout
```
pom.xml
config/application-example.properties   copy to config/application.properties (git-ignored)
src/main/java/com/example/budget/       model, repository, dto, service, controller, exception
src/main/resources/static/              index.html, css/styles.css, js/app.js
src/test/java/com/example/budget/       Mockito unit tests, WebMvc test, H2 integration test
```

## Windows setup (exact commands)

### 1. Install prerequisites
Open **PowerShell** and install with winget (or install the same tools manually):
```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install Oracle.MySQL
```
Close and reopen PowerShell, then verify:
```powershell
java -version
mvn -version
mysql --version
```
`java -version` must report 17 or newer. If `mysql` is not found, add MySQL's `bin` folder (for example `C:\Program Files\MySQL\MySQL Server 8.0\bin`) to your PATH.

### 2. Create the database and a dedicated user
```powershell
mysql -u root -p
```
Then, inside the MySQL prompt (replace the password with your own):
```sql
CREATE DATABASE budget_planner CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'budget_app'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON budget_planner.* TO 'budget_app'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```
Tables are created automatically on first start (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Provide the database settings (choose ONE option)

**Option A, environment variables for the current PowerShell window**
```powershell
cd path\to\smart-budget-planner
$env:DB_URL = "jdbc:mysql://localhost:3306/budget_planner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "budget_app"
$env:DB_PASSWORD = "REPLACE_WITH_A_STRONG_PASSWORD"
```

**Option A2, same thing in classic Command Prompt (cmd.exe)**
```bat
cd path\to\smart-budget-planner
set "DB_URL=jdbc:mysql://localhost:3306/budget_planner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
set "DB_USERNAME=budget_app"
set "DB_PASSWORD=REPLACE_WITH_A_STRONG_PASSWORD"
```

**Option B, local config file (git-ignored)**
```powershell
cd path\to\smart-budget-planner
Copy-Item config\application-example.properties config\application.properties
notepad config\application.properties
```
Edit the username and password, save, and run from the project root so Spring Boot finds `config\`.

Optional settings: `APP_CURRENCY` (ISO code, default `USD`, e.g. `EUR`, `INR`), `SERVER_PORT` (default `8080`), `DDL_AUTO` (default `update`; use `validate` once your schema is stable).

### 4. Test, run, open
```powershell
mvn clean test
mvn spring-boot:run
```
Open <http://localhost:8080>. The tests use an in-memory H2 database and never touch your MySQL data.

To build a runnable jar instead:
```powershell
mvn clean package
java -jar target\smart-budget-planner-1.0.0.jar
```
(Keep the same PowerShell window so the `DB_*` variables are still set, or use Option B.)

## REST API
| Method | Path | Notes |
|---|---|---|
| GET | `/api/transactions?type=&category=&from=&to=&search=` | `from`/`to` are `yyyy-MM-dd`; newest first |
| GET | `/api/transactions/{id}` | 404 if missing |
| POST | `/api/transactions` | 201 on success |
| PUT | `/api/transactions/{id}` | |
| DELETE | `/api/transactions/{id}` | 204 |
| GET | `/api/budgets?year=&month=` | budgets with spent, remaining, percentUsed, alertLevel |
| POST / PUT / DELETE | `/api/budgets`, `/api/budgets/{id}` | one budget per category per month (409 on duplicate) |
| GET | `/api/alerts?year=&month=` | budgets at 80%+ (`WARNING`) or 100%+ (`EXCEEDED`) |
| GET | `/api/analytics/summary?year=&month=` | income, expense, net, savings rate |
| GET | `/api/analytics/cashflow?year=&month=&months=6` | `months` 1 to 24, oldest first |
| GET | `/api/analytics/categories?year=&month=` | expense totals and share per category |
| GET | `/api/categories` | default plus custom categories |
| GET | `/api/config` | `{ "currency": "USD" }` |

`year`/`month` default to the current month. Errors share one JSON shape: `timestamp`, `status`, `error`, `message`, `fieldErrors`.

Example request body:
```json
{ "type": "EXPENSE", "amount": "42.50", "category": "Groceries", "description": "Weekly shop", "transactionDate": "2026-10-16" }
```

## Notes and limits
- Chart.js is bundled as a Maven WebJar and served from `/webjars/...`, so the dashboard has no external CDN dependency.
- There is no user login: the app is intended for personal or single-household use on a trusted machine. Add Spring Security before exposing it publicly.
- The transaction list is not paginated; this is fine for personal volumes.
- Category names are matched exactly after trimming; MySQL's default collation also ignores letter case.

## Troubleshooting
- `Could not resolve placeholder 'DB_URL'`: the environment variables are not set in this window (step 3).
- `Access denied for user`: the username or password does not match the MySQL user from step 2.
- `Communications link failure`: the MySQL service is not running (`Get-Service MySQL*`, then `Start-Service <name>`).
- Port 8080 busy: set `$env:SERVER_PORT = "8081"`.
