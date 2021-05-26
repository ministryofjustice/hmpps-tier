#lang: en
Feature: Criminogenic Needs

  Background:
    Given a calculation version of at least 1

  Scenario: NO NEED for ACCOMMODATION scores 0 points
    Given the assessment need "ACCOMMODATION" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for ACCOMMODATION scores 1 point
    Given the assessment need "ACCOMMODATION" with severity "STANDARD"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: SEVERE need for ACCOMMODATION scores 2 points
    Given the assessment need "ACCOMMODATION" with severity "SEVERE"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: NO NEED for EDUCATION_TRAINING_AND_EMPLOYABILITY scores 0 points
    Given the assessment need "EDUCATION_TRAINING_AND_EMPLOYABILITY" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for EDUCATION_TRAINING_AND_EMPLOYABILITY scores 1 point
    Given the assessment need "EDUCATION_TRAINING_AND_EMPLOYABILITY" with severity "STANDARD"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: SEVERE need for ACCOMMODATION scores 2 points
    Given the assessment need "EDUCATION_TRAINING_AND_EMPLOYABILITY" with severity "SEVERE"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: NO NEED for RELATIONSHIPS scores 0 points
    Given the assessment need "RELATIONSHIPS" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for RELATIONSHIPS scores 1 point
    Given the assessment need "RELATIONSHIPS" with severity "STANDARD"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: SEVERE need for RELATIONSHIPS scores 2 points
    Given the assessment need "RELATIONSHIPS" with severity "SEVERE"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: NO NEED for LIFESTYLE_AND_ASSOCIATES scores 0 points
    Given the assessment need "LIFESTYLE_AND_ASSOCIATES" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for LIFESTYLE_AND_ASSOCIATES scores 1 point
    Given the assessment need "LIFESTYLE_AND_ASSOCIATES" with severity "STANDARD"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: SEVERE need for LIFESTYLE_AND_ASSOCIATES scores 2 points
    Given the assessment need "LIFESTYLE_AND_ASSOCIATES" with severity "SEVERE"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: NO NEED for DRUG_MISUSE scores 0 points
    Given the assessment need "DRUG_MISUSE" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for DRUG_MISUSE scores 1 point
    Given the assessment need "DRUG_MISUSE" with severity "STANDARD"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: SEVERE need for DRUG_MISUSE scores 2 points
    Given the assessment need "DRUG_MISUSE" with severity "SEVERE"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: NO NEED for ALCOHOL_MISUSE scores 0 points
    Given the assessment need "ALCOHOL_MISUSE" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for ALCOHOL_MISUSE scores 1 point
    Given the assessment need "ALCOHOL_MISUSE" with severity "STANDARD"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: SEVERE need for ALCOHOL_MISUSE scores 2 points
    Given the assessment need "ALCOHOL_MISUSE" with severity "SEVERE"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: NO NEED for THINKING_AND_BEHAVIOUR scores 0 points
    Given the assessment need "THINKING_AND_BEHAVIOUR" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for THINKING_AND_BEHAVIOUR scores 2 points
    Given the assessment need "THINKING_AND_BEHAVIOUR" with severity "STANDARD"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: SEVERE need for THINKING_AND_BEHAVIOUR scores 4 points
    Given the assessment need "THINKING_AND_BEHAVIOUR" with severity "SEVERE"
    When a tier is calculated
    Then 4 change points are scored

  Scenario: NO NEED for ATTITUDES scores 0 points
    Given the assessment need "ATTITUDES" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for ATTITUDES scores 2 points
    Given the assessment need "ATTITUDES" with severity "STANDARD"
    When a tier is calculated
    Then 2 change points are scored

  Scenario: SEVERE need for ATTITUDES scores 4 points
    Given the assessment need "ATTITUDES" with severity "SEVERE"
    When a tier is calculated
    Then 4 change points are scored

  Scenario: NO NEED for FINANCIAL_MANAGEMENT_AND_INCOME scores 0 points
    Given the assessment need "FINANCIAL_MANAGEMENT_AND_INCOME" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for FINANCIAL_MANAGEMENT_AND_INCOME scores 0 points
    Given the assessment need "FINANCIAL_MANAGEMENT_AND_INCOME" with severity "STANDARD"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: SEVERE need for FINANCIAL_MANAGEMENT_AND_INCOME scores 0 points
    Given the assessment need "FINANCIAL_MANAGEMENT_AND_INCOME" with severity "SEVERE"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: NO NEED for EMOTIONAL_WELL_BEING scores 0 points
    Given the assessment need "EMOTIONAL_WELL_BEING" with severity "NO_NEED"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: STANDARD need for EMOTIONAL_WELL_BEING scores 0 points
    Given the assessment need "EMOTIONAL_WELL_BEING" with severity "STANDARD"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: SEVERE need for EMOTIONAL_WELL_BEING scores 0 points
    Given the assessment need "EMOTIONAL_WELL_BEING" with severity "SEVERE"
    When a tier is calculated
    Then 0 change points are scored

  Scenario: Different needs of different severities combine
    Given the assessment need "ACCOMMODATION" with severity "STANDARD"
    And the assessment need "ALCOHOL_MISUSE" with severity "NO_NEED"
    When a tier is calculated
    Then 1 change points are scored

  Scenario: Different needs of the same severity combine
    Given the assessment need "ACCOMMODATION" with severity "STANDARD"
    And the assessment need "ALCOHOL_MISUSE" with severity "STANDARD"
    When a tier is calculated
    Then 2 change points are scored