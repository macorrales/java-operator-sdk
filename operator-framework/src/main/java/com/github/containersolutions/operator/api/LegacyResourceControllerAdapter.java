package com.github.containersolutions.operator.api;

import com.github.containersolutions.operator.Context;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.Optional;

/**
 * Will be removed soon just to adapt quickly older version impls
 *
 * @param <R>
 */
@Deprecated
public interface LegacyResourceControllerAdapter<R extends CustomResource> extends ResourceController<R> {

    @Override
    default Optional<R> createResource(R resource, Context<R> context) {
        return createOrUpdateResource(resource, context);
    }

    @Override
    default Optional<R> updateResource(R resource, Context<R> context) {
        return createOrUpdateResource(resource, context);
    }

    Optional<R> createOrUpdateResource(R resource, Context<R> context);

}
