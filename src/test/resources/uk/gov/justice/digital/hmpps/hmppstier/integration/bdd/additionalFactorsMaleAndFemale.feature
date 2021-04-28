#lang: en
Feature: Additional Factors (Male and Female)

  Scenario: Child Concerns scores 2 points
    Given the following active registrations: "Child Concerns" "RCCO"
    When a tier is calculated
    Then "2" points are scored









