#lang: en
Feature: Mandate for change

  Scenario: Custodial sentence of type SC has mandate for change
    Given an offender with a current sentence of type 'SC'
    And a valid assessment
    When a tier is calculated
    Then there is a mandate for change

  Scenario: Custodial sentence of type NC has mandate for change
    Given an offender with a current sentence of type 'NC'
    And a valid assessment
    When a tier is calculated
    Then there is a mandate for change

  Scenario: Non-custodial sentence with only unpaid work has no mandate for change
    Given an offender with a current non-custodial sentence
    And unpaid work
    When a tier is calculated
    Then there is no mandate for change

  Scenario: Non-custodial sentence with order extended and unpaid work has no mandate for change
    Given an offender with a current non-custodial sentence
    And unpaid work
    And order extended
    When a tier is calculated
    Then there is no mandate for change

  Scenario: Non-custodial sentence with non-restrictive requirements has a mandate for change
    Given an offender with a current non-custodial sentence
    And a non restrictive requirement
    And a valid assessment
    When a tier is calculated
    Then there is a mandate for change

  Scenario: Sentence with no assessment has change level 2
    Given an offender with a current sentence of type 'NC'
    And no completed Layer 3 assessment
    When a tier is calculated
    Then a change level of 2 is returned and 0 points are scored

  Scenario: Sentence with out of date assessment has change level 2
    Given an offender with a current sentence of type 'NC'
    And a completed Layer 3 assessment dated 55 weeks and one day ago
    When a tier is calculated
    Then a change level of 2 is returned and 0 points are scored

  Scenario: Sentence with 55 week old assessment has a mandate for change
    Given an offender with a current sentence of type 'NC'
    And a completed Layer 3 assessment dated 55 weeks ago
    When a tier is calculated
    Then there is a mandate for change


