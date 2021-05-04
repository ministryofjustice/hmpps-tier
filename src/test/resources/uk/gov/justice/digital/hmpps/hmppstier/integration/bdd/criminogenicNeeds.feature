#lang: en
Feature: Criminogenic Needs

Given the following assessment needs:
|ACCOMMODATION|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|ACCOMMODATION|STANDARD|
When a tier is calculated
Then 1 change points are scored

Given the following assessment needs:
|ACCOMMODATION|SEVERE|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|EDUCATION_TRAINING_AND_EMPLOYABILITY|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|EDUCATION_TRAINING_AND_EMPLOYABILITY|STANDARD|
When a tier is calculated
Then 1 change points are scored

Given the following assessment needs:
|EDUCATION_TRAINING_AND_EMPLOYABILITY|SEVERE|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|RELATIONSHIPS|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|RELATIONSHIPS|STANDARD|
When a tier is calculated
Then 1 change points are scored

Given the following assessment needs:
|RELATIONSHIPS|SEVERE|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|LIFESTYLE_AND_ASSOCIATES|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|LIFESTYLE_AND_ASSOCIATES|STANDARD|
When a tier is calculated
Then 1 change points are scored

Given the following assessment needs:
|LIFESTYLE_AND_ASSOCIATES|SEVERE|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|DRUG_MISUSE|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|DRUG_MISUSE|STANDARD|
When a tier is calculated
Then 1 change points are scored

Given the following assessment needs:
|DRUG_MISUSE|SEVERE|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|ALCOHOL_MISUSE|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|ALCOHOL_MISUSE|STANDARD|
When a tier is calculated
Then 1 change points are scored

Given the following assessment needs:
|ALCOHOL_MISUSE|SEVERE|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|THINKING_AND_BEHAVIOUR|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|THINKING_AND_BEHAVIOUR|STANDARD|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|THINKING_AND_BEHAVIOUR|SEVERE|
When a tier is calculated
Then 4 change points are scored

Given the following assessment needs:
|ATTITUDES|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|ATTITUDES|STANDARD|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|ATTITUDES|SEVERE|
When a tier is calculated
Then 4 change points are scored

Given the following assessment needs:
|FINANCIAL_MANAGEMENT_AND_INCOME|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|FINANCIAL_MANAGEMENT_AND_INCOME|STANDARD|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|FINANCIAL_MANAGEMENT_AND_INCOME|SEVERE|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|EMOTIONAL_WELL_BEING|NO_NEED|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|EMOTIONAL_WELL_BEING|STANDARD|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|EMOTIONAL_WELL_BEING|SEVERE|
When a tier is calculated
Then 0 change points are scored

Given the following assessment needs:
|ACCOMMODATION|STANDARD|
|ALCOHOL_MISUSE|NO_NEED|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|ACCOMMODATION|STANDARD|
|ALCOHOL_MISUSE|STANDARD|
When a tier is calculated
Then 2 change points are scored

Given the following assessment needs:
|ACCOMMODATION|STANDARD|
|ALCOHOL_MISUSE|SEVERE|
When a tier is calculated
Then 3 change points are scored
