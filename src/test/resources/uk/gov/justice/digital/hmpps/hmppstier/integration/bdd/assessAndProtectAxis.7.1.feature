#lang: en
Feature: Assess and Protect Axis Calculation

  Scenario: 152 points scored Protect Level A (cannot score 151)
    Given an offender scores 152 protect points
    When a tier is calculated
    Then a protect level of "A" is returned and 152 points are scored

  Scenario: 150 points scored Protect Level A
    Given an offender scores 150 protect points
    When a tier is calculated
    Then a protect level of "A" is returned and 150 points are scored

  Scenario: Maximum points scored without crossing Level A threshold
    Given an offender scores 51 protect points
    When a tier is calculated
    Then a protect level of "B" is returned and 51 points are scored

  Scenario: 21 points scored Protect Level B
    Given an offender scores 21 protect points
    When a tier is calculated
    Then a protect level of "B" is returned and 21 points are scored

  Scenario: 20 points scored Protect Level B
    Given an offender scores 20 protect points
    When a tier is calculated
    Then a protect level of "B" is returned and 20 points are scored

  Scenario: 19 points scored Protect Level C
    Given an offender scores 19 protect points
    When a tier is calculated
    Then a protect level of "C" is returned and 19 points are scored

  Scenario: 11 points scored Protect Level C
    Given an offender scores 11 protect points
    When a tier is calculated
    Then a protect level of "C" is returned and 11 points are scored

  Scenario: 10 points scored Protect Level C
    Given an offender scores 10 protect points
    When a tier is calculated
    Then a protect level of "C" is returned and 10 points are scored

  Scenario: 9 points scored Protect Level D
    Given an offender scores 9 protect points
    When a tier is calculated
    Then a protect level of "D" is returned and 9 points are scored

  Scenario: 0 points scored Protect Level D
    Given an offender scores 0 protect points
    When a tier is calculated
    Then a protect level of "D" is returned and 0 points are scored

