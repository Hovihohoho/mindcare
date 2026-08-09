# MindCare local infrastructure

## Start

```powershell
docker compose up -d
docker compose ps
```

Run these commands from the `infrastructure` directory. Copy `.env.example` to
`.env` first when local credentials need to be changed.

## PostgreSQL connection

| Setting | Value |
| --- | --- |
| Host | `localhost` |
| Port | `5433` |
| Database | `mindcare_db` |
| Username | `postgres_admin` |
| Password | `secretpassword` |
| JDBC URL | `jdbc:postgresql://localhost:5433/mindcare_db` |

Services running inside Docker should use `postgres` as the host and port
`5432` instead of the host values above.

## pgAdmin

- URL: `http://localhost:5050`
- Email: `admin@mindcare.com`
- Password: `admin`

These are development defaults only. Do not reuse them in staging or
production.

## Mailpit

- Inbox: `http://localhost:8025`
- SMTP: `localhost:1025`

Mailpit captures local verification emails and does not deliver them to the public internet.

## Demo data

Start the complete application and load the idempotent demo dataset with:

```powershell
.\run-all.ps1 -SeedDemo
```

Re-running the command updates the same demo records instead of duplicating them. Demo accounts
use the password `MindCare@123`. Useful logins include:

| Role | Email |
| --- | --- |
| Admin | `admin@mindcare.local` |
| User | `user1@mindcare.local` through `user6@mindcare.local` |
| Expert | `expert1@mindcare.local` through `expert8@mindcare.local` |
| Pending applicant | `pending.expert@mindcare.local` |
| Rejected applicant | `rejected.expert@mindcare.local` |

The dataset includes approved expert profiles, future schedules, representative booking and
payment states, reviews, notifications, bookmarks, emotion journals, health metrics, PHQ-9
results, and AI knowledge documents.
