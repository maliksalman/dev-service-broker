package com.smalik.devservicebroker;

import com.smalik.devservicebroker.data.PlatformService;
import com.smalik.devservicebroker.data.PlatformServiceBinding;
import com.smalik.devservicebroker.data.PlatformServiceBindingRepository;
import com.smalik.devservicebroker.data.PlatformServiceRepository;
import com.smalik.devservicebroker.provisioners.PlatformServiceProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Profile("debug")
@RequestMapping("/debug")
public class DebugController {

    private final PlatformServiceProvisioner provisioner;
    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;

    @GetMapping("/services")
    public List<PlatformService> findAllServices() {
        return serviceRepository.findAll();
    }

    @GetMapping("/bindings")
    public List<PlatformServiceBinding> findAllBindings() {
        return serviceBindingRepository.findAll();
    }

    @GetMapping("/plans/{planId}/services/{serviceId}")
    public PlatformService findService(@PathVariable("planId") String planId, @PathVariable("serviceId") String serviceId) {
        return provisioner.findPlatformService(serviceId)
                .orElseThrow(NotFoundException::new);
    }

    @PutMapping("/plans/{planId}/services/{serviceId}")
    public PlatformService createService(@PathVariable("planId") String planId, @PathVariable("serviceId") String serviceId) throws Exception {
        return provisioner.provisionPlatformService(serviceId, planId, "service-id");
    }

    @DeleteMapping("/plans/{planId}/services/{serviceId}")
    public PlatformService deleteService(@PathVariable("planId") String planId, @PathVariable("serviceId") String serviceId) throws Exception {
        return provisioner.deletePlatformService(serviceId, planId, "service-id");
    }

    @GetMapping("/plans/{planId}/services/{serviceId}/bindings/{bindingId}")
    public PlatformServiceBinding findBinding(
            @PathVariable("planId") String planId,
            @PathVariable("serviceId") String serviceId,
            @PathVariable("bindingId") String bindingId) throws Exception {
        return provisioner.findPlatformServiceBinding(serviceId, bindingId)
                .orElseThrow(NotFoundException::new);
    }

    @PutMapping("/plans/{planId}/services/{serviceId}/bindings/{bindingId}")
    public PlatformServiceBinding createBinding(
            @PathVariable("planId") String planId,
            @PathVariable("serviceId") String serviceId,
            @PathVariable("bindingId") String bindingId) throws Exception {
        return provisioner.provisionPlatformServiceBinding(serviceId, bindingId, planId);
    }

    @DeleteMapping("/plans/{planId}/services/{serviceId}/bindings/{bindingId}")
    public PlatformServiceBinding deleteBinding(
            @PathVariable("planId") String planId,
            @PathVariable("serviceId") String serviceId,
            @PathVariable("bindingId") String bindingId) throws Exception {
        return provisioner.deletePlatformServiceBinding(serviceId, bindingId, planId);
    }

    @ControllerAdvice
    static class Advice {
        @ResponseStatus(HttpStatus.NOT_FOUND)
        @ExceptionHandler(NotFoundException.class)
        public void handleNotFound() { }
    }

    static class NotFoundException extends RuntimeException {
        public NotFoundException() { }
    }
}