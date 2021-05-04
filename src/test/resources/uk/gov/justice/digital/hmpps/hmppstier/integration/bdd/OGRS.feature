#lang: en
Feature: OGRS

  Scenario: Take 10's less than 10%
    Given an OGRS score of 9%
    When a tier is calculated
    Then 0 points are scored

  Scenario: Take 10's high boundary
    Given an OGRS score of 59%
    When a tier is calculated
    Then 5 points are scored

  Scenario: Take 10's low boundary
    Given an OGRS score of 51%
    When a tier is calculated
    Then 5 points are scored

  Scenario: Take 10's lowest boundary
    Given an OGRS score of 50%
    When a tier is calculated
    Then 5 points are scored

  Scenario: Take 10's 100
    Given an OGRS score of 100%
    When a tier is calculated
    Then 10 points are scored

  Scenario: Take 10's 90
    Given an OGRS score of 90%
    When a tier is calculated
    Then 9 points are scored

  Scenario: Take 10's 80
    Given an OGRS score of 80%
    When a tier is calculated
    Then 8 points are scored

  Scenario: Take 10's 70
    Given an OGRS score of 70%
    When a tier is calculated
    Then 7 points are scored

  Scenario: Take 10's 60
    Given an OGRS score of 60%
    When a tier is calculated
    Then 6 points are scored

  Scenario: Take 10's 50
    Given an OGRS score of 50%
    When a tier is calculated
    Then 5 points are scored

  Scenario: Take 10's 40
    Given an OGRS score of 40%
    When a tier is calculated
    Then 4 points are scored

  Scenario: Take 10's 30
    Given an OGRS score of 30%
    When a tier is calculated
    Then 3 points are scored

  Scenario: Take 10's 20
    Given an OGRS score of 20%
    When a tier is calculated
    Then 2 points are scored

  Scenario: Take 10's 10
    Given an OGRS score of 10%
    When a tier is calculated
    Then 1 points are scored