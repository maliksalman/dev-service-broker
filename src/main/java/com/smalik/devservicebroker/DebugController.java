package com.smalik.devservicebroker;

import com.smalik.devservicebroker.data.PlatformService;
import com.smalik.devservicebroker.data.PlatformServiceBinding;
import com.smalik.devservicebroker.provisioners.PlatformServiceProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Profile("debug")
public class DebugController {

    private final PlatformServiceProvisioner provisioner;

    @GetMapping("/plans/{planId}/services/{serviceId}")
    public Mono<PlatformService> findService(@PathVariable("planId") String planId, @PathVariable("serviceId") String serviceId) {
        return Mono.justOrEmpty(provisioner.findPlatformService(serviceId));
    }

    @PutMapping("/plans/{planId}/services/{serviceId}")
    public Mono<PlatformService> createService(@PathVariable("planId") String planId, @PathVariable("serviceId") String serviceId) throws Exception {
        return Mono.justOrEmpty(provisioner.provisionPlatformService(serviceId, planId, "service-id"));
    }

    @DeleteMapping("/plans/{planId}/services/{serviceId}")
    public Mono<PlatformService> deleteService(@PathVariable("planId") String planId, @PathVariable("serviceId") String serviceId) throws Exception {
        return Mono.justOrEmpty(provisioner.deletePlatformService(serviceId, planId, "service-id"));
    }

    @GetMapping("/plans/{planId}/services/{serviceId}/bindings/{bindingId}")
    public Mono<PlatformServiceBinding> findBinding(
            @PathVariable("planId") String planId,
            @PathVariable("serviceId") String serviceId,
            @PathVariable("bindingId") String bindingId) throws Exception {
        return Mono.justOrEmpty(provisioner.findPlatformServiceBinding(serviceId, bindingId));
    }

    @PutMapping("/plans/{planId}/services/{serviceId}/bindings/{bindingId}")
    public Mono<PlatformServiceBinding> createBinding(
            @PathVariable("planId") String planId,
            @PathVariable("serviceId") String serviceId,
            @PathVariable("bindingId") String bindingId) throws Exception {
        return Mono.justOrEmpty(provisioner.provisionPlatformServiceBinding(serviceId, bindingId, planId));
    }

    @DeleteMapping("/plans/{planId}/services/{serviceId}/bindings/{bindingId}")
    public Mono<PlatformServiceBinding> deleteBinding(
            @PathVariable("planId") String planId,
            @PathVariable("serviceId") String serviceId,
            @PathVariable("bindingId") String bindingId) throws Exception {
        return Mono.justOrEmpty(provisioner.deletePlatformServiceBinding(serviceId, bindingId, planId));
    }
}