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





