package com.smalik.devservicebroker.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformServiceBindingId {
    private String serviceId;
    private String bindingId;
}
