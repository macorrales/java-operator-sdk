package com.github.containersolutions.operator;

import com.github.containersolutions.operator.sample.TestCustomResource;
import com.github.containersolutions.operator.sample.TestCustomResourceSpec;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.github.containersolutions.operator.IntegrationTestSupport.TEST_NAMESPACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ControllerExecutionIT {

    private final static Logger log = LoggerFactory.getLogger(ControllerExecutionIT.class);
    private IntegrationTestSupport integrationTestSupport = new IntegrationTestSupport();

    public void initAndCleanup(boolean controllerStatusUpdate) {
        integrationTestSupport.initialize(controllerStatusUpdate);
        integrationTestSupport.cleanup();
    }

    @Test
    public void configMapGetsCreatedForTestCustomResource() {
        initAndCleanup(true);
        integrationTestSupport.teardownIfSuccess(() -> {
            TestCustomResource resource = testCustomResource();

            integrationTestSupport.getCrOperations().inNamespace(TEST_NAMESPACE).create(resource);

            awaitResourcesCreatedOrUpdated();
            awaitStatusUpdated();
            assertThat(integrationTestSupport.numberOfControllerExecutions()).isEqualTo(2);
        });
    }

    @Test
    public void eventIsSkippedChangedOnMetadataOnlyUpdate() {
        initAndCleanup(false);
        integrationTestSupport.teardownIfSuccess(() -> {
            TestCustomResource resource = testCustomResource();

            integrationTestSupport.getCrOperations().inNamespace(TEST_NAMESPACE).create(resource);

            awaitResourcesCreatedOrUpdated();
            assertThat(integrationTestSupport.numberOfControllerExecutions()).isEqualTo(1);
        });
    }

    // We test the scenario when we receive 2 events, while the generation is not increased by the other.
    // This will cause a conflict, and on retry the new version of the resource needs to be scheduled
    // to avoid repeating conflicts
    @Test
    public void generationAwareRetryConflict() {
        initAndCleanup(true);
        integrationTestSupport.teardownIfSuccess(() -> {
            TestCustomResource resource = testCustomResource();
            TestCustomResource resource2 = testCustomResource();
            resource2.getMetadata().getAnnotations().put("testannotation", "val");

            integrationTestSupport.getCrOperations().inNamespace(TEST_NAMESPACE).create(resource);
            integrationTestSupport.getCrOperations().inNamespace(TEST_NAMESPACE).createOrReplace(resource2);

            awaitResourcesCreatedOrUpdated();
            awaitStatusUpdated(10);
        });
    }


    void awaitResourcesCreatedOrUpdated() {
        await("configmap created").atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    ConfigMap configMap = integrationTestSupport.getK8sClient().configMaps().inNamespace(TEST_NAMESPACE)
                            .withName("test-config-map").get();
                    assertThat(configMap).isNotNull();
                    assertThat(configMap.getData().get("test-key")).isEqualTo("test-value");
                });
    }

    void awaitStatusUpdated() {
        awaitStatusUpdated(5);
    }

    void awaitStatusUpdated(int timeout) {
        await("cr status updated").atMost(timeout, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    TestCustomResource cr = integrationTestSupport.getCrOperations().inNamespace(TEST_NAMESPACE).withName("test-custom-resource").get();
                    assertThat(cr).isNotNull();
                    assertThat(cr.getStatus()).isNotNull();
                    assertThat(cr.getStatus().getConfigMapStatus()).isEqualTo("ConfigMap Ready");
                });
    }

    private TestCustomResource testCustomResource() {
        TestCustomResource resource = new TestCustomResource();
        resource.setMetadata(new ObjectMetaBuilder()
                .withName("test-custom-resource")
                .withNamespace(TEST_NAMESPACE)
                .build());
        resource.getMetadata().setAnnotations(new HashMap<>());
        resource.setKind("CustomService");
        resource.setSpec(new TestCustomResourceSpec());
        resource.getSpec().setConfigMapName("test-config-map");
        resource.getSpec().setKey("test-key");
        resource.getSpec().setValue("test-value");
        return resource;
    }

}
