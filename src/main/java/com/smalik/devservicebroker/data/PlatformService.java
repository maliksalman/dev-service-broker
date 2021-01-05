package com.smalik.devservicebroker.data;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@Builder
@Document
public class PlatformService {

    @Id
    private String id;

    private String planDefinitionId;
    private String serviceDefinitionId;
    private Credentials credentials;
    private String dashboardUrl;

    @Singular
    private Map<String, Object> properties;
}