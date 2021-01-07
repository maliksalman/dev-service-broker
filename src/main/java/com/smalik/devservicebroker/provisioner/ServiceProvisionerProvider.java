package com.smalik.devservicebroker.provisioner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ServiceProvisionerProvider {

    private final MysqlServiceProvisioner mysqlServiceProvisioner;
    private final RabbitServiceProvisioner rabbitServiceProvisioner;
    private final RedisServiceProvisioner redisServiceProvisioner;

    private Map<String, ServiceProvisioner> provisionerMap;

    @PostConstruct
    public void init() {
        provisionerMap = new HashMap<>();
        provisionerMap.put("k-mysql-default", mysqlServiceProvisioner);
        provisionerMap.put("k-rabbit-default", rabbitServiceProvisioner);
        provisionerMap.put("k-redis-default", redisServiceProvisioner);
    }

    public ServiceProvisioner findProvisionerForPlan(String planDefinitionId) {
        return provisionerMap.get(planDefinitionId);
    }

    public Set<String> getSupportedPlanDefinitionIds() {
        return provisionerMap.keySet();
    }
}
