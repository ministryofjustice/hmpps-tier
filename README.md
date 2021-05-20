# hmpps-tier

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-tier/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-tier)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://hmpps-tier-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

## Continuous Integration  
https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-tier

### Prerequisites  
* Java JDK 11+  
* An editor/IDE
* Gradle  
* Docker  
* OAuth token

## What it does

Listens to events from Delius, calculates a new offender tier and writes it back into Delius

Integration points:
Delius via SQS
Community-api read and write
Assessment-api read
  
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
This will bring up community-api, assessments-api and all the required queues and topics. The seed data will allow a successful tier calculation for CRN X320741

```sh
docker-compose-up
```

Run the HmppsTier application with `SPRING_PROFILES_ACTIVE=dev,localstack,docker`
If you want to write the tier back into community-api, also run hmpps-tier-to-delius-update locally

Localstack has SQS and SNS. The queue and topic are set up and populated in `setup-sqs.sh` You can access them from the command line as per the following example

```sh
AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws sqs get-queue-attributes --queue-url http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_offender_events_queue --attribute-names ApproximateNumberOfMessages --endpoint-url=http://localhost:4576
```

Force a local tier calculation by publishing a message to the SNS topic like this: 
```sh
AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --endpoint-url=http://localhost:4575 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --message-attributes '{"eventType": {"DataType": "String","StringValue": "OFFENDER_MANAGEMENT_TIER_CALCULATION_REQUIRED"},"source": {"DataType": "String","StringValue": "delius"},"id": {"DataType": "String","StringValue": "fcf89ef7-f6e8-ee95-326f-8ce87d3b8ea0"},"contentType": {"DataType": "String","StringValue": "text/plain;charset=UTF-8"},"timestamp": {"DataType": "Number","StringValue": "1611149702333"}}' \
    --message '{"offenderId":2500468261,"crn":"X320741","sourceId":11174,"eventDatetime":"2021-01-20T13:34:59"}'
```
    

### Build service and run tests  

#### testing and code coverage

Fix kotlin linting errors
```sh
./gradlew fix
```

Run lint and test
The integration and cucumber tests need localstack running

```sh
docker-compose-up localstack
./gradlew check
```

This runs tests and generate a coverage report in build/reports/coverage/index.html

##### Cucumber

You cannot run cucumber tests directly from IntelliJ. See https://github.com/gradle/gradle/issues/4773
Instead run
```docker-compose-up localstack```
```./gradlew cucumber```

If you want to run a single feature/scenario, tag it @single and add this to the cucumber task definition in build.gradle.kts 
```args("--include-tag", "single")```

This service is built using Gradle. In order to build the project from the command line and run the tests, use:
```
docker-compose-up localstack  
./gradlew clean build  
```  
The created JAR file will be named "`probation-tiering<yyyy-mm-dd>.jar`", using the date that the build takes place in the format `yyyy-mm-dd`. 

### Additional configuration  
The application is configurable with conventional Spring parameters.  
The Spring documentation can be found here: https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html  
  
#### Default port  
By default the application starts on port '8080'. To override, set server.port (e.g. `SERVER_PORT=8099 java -jar build/libs/csr-api-<yyyy-mm-dd>.jar` )  
  
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

##### Calculation versioning

The calculation used in calculating the tier score is versioned in whole numbers. If you make any change to the way the tier score is calculated, you must increment the version. 
