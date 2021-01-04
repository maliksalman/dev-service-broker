package com.smalik.mysqlbroker;

import java.util.Optional;

import com.smalik.mysqlbroker.data.PlatformService;

import com.smalik.mysqlbroker.provisioner.PlatformServiceProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Slf4j
@Service
public class ServiceInstanceService implements org.springframework.cloud.servicebroker.service.ServiceInstanceService {

    private PlatformServiceProvisioner provisioner;

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
                .dashboardUrl(platformService.getDashboardUrl())
                .instanceExisted(existingService)
                .async(false)
                .build());
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {

        provisioner.deletePlatformService(
                request.getServiceInstanceId(),
                request.getPlanId(),
                request.getServiceDefinitionId());
        return Mono.just(DeleteServiceInstanceResponse.builder()
                .async(false)
                .build());
    }

    @Override
    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {

        Optional<PlatformService> optionalPlatformService = provisioner.findPlatformService(request.getServiceInstanceId());
        if (optionalPlatformService.isPresent()) {
            PlatformService platformService = optionalPlatformService.get();
            return Mono.just(GetServiceInstanceResponse.builder()
                    .dashboardUrl(platformService.getDashboardUrl())
                    .planId(platformService.getPlanDefinitionId())
                    .serviceDefinitionId(platformService.getServiceDefinitionId())
                    .parameters(platformService.getProperties())
                    .build());
        } else {
        	return Mono.empty();
		}
    }
}