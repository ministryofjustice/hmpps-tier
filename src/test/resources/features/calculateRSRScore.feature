#lang: en
Feature: API test with WebClient


  Scenario: RSR 7.1 No ROSH 20 points scored
    Given an RSR score of "7.1"
    And no ROSH score
    When a tier is calculated
    Then "20" points are scored
