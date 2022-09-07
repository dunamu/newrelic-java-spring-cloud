package com.newrelic.labs;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class OutboundRequestWrapper implements OutboundHeaders {

    private ServerHttpRequest.Builder requestBuilder;

    OutboundRequestWrapper(ServerHttpRequest.Builder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public void setHeader(String key, String value) {
        requestBuilder.header(key, value);
    }

    public ServerHttpRequest build() {
        return requestBuilder.build();
    }
}