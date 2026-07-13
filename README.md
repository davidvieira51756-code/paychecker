# PayChecker

PayChecker is a full-stack fintech demo application for payment authorization, fraud/risk scoring, audit logging, and secure deployment on AWS.

The project includes a Java 21 / Spring Boot backend, a React / Vite frontend, PostgreSQL persistence, Docker local runtime, Terraform infrastructure, and GitHub Actions deployment workflows.

> PayChecker does not process real payments. It is a portfolio project focused on backend engineering, security, cloud deployment, and DevOps practices.

## Features

- User registration and login with JWT authentication
- Role-based access control with `CUSTOMER`, `ANALYST`, and `ADMIN`
- Account creation and account listing
- Payment authorization with validation rules
- Rule-based fraud/risk scoring
- Payment decisions: `APPROVED`, `DECLINED`, `MANUAL_REVIEW`
- Risk alert creation for suspicious payments
- Append-only financial and security event log
- Swagger / OpenAPI documentation
- Spring Boot Actuator health endpoint
- React frontend for authentication, accounts, payments, alerts, event logs, and admin users
- Unit and integration tests with PostgreSQL Testcontainers

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Boot Actuator
- Flyway
- Maven
- PostgreSQL
- JWT
- BCrypt
- Testcontainers

### Frontend

- React
- TypeScript
- Vite
- Lucide React

### Infrastructure and DevOps

- Docker
- Docker Compose
- Terraform
- AWS Elastic Container Registry
- AWS Elastic Container Service with Fargate
- AWS Application Load Balancer
- AWS Relational Database Service for PostgreSQL
- AWS Simple Storage Service
- AWS CloudFront
- AWS Secrets Manager
- AWS CloudWatch Logs
- GitHub Actions
- GitHub OIDC federation to AWS

## Architecture

```text
GitHub
  |
  | GitHub Actions
  | - build backend Docker image
  | - push image to ECR
  | - deploy backend to ECS
  | - build frontend
  | - upload frontend to S3
  | - invalidate CloudFront cache
  v

AWS
  |
  +-- CloudFront
  |     +-- S3 frontend origin
  |     +-- /api/* routed to Application Load Balancer
  |
  +-- Application Load Balancer
  |
  +-- ECS Fargate backend service
  |
  +-- RDS PostgreSQL
  |
  +-- Secrets Manager
  |
  +-- CloudWatch Logs
```

Locally, Docker Compose runs PostgreSQL and can also run the backend API. The frontend runs with Vite during development.

## Backend Domain Model

The backend is organized as a modular monolith:

```text
src/main/java/com/paychecker
  account/
  alert/
  auth/
  common/
  config/
  eventlog/
  payment/
  risk/
  user/
```

Main responsibilities:

- `account`: account data, balances, currency, limits, and status
- `payment`: payment authorization flow and persistence
- `risk`: rule-based risk scoring
- `alert`: risk alerts for manual review
- `eventlog`: append-only audit trail
- `auth`: registration, login, JWT, and user authentication
- `config`: Spring Security, CORS, and application configuration

## Payment Authorization Flow

```text
POST /api/payments/authorize
  |
  v
Load account
  |
  v
Run validation rules
  |
  |-- account active?
  |-- currency matches?
  |-- sufficient balance?
  |-- within daily limit?
  |
  v
If validation fails -> DECLINED
  |
  v
If validation passes -> calculate risk score
  |
  |-- high amount?
  |-- new beneficiary?
  |-- high payment velocity?
  |
  v
Risk score >= 60 -> MANUAL_REVIEW + risk alert
Risk score < 60  -> APPROVED
```

Important events are written to the append-only event log, including:

- `ACCOUNT_CREATED`
- `PAYMENT_REQUESTED`
- `PAYMENT_APPROVED`
- `PAYMENT_DECLINED`
- `PAYMENT_SENT_TO_REVIEW`
- `RISK_ALERT_CREATED`
- `RISK_ALERT_STATUS_UPDATED`
- `LOGIN_SUCCESS`
- `LOGIN_FAILED`

## API Overview

| Area | Endpoint | Access |
|---|---|---|
| Auth | `POST /api/auth/register` | Public |
| Auth | `POST /api/auth/login` | Public |
| Accounts | `/api/accounts/**` | Authenticated |
| Payments | `POST /api/payments/authorize` | Authenticated |
| Alerts | `/api/alerts/**` | `ANALYST`, `ADMIN` |
| Event Log | `/api/event-log/**` | `ADMIN` |
| Health | `/actuator/health` | Public |
| Swagger | `/swagger-ui/**`, `/v3/api-docs/**` | Public |

## Running Locally

### Prerequisites

- Java 21
- Node.js and npm
- Docker Desktop

### Start PostgreSQL and Backend with Docker Compose

```powershell
docker compose up -d --build
```

Backend API:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/actuator/health
```

### Run Only PostgreSQL in Docker

```powershell
docker compose up -d postgres
.\mvnw spring-boot:run
```

### Run the Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

### Stop Local Containers

```powershell
docker compose down
```

To also delete the local database volume:

```powershell
docker compose down -v
```

## Configuration

Backend configuration is split by environment:

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`

Useful environment variables:

- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `APP_JWT_EXPIRATION_MINUTES`
- `APP_CORS_ALLOWED_ORIGINS`

Frontend configuration:

- `frontend/.env.example`
- `VITE_API_BASE_URL`

In production, the frontend can use relative API calls because CloudFront routes `/api/*` to the backend load balancer.

## Database

The database schema is managed with Flyway migrations in:

```text
src/main/resources/db/migration
```

Hibernate uses:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Flyway owns schema changes and Hibernate only validates that the entities match the database.

## Testing

Run backend tests:

```powershell
.\mvnw test
```

The test suite includes:

- unit tests for validation and risk rules
- service tests for payment authorization and alerts
- integration tests with PostgreSQL Testcontainers
- authenticated API flow tests

Build the frontend:

```powershell
cd frontend
npm run build
```

## Docker

The backend API has a production Dockerfile:

```text
Dockerfile
```

Local services are defined in:

```text
docker-compose.yml
```

The compose setup is mainly for local development. AWS uses the Docker image pushed to Amazon Elastic Container Registry.

## Terraform

Terraform lives in:

```text
infra/
```

The infrastructure includes:

- VPC with public and private subnets
- security groups for the load balancer, API, and database
- Amazon Elastic Container Registry repository for the backend image
- Amazon Relational Database Service PostgreSQL instance
- AWS Secrets Manager secrets for database password and JWT secret
- Amazon Elastic Container Service cluster, task definition, and Fargate service
- Application Load Balancer and target group for the backend API
- S3 bucket for frontend static files
- CloudFront distribution for frontend delivery and `/api/*` routing
- CloudWatch log group for backend logs
- GitHub OIDC IAM role for deployments

Typical Terraform commands:

```powershell
cd infra
terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
```

Destroying the AWS infrastructure:

```powershell
terraform destroy
```

`terraform.tfvars` contains local/private values and must not be committed.

## GitHub Actions

The project has two deployment workflows:

```text
.github/workflows/deploy-backend.yml
.github/workflows/deploy-frontend.yml
```

### Backend Deployment

Triggered by changes to:

- `src/**`
- `pom.xml`
- `Dockerfile`
- `.dockerignore`
- backend workflow file

Main steps:

1. Authenticate to AWS using GitHub OIDC and `AWS_ROLE_ARN`
2. Login to Amazon Elastic Container Registry
3. Build the backend Docker image
4. Push image tags using the commit SHA and `latest`
5. Force a new Amazon Elastic Container Service deployment

### Frontend Deployment

Triggered by changes to:

- `frontend/**`
- frontend workflow file

Main steps:

1. Install frontend dependencies
2. Build the Vite frontend
3. Authenticate to AWS using GitHub OIDC and `AWS_ROLE_ARN`
4. Sync `frontend/dist` to S3
5. Invalidate the CloudFront cache

Required GitHub secret:

```text
AWS_ROLE_ARN
```

The role is created by Terraform in `infra/github_actions.tf`.

## AWS Deployment Outputs

Terraform exposes useful outputs such as:

- backend ECR repository URL
- backend load balancer DNS name
- backend health URL
- ECS cluster and service names
- RDS endpoint
- frontend S3 bucket name
- CloudFront distribution ID
- frontend URL
- GitHub Actions role ARN

These values are generated by Terraform and can change if the infrastructure is recreated.

## Security Notes

- JWT secret and database password are stored in AWS Secrets Manager in production.
- GitHub Actions uses OIDC instead of long-lived AWS access keys.
- The database runs in private subnets.
- The backend is reached through an Application Load Balancer.
- The frontend is served through CloudFront.
- CORS is configured through environment variables.
- API health checks are exposed through Spring Boot Actuator.

## Project Status

Completed:

- secured Spring Boot backend
- React/Vite frontend
- PostgreSQL persistence with Flyway
- local Docker runtime
- Dockerized backend API
- Terraform AWS infrastructure
- ECS Fargate backend deployment
- RDS PostgreSQL database
- S3 and CloudFront frontend hosting
- GitHub Actions deployment workflows
- GitHub OIDC authentication to AWS

Possible future improvements:

- custom domain and HTTPS certificate for the API
- rate limiting
- deeper CloudWatch alarms and dashboards
- automated security scans in CI
- richer admin/audit screens

## License

This project is licensed under the MIT License.
