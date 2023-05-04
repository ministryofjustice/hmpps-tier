#lang: en
Feature: Integrated Offender Management

  @single
  Scenario: IOM scores 2 points
    Given the following active registrations: "Integrated Offender Management" "IIOM"
    When a tier is calculated
    Then 2 change points are scored





