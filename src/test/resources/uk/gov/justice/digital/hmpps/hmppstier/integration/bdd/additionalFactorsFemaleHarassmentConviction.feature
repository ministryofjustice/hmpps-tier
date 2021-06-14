#lang: en
Feature: Additional Factors (Female only) Harassment

  Scenario: Main offence of Harassment scores 2 points
    Given an offender is "Female"
    And has a custodial sentence
    And has a Harassment offence
    When a tier is calculated
    Then 2 protect points are scored

  Scenario: Other main offence scores 0 points
    Given an offender is "Female"
    And has a custodial sentence
    And has an Abstracting Electricity offence
    When a tier is calculated
    Then 0 protect points are scored

  Scenario: Male offender with main offence of Harassment scores 0 points
    Given an offender is "Male"
    And has a custodial sentence
    And has a Harassment offence
    When a tier is calculated
    Then 0 protect points are scored


  Scenario: Non custodial sentence scores 2 points
    Given an offender is "Female"
    And has a non-custodial sentence
    And has a Harassment offence
    When a tier is calculated
    Then 2 protect points are scored