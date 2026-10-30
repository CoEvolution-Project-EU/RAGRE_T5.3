# RAGRE — Risk Assessment, Guidelines and Recommendation Engine

The CoEvolution risk assessment, guidelines and recommendation engine (RAGRE) aims to provide an effective risk management practice to secure the overall AI supply chain ecosystem and the entire lifecycle of AI development and deployment.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-green)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-Spring%20Kafka-purple)](https://spring.io/projects/spring-kafka)
[![CycloneDX](https://img.shields.io/badge/CycloneDX-1.7-blue)](https://cyclonedx.org)

## Scope within Task 5.3

Task 5.3 (Risk Assessment Analysis and Security Assessment Engines) describes two engines: RAGRE and the Security Assessment Engine. Both are implemented in this single service. The Security Assessment Engine logic (consuming the reports of the assessment tools, producing the assessment and defence reports, and calculating the AI risk score) runs inside RAGRE.

## Key Functionalities

- **Risk report**: When a model is registered, scans its requirements file into an SBOM, enriches the components with their known vulnerabilities, weaknesses, threats and mitigations, and produces a CycloneDX risk report for the model.
- **Security assessment**: Consumes the reports of the assessment tools, produces an assessment report (attacks only, and attacks with defences) and a defence report, and calculates the AI risk score of the model.
- **AI risk score**: Scores each finding by severity, confidence, occurrence and category, aggregates the scores per model and updates the score in the model's risk report.
- **Ethical guidelines**: Consumes the answers to the AI Act and GDPR questionnaires and produces a report listing the guidelines triggered by the answers.

All reports follow the CycloneDX format and are uploaded to the Orchestrator file store. Report signing is performed by STRAE.

## How It Works

RAGRE listens to Kafka events published by the Orchestrator. Each event references the input files in the Orchestrator file store; RAGRE downloads them, generates the reports and uploads them back.

| Kafka topic | Input | Output (bucket) |
|-------------|-------|-----------------|
| `ragre_models` | Model requirements file | Risk report (`risk-reports`) |
| `assessment_reports` | Assessment tool reports | Assessment report (`assessment-reports`), defence report (`defence-reports`), updated AI risk score in the risk report |
| `ethical_guidelines` | Questionnaire answers | Ethical guidelines report (`ethical-guidelines-reports`) |

## Project Structure

```text
RAGRE_T5.3/
├── src/main/java/eu/coevolution/adapter/
│   ├── config/              ← Kafka, Feign, Jackson and OpenAPI configuration
│   ├── controller/          ← REST endpoints
│   ├── dto/                 ← event, file and report data models
│   ├── enums/               ← risk scoring and vulnerability enumerations
│   ├── feign/               ← clients for the Mitigate scanning service
│   └── service/
│       ├── catalogue/       ← security and ethical guidelines catalogues
│       ├── ethics/          ← ethical guidelines report flow
│       ├── kafka/           ← Kafka consumers
│       ├── keycloak/        ← authentication against the Orchestrator and Mitigate
│       ├── ragre/           ← risk report, AI risk scoring and Orchestrator file handling
│       └── report/          ← CycloneDX report builders and upload
├── src/main/resources/
│   ├── application.properties
│   └── ethical-guidelines-catalogue.json
├── src/test/                ← tests
├── build.gradle
├── Dockerfile
├── docker-compose.yml
├── LICENSE
└── README.md
```

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Runtime |
| Spring Boot | 4.0.3 | Application framework |
| Spring Kafka | (Spring Boot managed) | Kafka event consumption |
| Spring Cloud OpenFeign | (Spring Cloud managed) | HTTP clients |
| CycloneDX Core Java | 9.0.2 | CycloneDX report generation and validation |
| springdoc-openapi | 3.0.1 | API documentation |
| Gradle | wrapper | Build |
| Docker Compose | latest | Deployment |

## Configuration

Environment-specific settings are read from environment variables:

| Variable | Purpose |
|----------|---------|
| `MITIGATE_BASE_URL` | Mitigate scanning service |
| `KC_HOST`, `KC_REALM`, `KC_CLIENT_NAME`, `KC_CLIENT_SECRET` | Authentication against the Mitigate service |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker |
| `AEGIS_FILES_BASE_URL` | Orchestrator file store |
| `AEGIS_TOKEN_URL`, `AEGIS_CLIENT_ID`, `AEGIS_USERNAME`, `AEGIS_PASSWORD` | Authentication against the Orchestrator |

## License

This project is licensed under the terms of the [Apache License 2.0](LICENSE).
