package com.jd.jr.risk.id.service.impl;

import com.jd.jr.risk.id.service.bean.Id;
import com.jd.jr.risk.id.service.impl.bean.IdType;
import com.jd.jr.risk.id.service.impl.populater.AtomicIdPopulator;
import com.jd.jr.risk.id.service.impl.populater.IdPopulator;
import com.jd.jr.risk.id.service.impl.populater.LockIdPopulator;
import com.jd.jr.risk.id.service.impl.populater.SyncIdPopulator;
import  com.jd.jr.risk.id.service.util.CommonUtils;

public class IdServiceImpl extends AbstractIdServiceImpl {

    private static final String SYNC_LOCK_IMPL_KEY = "risk.sync.lock.impl.key";

    private static final String ATOMIC_IMPL_KEY = "risk.atomic.impl.key";

    protected IdPopulator idPopulator;

    public IdServiceImpl() {
        super();
    }

    public IdServiceImpl(String type) {
        super(type);
    }

    public IdServiceImpl(long type) {
        super(type);
    }

    public IdServiceImpl(IdType type) {
        super(type);
    }

    @Override
    public void init() {
        super.init();
        initPopulator();
    }

    public void initPopulator() {
        if (idPopulator != null){
            log.info("The " + idPopulator.getClass().getCanonicalName() + " is used.");
        } else if (CommonUtils.isPropKeyOn(SYNC_LOCK_IMPL_KEY)) {
            log.info("The SyncIdPopulator is used.");
            idPopulator = new SyncIdPopulator();
        } else if (CommonUtils.isPropKeyOn(ATOMIC_IMPL_KEY)) {
            log.info("The AtomicIdPopulator is used.");
            idPopulator = new AtomicIdPopulator();
        } else {
            log.info("The default LockIdPopulator is used.");
            idPopulator = new LockIdPopulator();
        }
    }

    @Override
    protected void populateId(Id id) {
        idPopulator.populateId(timer, id, idMeta);
    }

    public void setIdPopulator(IdPopulator idPopulator) {
        this.idPopulator = idPopulator;
    }
}
