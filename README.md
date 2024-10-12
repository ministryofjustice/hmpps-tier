# hmpps-tier

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-tier/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-tier)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://hmpps-tier-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

## Continuous Integration  
https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-tier

### Prerequisites  
* Java JDK 21
* An editor/IDE
* Gradle  
* Docker  
* OAuth token

## What it does

Listens to events from Delius, calculates a new offender tier and writes it back into Delius

Integration points:
- tier calculation events from Delius via SQS
- Community-api read
- Assessment-api read
- writes updated tiers to SNS for https://github.com/ministryofjustice/hmpps-tier-to-delius-update to consume
  
### How to run the app locally 

#### OAuth security  
In order to run the service locally you need to add HMPPS auth token to your requests

#### How to start locally 
##### Against AWS
Make sure you have the necessary Access key and secret set as environment variables. 
You can do that by running this command before starting the app

```sh
eval $(cloud-platform decode-secret -n hmpps-tier-dev -s hmpps-tier-offender-events-sqs-instance-output --export-aws-credentials)
```

This uses SPRING_PROFILES_ACTIVE=dev which has an in-memory database.

```sh
./gradlew bootRun
```

##### Against all local dependencies in docker
This will bring up community-api, assessments-api and all the required queues and topics. The seed data will allow a successful tier calculation for CRN X320741. Make sure you have allocated enough memory to Docker to allow it to start all these containers - 2GB may not be enough

```sh
docker compose up -d
```

Run the HmppsTier application with `SPRING_PROFILES_ACTIVE=dev,localstack,docker`
If you want to write the tier back into community-api, also run hmpps-tier-to-delius-update locally

Localstack has SQS and SNS. The queue and topic are set up and populated in `setup-sqs.sh` You can access them from the command line as per the following example

Force a local tier calculation by calling the `/calculations` API
```shell
curl --json '["A123456"]' 'http://localhost:8080/calculations?dryRun=false'
```
    
View the tier calculation complete event:
```shell
AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws sqs receive-message --queue-url http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_calculation_complete_queue --endpoint-url=http://localhost:4576
```
### Build service and run tests  

#### testing and code coverage

Run lint and test

The integration and cucumber tests need localstack running

```sh
docker compose up localstack postgres -d
./gradlew check
```

This runs tests and generate a coverage report in build/reports/coverage/index.html

##### Cucumber

You cannot run cucumber tests directly from IntelliJ. See https://github.com/gradle/gradle/issues/4773
Instead run

```sh
docker compose up localstack postgres -d
./gradlew cucumber
```

If you want to run a single feature/scenario, tag it @single and add this to the cucumber task definition in build.gradle.kts 

```args("--include-tag", "single")```

### Additional configuration  
The application is configurable with conventional Spring parameters.  
The Spring documentation can be found here: https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html  
  
#### Default port  
By default, the application starts on port '8080'. To override, set server.port (e.g. `SERVER_PORT=8099 java -jar build/libs/csr-api-<yyyy-mm-dd>.jar` )  
  
### Documentation  
The generated documentation for the api can be viewed at http://localhost:8080/swagger-ui.html
  
### Health  
  
- `/ping`: will respond `pong` to all requests.  This should be used by dependent systems to check connectivity to   
csr-api, rather than calling the `/health` endpoint.  
- `/health`: provides information about the application health and its dependencies.  This should only be used  
by csr-api health monitoring (e.g. pager duty) and not other systems who wish to find out the   
state of csr-api.  
- `/info`: provides information about the version of deployed application.  
  
#### Health and info Endpoints (curl)  
  
##### Application info  
```sh
curl -X GET http://localhost:8080/info  
```  
  
##### Application health  
```sh
curl -X GET http://localhost:8080/health  
```  
  
##### Application ping  
```sh 
curl -X GET http://localhost:8080/ping  
```  
