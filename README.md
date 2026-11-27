# Rocket Fly III

Rocket Fly III is a mobile-friendly, read-only companion to [Firefly III](https://www.firefly-iii.org/). It aims for the ease of use of Rocket Money while keeping Firefly III authoritative for financial records, accounts, transactions, categories, and budgets.

## Local installation

Download the launcher ZIP from the [latest packaged release](https://github.com/blakbro2k/rocketflyIII/releases/latest). Follow the [local setup and upgrade instructions](distribution/README.md).

## Product principles

- **Firefly III is the system of record.** Rocket Fly III never writes financial changes back to Firefly III.
- **One-to-one installation mapping.** One Rocket Fly III instance connects to one Firefly III instance.
- **Useful, understandable analysis.** The UI presents imported data and clearly distinguishes derived findings from source records.
- **Mobile-friendly by design.** Responsive layouts and straightforward navigation are core MVP requirements.
- **Incremental delivery.** Build and verify one useful slice at a time.

## MVP experience

The main screen has three sections. A settings icon in the upper-left opens a drawer that slides in from the right.

### 1. Budgets

- Carousel of budgets fetched from Firefly III.
- A pie chart for each budget.
- First pass uses Firefly III transactions and budget data as-is; no custom left-to-spend interpretation yet.
- A **Transfers as Income** toggle at the top controls a Rocket Fly III reporting interpretation, without changing Firefly III records.
- Exact chart slices, time window, and effect of the toggle will be specified before implementation.

### 2. Accounts

Expandable groups for accounts fetched from Firefly III:

- Assets
- Expense
- Revenue
- Liabilities

### 3. Utilities

- **Transfer detection:** distinguish transfers explicitly recorded in Firefly III from potential transfers inferred from matching transactions.
- **Account reconciliation:** compare imported CSV transaction records against Firefly III records; flag mismatches and possible duplicates without changing source data.
- **Category gap analysis:** surface transactions with missing or potentially incomplete categorization for review.

## Outside the MVP

- **Section 4 — Recurring:** recurring transaction analysis and detection of gaps in expected recurring activity.
- Advanced budget **left-to-spend** features.
- Expanded account-scoped role-based access controls, subject to future prioritization.

## Technical direction

- Backend: Java 21, Spring Boot, Maven.
- Local development: IntelliJ IDEA on Windows.
- Frontend: responsive web UI; framework and component details to be confirmed.
- Firefly III integration: authenticated, read-only REST API requests.
- Credentials: keep API tokens out of source control, logs, and committed configuration; inject through local environment or an ignored configuration mechanism.
- Rocket Fly III may persist its own analysis settings and derived metadata, but not independent authoritative financial records or budget definitions.

## First local-development checkpoint

**Goal:** prove the application running in IntelliJ can read real data from the configured Firefly III instance.

1. Confirm the Firefly III base URL is reachable from the Windows development machine.
2. Supply a valid Firefly III personal access token through local configuration (never commit it).
3. Run the Spring Boot backend from IntelliJ.
4. Make an authenticated, read-only request to a Firefly III API endpoint such as `GET /api/v1/accounts`.
5. Confirm a successful HTTP response containing actual account data, with no secrets printed in logs.
6. Optionally verify budget retrieval through `GET /api/v1/budgets`.

**Pass condition:** a real account or budget response is successfully retrieved by the locally running application. Project bootstrap alone is not proof of API connectivity.

**Status:** not yet verified in this README. Requires a local run and observed response.

## Proposed delivery sequence (subject to roadmap planning)

1. Local authenticated Firefly III read connectivity.
2. Account listing grouped by type.
3. Budget carousel and pie charts, including the report-only Transfers as Income control.
4. Utilities: transfer detection, CSV reconciliation, category gap analysis.
5. Responsive polish, settings drawer, and MVP acceptance testing throughout the implementation.

## Future roadmap

The post-MVP roadmap will be developed separately, including recurring analysis, left-to-spend insights, and expanded RBAC.

## License

To be determined.

### Local profiles and roles

Start RocketFlyIII with the existing Firefly environment variables. In IntelliJ, use the project root as the working directory. Run `java scripts/SetupToken.java` from that same directory, open `http://localhost:8080/setup.html`, and enter the one-time token, a named admin username, and a new password. Then sign in at `/login`. The local profile database is stored in `rocketfly-data`. Set `ROCKETFLY_DATA_DIR` for both the app and command to use another private directory.

For Docker, run `docker compose up --build -d`, then `docker compose exec rocketflyiii java /app/SetupToken.java`. The Compose volume preserves users and grants across restarts. Use the same command for recovery, enter an existing System Admin username at `/setup.html`, and choose a new password. Generating a token replaces any previous token. A token expires after 15 minutes and is consumed on use. Keep access to the data directory and Docker restricted to installation operators. For remote deployment, serve the app through an HTTPS reverse proxy.

Use **Settings → Roles** to create a role and select its accounts and budgets. **Refresh from Firefly III** reloads the choices. Use **Settings → Profiles** to create a login and assign a role. A budget grant includes its matched transactions, while opening account history requires an account grant. Subscriptions are available to System Admins. Reassign profiles before deleting their role.
