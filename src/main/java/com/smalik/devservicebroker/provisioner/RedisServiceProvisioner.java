package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
public class RedisServiceProvisioner implements ServiceProvisioner {

    private final ResourceLoader resourceLoader;
    private final ProcessRunner runner;
    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = String.format("k-%s.service-broker.svc.cluster.local", serviceId);;
        int port = 6379;
        String password = UUID.randomUUID().toString();

        PlatformService data = PlatformService.builder()
                .id(serviceId)
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder()
                        .password(password)
                        .build())
                .property("host", host)
                .property("port", port)
                .build();
        serviceRepository.save(data);

        Resource resourceFile = resourceLoader.getResource("classpath:definitions/k-redis-default.yml");
        String yml = StreamUtils.copyToString(resourceFile.getInputStream(), Charset.defaultCharset());
        String newYml = yml
                .replaceAll("\\{name\\}", String.format("k-%s", serviceId))
                .replaceAll("\\{namespace\\}", "service-broker")
                .replaceAll("\\{port\\}", String.valueOf(port))
                .replaceAll("\\{rootpassword\\}", password);

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
                "configmap/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId);

        serviceRepository.delete(svc);
        return svc;
    }
    
    @SneakyThrows
    public PlatformServiceBinding provisionPlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        Optional<PlatformService> optionalService = serviceRepository.findById(serviceId);
        if (optionalService.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }
        
        PlatformService service = optionalService.get();
        PlatformServiceBinding binding = PlatformServiceBinding.builder()
                .id(PlatformServiceBindingId.builder()
                        .serviceId(serviceId)
                        .bindingId(bindingId)
                        .build())
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder()
                        .password(service.getCredentials().getPassword())
                        .build())
                .properties(new HashMap<>(service.getProperties()))
                .build();
        serviceBindingRepository.save(binding);

        return binding;
    }

    @SneakyThrows
    public PlatformServiceBinding deletePlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {

        Optional<PlatformServiceBinding> binding = serviceBindingRepository.findById(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build());
        if (binding.isPresent()) {

            Optional<PlatformService> svc = serviceRepository.findById(serviceId);
            if (svc.isEmpty()) {
                throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
            }
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
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));

        String uri = String.format("redis://%s@%s:%s",
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"));

        map.put("uri", uri);
        map.put("redisUri", uri);

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
