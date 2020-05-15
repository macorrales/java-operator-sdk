package com.github.containersolutions.operator.sample;

import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Controller(
        crdName = TestCustomResourceController.CRD_NAME,
        customResourceClass = TestCustomResource.class)
public class TestCustomResourceController implements ResourceController<TestCustomResource> {

    private static final Logger log = LoggerFactory.getLogger(TestCustomResourceController.class);

    public static final String CRD_NAME = "customservices.sample.javaoperatorsdk";

    private final KubernetesClient kubernetesClient;
    private final boolean updateStatus;
    private AtomicInteger numberOfExecutions = new AtomicInteger(0);

    public TestCustomResourceController(KubernetesClient kubernetesClient) {
        this(kubernetesClient, true);
    }

    public TestCustomResourceController(KubernetesClient kubernetesClient, boolean updateStatus) {
        this.kubernetesClient = kubernetesClient;
        this.updateStatus = updateStatus;
    }

    @Override
    public boolean deleteResource(TestCustomResource resource) {
        kubernetesClient.configMaps().inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getSpec().getConfigMapName()).delete();
        log.info("Deleting config map with name: {} for resource: {}", resource.getSpec().getConfigMapName(), resource.getMetadata().getName());
        return true;
    }

    @Override
    public Optional<TestCustomResource> createOrUpdateResource(TestCustomResource resource) {
        numberOfExecutions.addAndGet(1);
        ConfigMap existingConfigMap = kubernetesClient
                .configMaps().inNamespace(resource.getMetadata().getNamespace())
                .withName(resource.getSpec().getConfigMapName()).get();

        if (existingConfigMap != null) {
            existingConfigMap.setData(configMapData(resource));
//            existingConfigMap.getMetadata().setResourceVersion(null);
            kubernetesClient.configMaps().inNamespace(resource.getMetadata().getNamespace())
                    .withName(existingConfigMap.getMetadata().getName()).createOrReplace(existingConfigMap);
        } else {
            Map<String, String> labels = new HashMap<>();
            labels.put("managedBy", TestCustomResourceController.class.getSimpleName());
            ConfigMap newConfigMap = new ConfigMapBuilder()
                    .withMetadata(new ObjectMetaBuilder()
                            .withName(resource.getSpec().getConfigMapName())
                            .withNamespace(resource.getMetadata().getNamespace())
                            .withLabels(labels)
                            .build())
                    .withData(configMapData(resource)).build();
            kubernetesClient.configMaps().inNamespace(resource.getMetadata().getNamespace())
                    .createOrReplace(newConfigMap);
        }
        if (updateStatus) {
            if (resource.getStatus() == null) {
                resource.setStatus(new TestCustomResourceStatus());
            }
            resource.getStatus().setConfigMapStatus("ConfigMap Ready");
        }
        addOrUpdateAnnotation(resource);
        return Optional.of(resource);
    }

    // for testing purposes we change metadata
    private void addOrUpdateAnnotation(TestCustomResource resource) {
        if (resource.getMetadata().getAnnotations() == null) {
            resource.getMetadata().setAnnotations(new HashMap<>());
        }
        resource.getMetadata().getAnnotations().put("testAnnotation", LocalDateTime.now().toString());
    }

    private Map<String, String> configMapData(TestCustomResource resource) {
        Map<String, String> data = new HashMap<>();
        data.put(resource.getSpec().getKey(), resource.getSpec().getValue());
        return data;
    }

    public int getNumberOfExecutions() {
        return numberOfExecutions.get();
    }
}
