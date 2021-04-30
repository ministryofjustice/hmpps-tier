#lang: en
Feature: Change Level thresholds


  Scenario: Scoring over 20 points results in a change level of '3'
    Given an offender scores 21 points
    When a tier is calculated
    Then a Change level of '3' is returned

  Scenario: Scoring exactly 20 points results in a change level of '3'
    Given an offender scores 20 points
    When a tier is calculated
    Then a Change level of '3' is returned

  Scenario: Scoring less than 20 points does not result in a change level of '3'
    Given an offender scores 19 points
    When a tier is calculated
    Then a Change level of '2' is returned

  Scenario: Scoring over 10 points results in a change level of '2'
    Given an offender scores 11 points
    When a tier is calculated
    Then a Change level of '2' is returned

  Scenario: Scoring exactly 10 points results in a change level of '2'
    Given an offender scores 10 points
    When a tier is calculated
    Then a Change level of '2' is returned

  Scenario: Scoring less than 10 points results in a change level of '1'
    Given an offender scores 9 points
    When a tier is calculated
    Then a Change level of '1' is returned
