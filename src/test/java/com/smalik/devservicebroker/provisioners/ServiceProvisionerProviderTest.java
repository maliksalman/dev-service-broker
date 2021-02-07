package com.smalik.devservicebroker.provisioners;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
class ServiceProvisionerProviderTest {

    @Autowired
    WebTestClient client;

    @Test
    void verifyAllPlansHaveProvisioners() {

        JsonNode catalogNode = client.get()
            .uri("/v2/catalog").accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .returnResult(JsonNode.class).getResponseBody()
            .blockFirst();

        assertThat(catalogNode).isNotNull();
        JsonNode services = catalogNode.get("services");
        assertThat(services).isNotNull();
        assertThat(services).isNotEmpty();
    }
}