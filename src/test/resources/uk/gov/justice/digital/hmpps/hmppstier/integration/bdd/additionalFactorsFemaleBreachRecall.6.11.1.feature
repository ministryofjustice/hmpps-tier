#lang: en
Feature: Additional Factors (Female only) Breach and Recall

  Scenario: Male offender with Previous Enforcement Activity scores no points
    Given an offender is "Male"
    And has an active conviction with a Previous Enforcement Activity
    When a tier is calculated
    Then 0 protect points are scored

  Scenario: Female offender with Previous Enforcement Activity and an active conviction scores 2 points
    Given an offender is "Female"
    And has an active conviction with a Previous Enforcement Activity
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Female offender with D2 and 9 protect points and Previous Enforcement Activity to C2
    Given an offender scores 9 protect points
    And an offender is "Female"
    And has an active conviction with a Previous Enforcement Activity
    And has a tier of "D2"
    When a tier is calculated
    Then a protect level of "C" is returned and 2 change points are scored

  Scenario: Female offender with Previous Enforcement Activity and two active convictions scores 2 points
    Given an offender is "Female"
    And has two active convictions with a Previous Enforcement Activity
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Female offender with Breached Convictions and a Previous Enforcement Activity are not added up
    Given an offender is "Female"
    And has two breached active convictions with a "true" Previous Enforcement Activity
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Female offender with Breached Convictions with no Previous Enforcement Activity scores no points
    Given an offender is "Female"
    And has two breached active convictions with a "false" Previous Enforcement Activity
    When a tier is calculated
    Then 0 protect points are scored

