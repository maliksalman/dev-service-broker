package com.smalik.mysqlbroker.data;

import lombok.Singular;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Document
public class PlatformServiceBinding {

    @Id
    private PlatformServiceBindingId id;

    private String planDefinitionId;
    private Credentials credentials;

    @Singular
    private Map<String, Object> properties;
}