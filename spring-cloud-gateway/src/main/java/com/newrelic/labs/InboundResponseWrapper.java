package com.newrelic.labs;

import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.util.List;

public class InboundResponseWrapper extends ExtendedInboundHeaders {

    private final ServerHttpResponse response;

    public InboundResponseWrapper(ServerHttpResponse response) {
        this.response = response;
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    @Override
    public String getHeader(String name) {
        // header(name) will return an empty list if no header values are found
        final List<String> header = getHeaders(name);
        if (!header.isEmpty()) {
            return header.get(0);
        }
        return null;
    }

    @Override
    public List<String> getHeaders(String name) {
        return response.getHeaders().get(name);
    }
}