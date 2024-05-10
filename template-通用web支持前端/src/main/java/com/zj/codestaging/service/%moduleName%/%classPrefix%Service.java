package com.zj.codestaging.service.%moduleName%;

import org.springframework.stereotype.Service;

@Service
public interface %classPrefix%Service {
    /**
     * 验证数据库连接
     * @param xml
     * @return
     */
    Integer validate(boolean xml);
}
