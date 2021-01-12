package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RabbitServiceProvisioner implements ServiceProvisioner {

    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;
    private final KubernetesHelper kubernetesHelper;

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = kubernetesHelper.getKubernetesServiceFQDN(serviceId);
        String vhost = "/";
        int port = 5672;
        int adminPort = 15672;
        String password = UUID.randomUUID().toString();

        PlatformService data = PlatformService.builder()
                .id(serviceId)
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder()
                        .username("root")
                        .password(password)
                        .build())
                .property("host", host)
                .property("port", port)
                .property("adminPort", adminPort)
                .property("vhost", vhost)
                .build();
        serviceRepository.save(data);

        kubernetesHelper.applyKubernetesTemplate(
                "k-rabbit-default.yml",
                serviceId,
                Map.of(
                        "port", String.valueOf(port),
                        "rootusername", "root",
                        "rootpassword", password,
                        "adminport", String.valueOf(adminPort),
                        "vhost", vhost));

        return data;
    }

    @SneakyThrows
    public PlatformService deletePlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        Optional<PlatformService> data = serviceRepository.findById(serviceId);
        if (data.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: ServiceId=" + serviceId);
        }

        String k8sId = kubernetesHelper.getKubernetesServiceName(serviceId);
        kubernetesHelper.runKubernetesDeleteCommand(
                "statefulset/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId,
                "service/" + k8sId + "-admin",
                "configmap/" + k8sId);

        PlatformService svc = data.get();
        serviceRepository.delete(svc);

        return svc;
    }
    
    @SneakyThrows
    public PlatformServiceBinding provisionPlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        Optional<PlatformService> optionalService = serviceRepository.findById(serviceId);
        if (optionalService.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }
        
        String username = StringUtils.remove(UUID.randomUUID().toString(), "-").substring(0,30);
        String password = UUID.randomUUID().toString();

        PlatformService service = optionalService.get();
        PlatformServiceBinding binding = PlatformServiceBinding.builder()
                .id(PlatformServiceBindingId.builder()
                        .serviceId(serviceId)
                        .bindingId(bindingId)
                        .build())
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder()
                        .username(username)
                        .password(password)
                        .build())
                .properties(new HashMap<>(service.getProperties()))
                .build();
        serviceBindingRepository.save(binding);

        String glob = ".*";
        String vhost = service.getProperties().get("vhost").toString();

        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                "rabbitmqctl", "add_user", username, password);
        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                "rabbitmqctl", "set_user_tags", username, "monitoring");
        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                "rabbitmqctl", "set_permissions", "-p", vhost, username, glob, glob, glob);

        return binding;
    }

    @SneakyThrows
    public PlatformServiceBinding deletePlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {

        Optional<PlatformServiceBinding> binding = serviceBindingRepository.findById(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build());
        if (binding.isPresent()) {
            kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                    "rabbitmqctl", "delete_user", binding.get().getCredentials().getUsername());
        } else {
            throw new RuntimeException("Can't find the binding: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }

        return binding.get();
    }

    public String getDashboardUrl(PlatformService service) {
        return null;
    }

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

    public List<Endpoint> getEndpoints(PlatformServiceBinding binding) {
        return Arrays.asList(new Endpoint(
                String.valueOf(binding.getProperties().get("host")),
                Arrays.asList(String.valueOf(binding.getProperties().get("port"))),
                Endpoint.Protocol.TCP
        ));
    }
}
