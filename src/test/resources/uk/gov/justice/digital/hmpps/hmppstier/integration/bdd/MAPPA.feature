#lang: en
Feature: MAPPA

  Scenario: MAPPA M Level 3 scores 30 points
    Given an active MAPPA registration of M Level "3"
    When a tier is calculated
    Then "30" points are scored






