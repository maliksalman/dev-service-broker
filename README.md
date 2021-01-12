# dev-service-broker

A service-broker implementation the https://www.openservicebrokerapi.org/ API. This service-broker can be used with platforms that can use a service-broker to provision services and their credentials. This service-broker expects to have access to running Kubernetes cluster and runs services in the cluster.

### Local Development

The service-broker persists its data in MongoDB. For local development, Mongo can be started using:

```
docker run --rm --name mongo -d -p 27017:27017 mongo:4
```

To run the service-broker locally:

```
./gradlew bootRun

```

### Service Provided

Currently, the services offered by this service-broker are:

- MySQL
- Redis
- RabbitMQ