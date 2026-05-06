# Credential Vault

A multi-tenant credential vault: admins store encrypted credentials and team members auto-fill them via a Chrome extension — without ever seeing the plaintext password.

Built as an open-source portfolio project. Stack: **Java 21 · Spring Boot 3 · MongoDB · AES-256-GCM · JWT · React + TypeScript · Chrome MV3**.

---

## Architecture

```
┌─────────────────┐   JWT    ┌──────────────────────┐   AES-256-GCM   ┌──────────────┐
│ Chrome Extension│ ──────▶  │  Spring Boot Backend  │ ──────────────▶ │   MongoDB    │
│  (content.js)   │ ◀──────  │  (REST API + auth)    │ ◀──────────────  │  (encrypted) │
└─────────────────┘ plaintext└──────────────────────┘                 └──────────────┘
                                         │
                               ┌─────────▼────────┐
                               │   Key Provider    │
                               │  (local MVP /     │
                               │  Azure Key Vault) │
                               └──────────────────┘
```

---

## Security Model

### The Critical Permission Check (`GET /api/credentials/{id}/use`)

Before any decryption occurs, the backend enforces **three independent checks**:

1. **Authentication** — valid, unexpired JWT (enforced by `JwtAuthFilter` before the controller is reached)
2. **Org isolation** — `credential.orgId` must equal the JWT's `orgId` claim
3. **Team membership** — the user must belong to a team that has been explicitly granted access via `CredentialAccess` (admins bypass this check)

**All three must pass.** If any fails, the response is always `403 Forbidden` — never `404`. Returning 404 for credentials that exist-but-are-inaccessible would leak information about what credential IDs exist.

### Encryption Design

- **Algorithm**: AES-256-GCM (authenticated encryption — provides both confidentiality and integrity)
- **IV**: 96-bit (12 bytes), generated fresh per encryption via `SecureRandom` — never reused
- **Auth tag**: 128-bit, appended to ciphertext by Java's `Cipher`. Tampered ciphertext throws `AEADBadTagException` on decrypt.
- **Key storage**: MVP uses an env-var-supplied key. The `KeyProvider` interface abstracts this for a drop-in Azure Key Vault swap.
- **What's stored in MongoDB**: `encryptedPassword` (base64 ciphertext+tag), `iv` (base64), `keyReference`. The plaintext never touches the DB.

### What Is Never Logged

- Plaintext passwords (`UseCredentialResponse` is never written to any log)
- The `encryptedPassword`, `iv`, or `keyReference` fields never appear in API responses (metadata-only `CredentialResponse`)
- Spring request/response body logging is disabled in `application.yml`

---

## Setup

### Prerequisites

- Java 21
- Maven 3.9+
- MongoDB (local or Atlas) — or Docker: `docker run -p 27017:27017 mongo:7.0`
- Node 20+ (frontend)
- Docker (for running integration tests via Testcontainers)

### Backend

```bash
cd backend

# Generate required secrets
export JWT_SECRET=$(openssl rand -base64 32)
export VAULT_MASTER_KEY=$(openssl rand -base64 32)
export MONGODB_URI=mongodb://localhost:27017/credentialvault

mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

### Run Tests

```bash
cd backend
mvn test
# Testcontainers spins up mongo:7.0 automatically — Docker must be running
```

The integration tests specifically cover:
- `teamB_member_cannot_use_credential_granted_only_to_teamA`
- `admin_can_use_any_credential_in_their_org`
- `cross_org_access_is_forbidden`
- `nonexistent_credential_returns_403_not_404`
- `successful_credential_use_writes_audit_entry`

### Frontend

```bash
cd frontend
npm install
VITE_API_URL=http://localhost:8080 npm run dev
```

### Chrome Extension

1. Open Chrome → `chrome://extensions`
2. Enable **Developer mode**
3. **Load unpacked** → select `extension/`
4. Sign in with your vault credentials in the popup

---

## API Reference

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | — | Create user + org |
| POST | `/api/auth/login` | — | Get JWT |
| POST | `/api/credentials` | ADMIN | Create credential (encrypts on write) |
| GET | `/api/credentials` | ADMIN | List metadata (no plaintext) |
| DELETE | `/api/credentials/{id}` | ADMIN | Delete + cascade access grants |
| POST | `/api/credentials/{id}/grant` | ADMIN | Grant team access |
| DELETE | `/api/credentials/{id}/grant/{teamId}` | ADMIN | Revoke team access |
| GET | `/api/credentials/{id}/use` | ADMIN/MEMBER | **Decrypt + return plaintext** (enforces full permission check, writes audit log) |
| POST | `/api/teams` | ADMIN | Create team |
| POST | `/api/teams/{id}/members` | ADMIN | Add member |
| DELETE | `/api/teams/{id}/members/{userId}` | ADMIN | Remove member |
| GET | `/api/audit` | ADMIN | Query audit log (filterable by credential, user, date range) |

---

## Known Limitations / Future Work

| Area | Current | Production path |
|------|---------|-----------------|
| Key management | Local env-var key | Swap `LocalKeyProvider` for `AzureKeyVaultKeyProvider` (implement `KeyProvider`) |
| Key rotation | None | Add re-encryption endpoint; `keyReference` field already supports multiple versions |
| Rate limiting | None | Add Spring rate-limit filter on `/api/credentials/{id}/use` |
| `X-Forwarded-For` | Trusted blindly | Only trust from known proxy IPs |
| Audit write failure | Logged, not alerted | Wire to PagerDuty / Slack on `AUDIT WRITE FAILED` log line |
| Extension HTTPS | `localhost` only in `host_permissions` | Update for production API domain |
| CORS | Not configured | Add `@CrossOrigin` or `CorsConfigurationSource` bean for frontend origin |

---

## Project Structure

```
credential-vault/
├── backend/
│   └── src/
│       ├── main/java/com/credentialvault/
│       │   ├── auth/          # JWT, filter, AuthController
│       │   ├── credential/    # CRUD, permission check, encryption wiring
│       │   ├── team/          # Team management
│       │   ├── audit/         # Immutable audit log
│       │   ├── crypto/        # AES-256-GCM, KeyProvider interface
│       │   ├── model/         # User, Organization
│       │   └── config/        # SecurityConfig, exception handler
│       └── test/
│           ├── unit/crypto/   # EncryptionServiceTest (7 cases)
│           └── integration/   # CredentialAccessIntegrationTest (10 cases)
├── frontend/
│   └── src/
│       ├── pages/             # Login, Dashboard
│       └── components/        # CredentialCard, CreateCredentialModal
└── extension/
    ├── manifest.json
    ├── background.js          # Service worker, holds JWT, makes API calls
    ├── content.js             # Form detection + fill (never holds JWT)
    └── popup/                 # Extension UI
```
