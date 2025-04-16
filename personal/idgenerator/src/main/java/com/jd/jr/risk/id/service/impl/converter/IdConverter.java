package com.jd.jr.risk.id.service.impl.converter;


import com.jd.jr.risk.id.service.bean.Id;
import com.jd.jr.risk.id.service.impl.bean.IdMeta;

public interface IdConverter {

    long convert(Id id, IdMeta idMeta);

    Id convert(long id, IdMeta idMeta);

}
