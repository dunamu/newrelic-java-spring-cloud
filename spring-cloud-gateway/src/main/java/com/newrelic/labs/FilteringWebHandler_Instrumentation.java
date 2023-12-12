package com.newrelic.labs;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.logging.Level;
import java.util.regex.Pattern;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.cloud.gateway.handler.FilteringWebHandler")
public abstract class FilteringWebHandler_Instrumentation {
    
    @Trace(dispatcher = true)
    public Mono<Void> handle(ServerWebExchange exchange) {

        try {
            final Pattern versionPattern = Pattern.compile("[vV][0-9]{1,}");
            final Pattern idPattern = Pattern.compile("^(?=[^\\s]*?[0-9])[-{}().:_|0-9]+$");
            final Pattern codPattern = Pattern.compile("^(?=[^\\s]*?[0-9])(?=[^\\s]*?[a-zA-Z])(?!\\{id\\}).*$");

            final String gatewayRouteId = "gatewayRouteId";
            final String gatewayRouteUri = "gatewayRouteUri";

            String path = exchange.getRequest().getPath().value();

            String simplifiedPath = path;

            Route route = exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

            final String[] splitPath = path.split("/");

            if (splitPath.length > 0) {
                simplifiedPath = "";
                for (String p : splitPath) {
                    if (versionPattern.matcher(p).matches()) {
                        simplifiedPath = simplifiedPath.concat("/").concat(p.replaceAll(versionPattern.toString(), "{version}"));
                    } else if (idPattern.matcher(p).matches()) {
                        simplifiedPath = simplifiedPath.concat("/").concat(p.replaceAll(idPattern.toString(), "{id}"));
                    } else if (codPattern.matcher(p).matches()) {
                        simplifiedPath = simplifiedPath.concat("/").concat(p.replaceAll(codPattern.toString(), "{cod}"));
                    } else {
                        simplifiedPath = simplifiedPath.concat("/").concat(p);
                    }
                }
            }

            NewRelic.setTransactionName("Web", simplifiedPath);
            NewRelic.getAgent().getLogger().log(Level.FINER,
                    "spring-cloud-gateway Instrumentation: Setting web transaction name to " + simplifiedPath);
            NewRelic.addCustomParameter(gatewayRouteId, route.getId());
            NewRelic.addCustomParameter(gatewayRouteUri, route.getUri().toString());
        } catch (Exception e) {
            System.out.println("ERROR spring-cloud-gateway Instrumentation: " + e.getMessage());
        }

        return Weaver.callOriginal();
    }


}
