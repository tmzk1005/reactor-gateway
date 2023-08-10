package zk.rgw.common.event.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.common.definition.IdRouteDefinition;
import zk.rgw.common.event.RgwEvent;

@Getter
@Setter
@NoArgsConstructor
public class RouteDefinitionEvent implements RgwEvent {

    private IdRouteDefinition routeDefinition;

    private boolean add = true;

    public RouteDefinitionEvent(IdRouteDefinition routeDefinition, boolean isAdd) {
        this.routeDefinition = routeDefinition;
        this.add = isAdd;
    }

}
