package com.smalik.devservicebroker;

import com.smalik.devservicebroker.data.CatalogService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Component
@ConfigurationProperties(prefix = "broker")
public class ServiceBrokerConfig {

    private String namespace;
    private String clusterDomain;
    private List<CatalogService> services;

    @Bean
    public Catalog getCatalog() {
        return Catalog.builder()
                .serviceDefinitions(services.stream()
                        .map(service -> ServiceDefinition.builder()
                                .id(service.getId())
                                .name(service.getId())
                                .description(service.getDescription())
                                .bindable(true)
                                .tags(service.getTags())
                                .plans(service.getPlans().stream().map(plan -> Plan.builder()
                                        .id(plan.getId())
                                        .name(plan.getName())
                                        .description(plan.getDescription())
                                        .free(true)
                                        .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
