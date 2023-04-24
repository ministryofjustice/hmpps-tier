#lang: en
Feature: Calculate RSR score
  @single
  Scenario: RSR 7.1 No ROSH 20 points scored
    Given an RSR score of "7.1"
    And no ROSH score
    When a tier is calculated
    Then 20 protect points are scored

  Scenario: RSR 7.0 No ROSH 20 points scored
    Given an RSR score of "7.0"
    And no ROSH score
    When a tier is calculated
    Then 20 protect points are scored

  Scenario: RSR 6.9 No ROSH 10 points scored
    Given an RSR score of "6.9"
    And no ROSH score
    When a tier is calculated
    Then 10 protect points are scored

  Scenario: RSR 3.1 No ROSH 10 points scored
    Given an RSR score of "3.1"
    And no ROSH score
    When a tier is calculated
    Then 10 protect points are scored

  Scenario: RSR 3.0 No ROSH 10 points scored
    Given an RSR score of "3.0"
    And no ROSH score
    When a tier is calculated
    Then 10 protect points are scored

  Scenario: RSR 2.9 No ROSH 0 points scored
    Given an RSR score of "2.9"
    And no ROSH score
    When a tier is calculated
    Then 0 protect points are scored

  Scenario: RSR 999.99 No ROSH 0 points scored
    Given an RSR score of "999.99"
    And no ROSH score
    When a tier is calculated
    Then 0 protect points are scored