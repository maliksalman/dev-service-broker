package com.smalik.devservicebroker;

import com.smalik.devservicebroker.data.PlatformService;
import com.smalik.devservicebroker.provisioners.PlatformServiceProvisioner;
import lombok.RequiredArgsConstructor;
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

        Optional<PlatformService> optionalPlatformService = provisioner.findPlatformService(request.getServiceInstanceId());
        if (optionalPlatformService.isPresent()) {
            provisioner.deletePlatformService(
                    request.getServiceInstanceId(),
                    request.getPlanId(),
                    request.getServiceDefinitionId());
            return Mono.just(DeleteServiceInstanceResponse.builder()
                    .async(false)
                    .build());
        }

        return Mono.empty();
    }

    @Override
    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {

        Optional<PlatformService> optionalPlatformService = provisioner.findPlatformService(request.getServiceInstanceId());
        if (optionalPlatformService.isPresent()) {
            PlatformService platformService = optionalPlatformService.get();
            return Mono.just(GetServiceInstanceResponse.builder()
                    .dashboardUrl(provisioner.getDashboardUrl(platformService))
                    .planId(platformService.getPlanDefinitionId())
                    .serviceDefinitionId(platformService.getServiceDefinitionId())
                    .parameters(new HashMap<>(platformService.getProperties()))
                    .build());
        }

        return Mono.empty();
    }
}