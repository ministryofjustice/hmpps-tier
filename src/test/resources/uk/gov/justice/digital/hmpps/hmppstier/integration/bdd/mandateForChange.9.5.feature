#lang: en
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

#    Given an Offender with a current sentence not of type 'SC' or 'NC'
#    And an OGRS score of "50"%
#    And the offender has a restrictive requirement on that conviction
#    When a tier is calculated
#    Then there is a mandate for change and a change level of "1" is returned for "5" points

#
#    Given an Offender with a current sentence not of type 'SC' or 'NC'
#    And the offender has unpaid work on that conviction
#    And no other non-restrictive requirements exist
#    When a tier is calculated
#    Then there is a mandate for change
#    And the change level calculation continues
#
#    Given an Offender with a current sentence not of type 'SC' or 'NC'
#    And the offender has no restrictive requirement on that conviction
#    And the offender has no unpaid work on that conviction
#    When a tier is calculated
#    Then there is not a mandate for change
#    And a Change level of '0' is returned
#    And 0 points are scored
#    And no further factors are considered towards the change level calculation
#
#    Given an Offender with a Completed Layer 3 assessment dated 01/01/2020
#    And today is 20/01/2021 (55 weeks)
#    And the change level calculation continues
#
#    Given an Offender with a Completed Layer 3 assessment dated 01/01/2020
#    And today is 21/01/2021 (55 weeks + 1 day)
#    Then a Change level of '2' is returned
#    And 0 points are scored
#    And no further factors are considered towards the change level calculation
#
#    Given an Offender has no OASys Layer 3 assessment recorded
#    Then a Change Needs Axis level of 2 is returned



