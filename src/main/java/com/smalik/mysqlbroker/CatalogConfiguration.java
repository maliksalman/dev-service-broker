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
    Plan plan = Plan.builder()
      .id("f99f0464-a5a8-42b1-9499-f5172ccc5547")
      .name("default")
      .description("A shared DB plan")
      .free(true)
      .build();

    ServiceDefinition serviceDefinition = ServiceDefinition.builder()
      .id("70a4975b-13c2-4405-a340-39dd3ab28917")
      .name("k-mysql")
      .description("Shared MySQL on kubernetes")
      .bindable(true)
      .tags("relational", "mysql")
      .plans(plan)
      .build();

    return Catalog.builder()
      .serviceDefinitions(serviceDefinition)
      .build();
  }
}