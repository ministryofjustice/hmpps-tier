#lang: en
Feature: Additional Factors (Female only) Violence or Arson

  @disabled
#   for version 1.1
  Scenario: Main offence of Arson scores 2 points
    Given an offender is "Female"
    And has a custodial sentence
    And has a main offence of Arson
    When a tier is calculated
    Then "2" points are scored

  Scenario: Main offence of Violence scores 2 points
    Given an offender is "Female"
    And has a custodial sentence
    And has a main offence of Violence
    When a tier is calculated
    Then "2" points are scored

  Scenario: Other main offence scores 0 points
    Given an offender is "Female"
    And has a custodial sentence
    And has a main offence of Abstracting Electricity
    When a tier is calculated
    Then "0" points are scored

  Scenario: Male offender with main offence of Violence scores 0 points
    Given an offender is "Male"
    And has a custodial sentence
    And has a main offence of Violence
    When a tier is calculated
    Then "0" points are scored

