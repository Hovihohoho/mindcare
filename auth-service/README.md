# MindCare Auth Service

New registrations require email verification before login. Verification tokens expire after 30 minutes and are stored only as SHA-256 hashes. A pending user cannot log in until the mailbox owner clicks the verification link.

For local development, start Mailpit from `infrastructure`:

```powershell
docker compose up -d mailpit
```

- SMTP: `localhost:1025`
- Mailpit inbox: `http://localhost:8025`

Production SMTP settings are supplied through `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, and `SMTP_STARTTLS`.

## Send verification to real Gmail inboxes

Use a dedicated sender account with Google 2-Step Verification enabled, then create an App Password. Do not use the normal Google account password.

In the PowerShell window used to start `auth-service`:

```powershell
$env:SPRING_PROFILES_ACTIVE="real-email"
$env:SMTP_USERNAME="your-sender@gmail.com"
$env:SMTP_PASSWORD="your-16-character-app-password"
$env:MAIL_FROM="your-sender@gmail.com"
$env:FRONTEND_URL="http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

The `real-email` profile uses `smtp.gmail.com:587`, SMTP authentication, required STARTTLS, network timeouts, and verifies the SMTP connection during startup. It has no default credentials, so the service refuses to start when credentials are missing or Gmail rejects them.

Registration and verification-mail delivery run in one database transaction. If Gmail rejects the send immediately, registration is rolled back. An SMTP server may still accept a message for a syntactically valid but nonexistent mailbox and bounce it later; that pending account cannot activate or log in because nobody can receive and click its verification link.
