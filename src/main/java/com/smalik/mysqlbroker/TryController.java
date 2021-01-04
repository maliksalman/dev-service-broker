package com.smalik.mysqlbroker;

import com.smalik.mysqlbroker.data.PlatformService;
import com.smalik.mysqlbroker.data.PlatformServiceBinding;
import com.smalik.mysqlbroker.provisioner.PlatformServiceProvisioner;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
public class TryController {

  private PlatformServiceProvisioner provisioner;

  @GetMapping("/try/{serviceId}")
  public Mono<PlatformService> findService(@PathVariable("serviceId") String serviceId) {
    return Mono.justOrEmpty(provisioner.findPlatformService(serviceId));
  }

  @PutMapping("/try/{serviceId}")
  public Mono<PlatformService> createService(@PathVariable("serviceId") String serviceId) throws Exception {
    return Mono.justOrEmpty(provisioner.provisionPlatformService(serviceId, "plan-id", "service-id"));
  }

  @DeleteMapping("/try/{serviceId}")
  public Mono<PlatformService> deleteService(@PathVariable("serviceId") String serviceId) throws Exception {
    return Mono.justOrEmpty(provisioner.deletePlatformService(serviceId, "plan-id", "service-id"));
  }

  @GetMapping("/try/{serviceId}/bindings/{bindingId}")
  public Mono<PlatformServiceBinding> findBinding(
          @PathVariable("serviceId") String serviceId,
          @PathVariable("bindingId") String bindingId) throws Exception {
    return Mono.justOrEmpty(provisioner.findPlatformServiceBinding(serviceId, bindingId));
  }

  @PutMapping("/try/{serviceId}/bindings/{bindingId}")
  public Mono<PlatformServiceBinding> createBinding(
          @PathVariable("serviceId") String serviceId,
          @PathVariable("bindingId") String bindingId) throws Exception {
    return Mono.justOrEmpty(provisioner.provisionPlatformServiceBinding(serviceId, bindingId, "plan-id"));
  }

  @DeleteMapping("/try/{serviceId}/bindings/{bindingId}")
  public Mono<PlatformServiceBinding> deleteBinding(
          @PathVariable("serviceId") String serviceId,
          @PathVariable("bindingId") String bindingId) throws Exception {
    return Mono.justOrEmpty(provisioner.deletePlatformServiceBinding(serviceId, bindingId, "plan-id"));
  }
}