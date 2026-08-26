# Tier Calculation (V3 Model)

> This document explains, in plain language, how the system calculates a person's tier.
> It is aimed at non-technical readers.
>
> This document is checked automatically for staleness: if a developer changes
> the underlying tier calculation logic in the code, an automated test will fail until
> someone reviews this document and confirms it's still accurate (or updates it).
> See `docs-change-tracking.yaml` at the root of the repository if you want to
> understand how that check works.

## What this is for

The tier determines how intensively probation services should supervise a person. Higher tiers (A, B) indicate someone who needs more frequent contact and oversight. Lower tiers (F, G) indicate someone who can be managed with less intensive supervision.

The system evaluates eight separate factors and assigns the person the **highest** tier produced by any of those factors.

## Glossary of key terms

- **Tier** — A letter from A (highest supervision need) to G (lowest). There are also special values: "Not Supervised" (the person has no active probation event) and "Missing" (required data is unavailable).

- **ARP (All Reoffending Predictor)** — A percentage score (0-100) estimating the likelihood of any reoffending within two years. Higher scores indicate higher risk.

- **CSRP (Combined Serious Reoffending Predictor)** — A percentage score estimating the likelihood of serious reoffending. Higher scores indicate higher risk.

- **DC-SRP (Direct Contact Sexual Reoffending Predictor)** — A percentage score specifically for predicting sexual reoffending involving direct contact with a victim.

- **MAPPA (Multi-Agency Public Protection Arrangements)** — A framework for managing high-risk offenders. Someone on MAPPA is under enhanced multi-agency supervision.

- **RoSH (Risk of Serious Harm)** — An assessment of the risk a person poses to others, categorised as Very High, High, Medium, or Low.

- **Lifer/IPP** — Someone serving a life sentence or an Imprisonment for Public Protection sentence.

- **Static predictor** — A risk score calculated only from historical/unchanging information (like offence history). Contrasts with "dynamic" predictors that also incorporate current circumstances.

- **Provisional tier** — A tier that may change once more assessment data becomes available. This happens when key risk predictors are based only on static information, or when certain data is missing.

## Prerequisites

Before any tier can be calculated, the person must have an **active probation event** (be under current supervision). If there is no active event, the result is "Not Supervised" and no further calculation occurs.

If the person has an active event but the required reoffending risk data (ARP and CSRP scores) is missing, the result is "Missing".

## The eight factors

The system evaluates eight factors. Each factor can produce a tier (A-G) or no result. The person's final tier is the **highest** tier from any factor — so if one factor produces tier C and another produces tier E, the final result is tier C.

### 1. General reoffending risk (ARP and CSRP)

This factor uses the All Reoffending Predictor (ARP) and Combined Serious Reoffending Predictor (CSRP) scores together. The combination of these two scores is looked up in a table to determine the tier.

**Lookup table:**

| CSRP score | ARP 90+ | ARP 75-89 | ARP 50-74 | ARP 25-49 | ARP 15-24 | ARP 0-14 |
|------------|---------|-----------|-----------|-----------|-----------|----------|
| 6.9+       | A       | A         | B         | B         | B         | B        |
| 3.0-6.89   | A       | B         | C         | C         | C         | C        |
| 1.0-2.99   | B       | C         | D         | E         | E         | E        |
| 0.5-0.99   | C       | D         | E         | E         | F         | F        |
| 0-0.49     | D       | D         | E         | F         | F         | G        |

For example:
- ARP of 80 and CSRP of 4.0 → Tier B
- ARP of 30 and CSRP of 0.3 → Tier E

### 2. Sexual reoffending risk (DC-SRP)

This factor uses the Direct Contact Sexual Reoffending Predictor score and its associated risk band.

| DC-SRP band | Tier |
|-------------|------|
| Very High   | A    |
| High        | B    |
| Medium      | C or D (see below) |
| Low         | E    |

For the **Medium** band, the specific score determines the tier:
- Score of 3.36 or higher → Tier C
- Score of 2.11 to 3.35 → Tier D
- Score of 1.12 to 2.10 → Tier C
- Score of 0.60 to 1.11 → Tier D

(These thresholds account for whether risk reduction measures are in place.)

### 3. MAPPA and Risk of Serious Harm (combined)

This factor combines whether someone is subject to MAPPA and their Risk of Serious Harm level.

**If the person is subject to MAPPA:**

| RoSH level | Tier |
|------------|------|
| Very High  | A    |
| High       | C    |
| Medium     | D    |
| Low or missing | E |

**If the person is NOT subject to MAPPA:**

| RoSH level | Tier |
|------------|------|
| Very High  | C    |
| High       | D    |
| Medium or below | No tier from this factor |

### 4. Lifer or IPP status with time since release

If a person is serving a life sentence or Imprisonment for Public Protection, their tier depends on how recently they were released:

| Time since release | Tier |
|--------------------|------|
| Within the last year | B |
| 1-5 years ago | D |
| More than 5 years ago | E |

If the person is not a lifer/IPP, or their release date is unknown, this factor produces no tier.

### 5. Domestic abuse registration

If the person has a domestic abuse registration, this factor produces **Tier E**.

Otherwise, this factor produces no tier.

### 6. Stalking registration

If the person has a stalking registration, this factor produces **Tier F**.

Otherwise, this factor produces no tier.

### 7. Child protection registration

If the person has a child protection registration, this factor produces **Tier F**.

Otherwise, this factor produces no tier.

### 8. Sexual offence history

If the person has ever committed a sexual offence (as recorded in their assessment), this factor produces **Tier E**.

Otherwise, this factor produces no tier.

## How the final tier is determined

After evaluating all eight factors, the system takes the **highest** tier produced. For example:

- General reoffending produces Tier D
- Domestic abuse produces Tier E
- MAPPA/RoSH produces Tier C
- All other factors produce no result

The final tier is **C** (the highest of D, E, and C).

## Provisional status

A tier may be marked as **provisional**, meaning it could change once more complete data is available. This happens in two situations:

### Static risk predictors

If the ARP or CSRP scores are "static" (based only on historical information, not a full dynamic assessment), the tier is provisional unless one of the other factors already guarantees a tier at least as high as the maximum possible from the ARP/CSRP table.

Specifically:
- If **both** ARP and CSRP are static: the tier is provisional unless another factor produces Tier A
- If only **ARP** is static: the tier is provisional unless another factor produces a tier at least as high as the maximum possible for the actual CSRP value
- If only **CSRP** is static: the tier is provisional unless another factor produces a tier at least as high as the maximum possible for the actual ARP value

### Missing Risk of Serious Harm

If the Risk of Serious Harm (RoSH) level is missing, the tier may be provisional:

- **If the person is subject to MAPPA but RoSH is missing**: provisional unless another factor produces Tier A
- **If the person is NOT subject to MAPPA and RoSH is missing**: provisional unless another factor produces Tier C or higher

This ensures that missing data doesn't inadvertently result in someone being under-supervised.
