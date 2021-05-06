#lang: en
Feature: Additional Factors (Female only) Self-Control / Temper and Parenting / Caring Responsibility

  Scenario: Parenting Responsibilities scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "PARENTING_RESPONSIBILITIES" "6.9" : "YES"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Impulsivity and Temper Control 1 scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "1"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "1"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Temper Control 1 scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "1"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Impulsivity 1 scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "1"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Temper Control 2 scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "2"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Impulsivity 2 scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "2"
    When a tier is calculated
    Then "2" points are scored

  Scenario: Impulsivity 1 and Parenting Responsibilities scores 4 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "1"
    And has the following OASys complexity answer: "PARENTING_RESPONSIBILITIES" "6.9" : "YES"
    When a tier is calculated
    Then "4" points are scored

  Scenario: Temper Control 1 and Parenting Responsibilities scores 4 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "1"
    And has the following OASys complexity answer: "PARENTING_RESPONSIBILITIES" "6.9" : "YES"
    When a tier is calculated
    Then "4" points are scored

  Scenario: Temper Control 1, Impulsivity 1 and Parenting Responsibilities scores 4 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "1"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "1"
    And has the following OASys complexity answer: "PARENTING_RESPONSIBILITIES" "6.9" : "YES"
    When a tier is calculated
    Then "4" points are scored

  Scenario: Temper Control 0, Impulsivity 0 and Parenting Responsibilities No scores 0 points
    Given an offender is "Female"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "0"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "0"
    And has the following OASys complexity answer: "PARENTING_RESPONSIBILITIES" "6.9" : "NO"
    When a tier is calculated
    Then "0" points are scored

  Scenario: Male offender scores 0 points
    Given an offender is "Male"
    And has the following OASys complexity answer: "IMPULSIVITY" "11.2" : "1"
    And has the following OASys complexity answer: "TEMPER_CONTROL" "11.4" : "1"
    And has the following OASys complexity answer: "PARENTING_RESPONSIBILITIES" "6.9" : "YES"
    When a tier is calculated
    Then "0" points are scored
