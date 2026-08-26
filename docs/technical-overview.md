# Technical Overview

## Purpose

HMPPS Tier calculates risk-based tiering for probation cases. The tier determines the appropriate level of supervision and resource allocation for people on probation.

The service is event-driven: it listens for changes in probation case data, recalculates the tier, and publishes the result for downstream consumers.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           HMPPS Domain Events                               │
│  (probation-case.*, enforcement.*, risk-assessment.*, conviction.changed)   │
└─────────────────────────────────────────┬───────────────────────────────────┘
                                          │ SQS
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              hmpps-tier                                     │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐  │
│  │ Domain Event    │───▶│ Tier Calculation│───▶│ Domain Event Publisher  │  │
│  │ Listener        │    │ Service         │    │                         │  │
│  └─────────────────┘    └────────┬────────┘    └─────────────────────────┘  │
│                                  │                          │               │
│                                  ▼                          │               │
│                         ┌─────────────────┐                 │               │
│                         │   PostgreSQL    │                 │               │
│                         │ (calculations)  │                 │               │
│                         └─────────────────┘                 │               │
└─────────────────────────────────────────────────────────────┼───────────────┘
                                                              │ SNS
                                                              ▼
                                          ┌───────────────────────────────────┐
                                          │  tier.calculation.complete        │
                                          │  tier.calculation.changed         │
                                          └───────────────────────────────────┘
```

### Tech Stack

| Component     | Technology                            |
|---------------|---------------------------------------|
| Language      | Kotlin (JVM 25)                       |
| Framework     | Spring Boot                           |
| Database      | PostgreSQL                            |
| Messaging     | AWS SQS (consume) / AWS SNS (publish) |
| Auth          | HMPPS Auth (OAuth2)                   |
| Feature Flags | Flipt                                 |
| Deployment    | Kubernetes via Helm (Cloud Platform)  |

## External Dependencies

### APIs Consumed

| Service                           | Purpose                                                                  |
|-----------------------------------|--------------------------------------------------------------------------|
| **tier-to-delius-api**            | Probation case data: gender, registrations, convictions, RSR/OGRS scores |
| **Assess Risks and Needs (ARNS)** | Assessment data and risk predictors (OGRS4, RSR, DC-SRP)                 |
| **HMPPS Auth**                    | OAuth2 token authentication                                              |

### Messaging

| Resource                              | Direction | Purpose                      |
|---------------------------------------|-----------|------------------------------|
| `hmppsdomaineventsqueue` (SQS)        | Consume   | Triggers tier recalculations |
| `hmppscalculationcompletetopic` (SNS) | Publish   | Notifies downstream systems  |

## Data Gathered from External APIs

### From Tier-to-Delius API

| Field                         | Description                                                                   |
|-------------------------------|-------------------------------------------------------------------------------|
| `gender`                      | Person's gender (affects additional factors for women)                        |
| `registrations`               | Risk registrations (MAPPA, Lifer, Domestic Abuse, Stalking, Child Protection) |
| `convictions`                 | Convictions with sentence type and requirements                               |
| `rsrscore`                    | RSR (Risk of Serious Recidivism) score                                        |
| `ogrsscore`                   | OGRS (Offender Group Reconviction Scale) score                                |
| `previousEnforcementActivity` | Whether there's a breach/recall on an active conviction                       |
| `latestReleaseDate`           | Latest release date (for lifer/IPP first-year calculation)                    |
| `hasActiveEvent`              | Whether there's an active probation event                                     |

### From Assess Risks and Needs API (ARNS)

**Needs Assessment** (8 domains):

| Domain                           | Questions Assessed                                                                    |
|----------------------------------|---------------------------------------------------------------------------------------|
| Accommodation                    | No fixed abode, suitability, permanence, location                                     |
| Education/Training/Employability | Unemployment, employment history, work skills, attitude                               |
| Relationships                    | Close family, childhood experience, previous relationships, parental responsibilities |
| Lifestyle & Associates           | Activities encouraging offending, easily influenced, recklessness                     |
| Drug Misuse                      | Current use, injection history, motivation to tackle, major activity                  |
| Alcohol Misuse                   | Current use, binge drinking, frequency/level, motivation                              |
| Thinking & Behaviour             | Problem recognition, problem-solving, awareness of consequences, impulsivity, temper  |
| Attitudes                        | Pro-criminal attitudes, towards supervision, towards community, motivation to change  |

Each domain includes severity scoring and links to reoffending/harm.

**Risk Predictors** (OGRS4):

| Predictor | Description                                 |
|-----------|---------------------------------------------|
| ARP       | All Reoffending Predictor                   |
| VRP       | Violent Reoffending Predictor               |
| SVRP      | Serious Violent Reoffending Predictor       |
| DC-SRP    | Direct Contact Sexual Reoffending Predictor |
| IIC-SRP   | Indirect/Image Sexual Reoffending Predictor |
| CSRP      | Combined Serious Reoffending Predictor      |

## Tier Calculation Models

The service calculates two tier models for each case:

### V2 Model (Legacy)

Two-axis classification:

- **Protect Level (A-D)**: Based on RSR score, ROSH level, MAPPA status, and complexity factors
- **Change Level (0-3)**: Based on needs assessment, OGRS score, and IOM nominal status

Combined as `tierScore` (e.g., `A1`, `B2S` where `S` indicates unsupervised).

### V3 Model (Current)

Single-axis classification (A-G, or NA for no active event), based on:

- Non-sexual reoffending risk (CSRP vs ARP scores)
- Sexual reoffending risk (DC-SRP band/score)
- MAPPA level + Risk of Serious Harm combination
- Lifer/IPP status
- Domestic abuse, stalking, or child protection registrations

The active model is controlled by the `tier-v3-calculation` feature flag in Flipt.

## Data Model

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              tier_calculation                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  id              SERIAL PRIMARY KEY                                         │
│  uuid            UUID NOT NULL                                              │
│  crn             VARCHAR NOT NULL                                           │
│  created         TIMESTAMP NOT NULL                                         │
│  data            JSONB (TierCalculationResultEntity)                        │
│  change_reason   VARCHAR                                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  Indexes: (crn, created), (crn, uuid)                                       │
└─────────────────────────────────────────────────────────────────────────────┘
        │
        │ data (JSONB) contains:
        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TierCalculationResultEntity                         │
├─────────────────────────────────────────────────────────────────────────────┤
│  tier                Tier (A-G, NA)           ← V3 result                   │
│  protect             TierLevel<ProtectLevel>  ← V2 result (deprecated)      │
│  change              TierLevel<ChangeLevel>   ← V2 result (deprecated)      │
│  calculationVersion  String                                                 │
│  deliusInputs        DeliusInputs             ← Inputs from Delius          │
│  assessmentSummary   AssessmentForTier        ← Inputs from ARNS            │
│  riskPredictors      OGRS4Predictors          ← Predictors from ARNS        │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                               tier_summary                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  crn             VARCHAR PRIMARY KEY                                        │
│  uuid            UUID NOT NULL (references latest calculation)              │
│  tier            VARCHAR(2) (A-G, NA)                                       │
│  protect_level   VARCHAR (A-D)                                              │
│  change_level    SMALLINT (0-3)                                             │
│  unsupervised    BOOLEAN                                                    │
│  version         INTEGER (optimistic locking)                               │
│  last_modified   TIMESTAMP                                                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          ogrs4_rescored_assessment                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  id              SERIAL PRIMARY KEY                                         │
│  crn             CHAR(7) NOT NULL                                           │
│  completed_date  DATE NOT NULL                                              │
│  arp_score       NUMERIC(5,2)     arp_is_dynamic  BOOLEAN   arp_band  TEXT  │
│  csrp_score      NUMERIC(5,2)     csrp_is_dynamic BOOLEAN   csrp_band TEXT  │
│  dc_srp_score    NUMERIC(5,2)     dc_srp_band     TEXT                      │
│  iic_srp_score   NUMERIC(5,2)     iic_srp_band    TEXT                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  Index: (crn, completed_date)                                               │
│  Purpose: Stores rescored OGRS4 assessments for historical recalculation    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Domain Objects

**DeliusInputs** — Data from Tier-to-Delius API:
- `isFemale`, `rsrScore`, `ogrsScore`, `hasNoMandate`
- `registrations` (Registrations object)
- `previousEnforcementActivity`, `latestReleaseDate`, `hasActiveEvent`

**Registrations** — Parsed registration flags:
- `hasIomNominal`, `hasLiferIpp`, `hasDomesticAbuse`, `hasStalking`, `hasChildProtection`
- `complexityFactors`, `rosh`, `mappaLevel`, `mappaCategory`, `unsupervised`

**TierLevel<E>** — V2 tier result with breakdown:
- `tier` (ProtectLevel A-D or ChangeLevel 0-3)
- `points` (total points scored)
- `pointsBreakdown` (Map of CalculationRule → points)

## Security

- **Authentication**: All API requests require a valid HMPPS Auth JWT token
- **Roles**: Access controlled via Spring Security with HMPPS RBAC roles
- **Data Sensitivity**: Contains personal data (CRNs, risk scores); access restricted to authorised HMPPS services

## Monitoring

| Tool | Purpose |
|------|---------|
| Sentry | Error tracking and alerting |
| Application Insights | Performance monitoring and tracing |
| `/health` endpoint | Dependency health checks |
| `/info` endpoint | Deployment version information |

## Related Documentation

- [Integration Guide](integration-guide.md) — How to consume events and APIs
- [Security](security.md) — Security posture and features
- [Entity Relationship Diagram](diagrams/entity-relationship.puml) — PlantUML database schema
- [API Documentation](https://hmpps-tier-dev.hmpps.service.justice.gov.uk/swagger-ui.html) — OpenAPI/Swagger spec
