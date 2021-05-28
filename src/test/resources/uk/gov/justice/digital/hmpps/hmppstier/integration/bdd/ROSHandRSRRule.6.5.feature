#lang: en
Feature: Higher score out of ROSH or RSR is taken

  Scenario: ROSH 150 points RSR 20 points so 150 points scored
    Given a ROSH score of "VERY_HIGH"
    And an RSR score of "7.0"
    When a tier is calculated
    Then 150 protect points are scored

  Scenario: ROSH 10 points RSR 20 points so 20 points scored
    Given an RSR score of "7.0"
    And a ROSH score of "MEDIUM"
    When a tier is calculated
    Then 20 protect points are scored




