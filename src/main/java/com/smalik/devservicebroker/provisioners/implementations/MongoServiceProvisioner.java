package com.smalik.devservicebroker.provisioners.implementations;

import com.smalik.devservicebroker.data.*;
import com.smalik.devservicebroker.provisioners.KubernetesHelper;
import com.smalik.devservicebroker.provisioners.ServiceProvisioner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MongoServiceProvisioner extends ServiceProvisioner {

    public MongoServiceProvisioner(PlatformServiceRepository serviceRepository, PlatformServiceBindingRepository serviceBindingRepository, KubernetesHelper kubernetesHelper) {
        super(serviceRepository, serviceBindingRepository, kubernetesHelper, "k-mongo-default");
    }

    @Override
    public Credentials onProvisionPlatformServiceRootCredentials() {
        return Credentials.builder()
                .username(StringUtils.remove(UUID.randomUUID().toString(), "-").substring(0,30))
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceProperties(String host) {
        return Map.of(
                "host", host,
                "port", "27017",
                "db", "db"
        );
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root) {
        return Map.of(
                "port", "27017",
                "db", "db",
                "rootpassword", root.getPassword(),
                "rootusername", root.getUsername());

    }

    @Override
    public String[] onDeletePlatformServiceKubernetesResourceNames(PlatformService service, String k8sId) {
        return new String[] {
                "statefulset/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId
        };
    }

    @Override
    public void onProvisionPlatformServiceBinding(PlatformService service, PlatformServiceBinding binding) {
        runKubernetesExecOnPodCommand(service.getId(),
                "mongo", "admin",
                "-u", service.getCredentials().getUsername(),
                "-p", service.getCredentials().getPassword(),
                "--eval", String.format("db.getSiblingDB('%s').createUser({user:'%s', pwd:'%s', roles:['readWrite']})",
                        service.getProperties().get("db"),
                        binding.getCredentials().getUsername(),
                        binding.getCredentials().getPassword())
        );
    }

    @Override
    public Credentials onProvisionPlatformServiceBindingCredentials(PlatformService service) {
        return Credentials.builder()
                .username(StringUtils.remove(UUID.randomUUID().toString(), "-").substring(0,30))
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public void onDeletePlatformServiceBinding(PlatformService service, PlatformServiceBinding serviceBinding) {
        String serviceId = service.getId();
        runKubernetesExecOnPodCommand(service.getId(),
                "mongo", "admin",
                "-u", service.getCredentials().getUsername(),
                "-p", service.getCredentials().getPassword(),
                "--eval", String.format("db.getSiblingDB('%s').dropUser('%s')",
                        serviceBinding.getProperties().get("db"),
                        serviceBinding.getCredentials().getUsername())
        );
    }

    @Override
    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {

        Map<String, Object> map = new HashMap<>();
        map.put("username", binding.getCredentials().getUsername());
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));

        String uri = String.format("mongodb://%s:%s@%s:%s/%s",
                binding.getCredentials().getUsername(),
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"),
                binding.getProperties().get("db"));
        map.put("uri", uri);
        map.put("mongoUri", uri);
        map.put("type", "mongodb");

        return map;
    }
}
