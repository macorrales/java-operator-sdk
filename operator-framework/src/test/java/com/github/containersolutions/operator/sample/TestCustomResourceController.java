package com.github.containersolutions.operator.sample;

import com.github.containersolutions.operator.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;

import java.util.Optional;

@Controller(
        group = TestCustomResourceController.TEST_GROUP,
        kind = TestCustomResourceController.KIND_NAME,
        customResourceClass = TestCustomResource.class,
        customResourceListClass = TestCustomResourceList.class,
        customResourceDonebaleClass = TestCustomResourceDoneable.class)
public class TestCustomResourceController implements ResourceController<TestCustomResource> {

    public static final String KIND_NAME = "customResourceDefinition";
    public static final String TEST_GROUP = "test.group";

    @Override
    public boolean deleteResource(TestCustomResource resource, Context<TestCustomResource> context) {
        return true;
    }

    @Override
    public Optional<TestCustomResource> createResource(TestCustomResource resource, Context<TestCustomResource> context) {
        return Optional.empty();
    }

    @Override
    public Optional<TestCustomResource> updateResource(TestCustomResource resource, Context<TestCustomResource> context) {
        return Optional.empty();
    }
}
