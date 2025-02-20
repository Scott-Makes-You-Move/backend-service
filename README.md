# Getting Started

Create an application-local.yml file and include the following. Add 'local,logging' profile to the run configuration

```
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: 
          jwk-set-uri: 

jwt:
  auth:
    converter:
      resource-id: 
      principal-attribute: 
```