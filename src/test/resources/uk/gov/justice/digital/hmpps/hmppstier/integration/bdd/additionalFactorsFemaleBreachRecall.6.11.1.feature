#lang: en
Feature: Additional Factors (Female only) Breach and Recall

  Background:
    Given a calculation version of at least 1

  Scenario: Male offender with breach/recall scores no points
    Given an offender is "Male"
    And has an active conviction with NSI Outcome code "BRE01"
    When a tier is calculated
    Then 0 protect points are scored

  Scenario: Female offender with breach/recall on an active conviction scores 2 points
    Given an offender is "Female"
    And has an active conviction with NSI Outcome code "BRE02"
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Female offender with breach/recall on two active convictions scores 2 points
    Given an offender is "Female"
    And has two active convictions with NSI Outcome code "BRE03"
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Convictions up to 12 months old are counted
    Given an offender is "Female"
    And has a conviction terminated 365 days ago with NSI Outcome code "BRE04"
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Convictions over 12 months old are not counted
    Given an offender is "Female"
    And has a conviction terminated 366 days ago with NSI Outcome code "BRE04"
    When a tier is calculated
    Then 0 protect points are scored

  Scenario: Different types of breach/recall don't add together
    Given an offender is "Female"
    And has two active convictions with NSI Outcome codes "BRE05" and "REC01"
    When a tier is calculated
    Then 2 protect points are scored

