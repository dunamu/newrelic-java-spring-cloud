package com.newrelic.labs;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.cloud.gateway.filter.NettyRoutingFilter")
public class NettyRoutingFilter_Instrumentation {

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Segment segment = Util.startSegment();
        exchange = exchange.mutate().request(Util.addHeaders(exchange.getRequest(), segment)).build();
        Mono<Void> filter = Weaver.callOriginal();

        return Util.reportAsExternal(exchange, filter, segment);
    }

}
