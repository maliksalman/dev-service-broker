package com.smalik.mysqlbroker.provisioner;

import com.smalik.mysqlbroker.data.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlatformServiceProvisioner {

    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;

    private final MysqlServiceProvisioner mysqlServiceProvisioner;

    private Map<String, ServiceProvisioner> serviceProvisioners;

    @PostConstruct
    public void init() {
        serviceProvisioners = new HashMap<>();
        serviceProvisioners.put("k-mysql-default", mysqlServiceProvisioner);
    }

    public Optional<PlatformService> findPlatformService(String serviceId) {
        return serviceRepository.findById(serviceId);
    }

    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        return serviceProvisioners.get(planDefinitionId)
                .provisionPlatformService(serviceId, planDefinitionId, serviceDefinitionId);
    }

    public PlatformService deletePlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        return serviceProvisioners.get(planDefinitionId)
                .deletePlatformService(serviceId, planDefinitionId, serviceDefinitionId);
    }
    
    public Optional<PlatformServiceBinding> findPlatformServiceBinding(String serviceId, String bindingId) {
        return serviceBindingRepository.findById(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build());
    }

    public PlatformServiceBinding provisionPlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        return serviceProvisioners.get(planDefinitionId)
                .provisionPlatformServiceBinding(serviceId, bindingId, planDefinitionId);
    }

    public PlatformServiceBinding deletePlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        return serviceProvisioners.get(planDefinitionId)
                .deletePlatformServiceBinding(serviceId, bindingId, planDefinitionId);
    }
}
