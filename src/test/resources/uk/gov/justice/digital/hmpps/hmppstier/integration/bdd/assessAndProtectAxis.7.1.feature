#lang: en
Feature: Assess and Protect Axis Calculation

  Scenario: 31 points scored Protect Level A
    Given an offender scores 31 protect points
    When a tier is calculated
    Then a protect level of "A" is returned and 31 points are scored

  Scenario: 30 points scored Protect Level A
    Given an offender scores 30 protect points
    When a tier is calculated
    Then a protect level of "A" is returned and 30 points are scored

  Scenario: 29 points scored Protect Level B
    Given an offender scores 29 protect points
    When a tier is calculated
    Then a protect level of "B" is returned and 29 points are scored

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

