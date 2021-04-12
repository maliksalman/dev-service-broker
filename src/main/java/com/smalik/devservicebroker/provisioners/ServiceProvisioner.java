package com.smalik.devservicebroker.provisioners;

import com.smalik.devservicebroker.data.*;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@AllArgsConstructor
public abstract class ServiceProvisioner {

    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;
    private final KubernetesHelper kubernetesHelper;
    private final String planName;

    public String getPlanName() {
        return planName;
    }

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = kubernetesHelper.getKubernetesServiceFQDN(serviceId);
        Map<String, String> serviceProperties = onProvisionPlatformServiceProperties(host);
        Credentials credentials = onProvisionPlatformServiceRootCredentials();

        PlatformService data = PlatformService.builder()
                .id(serviceId)
                .planDefinitionId(planDefinitionId)
                .credentials(credentials)
                .properties(serviceProperties)
                .build();
        serviceRepository.save(data);

        kubernetesHelper.applyKubernetesTemplate(
                getPlanName() + ".yml",
                serviceId,
                onProvisionPlatformServiceKubernetesTemplateProperties(credentials));

        return data;
    }

    protected abstract Credentials onProvisionPlatformServiceRootCredentials();
    protected abstract Map<String, String> onProvisionPlatformServiceProperties(String serviceId);
    protected abstract Map<String, String> onProvisionPlatformServiceKubernetesTemplateProperties(Credentials root);

    @SneakyThrows
    public PlatformService deletePlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        Optional<PlatformService> service = serviceRepository.findById(serviceId);
        if (service.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: ServiceId=" + serviceId);
        }

        String k8sId = kubernetesHelper.getKubernetesServiceName(serviceId);
        String[] resourceNamesToDelete = onDeletePlatformServiceKubernetesResourceNames(service.get(), k8sId);
        kubernetesHelper.runKubernetesDeleteCommand(resourceNamesToDelete);

        PlatformService svc = service.get();
        serviceRepository.delete(svc);

        return svc;
    }

    protected abstract String[] onDeletePlatformServiceKubernetesResourceNames(PlatformService service, String k8sId);

    @SneakyThrows
    public PlatformServiceBinding provisionPlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId, Map<String, Object> context, String platform) {
        Optional<PlatformService> optionalService = serviceRepository.findById(serviceId);
        if (optionalService.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }

        PlatformService service = optionalService.get();
        PlatformServiceBinding binding = PlatformServiceBinding.builder()
                .id(PlatformServiceBindingId.builder()
                        .serviceId(service.getId())
                        .bindingId(bindingId)
                        .build())
                .planDefinitionId(planDefinitionId)
                .credentials(onProvisionPlatformServiceBindingCredentials(service))
                .properties(new HashMap<>(service.getProperties()))
                .context(context)
                .platform(platform)
                .created(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        onProvisionPlatformServiceBinding(service, binding);
        return serviceBindingRepository.save(binding);
    }

    protected abstract Credentials onProvisionPlatformServiceBindingCredentials(PlatformService service);
    protected void onProvisionPlatformServiceBinding(PlatformService service, PlatformServiceBinding binding) { }

    @SneakyThrows
    public PlatformServiceBinding deletePlatformServiceBinding(
            String serviceId, String bindingId, String planDefinitionId) {

        Optional<PlatformServiceBinding> binding = serviceBindingRepository.findById(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build());
        if (binding.isPresent()) {
            Optional<PlatformService> svc = serviceRepository.findById(serviceId);
            if (svc.isPresent()) {
                PlatformServiceBinding serviceBinding = binding.get();
                onDeletePlatformServiceBinding(svc.get(), serviceBinding);
                serviceBindingRepository.delete(serviceBinding);
                return serviceBinding;
            } else {
                throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
            }
        } else {
            throw new RuntimeException("Can't find the binding: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }
    }

    protected void onDeletePlatformServiceBinding(
            PlatformService service, PlatformServiceBinding serviceBinding) { }

    public String getDashboardUrl(
            PlatformService service) { return null; };

    public abstract Map<String, Object> getCredentials(
            PlatformServiceBinding binding);


    public List<Endpoint> getEndpoints(PlatformServiceBinding binding) {
        return Arrays.asList(new Endpoint(
                binding.getProperties().get("host"),
                Arrays.asList(binding.getProperties().get("port")),
                Endpoint.Protocol.TCP
        ));
    }

    protected void runKubernetesExecOnPodCommand(String serviceId, String... args) {
        kubernetesHelper.runKubernetesExecOnPodCommand(serviceId, args);
    }
}
