# Security

This document describes the security posture and features of the HMPPS Tier service.

## Authentication

### OAuth2 / JWT

All API requests require a valid JWT token from HMPPS Auth. The service is configured as an OAuth2 Resource Server using Spring Security.

**Configuration:**
- JWT tokens are validated against the HMPPS Auth JWK Set endpoint
- Environment-specific auth endpoints are configured via Helm values
- Service-to-service calls (to Delius and ARNS) use OAuth2 client credentials flow

**Environments:**

| Environment | Auth Endpoint |
|-------------|---------------|
| Production | `https://sign-in.hmpps.service.justice.gov.uk/auth` |
| Pre-Production | `https://sign-in-preprod.hmpps.service.justice.gov.uk/auth` |
| Development | `https://sign-in-dev.hmpps.service.justice.gov.uk/auth` |

## Authorisation

### Role-Based Access Control (RBAC)

All endpoints are protected with `@PreAuthorize` annotations requiring specific roles:

| Endpoint | Required Roles |
|----------|----------------|
| `/v2/crn/{crn}/tier/**` | `HMPPS_TIER` or `PROBATION_INTEGRATION_ADMIN` |
| `/v3/crn/{crn}/tier/**` | `HMPPS_TIER` or `PROBATION_INTEGRATION_ADMIN` |
| `/crn/{crn}/tier/**` (legacy) | `HMPPS_TIER` or `PROBATION_INTEGRATION_ADMIN` |
| `POST /calculations` | `ROLE_MANAGEMENT_TIER_UPDATE` or `PROBATION_INTEGRATION_ADMIN` |
| Queue admin endpoints | `ROLE_TIER_API_QUEUE_ADMIN` |

### Access Denied Handling

Requests without valid tokens receive HTTP 401. Requests with tokens lacking required roles receive HTTP 403 with a generic error message (no information leakage).

## Transport Security

### TLS/HTTPS

- All ingress traffic is terminated with TLS
- TLS certificate managed via Kubernetes secret (`hmpps-tier-cert`)
- All external API calls use HTTPS

### Network Security

**IP Allowlisting** is configured at the ingress level:

| Environment | Allowed Sources |
|-------------|-----------------|
| Production | Internal HMPPS networks, Delius production IPs |
| Pre-Production | Internal networks, Delius pre-prod IPs |
| Development | Internal networks, DXW VPN, Delius test IPs |

**Blocked Endpoints:**

The `/queue-admin/retry-all-dlqs` endpoint is explicitly blocked at ingress level to prevent accidental mass retries.

## Secrets Management

All secrets are stored in Kubernetes Secrets and injected as environment variables:

| Secret | Contents |
|--------|----------|
| `hmpps-tier` | OAuth client ID/secret, Application Insights key |
| `hmpps-tier-sentry` | Sentry DSN |
| `rds-instance-output` | Database credentials and connection details |
| `hmpps-domain-events-topic` | SNS topic ARN |
| `sqs-domain-events-secret` | SQS queue configuration |

No secrets are stored in code or configuration files.

## Input Validation

- Spring Validation (`spring-boot-starter-validation`) handles request validation
- Invalid requests return structured error responses with appropriate HTTP status codes
- Custom exception handler sanitises error messages to prevent information leakage

## Audit & Monitoring

### Telemetry Events

All tier calculations are logged to Application Insights with the following event types:

| Event | Description |
|-------|-------------|
| `TIER_CHANGED` | Tier calculation resulted in a change |
| `TIER_UNCHANGED` | Tier calculation with no change |
| `TIER_CALCULATION_FAILED` | Calculation failed |
| `TIER_CALCULATION_REMOVED` | Tier data deleted (e.g., GDPR) |
| `TIER_RECALCULATION_DRY_RUN` | Dry run calculation completed |

Each event includes dimensions: CRN, tier values, calculation version, and recalculation source.

### Error Tracking

- Sentry integration captures and reports application exceptions
- Message processing failures are explicitly captured to Sentry for investigation

### Health Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/health` | Application and dependency health (for monitoring systems) |
| `/ping` | Simple connectivity check |
| `/info` | Application version information |

## Dependency Security

### OWASP Dependency Check

The build includes OWASP Dependency Check to scan for known vulnerabilities in dependencies:

```bash
./gradlew dependencyCheckAnalyze
```

Known false positives are documented in `suppressions.xml`.

### HMPPS Security Baseline

The service uses the HMPPS Kotlin starter (`hmpps-kotlin-spring-boot-starter`) which provides:
- Secure defaults for Spring Security
- Standard authentication/authorisation patterns
- Consistent error handling

## Container Security

Kubernetes containers run with restricted security context:

```yaml
securityContext:
  capabilities:
    drop:
      - ALL
  runAsNonRoot: true
  allowPrivilegeEscalation: false
  seccompProfile:
    type: RuntimeDefault
```

## Data Sensitivity

The service handles personal data including:
- CRNs (Case Reference Numbers)
- Risk scores (RSR, OGRS, ROSH)
- Assessment data
- Registration information

Access is restricted to authorised HMPPS services only. All data is stored in encrypted PostgreSQL (RDS) with access controlled via IAM and Kubernetes secrets.

## Related Documentation

- [Technical Overview](technical-overview.md)
- [Integration Guide](integration-guide.md)
