package com.smalik.devservicebroker.data;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Document
public class PlatformServiceBinding {

    @Id
    private PlatformServiceBindingId id;

    private String planDefinitionId;
    private Credentials credentials;

    private Map<String, String> properties;
    private Map<String, Object> context;
    private String platform;
    private LocalDateTime created;
}