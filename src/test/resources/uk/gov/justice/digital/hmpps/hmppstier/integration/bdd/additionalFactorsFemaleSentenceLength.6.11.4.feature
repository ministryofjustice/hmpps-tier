#lang: en
Feature: Additional Factors (Female only) sentence length

  Background:
    Given a calculation version of at least 3

  Scenario: Sentence length 10 months or over scores 2 points
    Given an offender is "Female"
    And has a sentence length of 10 months
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Sentence length indeterminate scores 2 points
    Given an offender is "Female"
    And has an indeterminate sentence length
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Sentence length under 10 months scores 0 points
    Given an offender is "Female"
    And has a sentence length of 9 months
    When a tier is calculated
    Then 0 protect points are scored


