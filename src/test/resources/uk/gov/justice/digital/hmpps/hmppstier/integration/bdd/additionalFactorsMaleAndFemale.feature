#lang: en
Feature: Additional Factors (Male and Female)

  Scenario: Child Concerns scores 2 points
    Given the following active registrations: "Child Concerns" "RCCO"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Child Protection scores 2 points
    Given the following active registrations: "Child Protection" "RCPR"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Risk to Children scores 2 points
    Given the following active registrations: "Risk to Children" "RCHD"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Public Interest Case scores 2 points
    Given the following active registrations: "Public Interest Case" "RPIR"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Terrorism Act Offender scores 2 points
    Given the following active registrations: "Terrorism Act Offender" "RTAO"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Safeguarding – Adult at Risk scores 2 points
    Given the following active registrations: "Safeguarding – Adult at Risk" "RVAD"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Street Gangs scores 2 points
    Given the following active registrations: "Street Gangs" "STRG"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Mental Health issues scores 2 points
    Given the following active registrations: "Mental Health issues" "RMDO"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Attempted Suicide and self-harm scores 2 points
    Given the following active registrations: "Attempted Suicide and self-harm" "ALSH"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Vulnerability scores 2 points
    Given the following active registrations: "Vulnerability" "RVLN"
    When a tier is calculated
    Then "2" points are scored
