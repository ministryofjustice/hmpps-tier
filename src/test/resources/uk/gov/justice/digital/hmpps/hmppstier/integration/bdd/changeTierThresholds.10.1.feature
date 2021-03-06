#lang: en
Feature: Change Level thresholds


  Scenario: Scoring over 20 points results in a change level of '3'
    Given an offender scores 21 change points
    When a tier is calculated
    Then a change level of 3 is returned and 21 points are scored

  Scenario: Scoring exactly 20 points results in a change level of '3'
    Given an offender scores 20 change points
    When a tier is calculated
    Then a change level of 3 is returned and 20 points are scored

  Scenario: Scoring less than 20 points does not result in a change level of '3'
    Given an offender scores 19 change points
    When a tier is calculated
    Then a change level of 2 is returned and 19 points are scored

  Scenario: Scoring over 10 points results in a change level of '2'
    Given an offender scores 11 change points
    When a tier is calculated
    Then a change level of 2 is returned and 11 points are scored

  Scenario: Scoring exactly 10 points results in a change level of '2'
    Given an offender scores 10 change points
    When a tier is calculated
    Then a change level of 2 is returned and 10 points are scored

  Scenario: Scoring less than 10 points results in a change level of '1'
    Given an offender scores 9 change points
    When a tier is calculated
    Then a change level of 1 is returned and 9 points are scored
