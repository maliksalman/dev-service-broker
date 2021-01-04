package com.smalik.mysqlbroker.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformServiceBindingId {
    private String serviceId;
    private String bindingId;
}
