package com.zj.codestaging.service.%moduleName%.impl;

import com.zj.codestaging.mapper.%moduleName%.%classPrefix%Mapper;
import com.zj.codestaging.service.%moduleName%.%classPrefix%Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class %classPrefix%ServiceImpl implements %classPrefix%Service {

    @Autowired
    %classPrefix%Mapper %moduleName%Mapper;

    @Override
    public Integer validate(boolean xml) {
        return xml?%moduleName%Mapper.validateXml():%moduleName%Mapper.validateAnn();
    }
}
