#lang: en
Feature: OGRS

  Background:
    Given a calculation version of at least 1

  Scenario: An OGRS score of 59% only results in 5 points
    Given an OGRS score of "59"%
    When a tier is calculated
    Then 5 change points are scored

  Scenario: An OGRS score of 51% only results in 5 points
    Given an OGRS score of "51"%
    When a tier is calculated
    Then 5 change points are scored

  Scenario: An OGRS score of 50% only results in 5 points
    Given an OGRS score of "50"%
    When a tier is calculated
    Then 5 change points are scored

  Scenario: An OGRS score of 100% results in 10 points
    Given an OGRS score of "100"%
    When a tier is calculated
    Then 10 change points are scored

  Scenario: An OGRS score of 90% results in 9 points
    Given an OGRS score of "90"%
    When a tier is calculated
    Then 9 change points are scored

  Scenario: An OGRS score of 80% results in 8 points
    Given an OGRS score of "80"%
    When a tier is calculated
    Then 8 change points are scored

  Scenario: An OGRS score of 70% results in 7 points
    Given an OGRS score of "70"%
    When a tier is calculated
    Then 7 change points are scored

  Scenario: An OGRS score of 60% results in 6 points
    Given an OGRS score of "60"%
    When a tier is calculated
    Then 6 change points are scored

  Scenario: An OGRS score of 50% results in 5 points
    Given an OGRS score of "50"%
    When a tier is calculated
    Then 5 change points are scored

  Scenario: An OGRS score of 40% results in 4 points
    Given an OGRS score of "40"%
    When a tier is calculated
    Then 4 change points are scored

  Scenario: An OGRS score of 30% results in 3 points
    Given an OGRS score of "30"%
    When a tier is calculated
    Then 3 change points are scored

  Scenario: An OGRS score of 20% results in 2 points
    Given an OGRS score of "20"%
    When a tier is calculated
    Then 2 change points are scored

  Scenario: An OGRS score of 10% results in 1 points
    Given an OGRS score of "10"%
    When a tier is calculated
    Then 1 change points are scored

  Scenario: An OGRS score of less than 10% results in 0 points
    Given an OGRS score of "9"%
    When a tier is calculated
    Then 0 change points are scored