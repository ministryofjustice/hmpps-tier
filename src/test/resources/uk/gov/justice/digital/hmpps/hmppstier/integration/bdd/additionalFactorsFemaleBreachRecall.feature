#lang: en
Feature: Additional Factors (Female only) Breach and Recall

  Scenario: Male offender with breach/recall scores no points
    Given an offender is "Male"
    And has an active conviction with NSI Outcome code "BRE01"
    When a tier is calculated
    Then "0" points are scored

#  Scenario: Female offender with breach/recall on an active conviction scores 2 points
#    Given an offender is "Female"
#    And has an active conviction
#    And the conviction has a relevant NSI Outcome code
#    Then "2" points are scored
#
#  Scenario: Female offender with breach/recall on two active conviction scores 2 points
#    Given an offender is "Female"
#    And has two active convictions
#    And both convictions have a relevant NSI Outcome code
#    Then "2" points are scored
#
#  Scenario: Convictions up to 12 months old are counted
#    Given an offender is "Female"
#    And has a conviction terminated 02/01/2020
#    And today is 02/01/2021 (terminated 365 days ago)
#    And the conviction has a relevant NSI Outcome code
#    Then "2" points are scored
#
#  Scenario: Convictions over 12 months old are not
#    Given an offender is "Female"
#    And has a conviction terminated 02/01/2020
#    And today is 03/01/2021 (terminated 366 days ago)
#    And the conviction has a relevant NSI Outcome code
#    Then "0" points are scored
#
#  Scenario: Different types of breach/recall don't add together
#    Given an offender is "Female"
#    And has an active conviction with NSI Outcome code BRE01
#    And has an(other) active conviction with NSI Outcome code REC01
#    Then "2" points are scored
#
#  Scenario: The same type of breach/recall don't add together
#    Given an offender is "Female"
#    And has an active conviction with NSI Outcome code BRE01
#    And has an(other) active conviction with NSI Outcome code BRE01
#    Then "2" points are scored
#
# TODO update BDD document with this