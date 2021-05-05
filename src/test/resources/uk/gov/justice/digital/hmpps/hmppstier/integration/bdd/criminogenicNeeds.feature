#lang: en
Feature: Criminogenic Needs

  Scenario: NO NEED for ACCOMMODATION scores 0 points
    Given the following assessment needs:
    |ACCOMMODATION|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for ACCOMMODATION scores 1 point
    Given the following assessment needs:
    |ACCOMMODATION|STANDARD|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: SEVERE need for ACCOMMODATION scores 2 points
    Given the following assessment needs:
    |ACCOMMODATION|SEVERE|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: NO NEED for EDUCATION_TRAINING_AND_EMPLOYABILITY scores 0 points
    Given the following assessment needs:
    |EDUCATION_TRAINING_AND_EMPLOYABILITY|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for EDUCATION_TRAINING_AND_EMPLOYABILITY scores 1 point
    Given the following assessment needs:
    |EDUCATION_TRAINING_AND_EMPLOYABILITY|STANDARD|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: SEVERE need for ACCOMMODATION scores 2 points
    Given the following assessment needs:
    |EDUCATION_TRAINING_AND_EMPLOYABILITY|SEVERE|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: NO NEED for RELATIONSHIPS scores 0 points
    Given the following assessment needs:
    |RELATIONSHIPS|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for RELATIONSHIPS scores 1 point
    Given the following assessment needs:
    |RELATIONSHIPS|STANDARD|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: SEVERE need for RELATIONSHIPS scores 2 points
    Given the following assessment needs:
    |RELATIONSHIPS|SEVERE|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: NO NEED for LIFESTYLE_AND_ASSOCIATES scores 0 points
    Given the following assessment needs:
    |LIFESTYLE_AND_ASSOCIATES|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for LIFESTYLE_AND_ASSOCIATES scores 1 point
    Given the following assessment needs:
    |LIFESTYLE_AND_ASSOCIATES|STANDARD|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: SEVERE need for LIFESTYLE_AND_ASSOCIATES scores 2 points
    Given the following assessment needs:
    |LIFESTYLE_AND_ASSOCIATES|SEVERE|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: NO NEED for DRUG_MISUSE scores 0 points
    Given the following assessment needs:
    |DRUG_MISUSE|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for DRUG_MISUSE scores 1 point
    Given the following assessment needs:
    |DRUG_MISUSE|STANDARD|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: SEVERE need for DRUG_MISUSE scores 2 points
    Given the following assessment needs:
    |DRUG_MISUSE|SEVERE|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: NO NEED for ALCOHOL_MISUSE scores 0 points
    Given the following assessment needs:
    |ALCOHOL_MISUSE|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for ALCOHOL_MISUSE scores 1 point
    Given the following assessment needs:
    |ALCOHOL_MISUSE|STANDARD|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: SEVERE need for ALCOHOL_MISUSE scores 2 points
    Given the following assessment needs:
    |ALCOHOL_MISUSE|SEVERE|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: NO NEED for THINKING_AND_BEHAVIOUR scores 0 points
    Given the following assessment needs:
    |THINKING_AND_BEHAVIOUR|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for THINKING_AND_BEHAVIOUR scores 2 points
    Given the following assessment needs:
    |THINKING_AND_BEHAVIOUR|STANDARD|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: SEVERE need for THINKING_AND_BEHAVIOUR scores 4 points
    Given the following assessment needs:
    |THINKING_AND_BEHAVIOUR|SEVERE|
    When a tier is calculated
    Then "4" change points are scored

  Scenario: NO NEED for ATTITUDES scores 0 points
    Given the following assessment needs:
    |ATTITUDES|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for ATTITUDES scores 2 points
    Given the following assessment needs:
    |ATTITUDES|STANDARD|
    When a tier is calculated
    Then "2" change points are scored

  Scenario: SEVERE need for ATTITUDES scores 4 points
    Given the following assessment needs:
    |ATTITUDES|SEVERE|
    When a tier is calculated
    Then "4" change points are scored

  Scenario: NO NEED for FINANCIAL_MANAGEMENT_AND_INCOME scores 0 points
    Given the following assessment needs:
    |FINANCIAL_MANAGEMENT_AND_INCOME|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for FINANCIAL_MANAGEMENT_AND_INCOME scores 0 points
    Given the following assessment needs:
    |FINANCIAL_MANAGEMENT_AND_INCOME|STANDARD|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: SEVERE need for FINANCIAL_MANAGEMENT_AND_INCOME scores 0 points
    Given the following assessment needs:
    |FINANCIAL_MANAGEMENT_AND_INCOME|SEVERE|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: NO NEED for EMOTIONAL_WELL_BEING scores 0 points
    Given the following assessment needs:
    |EMOTIONAL_WELL_BEING|NO_NEED|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: STANDARD need for EMOTIONAL_WELL_BEING scores 0 points
    Given the following assessment needs:
    |EMOTIONAL_WELL_BEING|STANDARD|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: SEVERE need for EMOTIONAL_WELL_BEING scores 0 points
    Given the following assessment needs:
    |EMOTIONAL_WELL_BEING|SEVERE|
    When a tier is calculated
    Then "0" change points are scored

  Scenario: Different needs of different severities combine
    Given the following assessment needs:
    |ACCOMMODATION|STANDARD|
    |ALCOHOL_MISUSE|NO_NEED|
    When a tier is calculated
    Then "1" change points are scored

  Scenario: Different needs of the same severity combine
    Given the following assessment needs:
    |ACCOMMODATION|STANDARD|
    |ALCOHOL_MISUSE|STANDARD|
    When a tier is calculated
    Then "2" change points are scored