package com.smalik.devservicebroker;

import java.util.HashMap;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smalik.devservicebroker.data.PlatformServiceBinding;
import com.smalik.devservicebroker.provisioners.PlatformServiceProvisioner;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceBindingService implements ServiceInstanceBindingService {

    private final PlatformServiceProvisioner provisioner;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @SneakyThrows
    public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(CreateServiceInstanceBindingRequest request) {

        String serviceId = request.getServiceInstanceId();
        String bindingId = request.getBindingId();
        log.debug(mapper.writeValueAsString(request.getBindResource()));

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
                    request.getPlanId(),
                    request.getContext().getProperties(),
                    request.getContext().getPlatform());
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

        Optional<PlatformServiceBinding> platformServiceBinding = provisioner.findPlatformServiceBinding(serviceId, bindingId);
        if (platformServiceBinding.isPresent()) {
            provisioner.deletePlatformServiceBinding(serviceId, bindingId, request.getPlanId());
            return Mono.just(DeleteServiceInstanceBindingResponse.builder()
                    .async(false)
                    .build());
        }

        return Mono.empty();
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
                    .parameters(new HashMap<>(binding.getProperties()))
                    .build();

            return Mono.just(response);
        }

        return Mono.empty();
    }
}