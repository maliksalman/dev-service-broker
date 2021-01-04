package com.smalik.mysqlbroker.provisioner;

import com.smalik.mysqlbroker.data.PlatformService;
import com.smalik.mysqlbroker.data.PlatformServiceBinding;

public interface ServiceProvisioner {

    PlatformService provisionPlatformService(
            String serviceId, String planDefinitionId, String serviceDefinitionId);

    PlatformService deletePlatformService(
            String serviceId, String planDefinitionId, String serviceDefinitionId);

    PlatformServiceBinding provisionPlatformServiceBinding(
            String serviceId, String bindingId, String planDefinitionId);

    PlatformServiceBinding deletePlatformServiceBinding(
            String serviceId, String bindingId, String planDefinitionId);
}
