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
public class PostgresServiceProvisioner extends ServiceProvisioner {

    public PostgresServiceProvisioner(PlatformServiceRepository serviceRepository, PlatformServiceBindingRepository serviceBindingRepository, KubernetesHelper kubernetesHelper) {
        super(serviceRepository, serviceBindingRepository, kubernetesHelper, "k-postgres-default");
    }

    @Override
    public Credentials onProvisionPlatformServiceRootCredentials() {
        return Credentials.builder()
                .username("postgres")
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceProperties(String host) {
        return Map.of(
                "host", host,
                "port", "5432",
                "schema", "postgres"
        );
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root) {
        return Map.of(
                "port", "5432",
                "schema", "postgres",
                "rootpassword", root.getPassword());

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
                "psql", "-U" + service.getCredentials().getUsername(), "-c",
                "CREATE USER " + binding.getCredentials().getUsername() + " WITH PASSWORD '" + binding.getCredentials().getPassword() + "'");
        runKubernetesExecOnPodCommand(service.getId(),
                "psql", "-U" + service.getCredentials().getUsername(), "-c",
                "GRANT ALL PRIVILEGES ON DATABASE postgres TO " + binding.getCredentials().getUsername());
    }

    @Override
    public Credentials onProvisionPlatformServiceBindingCredentials(PlatformService service) {
        return Credentials.builder()
                // postgres identifiers can't contain '-' and must start with a char
                .username("u_" + StringUtils.remove(UUID.randomUUID().toString(), "-"))
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public void onDeletePlatformServiceBinding(PlatformService service, PlatformServiceBinding serviceBinding) {
        runKubernetesExecOnPodCommand(service.getId(),
                "psql", "-U" + service.getCredentials().getUsername(), "-c",
                "DROP OWNED BY " + serviceBinding.getCredentials().getUsername());
        runKubernetesExecOnPodCommand(service.getId(),
                "psql", "-U" + service.getCredentials().getUsername(), "-c",
                "DROP USER " + serviceBinding.getCredentials().getUsername());
    }

    @Override
    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {

        Map<String, Object> map = new HashMap<>();
        map.put("username", binding.getCredentials().getUsername());
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));
        map.put("database", binding.getProperties().get("schema"));
        String uri = String.format("postgres://%s:%s@%s:%s/%s",
                binding.getCredentials().getUsername(),
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"),
                binding.getProperties().get("schema"));
        map.put("uri", uri);
        map.put("type", "postgres");

        return map;
    }
}
