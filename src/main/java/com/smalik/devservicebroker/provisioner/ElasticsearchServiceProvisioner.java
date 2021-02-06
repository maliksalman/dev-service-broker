package com.smalik.devservicebroker.provisioner;

import com.smalik.devservicebroker.data.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ElasticsearchServiceProvisioner implements ServiceProvisioner {

    private final PlatformServiceRepository serviceRepository;
    private final PlatformServiceBindingRepository serviceBindingRepository;
    private final KubernetesHelper kubernetesHelper;

    public String getDefaultPlanName() {
        return "k-elasticsearch-default";
    }

    @SneakyThrows
    public PlatformService provisionPlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {

        String host = kubernetesHelper.getKubernetesServiceFQDN(serviceId);
        int port = 9200;

        PlatformService data = PlatformService.builder()
                .id(serviceId)
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder().build())
                .property("host", host)
                .property("port", port)
                .build();
        serviceRepository.save(data);

        kubernetesHelper.applyKubernetesTemplate(
                getDefaultPlanName() + ".yml",
                serviceId,
                Map.of("port", String.valueOf(port)));

        return data;
    }

    @SneakyThrows
    public PlatformService deletePlatformService(String serviceId, String planDefinitionId, String serviceDefinitionId) {
        Optional<PlatformService> data = serviceRepository.findById(serviceId);
        if (data.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: ServiceId=" + serviceId);
        }

        String k8sId = kubernetesHelper.getKubernetesServiceName(serviceId);
        kubernetesHelper.runKubernetesDeleteCommand(
                "statefulset/" + k8sId,
                "pvc/data-" + k8sId + "-0",
                "service/" + k8sId);

        PlatformService svc = data.get();
        serviceRepository.delete(svc);

        return svc;
    }
    
    @SneakyThrows
    public PlatformServiceBinding provisionPlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {
        Optional<PlatformService> optionalService = serviceRepository.findById(serviceId);
        if (optionalService.isEmpty()) {
            throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }
        
        PlatformService service = optionalService.get();
        PlatformServiceBinding binding = PlatformServiceBinding.builder()
                .id(PlatformServiceBindingId.builder()
                        .serviceId(serviceId)
                        .bindingId(bindingId)
                        .build())
                .planDefinitionId(planDefinitionId)
                .credentials(Credentials.builder().build())
                .properties(new HashMap<>(service.getProperties()))
                .build();
        serviceBindingRepository.save(binding);

        return binding;
    }

    @SneakyThrows
    public PlatformServiceBinding deletePlatformServiceBinding(String serviceId, String bindingId, String planDefinitionId) {

        Optional<PlatformServiceBinding> binding = serviceBindingRepository.findById(PlatformServiceBindingId.builder()
                .serviceId(serviceId)
                .bindingId(bindingId)
                .build());
        if (binding.isPresent()) {

            Optional<PlatformService> svc = serviceRepository.findById(serviceId);
            if (svc.isEmpty()) {
                throw new RuntimeException("Can't find the service instance: BindingId=" + bindingId + ", ServiceId=" + serviceId);
            }
        } else {
            throw new RuntimeException("Can't find the binding: BindingId=" + bindingId + ", ServiceId=" + serviceId);
        }

        return binding.get();
    }

    public String getDashboardUrl(PlatformService service) {
        return null;
    }

    public Map<String, Object> getCredentials(PlatformServiceBinding binding) {
        Map<String, Object> map = new HashMap<>();
        map.put("host", binding.getProperties().get("host"));
        map.put("port", binding.getProperties().get("port"));

        String uri = String.format("http://%s:%s",
                binding.getProperties().get("host"),
                binding.getProperties().get("port"));

        map.put("uri", uri);
        map.put("elasticsearchUri", uri);

        return map;
    }

    public List<Endpoint> getEndpoints(PlatformServiceBinding binding) {
        return Arrays.asList(new Endpoint(
                String.valueOf(binding.getProperties().get("host")),
                Arrays.asList(String.valueOf(binding.getProperties().get("port"))),
                Endpoint.Protocol.TCP
        ));
    }
}
