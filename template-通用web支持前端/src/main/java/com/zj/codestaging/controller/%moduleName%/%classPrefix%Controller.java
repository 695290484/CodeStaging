package com.zj.codestaging.controller.%moduleName%;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/%moduleName%")
public class %classPrefix%Controller {

    @GetMapping("")
    @ResponseBody
    public String index(){
        return "success";
    }
}
