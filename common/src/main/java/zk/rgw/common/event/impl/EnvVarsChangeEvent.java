package zk.rgw.common.event.impl;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import zk.rgw.common.event.RgwEvent;

@Getter
@Setter
@NoArgsConstructor
public class EnvVarsChangeEvent implements RgwEvent {

    private String orgId;

    private Map<String, String> vars;

    public EnvVarsChangeEvent(String orgId, Map<String, String> vars) {
        this.orgId = orgId;
        this.vars = vars;
    }

}
