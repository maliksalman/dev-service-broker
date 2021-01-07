package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.CatalogConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceProvisionerProviderTest {

    @InjectMocks
    ServiceProvisionerProvider provider;

    @BeforeEach
    void setup() {
        provider.init();
    }

    @Test
    void verifyAllPlansHaveProvisioners() {

        Set<String> supportedPlanDefinitionIds = provider.getSupportedPlanDefinitionIds();

        Catalog catalog = new CatalogConfiguration().getCatalog();
        catalog.getServiceDefinitions().stream().forEach(svc -> {
            svc.getPlans().stream().forEach(plan -> {
                assertThat(plan.getId()).isIn(supportedPlanDefinitionIds);
            });
        });
    }
}