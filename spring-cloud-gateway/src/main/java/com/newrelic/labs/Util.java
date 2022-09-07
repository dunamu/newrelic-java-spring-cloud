package com.newrelic.labs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import java.util.logging.Level;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

public class Util {

    private static final String LIBRARY = "SpringCloudGateway-NettyRoutingFilter";
    private static final String SEGMENT = "NettyRoutingFilter.filter";
    private static final String PROCEDURE = "filter";

    public static Segment startSegment() {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        return txn == null ? null : txn.startSegment(SEGMENT);
    }

    public static ServerHttpRequest addHeaders(ServerHttpRequest request, Segment segment) {
        if (segment != null) {
            OutboundRequestWrapper outboundRequestWrapper = new OutboundRequestWrapper(request.mutate());
            segment.addOutboundRequestHeaders(outboundRequestWrapper);
            request = outboundRequestWrapper.build();
        }
        return request;
    }

    public static Mono<Void> reportAsExternal(ServerWebExchange exchange, Mono<Void> filter, Segment segment) {
        if (segment == null) {
            return filter;
        }
        URI uri = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        return filter
                .doOnError(reportFailed(segment, uri))
                .doOnCancel(reportCancelled(segment, uri)).then()
                .then(Mono.fromRunnable(() -> {
                    try {
                        segment.reportAsExternal(
                                HttpParameters.library(LIBRARY)
                                        .uri(uri)
                                        .procedure(PROCEDURE)
                                        .inboundHeaders(new InboundResponseWrapper(exchange.getResponse()))
                                        .status(exchange.getResponse().getRawStatusCode(), null)
                                        .build()
                        );
                        segment.end();
                    } catch (Throwable e) {
                        reportInstrumentationError(e);
                    }
                }));
    }

    private static Consumer<Throwable> reportFailed(Segment segment, final URI uri) {
        return throwable -> {
            try {
                if (throwable instanceof UnknownHostException) {
                    segment.reportAsExternal(GenericParameters
                            .library(LIBRARY)
                            .uri(uri)
                            .procedure("failed")
                            .build());
                }
                segment.end();
            } catch (Throwable e) {
                reportInstrumentationError(e);
            }
        };
    }

    private static Runnable reportCancelled(Segment segment, final URI uri) {
        return () -> {
            try {
                segment.reportAsExternal(HttpParameters
                        .library(LIBRARY)
                        .uri(uri)
                        .procedure(PROCEDURE)
                        .noInboundHeaders()
                        .build());
                segment.end();
            } catch (Throwable e) {
                reportInstrumentationError(e);
            }
        };
    }

    private static void reportInstrumentationError(Throwable e) {
        AgentBridge.getAgent()
                .getLogger()
                .log(Level.FINEST, e, "Caught exception in NettyRoutingFilter instrumentation: {0}");
        AgentBridge.instrumentation.noticeInstrumentationError(e, Weaver.getImplementationTitle());
    }
}
