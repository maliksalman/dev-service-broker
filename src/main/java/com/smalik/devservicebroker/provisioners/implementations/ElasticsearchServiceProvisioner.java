package com.smalik.devservicebroker.provisioners.implementations;

import com.smalik.devservicebroker.data.*;
import com.smalik.devservicebroker.provisioners.KubernetesHelper;
import com.smalik.devservicebroker.provisioners.ServiceProvisioner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ElasticsearchServiceProvisioner extends ServiceProvisioner {

    public ElasticsearchServiceProvisioner(PlatformServiceRepository serviceRepository, PlatformServiceBindingRepository serviceBindingRepository, KubernetesHelper kubernetesHelper) {
        super(serviceRepository, serviceBindingRepository, kubernetesHelper, "k-elasticsearch-default");
    }

    @Override
    public Credentials onProvisionPlatformServiceRootCredentials() {
        return Credentials.builder().build();
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceProperties(String host) {
        return Map.of(
                "host", host,
                "port", "9200"
        );
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root) {
        return Map.of("port", "9200");
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
    public Credentials onProvisionPlatformServiceBindingCredentials(PlatformService service) {
        return Credentials.builder().build();
    }

    @Override
    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {
        Map<String, Object> map = new HashMap<>();
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));

        String uri = String.format("http://%s:%s",
                binding.getProperties().get("host"),
                binding.getProperties().get("port"));

        map.put("uris", uri);
        map.put("type", "elasticsearch");

        return map;
    }
}
