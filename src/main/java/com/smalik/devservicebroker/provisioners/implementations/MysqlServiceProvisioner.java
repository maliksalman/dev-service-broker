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
public class MysqlServiceProvisioner extends ServiceProvisioner {

    public MysqlServiceProvisioner(PlatformServiceRepository serviceRepository, PlatformServiceBindingRepository serviceBindingRepository, KubernetesHelper kubernetesHelper) {
        super(serviceRepository, serviceBindingRepository, kubernetesHelper, "k-mysql-default");
    }

    @Override
    public Credentials onProvisionPlatformServiceRootCredentials() {
        return Credentials.builder()
                .username("root")
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceProperties(String host) {
        return Map.of(
                "host", host,
                "port", "3306",
                "schema", "db"
        );
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root) {
        return Map.of(
                "port", "3306",
                "schema", "db",
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
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "CREATE USER '" + binding.getCredentials().getUsername() + "' IDENTIFIED BY '" + binding.getCredentials().getPassword() + "'");
        runKubernetesExecOnPodCommand(service.getId(),
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "GRANT ALL PRIVILEGES ON db.* TO '" + binding.getCredentials().getUsername() + "'@'%'");
        runKubernetesExecOnPodCommand(service.getId(),
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "FLUSH PRIVILEGES");
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
        runKubernetesExecOnPodCommand(serviceId,
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "DROP USER '" + serviceBinding.getCredentials().getUsername() + "'@'%'");
        runKubernetesExecOnPodCommand(serviceId,
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "FLUSH PRIVILEGES");
    }

    @Override
    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {

        Map<String, Object> map = new HashMap<>();
        map.put("username", binding.getCredentials().getUsername());
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));
        map.put("database", binding.getProperties().get("schema"));
        String uri = String.format("mysql://%s:%s@%s:%s/%s",
                binding.getCredentials().getUsername(),
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"),
                binding.getProperties().get("schema"));
        map.put("uri", uri);
        map.put("type", "mysql");

        return map;
    }
}
