# Indy Promote Service

A microservice for promoting artifacts between repositories with validation and tracking capabilities. Built with Quarkus and designed for the Commonjava/Indy ecosystem.

## Overview

The Indy Promote Service enables controlled promotion of artifacts from source repositories to target repositories with comprehensive validation rules, conflict detection, and audit tracking. It supports multiple package types including Maven, NPM, and Generic HTTP content.

## Key Features

- **Multi-package Support**: Maven, NPM, and Generic HTTP artifacts
- **Validation Framework**: Groovy-based validation rules with rule sets
- **Conflict Detection**: Prevents overwriting existing artifacts with different checksums
- **All-or-Nothing Policy**: Automatic rollback on any failure
- **Async/Sync Operations**: Support for both synchronous and asynchronous promotion
- **Audit Tracking**: Cassandra-based tracking of all promotion activities
- **RESTful API**: OpenAPI/Swagger documented endpoints

## Architecture

### Core Components

- **PromotionManager**: Orchestrates the promotion process
- **PromotionValidator**: Executes validation rules against promotion requests
- **ContentDigester**: Handles checksum calculation and verification
- **PathConflictManager**: Manages path conflicts and rollback operations
- **PromoteTrackingManager**: Tracks promotion history in Cassandra

### Package Types

- **Maven**: Java artifacts with POM validation
- **NPM**: Node.js packages with package.json validation  
- **Generic**: HTTP-based content with basic validation

## API Endpoints

### Promotion Operations
- `POST /api/promotion/paths/promote` - Promote artifacts between repositories
- `GET /api/promotion/admin/validation/rules/all` - List all validation rules
- `GET /api/promotion/admin/validation/rulesets` - List rule sets
- `GET /api/promotion/admin/query/{packageType}/{type}/{name}/{path}` - Query promotion history

## Configuration

### Application Properties

```yaml
promote:
  baseDir: "data"                    # Directory for rules and rule-sets
  callbackUri: "callbackUri"         # Callback URL for async operations
  threadpools:
    promote-runner: 8                # Promotion execution threads
    promote-rules-runner: 16         # Validation rule execution threads
    promote-rules-batch-executor: 16 # Batch processing threads
```

### Service Dependencies

- **Storage Service**: For artifact storage operations
- **Content Service**: For content retrieval and metadata
- **Repository Service**: For repository management
- **Kafka**: For event publishing (optional)

## Validation Rules

Validation rules are written in Groovy and support:

- **Path Validation**: Check for pre-existing paths and conflicts
- **Content Validation**: Verify POM/package.json structure
- **Version Patterns**: Enforce version naming conventions
- **Checksum Verification**: Ensure content integrity

### Example Rule Set

```json
{
  "name": "maven-pnc-builds",
  "storeKeyPattern": "maven:[^:]+:pnc-builds",
  "ruleNames": [
    "parsable-pom.groovy",
    "no-pre-existing-paths.groovy", 
    "project-version-pattern.groovy"
  ],
  "validationParameters": {
    "availableInStores": "group:builds-untested",
    "versionPattern": "\\d+\\.\\d+\\.\\d+\\.(?:[\\w_-]+-)?redhat-\\d{5}"
  }
}
```

## Development

### Prerequisites

- Java 11+
- Maven 3.6+
- Cassandra (for tracking)
- Docker (optional, for testing)

### Building

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Running Locally

```bash
mvn quarkus:dev
```

The service will start on `http://localhost:8080` with Swagger UI available at `http://localhost:8080/q/swagger-ui`.

### Docker

```bash
docker-compose up
```

## Error Handling

The service uses two main exception types for error handling:

### PromotionException
The primary exception for promotion operation failures. Used when:
- Promotion execution fails due to system errors
- Network or storage service communication issues
- General promotion process failures

### PromotionValidationException
Used for validation-specific failures with detailed error messages and cause information. Extends `Exception` and includes:
- Custom `toString()` method that incorporates cause messages
- Support for rule-specific error context
- Detailed validation failure information

All promotion operations follow an all-or-nothing policy with automatic rollback on any failure.

## Monitoring

- **Logging**: Structured logging with configurable levels
- **Metrics**: OpenTelemetry integration for observability
- **Health Checks**: Built-in health endpoints

## License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.