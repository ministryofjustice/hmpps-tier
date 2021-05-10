#lang: en
@single
Feature: Mandate for change

  Scenario: Custodial sentence of type SC has mandate for change
    Given an offender with a current sentence of type 'SC'
    And an OGRS score of "50"%
    When a tier is calculated
    Then there is a mandate for change and a change level of "1" is returned for "5" points

  Scenario: Custodial sentence of type NC has mandate for change
    Given an offender with a current sentence of type 'NC'
    And an OGRS score of "50"%
    When a tier is calculated
    Then there is a mandate for change and a change level of "1" is returned for "5" points

  Scenario: Non-custodial sentence with only unpaid work has no mandate for change
    Given an offender with a current non-custodial sentence
    And unpaid work
    And an OGRS score of "50"%
    When a tier is calculated
    Then there is no mandate for change

  Scenario: Non-custodial sentence with order extended and unpaid work has no mandate for change
    Given an offender with a current non-custodial sentence
    And unpaid work
    And order extended
    And an OGRS score of "50"%
    When a tier is calculated
    Then there is no mandate for change

  Scenario: Non-custodial sentence with non-restrictive requirements has a mandate for change
    Given an offender with a current non-custodial sentence
    And a non restrictive requirement
    And an OGRS score of "50"%
    When a tier is calculated
    Then there is a mandate for change and a change level of "1" is returned for "5" points

  Scenario: Sentence with no assessment has change level 2
    Given an offender with a current sentence of type 'NC'
    And no completed Layer 3 assessment
    When a tier is calculated
    Then a change level of "2" is returned for "0" points

  Scenario: Sentence with out of date assessment has change level 2
    Given an offender with a current sentence of type 'NC'
    And a completed Layer 3 assessment dated 55 weeks and one day ago
    When a tier is calculated
    Then a change level of "2" is returned for "0" points




