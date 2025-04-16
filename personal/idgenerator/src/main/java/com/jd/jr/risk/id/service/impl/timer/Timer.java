package com.jd.jr.risk.id.service.impl.timer;


import com.jd.jr.risk.id.service.impl.bean.IdMeta;
import com.jd.jr.risk.id.service.impl.bean.IdType;

import java.util.Date;

public interface Timer {
    long EPOCH = 1514736000000L;

    void init(IdMeta idMeta, IdType idType);

    Date transTime(long time);

    void validateTimestamp(long lastTimestamp, long timestamp);

    long tillNextTimeUnit(long lastTimestamp);

    long genTime();

}
