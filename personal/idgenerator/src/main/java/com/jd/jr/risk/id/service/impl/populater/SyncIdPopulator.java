package com.jd.jr.risk.id.service.impl.populater;

import com.jd.jr.risk.id.service.bean.Id;
import com.jd.jr.risk.id.service.impl.bean.IdMeta;
import com.jd.jr.risk.id.service.impl.timer.Timer;

public class SyncIdPopulator extends BasePopulator {

    public SyncIdPopulator() {
        super();
    }

    public synchronized void populateId(Timer timer, Id id, IdMeta idMeta) {
        super.populateId(timer, id, idMeta);
    }

}
