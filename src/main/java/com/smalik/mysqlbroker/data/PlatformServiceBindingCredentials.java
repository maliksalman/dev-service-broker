package com.smalik.mysqlbroker.data;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

@Data
@Builder
public class PlatformServiceBindingCredentials {

    private String username;
    private String password;
    private String host;
    private int port;

    @Singular
    private Map<String, String> properties;
}
