#lang: en
Feature: Higher score out of ROSH or RSR is taken

  Background:
    Given a calculation version of at least 1

  Scenario: ROSH 30 points RSR 20 points so 30 points scored
    Given a ROSH score of "VERY_HIGH"
    And an RSR score of "7.0"
    When a tier is calculated
    Then 30 protect points are scored

  Scenario: ROSH 10 points RSR 20 points so 20 points scored
    Given an RSR score of "7.0"
    And a ROSH score of "MEDIUM"
    When a tier is calculated
    Then 20 protect points are scored




