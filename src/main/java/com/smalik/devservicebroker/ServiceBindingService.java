package com.smalik.devservicebroker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalik.devservicebroker.data.PlatformServiceBinding;
import com.smalik.devservicebroker.provisioner.PlatformServiceProvisioner;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.servicebroker.model.binding.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceBindingService implements ServiceInstanceBindingService {

    private final PlatformServiceProvisioner provisioner;

    @Override
    @SneakyThrows
    public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {

        String serviceId = request.getServiceInstanceId();
        String bindingId = request.getBindingId();
        log.info(new ObjectMapper().writeValueAsString(request.getBindResource()));

        PlatformServiceBinding binding = null;
        boolean bindingExists = false;

        Optional<PlatformServiceBinding> optionalBinding = provisioner.findPlatformServiceBinding(serviceId, bindingId);
        if (optionalBinding.isPresent()) {
            binding = optionalBinding.get();
            bindingExists = true;
        } else {
            binding = provisioner.provisionPlatformServiceBinding(
                    serviceId,
                    bindingId,
                    request.getPlanId());
        }

        return Mono.just(CreateServiceInstanceAppBindingResponse.builder()
                .credentials(provisioner.getCredentials(binding))
                .endpoints(provisioner.getEndpoints(binding))
                .bindingExisted(bindingExists)
                .async(false)
                .build());
    }

    @Override
    public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest request) {

        String serviceId = request.getServiceInstanceId();
        String bindingId = request.getBindingId();

        provisioner.deletePlatformServiceBinding(serviceId, bindingId, request.getPlanId());
        return Mono.just(DeleteServiceInstanceBindingResponse.builder()
                .async(false)
                .build());
    }

    @Override
    public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {

        String serviceId = request.getServiceInstanceId();
        String bindingId = request.getBindingId();

        Optional<PlatformServiceBinding> optionalBinding = provisioner.findPlatformServiceBinding(serviceId, bindingId);
        if (optionalBinding.isPresent()) {
            PlatformServiceBinding binding = optionalBinding.get();
            GetServiceInstanceBindingResponse response = GetServiceInstanceAppBindingResponse.builder()
                    .credentials(provisioner.getCredentials(binding))
                    .endpoints(provisioner.getEndpoints(binding))
                    .build();



            return Mono.just(response);
        } else {
            return Mono.empty();
        }
    }

    private String getJdbcUrl(PlatformServiceBinding binding) {
        return "jdbc:mysql://" + binding.getProperties().get("host") + ":" + binding.getProperties().get("port") + "/" + binding.getProperties().get("schema");
    }
}