package com.smalik.devservicebroker;

import com.smalik.devservicebroker.data.PlatformService;
import com.smalik.devservicebroker.provisioners.PlatformServiceProvisioner;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceInstanceService implements org.springframework.cloud.servicebroker.service.ServiceInstanceService {

    private final PlatformServiceProvisioner provisioner;

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(
            CreateServiceInstanceRequest request) {

        Optional<PlatformService> optionalPlatformService = provisioner.findPlatformService(request.getServiceInstanceId());

        PlatformService platformService = null;
        boolean existingService = false;

        if (optionalPlatformService.isPresent()) {
            platformService = optionalPlatformService.get();
            existingService = true;
        } else {
            platformService = provisioner.provisionPlatformService(
                    request.getServiceInstanceId(),
                    request.getPlanId(),
                    request.getServiceDefinitionId());
        }

        return Mono.just(CreateServiceInstanceResponse.builder()
                .dashboardUrl(provisioner.getDashboardUrl(platformService))
                .instanceExisted(existingService)
                .async(false)
                .build());
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {

        String id = request.getServiceInstanceId();
        return provisioner.findPlatformService(id)
                .map(service -> {
                    provisioner.deletePlatformService(
                            request.getServiceInstanceId(),
                            request.getPlanId(),
                            request.getServiceDefinitionId());
                    return Mono.just(DeleteServiceInstanceResponse.builder()
                            .async(false)
                            .build());
                })
                .orElseThrow(() -> new ServiceInstanceDoesNotExistException(id));
    }

    @Override
    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
        String id = request.getServiceInstanceId();
        return provisioner.findPlatformService(id)
                .map(service -> Mono.just(GetServiceInstanceResponse.builder()
                        .dashboardUrl(provisioner.getDashboardUrl(service))
                        .planId(service.getPlanDefinitionId())
                        .serviceDefinitionId(service.getServiceDefinitionId())
                        .parameters(new HashMap<>(service.getProperties()))
                        .build()))
                .orElseThrow(() -> new ServiceInstanceDoesNotExistException(id));
    }
}