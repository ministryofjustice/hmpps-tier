# Integration Guide

This guide explains how to integrate with the HMPPS Tier service, either by subscribing to domain events or calling the REST API.

## Quick Start

**To receive tier updates:** Subscribe to the `tier.calculation.complete` or `tier.calculation.changed` events from the HMPPS Domain Events SNS topic.

**To query tier on demand:** Call the REST API at `/v3/crn/{crn}/tier` with a valid HMPPS Auth token.

## Events Published

The service publishes events to the `hmppscalculationcompletetopic` SNS topic.

### `tier.calculation.complete`

Published after every tier calculation, regardless of whether the tier changed.

```json
{
  "eventType": "tier.calculation.complete",
  "version": 1,
  "occurredAt": "2024-01-15T10:30:00Z",
  "detailUrl": "https://hmpps-tier.hmpps.service.justice.gov.uk/v3/crn/X123456/tier/abc-123",
  "personReference": {
    "identifiers": [
      { "type": "CRN", "value": "X123456" }
    ]
  },
  "additionalInformation": {
    "calculationId": "abc-123"
  }
}
```

### `tier.calculation.changed`

Published only when the tier value changes from the previous calculation.

```json
{
  "eventType": "tier.calculation.changed",
  "version": 1,
  "occurredAt": "2024-01-15T10:30:00Z",
  "detailUrl": "https://hmpps-tier.hmpps.service.justice.gov.uk/v3/crn/X123456/tier/abc-123",
  "personReference": {
    "identifiers": [
      { "type": "CRN", "value": "X123456" }
    ]
  },
  "additionalInformation": {
    "calculationId": "abc-123"
  }
}
```

## Events Consumed

The service recalculates tiers in response to these domain events:

| Event Type | Trigger |
|------------|---------|
| `conviction.changed` | Supervision status changed |
| `risk-assessment.scores.determined` | OASys assessment produced |
| `probation-case.engagement.created` | New case created |
| `probation-case.registration.added` | Registration added |
| `probation-case.registration.deleted` | Registration removed |
| `probation-case.registration.deregistered` | Registration deregistered |
| `probation-case.registration.updated` | Registration updated |
| `probation-case.requirement.created` | Requirement added |
| `probation-case.requirement.deleted` | Requirement removed |
| `probation-case.requirement.terminated` | Requirement terminated |
| `probation-case.requirement.unterminated` | Requirement reactivated |
| `probation-case.sentence.created` | Sentence added |
| `probation-case.sentence.amended` | Sentence amended |
| `probation-case.sentence.terminated` | Sentence terminated |
| `probation-case.sentence.unterminated` | Sentence reactivated |
| `probation-case.sentence.deleted` | Sentence removed |
| `probation-case.sentence.moved` | Sentence moved |
| `probation-case.merge.completed` | Cases merged |
| `probation-case.unmerge.completed` | Cases unmerged |
| `probation-case.deleted.gdpr` | GDPR deletion (deletes tier data) |
| `enforcement.breach.raised` | Breach raised |
| `enforcement.breach.concluded` | Breach concluded |
| `enforcement.recall.raised` | Recall started |
| `enforcement.recall.concluded` | Recall concluded |

## REST API

All endpoints require a valid HMPPS Auth JWT token in the `Authorization` header.

### Authentication

Request a client credentials token from HMPPS Auth:

```bash
curl -X POST "https://sign-in.hmpps.service.justice.gov.uk/auth/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -u "CLIENT_ID:CLIENT_SECRET"
```

### Get Current Tier (V3)

```
GET /v3/crn/{crn}/tier
```

**Response:**

```json
{
  "tierScore": "C",
  "calculationId": "abc-123",
  "calculationDate": "2024-01-15T10:30:00Z"
}
```

### Get Tier with Calculation Details

```
GET /v3/crn/{crn}/tier/details
```

Returns the tier along with the input data used in the calculation.

### Get Tier History

```
GET /v3/crn/{crn}/tier/history
```

Returns a list of all tier calculations for the CRN.

### V2 Endpoints

V2 endpoints return the legacy two-axis model (`protect` + `change`):

- `GET /v2/crn/{crn}/tier`
- `GET /v2/crn/{crn}/tier/details`
- `GET /v2/crn/{crn}/tier/history`

V2 endpoints can return both V2 and V3 calculations, as V3 calculations include backward-compatible `protect` and `change` values.

### Trigger Calculation (Admin)

```
POST /calculations
Content-Type: application/json

["X123456", "X789012"]
```

Query parameters:
- `dryRun=true` (default): Calculates without persisting
- `dryRun=false`: Calculates and persists results

## Error Handling

| HTTP Status | Meaning |
|-------------|---------|
| 401 | Invalid or missing auth token |
| 403 | Insufficient permissions |
| 404 | CRN not found or no tier calculation exists |
| 500 | Internal error (logged to Sentry) |

When a tier calculation fails due to missing upstream data (e.g., Delius unavailable), the event is retried via SQS dead-letter queue handling.

## OpenAPI Specification

Full API documentation is available at:

- **Dev**: https://hmpps-tier-dev.hmpps.service.justice.gov.uk/swagger-ui.html
- **Preprod**: https://hmpps-tier-preprod.hmpps.service.justice.gov.uk/swagger-ui.html
- **Prod**: https://hmpps-tier.hmpps.service.justice.gov.uk/swagger-ui.html
