package com.smalik.mysqlbroker;

import java.util.Optional;
import java.util.UUID;

import com.smalik.mysqlbroker.data.PlatformService;
import com.smalik.mysqlbroker.data.PlatformServiceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Service
public class ServiceInstanceService implements org.springframework.cloud.servicebroker.service.ServiceInstanceService {

  private final Logger logger = LoggerFactory.getLogger(ServiceInstanceService.class);
  private final PlatformServiceRepository repository;

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {

    String rootPassword = UUID.randomUUID().toString();
    repository.save(PlatformService.builder()
      .id(request.getServiceInstanceId())
      .planDefinitionId(request.getPlanId())
      .serviceDefinitionId(request.getServiceDefinitionId())
      .property("rootPassword", rootPassword)
      .build());

    String template = getClass().getResourceAsStream("classpath:application.yml").toString();
    logger.info(template);

		return Mono.just(CreateServiceInstanceResponse.builder()
        .dashboardUrl(request.getServiceInstanceId())
        .instanceExisted(false)
				.async(false)
				.build());
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
    String serviceInstanceId = request.getServiceInstanceId();
    repository.deleteById(serviceInstanceId);

		return Mono.just(DeleteServiceInstanceResponse.builder()
        .async(false)
				.build());
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
    String serviceInstanceId = request.getServiceInstanceId();
    Optional<PlatformService> svc = repository.findById(serviceInstanceId);

    return Mono.just(GetServiceInstanceResponse.builder()
        .dashboardUrl(serviceInstanceId)
        .planId(svc.get().getPlanDefinitionId())
        .serviceDefinitionId(svc.get().getServiceDefinitionId())
        .parameters(svc.get().getProperties())
				.build());
  }
}