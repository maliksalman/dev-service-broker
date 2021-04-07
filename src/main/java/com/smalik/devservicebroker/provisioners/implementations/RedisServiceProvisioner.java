package com.smalik.devservicebroker.provisioners.implementations;

import com.smalik.devservicebroker.data.*;
import com.smalik.devservicebroker.provisioners.KubernetesHelper;
import com.smalik.devservicebroker.provisioners.ServiceProvisioner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RedisServiceProvisioner extends ServiceProvisioner {

    public RedisServiceProvisioner(PlatformServiceRepository serviceRepository, PlatformServiceBindingRepository serviceBindingRepository, KubernetesHelper kubernetesHelper) {
        super(serviceRepository, serviceBindingRepository, kubernetesHelper, "k-redis-default");
    }

    @Override
    public Credentials onProvisionPlatformServiceRootCredentials() {
        return Credentials.builder()
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceProperties(String host) {
        return Map.of(
                "host", host,
                "port", "6379"
        );
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root) {
        return Map.of(
                "port", "6379",
                "rootpassword", root.getPassword());

    }

    @Override
    public String[] onDeletePlatformServiceKubernetesResourceNames(PlatformService service, String k8sId) {
        return new String[] {
                "statefulset/" + k8sId,
                "configmap/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId
        };
    }

    @Override
    public Credentials onProvisionPlatformServiceBindingCredentials(PlatformService service) {
        return Credentials.builder()
                .password(service.getCredentials().getPassword())
                .build();
    }

    @Override
    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {
        Map<String, Object> map = new HashMap<>();
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));

        String uri = String.format("redis://%s@%s:%s",
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"));

        map.put("uri", uri);
        map.put("redisUri", uri);
        map.put("type", "redis");

        return map;
    }
}
