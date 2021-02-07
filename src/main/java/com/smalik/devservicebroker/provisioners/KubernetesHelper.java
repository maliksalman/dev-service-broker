package com.smalik.devservicebroker.provisioners;

import com.smalik.devservicebroker.ServiceBrokerConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KubernetesHelper {

    private final ResourceLoader resourceLoader;
    private final ProcessRunner runner;
    private final ServiceBrokerConfig config;

    public String getKubernetesServiceName(String serviceId) {
        return String.format("k-%s", serviceId);
    }
    public String getKubernetesServiceFQDN(String serviceId) {
        return String.format("%s.%s.svc.%s",
                getKubernetesServiceName(serviceId),
                config.getNamespace(),
                config.getClusterDomain());
    }

    @SneakyThrows
    public void applyKubernetesTemplate(String definitionTemplateName, String serviceId, Map<String, String> substitutions) {
        Resource resourceFile = resourceLoader.getResource("classpath:definitions/" + definitionTemplateName);
        String yml = StreamUtils.copyToString(resourceFile.getInputStream(), Charset.defaultCharset())
                .replaceAll("\\{name\\}", getKubernetesServiceName(serviceId))
                .replaceAll("\\{namespace\\}", config.getNamespace());

        for (String k : substitutions.keySet()) {
            yml = yml.replaceAll("\\{" + k + "\\}", substitutions.get(k));
        }

        File tempFile = File.createTempFile(serviceId, ".yml");
        FileCopyUtils.copy(yml.getBytes(), tempFile);
        runner.runProcess("kubectl", "apply", "-f", tempFile.getAbsolutePath());
    }

    @SneakyThrows
    public void runKubernetesDeleteCommand(String... resourceNames) {
        runner.runProcess(ArrayUtils.addAll(
                new String[] { "kubectl", "delete", "-n", config.getNamespace() },
                resourceNames
        ));
    }

    @SneakyThrows
    public void runKubernetesExecOnPodCommand(String serviceId, String... args) {
        String pod = String.format("%s-0", getKubernetesServiceName(serviceId));
        runner.runProcess(ArrayUtils.addAll(
                new String[] { "kubectl", "exec", pod, "-n", config.getNamespace(), "--" },
                args
        ));
    }
}
