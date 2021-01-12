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
public class MysqlServiceProvisioner implements ServiceProvisioner {

    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;
    private final KubernetesHelper kubernetesHelper;

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = kubernetesHelper.getKubernetesServiceFQDN(serviceId);
        String schema = "db";
        int port = 3306;
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
                .property("schema", schema)
                .build();
        serviceRepository.save(data);

        kubernetesHelper.applyKubernetesTemplate(
                "k-mysql-default.yml",
                serviceId,
                Map.of(
                        "port", String.valueOf(port),
                        "schema", schema,
                        "rootpassword", password));

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
                "service/" + k8sId);

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
        
        PlatformService service = optionalService.get();
        PlatformServiceBinding binding = PlatformServiceBinding.builder()
                .id(PlatformServiceBindingId.builder()
                        .serviceId(serviceId)
                        .bindingId(bindingId)
                        .build())
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder()
                        .username(StringUtils.remove(UUID.randomUUID().toString(), "-").substring(0,30))
                        .password(UUID.randomUUID().toString())
                        .build())
                .properties(new HashMap<>(service.getProperties()))
                .build();
        serviceBindingRepository.save(binding);

        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "CREATE USER '" + binding.getCredentials().getUsername() + "' IDENTIFIED BY '" + binding.getCredentials().getPassword() + "'");
        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "GRANT ALL PRIVILEGES ON db.* TO '" + binding.getCredentials().getUsername() + "'@'%'");
        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "FLUSH PRIVILEGES");

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
            if (svc.isPresent()) {
                kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                        "mysql", "-p" + svc.get().getCredentials().getPassword(), "-e",
                        "DROP USER '" + binding.get().getCredentials().getUsername() + "'@'%'");
                kubernetesHelper.runKubernetesExecOnPodCommand(serviceId,
                        "mysql", "-p" + svc.get().getCredentials().getPassword(), "-e",
                        "FLUSH PRIVILEGES");
            } else {
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
        map.put("username", binding.getCredentials().getUsername());
        map.put("password", binding.getCredentials().getPassword());
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));

        String uri = String.format("mysql://%s:%s@%s:%s/%s",
                binding.getCredentials().getUsername(),
                binding.getCredentials().getPassword(),
                binding.getProperties().get("host"),
                binding.getProperties().get("port"),
                binding.getProperties().get("schema"));
        map.put("uri", uri);
        map.put("mysqlUri", uri);

        map.put("jdbcUrl", String.format("jdbc:%s", uri));

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
