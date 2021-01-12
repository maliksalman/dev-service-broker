package com.smalik.devservicebroker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix="broker")
public class ServiceBrokerConfig {
    private String namespace;
    private String clusterDomain;
}
