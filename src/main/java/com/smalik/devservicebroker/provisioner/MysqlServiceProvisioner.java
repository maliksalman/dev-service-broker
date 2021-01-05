package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MysqlServiceProvisioner implements ServiceProvisioner {

    private final ResourceLoader resourceLoader;
    private final ProcessRunner runner;
    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = serviceId + ".service-broker.svc.cluster.local";
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

        Resource resourceFile = resourceLoader.getResource("classpath:definitions/k-mysql-default.yml");
        String yml = StreamUtils.copyToString(resourceFile.getInputStream(), Charset.defaultCharset());
        String newYml = yml
                .replaceAll("\\{name\\}", serviceId)
                .replaceAll("\\{namespace\\}", "service-broker")
                .replaceAll("\\{port\\}", String.valueOf(port))
                .replaceAll("\\{schema\\}", schema)
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
        runner.runProcess("kubectl", "delete", "-n", "service-broker",
                "statefulset/" + svc.getId(),
                "pvc/data-" + svc.getId() + "-0",
                "service/" + svc.getId());

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
                        .username(bindingId.replaceAll("-", "_"))
                        .password(UUID.randomUUID().toString())
                        .build())
                .properties(new HashMap<>(service.getProperties()))
                .build();
        serviceBindingRepository.save(binding);

        runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "CREATE USER '" + binding.getCredentials().getUsername() + "' IDENTIFIED BY '" + binding.getCredentials().getPassword() + "'");
        runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
                "mysql", "-p" + service.getCredentials().getPassword(), "-e",
                "GRANT ALL PRIVILEGES ON db.* TO '" + binding.getCredentials().getUsername() + "'@'%'");
        runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
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
                runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
                        "mysql", "-p" + svc.get().getCredentials().getPassword(), "-e",
                        "DROP USER '" + binding.get().getCredentials().getUsername() + "'@'%'");
                runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
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
}