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
public class RabbitServiceProvisioner extends ServiceProvisioner {

    public RabbitServiceProvisioner(PlatformServiceRepository serviceRepository, PlatformServiceBindingRepository serviceBindingRepository, KubernetesHelper kubernetesHelper) {
        super(serviceRepository, serviceBindingRepository, kubernetesHelper, "k-rabbit-default");
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
                "port", "5672",
                "adminPort", "15672",
                "vhost", "/"
        );
    }

    @Override
    public Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root) {
        return Map.of(
                "port", "5672",
                "rootusername", root.getUsername(),
                "rootpassword", root.getPassword(),
                "adminport", "15672",
                "vhost", "/");

    }

    @Override
    public String[] onDeletePlatformServiceKubernetesResourceNames(PlatformService service, String k8sId) {
        return new String[] {
                "statefulset/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId,
                "service/" + k8sId + "-admin",
                "configmap/" + k8sId
        };
    }

    @Override
    public Credentials onProvisionPlatformServiceBindingCredentials(PlatformService service) {
        return Credentials.builder()
                .username(StringUtils.remove(UUID.randomUUID().toString(), "-").substring(0,30))
                .password(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public void onProvisionPlatformServiceBinding(PlatformService service, PlatformServiceBinding binding) {
        String glob = ".*";
        String vhost = service.getProperties().get("vhost").toString();
        String username = binding.getCredentials().getUsername();
        String password = binding.getCredentials().getPassword();

        runKubernetesExecOnPodCommand(service.getId(),
                "rabbitmqctl", "add_user", username, password);
        runKubernetesExecOnPodCommand(service.getId(),
                "rabbitmqctl", "set_user_tags", username, "monitoring");
        runKubernetesExecOnPodCommand(service.getId(),
                "rabbitmqctl", "set_permissions", "-p", vhost, username, glob, glob, glob);
    }

    @Override
    public void onDeletePlatformServiceBinding(PlatformServiceBinding serviceBinding, PlatformService service) {
        runKubernetesExecOnPodCommand(service.getId(),
                "rabbitmqctl", "delete_user", serviceBinding.getCredentials().getUsername());
    }

    @Override
    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", binding.getCredentials().getUsername());
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));
        map.put("vhost", binding.getProperties().get("vhost"));

        String uri = String.format("amqp://%s:%s@%s:%s%s",
                binding.getCredentials().getUsername(),
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"),
                binding.getProperties().get("vhost"));
        map.put("uri", uri);
        map.put("amqpUri", uri);

        return map;
    }
}
