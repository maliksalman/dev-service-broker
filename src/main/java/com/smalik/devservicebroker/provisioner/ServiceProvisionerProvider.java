package com.smalik.devservicebroker.provisioner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ServiceProvisionerProvider {

    private final List<ServiceProvisioner> provisionersList;
    private Map<String, ServiceProvisioner> provisionerMap;

    @PostConstruct
    public void init() {
        provisionerMap = new HashMap<>();
        provisionersList.forEach(p -> provisionerMap.put(p.getDefaultPlanName(), p));
    }

    public ServiceProvisioner findProvisionerForPlan(String planDefinitionId) {
        return provisionerMap.get(planDefinitionId);
    }

    public Set<String> getSupportedPlanDefinitionIds() {
        return provisionerMap.keySet();
    }
}
