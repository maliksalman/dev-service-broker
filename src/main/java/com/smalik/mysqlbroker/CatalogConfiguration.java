package com.smalik.mysqlbroker;

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
                .tags("relational", "mysql")
                .plans(mysqlPlan)
                .build();

        return Catalog.builder()
                .serviceDefinitions(mysqlServiceDefinition, rabbitServiceDefinition)
                .build();
    }
}