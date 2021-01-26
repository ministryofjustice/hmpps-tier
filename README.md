# hmpps-tier

## Continuous Integration  
https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-tier

### Prerequisites  
* Java JDK 11+  
* An editor/IDE
* Gradle  
* Docker  
* OAuth token
  
### How to run the app locally 

#### OAuth security  
In order to run the service locally you need to add HMPPS auth token to your requests

#### How to start locally 
##### Against AWS
Make sure you have the necessary Access key and secret set as environment variables. 
You can do that by running this command before starting the app

```eval $(cloud-platform decode-secret -n hmpps-tier-dev -s hmpps-tier-offender-events-sqs-instance-output --export-aws-credentials)```

This uses SPRING_PROFILES_ACTIVE=dev which has an in-memory database.

```./gradlew bootRun```

##### Against localstack

```docker-compose-up```

Localstack has SQS and SNS. The queue and topic are set up and populated in `setup-sqs.sh` You can access them from the command line as per the following example

```AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws sqs get-queue-attributes --queue-url http://localhost:4576/queue/Digital-Prison-Services-dev-hmpps_tier_offender_events_queue --attribute-names ApproximateNumberOfMessages --endpoint-url=http://localhost:4576```

### Build service and run tests  
This service is built using Gradle. In order to build the project from the command line and run the tests, use:
```  
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
```  
curl -X GET http://localhost:8080/info  
```  
  
##### Application health  
```  
curl -X GET http://localhost:8080/health  
```  
  
##### Application ping  
```  
curl -X GET http://localhost:8080/ping  
```  

