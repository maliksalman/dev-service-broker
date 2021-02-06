package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.PlatformService;
import com.smalik.devservicebroker.data.PlatformServiceBinding;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;

import java.util.List;
import java.util.Map;

public interface ServiceProvisioner {

    String getDefaultPlanName();
    
    PlatformService provisionPlatformService(
            String serviceId, String planDefinitionId, String serviceDefinitionId);

    PlatformService deletePlatformService(
            String serviceId, String planDefinitionId, String serviceDefinitionId);

    PlatformServiceBinding provisionPlatformServiceBinding(
            String serviceId, String bindingId, String planDefinitionId);

    PlatformServiceBinding deletePlatformServiceBinding(
            String serviceId, String bindingId, String planDefinitionId);

    String getDashboardUrl(
            PlatformService service);

    Map<String, Object> getCredentials(
            PlatformServiceBinding binding);

    List<Endpoint> getEndpoints(
            PlatformServiceBinding binding);
}
