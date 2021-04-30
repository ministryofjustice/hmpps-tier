#lang: en
Feature: Additional Factors (Female only) Self-Control / Temper and Parenting / Caring Responsibility

  Scenario: Parenting responsibilities scores 2 points
    Given an offender is "Female"
    And has the following OASys complexity answers: "PARENTING_RESPONSIBILITIES" "6.9" : "YES"
    When a tier is calculated
    Then "2" points are scored

#Given an offender is Female
#And has the following OASys complexity answers:
#IMPULSIVITY("11.2"): 1
#TEMPER_CONTROL("11.4"): 1
#When a tier is calculated
#Then "2" points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#TEMPER_CONTROL("11.4"): 1
#When a tier is calculated
#Then "2" points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#IMPULSIVITY("11.2"): 1
#When a tier is calculated
#Then "2" points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#TEMPER_CONTROL("11.4"): 2
#When a tier is calculated
#Then "2" points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#IMPULSIVITY("11.2"): 2
#When a tier is calculated
#Then "2" points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#IMPULSIVITY("11.2"): 1
#PARENTING_RESPONSIBILITIES("6.9") : YES
#When a tier is calculated
#Then 4 points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#TEMPER_CONTROL("11.4"): 1
#PARENTING_RESPONSIBILITIES("6.9") : YES
#When a tier is calculated
#Then 4 points are scored
#
#Given an offender is Female
#And has the following OASys complexity answers:
#IMPULSIVITY("11.2"): 1
#TEMPER_CONTROL("11.4"): 1
#PARENTING_RESPONSIBILITIES("6.9") : YES
#When a tier is calculated
#Then 4 points are scored
#
#Given an offender is Male
#And has the following OASys complexity answers:
#IMPULSIVITY("11.2"): 1
#TEMPER_CONTROL("11.4"): 1
#PARENTING_RESPONSIBILITIES("6.9") : YES
#When a tier is calculated
#Then 0 points are scored
