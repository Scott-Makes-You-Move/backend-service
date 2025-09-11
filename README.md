# Backend Service SMYM

## Overview

This project is a backend service designed to manage user accounts, biometrics, leaderboards, mobility data, and progress tracking. It integrates with Keycloak for authentication and utilizes JPA repositories to interact with the database. The service is built using Spring Boot and PostgreSQL, with support for Docker-based development.

## Project Structure
```
/
├── src/main/java               # Java source code for controllers, models, repositories, and security
│   ├── config                  # Contains configuration classes
│   ├── controller              # Contains controllers for handling HTTP requests and interactions with the service
│   ├── model                   # Defines the domain models
│   ├── repository              # JPA repository classes for interacting with the database
│   └── security                # Classes for JWT conversion and security configurations
├── src/main/resources          # Resources like configuration files
│   ├── application.yml         # Environment-agnostic properties
│   ├── application-local.yml   # Local development properties
│   ├── application-cloud.yml   # Cloud deployment properties
│   ├── application-logging.yml # Logging-related properties
├── src/test/java               # Unit tests and integration tests
├── pom.xml                     # Maven configuration
└── README.md                   # This file
```

## Running the Project Locally

1. **Cloning the repository**
    ```bash
    git clone  git@github.com:Scott-Makes-You-Move/backend-service.git
    cd backend-service
    ```

2. **Running Keycloak Locally with Docker Compose**
    
    The project includes a `docker-compose.yml` and a `realm-export.json` file to set up local instances of Keycloak and PostgreSQL. Keycloak will be available at `http://localhost:8080/admin`. To start the containers, run the following command in the root directory:
    ```bash
    docker-compose up
    ````
3. **Add a local properties file**

    In src/main/resources add an `application-local.yml` and add the following properties:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:backend-service
    username: sa
  h2:
    console:
      enabled: true
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/${keycloak.realm.smym}
          jwk-set-uri: http://localhost:8080/realms/${keycloak.realm.smym}/protocol/openid-connect/certs
  ai:
    vectorstore:
      cosmosdb:
        endpoint: someendpoint
        key: somekey
        database-name: somedatabase
        container-name: somecontainer
        partition-key-path: /metadata/topic
        metadata-fields: topic,subtopic
        vector-store-throughput: 1000
        vector-dimensions: 1536
  openai:
    api-key: somekey
    chat:
      options:
        model: gpt-3.5-turbo
    embedding:
      options:
        model: text-embedding-3-small

chat:
  client:
    advisors:
      masking:
        enabled: true
      rag:
        enabled: true
        similarity-threshold: 0.2

microsoft:
  entra:
    id:
      client-id: someclientid
      client-secret: somesecret
      tenant-id: sometenantid

jwt:
  auth:
    converter:
      resource-id: myclient
      principal-attribute: principal_username

keycloak:
  auth-server-url: http://localhost:8080
  realm:
    smym: smym-dev
    master: master
  client:
    admin: admin-cli
  username: admin
  password: admin

cron:
  sessions:
    morning:
      create: 0 0 10 ? * MON-FRI
      update: 0 0 11 ? * MON-FRI
    lunch:
      create: 0 30 13 ? * MON-FRI
      update: 0 30 14 ? * MON-FRI
    afternoon:
      create: 0 0 15 ? * MON-FRI
      update: 0 0 16 ? * MON-FRI
    leaderboard:
      reset: 0 0 12 ? * 5#1
    drive:
      sync: 0 0 18 ? * 6

notification:
  user-id: userid
  user-email: notification@user.com
  user-name: Notification User

frontend:
  url: https://scottmakesyoumove.com
```


4. Run the application

    Add a Spring Boot Run Configuration and include the local,logging profiles. Or run the application using `mvn spring-boot:run -Dspring-boot.run.profiles=local,logging`

## API Documentation
The application exposes an OpenAPI 3.0 specification for the APIs. Once the application is running, you can view the API documentation at: `http://localhost:9000/swagger-ui.html`. Otherwise, see Postman API documentation for all available endpoints and how to call them. 

## Bootstrapping Data
There is an endpoint available for bootstrapping data at: `http://localhost:9000/api/v1/bootstrap`.

---
Happy developing! 🚀
