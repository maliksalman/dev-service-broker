package com.smalik.devservicebroker.provisioners;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceProvisionerProviderTest {

    @Autowired
    private MockMvc client;

    @Test
    void verifyAllPlansHaveProvisioners() throws Exception {

        MvcResult asyncResult = client.perform(get("/v2/catalog"))
            .andExpect(request().asyncStarted())
            .andReturn();

        MvcResult actualResult = client.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        String content = actualResult.getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(content);

        assertThat(jsonNode).isNotNull();
        JsonNode services = jsonNode.get("services");
        assertThat(services).isNotNull();
        assertThat(services).isNotEmpty();
    }
}