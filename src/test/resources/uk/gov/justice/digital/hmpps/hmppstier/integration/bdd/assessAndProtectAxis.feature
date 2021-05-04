#lang: en
@disabled
Feature: Assess and Protect Axis Calculation

  Scenario: 31 points scored Protect Level A
    Given an offender scores 31 protect points
    When a tier is calculated
    Then a protect level of "A" is returned

  Scenario: 30 points scored Protect Level A
    Given an offender scores 30 protect points
    When a tier is calculated
    Then a protect level of "A" is returned

  Scenario: 29 points scored Protect Level B
    Given an offender scores 29 protect points
    When a tier is calculated
    Then a protect level of "B" is returned

  Scenario: 21 points scored Protect Level B
    Given an offender scores 21 protect points
    When a tier is calculated
    Then a protect level of "B" is returned

  Scenario: 20 points scored Protect Level B
    Given an offender scores 20 protect points
    When a tier is calculated
    Then a protect level of "B" is returned

  Scenario: 19 points scored Protect Level C
    Given an offender scores 19 protect points
    When a tier is calculated
    Then a protect level of "C" is returned

  Scenario: 11 points scored Protect Level C
    Given an offender scores 11 protect points
    When a tier is calculated
    Then a protect level of "C" is returned

  Scenario: 10 points scored Protect Level C
    Given an offender scores 10 protect points
    When a tier is calculated
    Then a protect level of "C" is returned

  Scenario: 9 points scored Protect Level D
    Given an offender scores 9 protect points
    When a tier is calculated
    Then a protect level of "D" is returned

  Scenario: 0 points scored Protect Level D
    Given an offender scores 0 protect points
    When a tier is calculated
    Then a protect level of "D" is returned

