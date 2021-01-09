package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlatformServiceProvisioner {

    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;
    private final ServiceProvisionerProvider provisioners;

    public String getDashboardUrl(PlatformService service) {
        return provisioners.findProvisionerForPlan(service.getPlanDefinitionId())
                .getDashboardUrl(service);
    }

    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {
        return provisioners.findProvisionerForPlan(binding.getPlanDefinitionId())
                .getCredentials(binding);
    }

    public List<Endpoint> getEndpoints(PlatformServiceBinding binding) {
        return provisioners.findProvisionerForPlan(binding.getPlanDefinitionId())
                .getEndpoints(binding);
    }


    public Optional<PlatformService> findPlatformService(String serviceId) {
        return serviceRepository.findById(serviceId);
    }

    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        return provisioners.findProvisionerForPlan(planDefinitionId)
                .provisionPlatformService(serviceId, planDefinitionId, serviceDefinitionId);
    }

    public PlatformService deletePlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        return provisioners.findProvisionerForPlan(planDefinitionId)
                .deletePlatformService(serviceId, planDefinitionId, serviceDefinitionId);
    }
    
    public Optional<PlatformServiceBinding> findPlatformServiceBinding(String serviceId, String bindingId) {
        return serviceBindingRepository.findById(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build());
    }

    public PlatformServiceBinding provisionPlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        return provisioners.findProvisionerForPlan(planDefinitionId)
                .provisionPlatformServiceBinding(serviceId, bindingId, planDefinitionId);
    }

    public PlatformServiceBinding deletePlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        return provisioners.findProvisionerForPlan(planDefinitionId)
                .deletePlatformServiceBinding(serviceId, bindingId, planDefinitionId);
    }
}
