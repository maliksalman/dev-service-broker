package com.smalik.mysqlbroker;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import com.smalik.mysqlbroker.data.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
public class TryController {

  private ResourceLoader resourceLoader;
  private ProcessRunner runner;
  private PlatformServiceRepository serviceRepository;
  private PlatformServiceBindingRepository serviceBindingRepository;

  @GetMapping("/try/{name}")
  public Mono<PlatformService> service(@PathVariable("name") String name) {
    return Mono.justOrEmpty(serviceRepository.findById(name));
  }

  @PutMapping("/try/{name}")
  public Mono<PlatformService> create(@PathVariable("name") String name) throws Exception {

    String password = UUID.randomUUID().toString();
    PlatformService data = PlatformService.builder()
            .id(name)
            .planDefinitionId("plan-id")
            .serviceDefinitionId("service-id")
            .property("rootPassword", password)
            .build();
    serviceRepository.save(data);

    Resource resourceFile = resourceLoader.getResource("classpath:definitions/mysql-db-small.yml");
    String yml = StreamUtils.copyToString(resourceFile.getInputStream(), Charset.defaultCharset());
    String newYml = yml
    .replaceAll("\\{name\\}", name)
    .replaceAll("\\{namespace\\}", "service-broker")
    .replaceAll("\\{rootpassword\\}", password);

    File tempFile = File.createTempFile(name, ".yml");
    FileCopyUtils.copy(newYml.getBytes(), tempFile);
    runner.runProcess("kubectl", "apply", "-f", tempFile.getAbsolutePath());

    return Mono.just(data);
  }

  @DeleteMapping("/try/{name}")
  public Mono<PlatformService> delete(@PathVariable("name") String name) throws Exception {

    Optional<PlatformService> data = serviceRepository.findById(name);
    if (data.isPresent()) {
      PlatformService svc = data.get();
      runner.runProcess("kubectl", "delete", "-n", "service-broker",
            "statefulset/" + svc.getId(),
            "pvc/data-" + svc.getId() + "-0",
            "service/" + svc.getId());

      serviceRepository.delete(svc);
    }

    return Mono.justOrEmpty(data);
  }

  @PutMapping("/try/{serviceId}/bindings/{bindingId}")
  public Mono<PlatformServiceBinding> createBinding(
    @PathVariable("serviceId") String serviceId,
    @PathVariable("bindingId") String bindingId) throws Exception {

    Optional<PlatformService> svc = serviceRepository.findById(serviceId);
    PlatformServiceBinding binding = null;

    if (svc.isPresent()) {

      String host = serviceId + ".service-broker.svc.cluster.local";
      String schema = "db";
      int port = 3306;

      binding = PlatformServiceBinding.builder()
        .id(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build())
        .credentials(PlatformServiceBindingCredentials.builder()
                .username(bindingId.replaceAll("-", "_"))
                .password(UUID.randomUUID().toString())
                .host(host)
                .port(port)
                .property("schema", schema)
                .property("jdbcUrl", "jdbc:mysql://" + host + ":" + port + "/" + schema)
                .build())
        .build();
      serviceBindingRepository.save(binding);
      
      runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
        "mysql", "-p" + svc.get().getProperties().get("rootPassword"), "-e", 
        "CREATE USER '" + binding.getCredentials().getUsername() + "' IDENTIFIED BY '" + binding.getCredentials().getPassword() + "'");
      runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
        "mysql", "-p" + svc.get().getProperties().get("rootPassword"), "-e", 
        "GRANT ALL PRIVILEGES ON db.* TO '" + binding.getCredentials().getUsername() + "'@'%'");
      runner.runProcess("kubectl", "exec", serviceId + "-0", "-n", "service-broker", "--",
        "mysql", "-p" + svc.get().getProperties().get("rootPassword"), "-e", 
        "FLUSH PRIVILEGES");
    }

    return Mono.justOrEmpty(binding);
  }

  @GetMapping("/try/{serviceId}/bindings/{bindingId}")
  public Mono<PlatformServiceBinding> getBinding(
    @PathVariable("serviceId") String platformServiceId,
    @PathVariable("bindingId") String id) throws Exception {
  
      return Mono.justOrEmpty(serviceBindingRepository.findById(PlatformServiceBindingId.builder()
              .serviceId(platformServiceId)
              .bindingId(id)
              .build()));
  }
}