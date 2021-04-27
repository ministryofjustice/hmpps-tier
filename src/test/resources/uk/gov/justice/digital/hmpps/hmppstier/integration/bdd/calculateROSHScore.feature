#lang: en
Feature: Calculate ROSH score

  Scenario: ROSH very high 30 points scored
    Given a ROSH score of "VERY_HIGH"
    And no RSR score
    When a tier is calculated
    Then "30" points are scored



