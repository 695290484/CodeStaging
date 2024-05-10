package com.zj.codestaging.mapper.%moduleName%;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface %classPrefix%Mapper {

    /**
     * 测试xml验证数据库
     */
    public Integer validateXml();

    /**
     * 测试注解验证数据库
     */
    @Select({"select 1"})
    public Integer validateAnn();
}
