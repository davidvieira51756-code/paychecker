# PayChecker

**Payment Authorization, Fraud Risk Scoring & Audit Engine**

PayChecker is a backend fintech application built with **Java 21** and **Spring Boot 3.5.x**.

It simulates a payment authorization engine where payment requests are validated through a rule-based pipeline, assigned a fraud/risk score, and then classified as:

- `APPROVED`
- `DECLINED`
- `MANUAL_REVIEW`

The system also creates risk alerts for analyst review and records critical financial actions in an append-only event log for auditability.

> This project does not process real payments. It is a backend engineering portfolio project focused on fintech domain logic, clean architecture, auditability, testing, and future cloud/security evolution.

---

## Table of Contents

- [Project Goal](#project-goal)
- [Core Features](#core-features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Main Payment Authorization Flow](#main-payment-authorization-flow)
- [API Endpoints](#api-endpoints)
- [Example Responses](#example-responses)
- [Running Locally](#running-locally)
- [Swagger / OpenAPI](#swagger--openapi)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Error Handling](#error-handling)
- [Design Decisions](#design-decisions)
- [Roadmap](#roadmap)
- [Future Cloud Security / DevSecOps Plan](#future-cloud-security--devsecops-plan)
- [Project Status](#project-status)

---

## Project Goal

The goal of PayChecker is to simulate part of a real fintech/banking backend.

When a payment request is received, the system:

1. Loads the source account
2. Runs hard validation rules
3. Calculates a fraud/risk score
4. Makes a payment decision
5. Stores the payment result
6. Creates a risk alert if manual review is required
7. Records critical actions in an append-only financial event log

This project was designed to demonstrate real backend engineering skills such as:

- Clean domain separation
- DTO-based API contracts
- Service-layer business logic
- JPA persistence
- Flyway-controlled database schema
- Rule-based validation
- Fraud/risk scoring
- Event logging and auditability
- Unit testing
- Integration testing with PostgreSQL using Testcontainers

---

## Core Features

### Accounts

The account module supports:

- Creating accounts
- Listing accounts with pagination
- Getting accounts by ID
- Storing account balance, currency, limits, and status

Account statuses:

```text
ACTIVE
BLOCKED
CLOSED
```

---

### Payment Authorization

The payment module exposes the main endpoint:

```http
POST /api/payments/authorize
```

A payment request is evaluated and classified as:

```text
APPROVED
DECLINED
MANUAL_REVIEW
```

The system currently checks:

- Account existence
- Account status
- Currency match
- Sufficient balance
- Daily payment limit
- Risk score based on fraud rules

---

### Validation Pipeline

Payment validation is implemented using independent validation rule classes.

Current validation rules:

```text
AccountIsActiveRule
CurrencyMatchesRule
SufficientBalanceRule
PaymentWithinDailyLimitRule
```

These are hard validation rules. If one or more fail, the payment is immediately declined.

Example:

```text
Blocked account      -> DECLINED
Currency mismatch    -> DECLINED
Insufficient balance -> DECLINED
Daily limit exceeded -> DECLINED
```

---

### Rule-Based Risk Scoring

If the payment passes the validation pipeline, the risk scoring engine evaluates fraud indicators.

Current risk rules:

```text
HighAmountRiskRule
NewBeneficiaryRiskRule
VelocityRiskRule
```

Example scoring:

```text
VERY_HIGH_AMOUNT       +50
HIGH_AMOUNT            +30
NEW_BENEFICIARY        +25
HIGH_PAYMENT_VELOCITY  +40
```

Decision logic:

```text
Validation failure       -> DECLINED
Risk score >= 60         -> MANUAL_REVIEW
Risk score < 60          -> APPROVED
```

---

### Risk Alerts

When a payment is sent to manual review, the system automatically creates a risk alert.

Alert statuses:

```text
OPEN
IN_REVIEW
FALSE_POSITIVE
CONFIRMED_FRAUD
CLOSED
```

Alert severities:

```text
LOW
MEDIUM
HIGH
CRITICAL
```

Example use case:

```text
Payment amount: 6000 EUR
Beneficiary: new beneficiary
Risk score: 75
Decision: MANUAL_REVIEW
Alert severity: HIGH
Alert status: OPEN
```

---

### Append-Only Financial Event Log

PayChecker records important financial events in an append-only event log.

Examples:

```text
ACCOUNT_CREATED
PAYMENT_REQUESTED
PAYMENT_APPROVED
PAYMENT_DECLINED
PAYMENT_SENT_TO_REVIEW
RISK_ALERT_CREATED
RISK_ALERT_STATUS_UPDATED
```

The event log provides traceability for sensitive actions and supports a basic audit trail.

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Bean Validation
- Lombok
- Maven

### Database

- PostgreSQL 16
- Flyway migrations
- Docker Compose for local development

### API Documentation

- Swagger / OpenAPI
- Springdoc OpenAPI

### Testing

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- Testcontainers
- PostgreSQL Testcontainer

---

## Architecture

PayChecker follows a **modular monolith** architecture.

The project is organized by domain, not by technical layer only.

```text
com.paychecker
│
├── account
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   └── service
│
├── payment
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   ├── service
│   └── validation
│
├── risk
│   ├── engine
│   └── rules
│
├── alert
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   └── service
│
├── eventlog
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   └── service
│
├── common
│   ├── dto
│   └── exception
│
└── config
```

### Layer Responsibilities

| Layer | Responsibility |
|---|---|
| Controller | Receives HTTP requests and returns API responses |
| DTO | Defines request and response contracts |
| Service | Contains business logic and orchestration |
| Domain | Represents business entities and enums |
| Repository | Communicates with the database through Spring Data JPA |
| Flyway Migration | Controls database schema changes |

---

## Main Payment Authorization Flow

```text
Create account
   |
   v
POST /api/accounts
   |
   v
Account stored in PostgreSQL
   |
   v
Event: ACCOUNT_CREATED


Authorize payment
   |
   v
POST /api/payments/authorize
   |
   v
Load account
   |
   v
Run validation pipeline
   |
   |-- Is account active?
   |-- Does currency match?
   |-- Is balance sufficient?
   |-- Is payment within daily limit?
   |
   v
If validation fails
   |
   v
DECLINED
   |
   v
Store payment
   |
   v
Events:
PAYMENT_REQUESTED
PAYMENT_DECLINED


If validation passes
   |
   v
Run risk scoring engine
   |
   |-- High amount?
   |-- New beneficiary?
   |-- High payment velocity?
   |
   v
Calculate risk score
   |
   v
If risk score < 60
   |
   v
APPROVED
   |
   v
Events:
PAYMENT_REQUESTED
PAYMENT_APPROVED


If risk score >= 60
   |
   v
MANUAL_REVIEW
   |
   v
Store payment
   |
   v
Events:
PAYMENT_REQUESTED
PAYMENT_SENT_TO_REVIEW
   |
   v
Create RiskAlert with status OPEN
   |
   v
Event:
RISK_ALERT_CREATED
```

---

## API Endpoints

### Accounts

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/accounts` | Create a new account |
| GET | `/api/accounts` | List accounts with pagination |
| GET | `/api/accounts/{id}` | Get account by ID |

---

### Payments

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments/authorize` | Authorize a payment request |

---

### Alerts

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/alerts` | List alerts with pagination |
| GET | `/api/alerts/open` | List open alerts |
| GET | `/api/alerts/{id}` | Get alert by ID |
| PATCH | `/api/alerts/{id}/status` | Update alert status |

---

### Event Log

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/event-log` | List financial events with pagination |
| GET | `/api/event-log/{entityType}/{entityId}` | List events for a specific entity |

---

## Example Requests

### Create Account

```http
POST /api/accounts
Content-Type: application/json
```

```json
{
  "ownerName": "David Vieira",
  "iban": "PT50005500000000000000055",
  "currency": "EUR",
  "initialBalance": 10000.00,
  "dailyLimit": 10000.00,
  "monthlyLimit": 50000.00
}
```

---

### Authorize Payment

```http
POST /api/payments/authorize
Content-Type: application/json
```

```json
{
  "accountId": 1,
  "amount": 6000.00,
  "currency": "EUR",
  "beneficiaryIban": "PT50008800000000000000088",
  "beneficiaryName": "New Large Beneficiary",
  "beneficiaryCountry": "PT"
}
```

---

### Update Alert Status

```http
PATCH /api/alerts/1/status
Content-Type: application/json
```

```json
{
  "status": "IN_REVIEW"
}
```

---

## Example Responses

### Approved Payment

```json
{
  "paymentId": 1,
  "decision": "APPROVED",
  "riskScore": 25,
  "reasons": [
    "NEW_BENEFICIARY"
  ],
  "createdAt": "2026-05-03T12:00:00Z"
}
```

---

### Declined Payment

```json
{
  "paymentId": 2,
  "decision": "DECLINED",
  "riskScore": 0,
  "reasons": [
    "INSUFFICIENT_BALANCE",
    "DAILY_LIMIT_EXCEEDED"
  ],
  "createdAt": "2026-05-03T12:00:00Z"
}
```

---

### Manual Review

```json
{
  "paymentId": 3,
  "decision": "MANUAL_REVIEW",
  "riskScore": 75,
  "reasons": [
    "VERY_HIGH_AMOUNT",
    "NEW_BENEFICIARY"
  ],
  "createdAt": "2026-05-03T12:00:00Z"
}
```

---

### Risk Alert

```json
{
  "id": 1,
  "paymentId": 3,
  "accountId": 1,
  "riskScore": 75,
  "severity": "HIGH",
  "status": "OPEN",
  "reasonSummary": "VERY_HIGH_AMOUNT, NEW_BENEFICIARY",
  "createdAt": "2026-05-03T12:00:00Z",
  "updatedAt": "2026-05-03T12:00:00Z"
}
```

---

### Financial Event

```json
{
  "id": 1,
  "eventType": "PAYMENT_SENT_TO_REVIEW",
  "entityType": "PAYMENT",
  "entityId": 3,
  "payloadJson": "{\"status\":\"MANUAL_REVIEW\",\"riskScore\":75}",
  "createdBy": "SYSTEM",
  "createdAt": "2026-05-03T12:00:00Z"
}
```

---

## Running Locally

### Prerequisites

- Java 21
- Docker Desktop
- Maven Wrapper included in the project

---

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts a local PostgreSQL database using Docker Compose.

---

### 2. Run the Application

On Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

Or run the `PaycheckerApplication` class directly from IntelliJ.

---

### 3. Stop PostgreSQL

```bash
docker compose down
```

To remove the database volume as well:

```bash
docker compose down -v
```

Use `-v` only when you intentionally want to reset local database data.

---

## Swagger / OpenAPI

After starting the application, open:

```text
http://localhost:8080/swagger-ui.html
```

or:

```text
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

Swagger can be used to test:

- Account creation
- Payment authorization
- Alert review
- Event log queries

---

## Pagination

List endpoints support pagination.

Example:

```http
GET /api/accounts?page=0&size=5&sort=createdAt,desc
```

Response format:

```json
{
  "content": [],
  "page": 0,
  "size": 5,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

Pagination is currently supported for:

```text
GET /api/accounts
GET /api/alerts
GET /api/alerts/open
GET /api/event-log
GET /api/event-log/{entityType}/{entityId}
```

---

## Database Migrations

The project uses Flyway to manage database schema changes.

Current migrations:

```text
V1__init.sql
V2__create_accounts_table.sql
V3__create_payments_table.sql
V4__create_risk_alerts_table.sql
V5__create_financial_events_table.sql
V6__add_risk_alert_status_updated_event.sql
```

Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Hibernate validates the schema but does not create or update tables automatically.

Database structure is controlled through Flyway migrations.

---

## Testing

Run all tests:

```powershell
.\mvnw test
```

The project includes both unit tests and integration tests.

---

### Unit Tests

Current unit test coverage includes:

- Payment validation rules
- Risk scoring rules
- Payment authorization service
- Risk alert service

These tests validate business logic in isolation without starting the full application.

---

### Integration Tests

Integration tests use Testcontainers to start a temporary PostgreSQL database and test real application flows.

Current integration test coverage includes:

- Account creation
- Duplicate IBAN handling
- Request validation errors
- Payment authorization flow
- Risk alert creation
- Financial event log creation

Integration tests verify the full flow across:

```text
HTTP request
Controller
DTO validation
Service
Repository
PostgreSQL
Flyway migrations
HTTP response
```

---

## Error Handling

The project includes a global exception handler that returns consistent API error responses.

### Validation Error Example

```json
{
  "timestamp": "2026-05-03T13:21:41.629700500Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/accounts",
  "validationErrors": {
    "ownerName": "Owner name is required",
    "iban": "IBAN is required",
    "currency": "Currency must be a valid 3-letter ISO code"
  }
}
```

---

### Not Found Example

```json
{
  "timestamp": "2026-05-03T13:22:31.705041800Z",
  "status": 404,
  "error": "Not Found",
  "message": "Account not found",
  "path": "/api/accounts/9999",
  "validationErrors": null
}
```

---

## Design Decisions

### Why DTOs?

DTOs are used to separate the public API contract from internal persistence entities.

```text
Request DTO   -> data received by the API
Entity        -> internal database/domain model
Response DTO  -> data returned by the API
```

This avoids exposing JPA entities directly through the API and gives better control over input validation and output formatting.

---

### Why Records for DTOs?

Java records are used for DTOs because they are concise, immutable-style data carriers.

They are well suited for request and response objects that mainly transport data.

Example:

```java
public record AccountResponse(
    Long id,
    String ownerName,
    String iban,
    String currency
) {
}
```

Entities are not implemented as records because JPA entities need constructors, mutable fields, lifecycle callbacks, and ORM support.

---

### Why Flyway?

Flyway gives explicit control over schema changes.

Instead of allowing Hibernate to create or update tables automatically, schema changes are versioned through SQL migration files.

This is closer to real-world backend development and makes database evolution predictable.

---

### Why `ddl-auto=validate`?

The project uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means:

- Flyway creates and changes tables
- Hibernate only validates that Java entities match the database schema

This prevents accidental schema changes caused by application startup.

---

### Why a Validation Pipeline?

Payment validation rules are implemented as independent classes.

This keeps the payment authorization logic extensible and easier to test.

Instead of a large service full of `if` statements, each rule has one responsibility.

Example:

```text
AccountIsActiveRule
SufficientBalanceRule
CurrencyMatchesRule
PaymentWithinDailyLimitRule
```

This follows the open/closed principle:

```text
Open for extension
Closed for modification
```

---

### Why Rule-Based Risk Scoring?

The current risk scoring system is intentionally rule-based instead of machine-learning-based.

This makes the decision process transparent, explainable, and easier to test.

Example:

```text
VERY_HIGH_AMOUNT +50
NEW_BENEFICIARY  +25
Final risk score = 75
Decision = MANUAL_REVIEW
```

Explainability is especially important in financial systems.

---

### Why an Event Log?

Financial systems require traceability.

The append-only event log records important business events such as:

```text
ACCOUNT_CREATED
PAYMENT_REQUESTED
PAYMENT_APPROVED
PAYMENT_DECLINED
PAYMENT_SENT_TO_REVIEW
RISK_ALERT_CREATED
RISK_ALERT_STATUS_UPDATED
```

This supports auditability and future security monitoring.

---

### Why Modular Monolith Instead of Microservices?

PayChecker is intentionally built as a modular monolith.

At this stage, a monolith is simpler to develop, test, and understand.

The code is still separated by domain modules, which keeps the architecture clean without introducing the operational complexity of microservices.

Microservices are intentionally avoided at this stage.

---

## Current Project Structure

```text
paychecker/
│
├── src/
│   ├── main/
│   │   ├── java/com/paychecker/
│   │   │   ├── account/
│   │   │   ├── alert/
│   │   │   ├── common/
│   │   │   ├── config/
│   │   │   ├── eventlog/
│   │   │   ├── payment/
│   │   │   └── risk/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │
│   └── test/
│       └── java/com/paychecker/
│           ├── alert/
│           ├── integration/
│           ├── payment/
│           └── risk/
│
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## Roadmap

### Backend Core

- [x] Spring Boot project setup
- [x] PostgreSQL with Docker Compose
- [x] Flyway migrations
- [x] Account module
- [x] Payment authorization endpoint
- [x] Validation pipeline
- [x] Rule-based risk scoring
- [x] Risk alerts
- [x] Append-only financial event log
- [x] Swagger / OpenAPI
- [x] Global exception handling
- [x] Pagination
- [x] Unit tests
- [x] Integration tests with Testcontainers

---

### Application Security

- [ ] Spring Security
- [ ] User registration and login
- [ ] Password hashing
- [ ] JWT authentication
- [ ] Roles:
    - `CUSTOMER`
    - `ANALYST`
    - `ADMIN`
- [ ] Endpoint authorization
- [ ] Security event logs
- [ ] Rate limiting

---

### DevSecOps / Cloud Security

- [ ] Dockerfile for the API
- [ ] GitHub Actions CI pipeline
- [ ] Secret scanning with Gitleaks
- [ ] SAST with Semgrep
- [ ] Dependency and container scanning with Trivy
- [ ] Azure Container Registry
- [ ] Azure Container Apps
- [ ] Azure Database for PostgreSQL
- [ ] Azure Key Vault
- [ ] Managed Identity
- [ ] Log Analytics
- [ ] Application Insights
- [ ] Azure Monitor security alerts

---

### Future Frontend

- [ ] Login page
- [ ] Accounts dashboard
- [ ] Payment authorization form
- [ ] Decision result view
- [ ] Risk alert dashboard
- [ ] Event log viewer

---

## Future Cloud Security / DevSecOps Plan

After the local backend MVP is complete, PayChecker is planned to evolve into a cloud security and DevSecOps lab on Azure.

Target architecture:

```text
GitHub
  |
  | GitHub Actions
  | - tests
  | - secret scanning
  | - SAST
  | - dependency scanning
  | - Docker image scanning
  | - deploy with OIDC
  v

Azure
  |
  +-- Azure Container Registry
  +-- Azure Container Apps
  +-- Azure Database for PostgreSQL
  +-- Azure Key Vault
  +-- Managed Identity
  +-- Log Analytics Workspace
  +-- Application Insights
  +-- Azure Monitor Alerts
```

Planned security practices:

```text
No long-lived Azure credentials stored in GitHub
Secrets stored in Azure Key Vault
Managed Identity for cloud resource access
Least-privilege RBAC
Automated security scanning in CI/CD
Centralized logs
Alert rules for suspicious activity
```

Example detections:

```text
More than 10 failed login attempts in 5 minutes
Multiple unauthorized access attempts
Suspicious access to admin endpoints
Many payments sent to manual review from the same account
Repeated high-value payment attempts
```

---

## Future Security Documentation

Planned security documentation:

```text
security/
├── threat-model.md
├── risk-register.md
├── hardening-checklist.md
├── detection-rules.md
└── incident-response-playbook.md
```

These documents will describe:

- Threats
- Risks
- Mitigations
- Detection rules
- Incident response procedures
- Cloud hardening steps

---

## Project Status

PayChecker currently implements the backend core for:

```text
Accounts
Payment authorization
Validation pipeline
Risk scoring
Risk alerts
Financial event logging
Swagger documentation
Unit testing
Integration testing
```

The next major phase is application security:

```text
Spring Security
JWT authentication
User roles
Security event logs
Rate limiting
```

After that, the project will evolve toward:

```text
Frontend demo
Dockerized API
GitHub Actions
DevSecOps pipeline
Azure deployment
Cloud monitoring and alerts
```

---

## License

This project is licensed under the MIT License.