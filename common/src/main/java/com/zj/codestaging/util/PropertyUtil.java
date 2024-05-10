package com.zj.codestaging.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 加载Properties文件的内容，通过key获取value
 */
public class PropertyUtil {


    /**
     * 根据指定文件的key获取value
     *
     * @param relativeClassPath 相对于类路径的文件路径
     * @param key               键
     * @return 值
     */
    public static String getValue(String relativeClassPath, String key) {
        if (key.isEmpty()) {
            return "";
        }
        Properties properties = new Properties();
        try {
            InputStream resourceAsStream = PropertyUtil.class.getClassLoader().getResourceAsStream(relativeClassPath);
            properties.load(resourceAsStream);

            resourceAsStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties.getProperty(key);
    }


    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        InputStream resourceAsStream = PropertyUtil.class.getClassLoader().getResourceAsStream("test.properties");
        properties.load(resourceAsStream);

        String name = properties.getProperty("name");
        String key = properties.getProperty("key");
        System.out.println("name = " + name);
        System.out.println("key = " + key);


        String by = getValue("test.properties", "name");
        System.out.println("by = " + by);

    }
}
