package org.acme.domains.shared.api;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

public interface BaseResource {

    default <T> Uni<Response> toCreated(Uni<T> useCaseResult){

        return useCaseResult.onItem()
                .transform(result ->
                        Response
                                .status(Response.Status.CREATED)
                                .entity(result)
                                .build());
    }

    default <T> Uni<Response> toOk(Uni<T> useCaseResult){
        return useCaseResult.onItem()
                .transform(result ->
                        Response
                                .status(Response.Status.OK)
                                .build());
    }

    default <T> Uni<Response> delete(Uni<T> useCaseResult){
        return useCaseResult.onItem()
                .transform(result ->
                        Response.status(Response.Status.NO_CONTENT)
                                .entity(result)
                                .build());

    }

}
