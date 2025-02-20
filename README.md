# Getting Started

## Configuration ##

Create an application-local.yml file and include the following.

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

## How to run ##
Include 'local,logging' to the run configurations.