#lang: en
Feature: MAPPA

  Background:
    Given a calculation version of at least 1

  Scenario: MAPPA M Level 3 scores 30 points
    Given an active MAPPA registration of M Level "3"
    When a tier is calculated
    Then 30 protect points are scored

  Scenario: MAPPA M Level 2 scores 30 points
    Given an active MAPPA registration of M Level "2"
    When a tier is calculated
    Then 30 protect points are scored

  Scenario: MAPPA M Level 1 scores 5 points
    Given an active MAPPA registration of M Level "1"
    When a tier is calculated
    Then 5 protect points are scored

  Scenario: No active MAPPA scores 0 points
    Given no active MAPPA Registration
    When a tier is calculated
    Then 0 protect points are scored








