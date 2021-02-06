package com.smalik.devservicebroker;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfiguration {

    @Bean
    public Catalog getCatalog() {

        Plan rabbitPlan = Plan.builder()
                .id("k-rabbit-default")
                .name("default")
                .description("Small RabbitMQ message broker suitable for development purposes")
                .free(true)
                .build();

        ServiceDefinition rabbitServiceDefinition = ServiceDefinition.builder()
                .id("k-rabbit")
                .name("k-rabbit")
                .description("Dedicated RabbitMQ running on kubernetes")
                .bindable(true)
                .tags("rabbitmq", "amqp")
                .plans(rabbitPlan)
                .build();

        Plan mysqlPlan = Plan.builder()
                .id("k-mysql-default")
                .name("default")
                .description("Small database suitable for development purposes")
                .free(true)
                .build();

        ServiceDefinition mysqlServiceDefinition = ServiceDefinition.builder()
                .id("k-mysql")
                .name("k-mysql")
                .description("Dedicated MySQL running on kubernetes")
                .bindable(true)
                .tags("mysql", "relational")
                .plans(mysqlPlan)
                .build();

        Plan redisPlan = Plan.builder()
                .id("k-redis-default")
                .name("default")
                .description("Small redis server suitable for development purposes")
                .free(true)
                .build();

        ServiceDefinition redisServiceDefinition = ServiceDefinition.builder()
                .id("k-redis")
                .name("k-redis")
                .description("Dedicated Redis running on kubernetes")
                .bindable(true)
                .tags("redis", "caching")
                .plans(redisPlan)
                .build();
        Plan elasticsearchPlan = Plan.builder()
                .id("k-elasticsearch-default")
                .name("default")
                .description("Small elasticsearch server suitable for development purposes")
                .free(true)
                .build();

        ServiceDefinition elasticsearchServiceDefinition = ServiceDefinition.builder()
                .id("k-elasticsearch")
                .name("k-elasticsearch")
                .description("Dedicated elasticsearch running on kubernetes")
                .bindable(true)
                .tags("elasticsearch", "search")
                .plans(elasticsearchPlan)
                .build();

        return Catalog.builder()
                .serviceDefinitions(
                        mysqlServiceDefinition,
                        rabbitServiceDefinition,
                        redisServiceDefinition,
                        elasticsearchServiceDefinition)
                .build();
    }
}