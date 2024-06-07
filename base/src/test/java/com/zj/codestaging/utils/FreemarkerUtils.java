package com.zj.codestaging.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Created by hcitl on 2017/1/29.
 */
public class FreemarkerUtils {

    public static void execute(String ftlNameWithPath, Map<String, Object> data, Writer out) throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);//创建Freemarker配置实例

        int i = ftlNameWithPath.lastIndexOf("/") == -1 ? ftlNameWithPath.lastIndexOf("\\") : ftlNameWithPath.lastIndexOf("/");

        cfg.setDirectoryForTemplateLoading(new File(ftlNameWithPath.substring(0, i + 1)));

        cfg.setDefaultEncoding("UTF-8");

        Template t1 = cfg.getTemplate(ftlNameWithPath.substring(i + 1));//加载模板文件
        t1.process(data, out);
        out.flush();
    }

}
