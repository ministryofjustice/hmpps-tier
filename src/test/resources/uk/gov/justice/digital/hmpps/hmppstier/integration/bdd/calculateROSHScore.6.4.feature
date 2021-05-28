#lang: en
Feature: Calculate ROSH score

  Scenario: ROSH very high 150 points scored
    Given a ROSH score of "VERY_HIGH"
    And no RSR score
    When a tier is calculated
    Then 150 protect points are scored

  Scenario: ROSH high 20 points scored
    Given a ROSH score of "HIGH"
    And no RSR score
    When a tier is calculated
    Then 20 protect points are scored

  Scenario: ROSH medium 10 points scored
    Given a ROSH score of "MEDIUM"
    And no RSR score
    When a tier is calculated
    Then 10 protect points are scored

  Scenario: ROSH low 0 points scored
    Given a ROSH score of "LOW"
    And no RSR score
    When a tier is calculated
    Then 0 protect points are scored



