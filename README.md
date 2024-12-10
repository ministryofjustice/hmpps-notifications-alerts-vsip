# hmpps-notifications-alerts-vsip

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-notifications-alerts-vsip/tree/main.svg?style=shield)](https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-notifications-alerts-vsip)

This is a Spring Boot application, written in Kotlin, used to send notifications to users about their visits. Used by [Visits](https://developer-portal.hmpps.service.justice.gov.uk/products/v-isit-someone-in-prison-v-si-p-2).

## Building

To build the project (without tests):
```
./gradlew clean build -x test
```

## Testing

Run:
```
./gradlew test 
```

## Running

This service uses the deployed dev environment to connect to most of the required services,
with an exception of the visit-scheduler and local-stack.

To run this service, clone and run the visit-scheduler locally (with local-stack) 

Then create a .env file at the project root and add 2 secrets to it
```
SYSTEM_CLIENT_ID="get from kubernetes secrets for dev namespace"
SYSTEM_CLIENT_SECRET"get from kubernetes secrets for dev namespace"
```

Then create a Spring Boot run configuration with active profile of 'dev' and set an environments file to the
`.env` file we just created. Run the service in your chosen IDE.

Ports

| Service             | Port |  
|---------------------|------|
| notification-service| 8080 |
| visit-scheduler     | 8081 |

### Auth token retrieval

To create a Token via curl (local):
```
curl --location --request POST "https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token?grant_type=client_credentials" --header "Authorization: Basic $(echo -n {Client}:{ClientSecret} | base64)"
```

or via postman collection using the following authorisation urls:
```
Grant type: Client Credentials
Access Token URL: https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token
Client ID: <get from kubernetes secrets for dev namespace>
Client Secret: <get from kubernetes secrets for dev namespace>
Client Authentication: "Send as Basic Auth Header"
```

Call info endpoint:
```
$ curl 'http://localhost:8080/info' -i -X GET
```

## Gov notify callbacks
This service is configured to accept callbacks from gov notify. See the GovNotifyCallbackController.kt class 
and ResourceServerConfiguration.kt class for implementation details.

To test this functionality locally, use postman to construct an expected request and populate the authheader 
with the gov notify callback token found in the kubernetes secrets for this service.

Example postman request:
```
URL: /viists/notify/callbacks
Authorization: Bearer <token_from_secrets>
Body:
{
    "id": "bb4158e3-1e85-4b66-86f1-7d84fe78804e",
    "reference": "12345",
    "status": "delivered",
    "created_at": "2024-12-05T11:26:38.571735Z",
    "completed_at": "2024-12-05T11:26:40.989392Z",
    "sent_at": "2024-12-05T11:26:38.647725Z",
    "notification_type": "sms",
    "template_id": "85904166-e539-43f5-9f51-7ba106cc61bd",
    "template_version": 8
}
```

You'll need the visit-scheduler running to accept the downstream call this service makes to store the result.

## Application Tracing
The application sends telemetry information to Azure Application Insights which allows log queries and end-to-end request tracing across services

##### Application Insights
#### Example queries

Requests
```azure
requests 
| where cloud_RoleName == 'hmpps-notifications-alerts-vsip' 
| summarize count() by name
```

Performance
```azure
requests
| where cloud_RoleName == 'hmpps-notifications-alerts-vsip' 
| summarize RequestsCount=sum(itemCount), AverageDuration=avg(duration), percentiles(duration, 50, 95, 99) by operation_Name // you can replace 'operation_Name' with another value to segment by a different property
| order by RequestsCount desc // order from highest to lower (descending)
```

Charts
```azure
requests
| where cloud_RoleName == 'hmpps-notifications-alerts-vsip' 
| where timestamp > ago(12h) 
| summarize avgRequestDuration=avg(duration) by bin(timestamp, 10m) // use a time grain of 10 minutes
| render timechart
```

## Common gradle tasks

To list project dependencies, run:

```
./gradlew dependencies
``` 

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```

To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```

#### Ktlint Gradle Tasks

To run Ktlint check:
```
./gradlew ktlintCheck
```

To run Ktlint format:
```
./gradlew ktlintFormat
```

To apply ktlint styles to intellij
```
./gradlew ktlintApplyToIdea
```

To register pre-commit check to run Ktlint format:
```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook 
```

...or to register pre-commit check to only run Ktlint check:
```
./gradlew ktlintApplyToIdea addKtlintCheckGitPreCommitHook
```