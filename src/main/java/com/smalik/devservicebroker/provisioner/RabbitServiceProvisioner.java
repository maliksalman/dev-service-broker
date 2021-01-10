package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RabbitServiceProvisioner implements ServiceProvisioner {

    private final ResourceLoader resourceLoader;
    private final ProcessRunner runner;
    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = String.format("k-%s.service-broker.svc.cluster.local", serviceId);
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

        Resource resourceFile = resourceLoader.getResource("classpath:definitions/k-rabbit-default.yml");
        String yml = StreamUtils.copyToString(resourceFile.getInputStream(), Charset.defaultCharset());
        String newYml = yml
                .replaceAll("\\{name\\}", String.format("k-%s", serviceId))
                .replaceAll("\\{namespace\\}", "service-broker")
                .replaceAll("\\{rootusername\\}", "root")
                .replaceAll("\\{rootpassword\\}", password)
                .replaceAll("\\{port\\}", String.valueOf(port))
                .replaceAll("\\{adminport\\}", String.valueOf(adminPort))
                .replaceAll("\\{vhost\\}", vhost);

        File tempFile = File.createTempFile(serviceId, ".yml");
        FileCopyUtils.copy(newYml.getBytes(), tempFile);
        runner.runProcess("kubectl", "apply", "-f", tempFile.getAbsolutePath());

        return data;
    }

    @SneakyThrows
    public PlatformService deletePlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        Optional<PlatformService> data = serviceRepository.findById(serviceId);
        if (data.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: ServiceId=" + serviceId);
        }

        PlatformService svc = data.get();
        String k8sId = String.format("k-%s", svc.getId());

        runner.runProcess("kubectl", "delete", "-n", "service-broker",
                "statefulset/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId,
                "service/" + k8sId + "-admin",
                "configmap/" + k8sId);

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

        runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
                "rabbitmqctl", "add_user", username, password);
        runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
                "rabbitmqctl", "set_user_tags", username, "monitoring");
        runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
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
            String username = binding.get().getCredentials().getUsername();
            runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
                    "rabbitmqctl", "delete_user", username);
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
