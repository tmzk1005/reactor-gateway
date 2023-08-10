package zk.rgw.common.event.impl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.common.definition.AppDefinition;
import zk.rgw.common.event.RgwEvent;

@Getter
@Setter
@NoArgsConstructor
public class AppSubRouteEvent implements RgwEvent {

    private String routeId;

    private AppDefinition appDefinition;

    private boolean sub = true;

    public AppSubRouteEvent(String routeId, AppDefinition appDefinition, boolean isSub) {
        this.routeId = routeId;
        this.appDefinition = appDefinition;
        this.sub = isSub;
    }

}
