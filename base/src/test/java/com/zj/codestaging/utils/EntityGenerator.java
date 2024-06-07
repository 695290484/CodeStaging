package com.zj.codestaging.utils;

/**
 * @Description: 生成实体类
 * @Author: zhijian
 * @Date: 2024/6/7  10:24
 */
public class EntityGenerator {

    static enum DB{
        MYSQL,
        POSTGRESQL,
        ORACLE
    }

    public static void generateEntity(String driver, String url, String username, String password, String db, String moduleName) throws Exception {
        DBUtils dbUtils = new DBUtils(driver, url, username, password);

        dbUtils.readTableMetaData(db);
        dbUtils.createEntityFile(moduleName);
    }

    public static void generateEntity(EntityGenerator.DB dbType, String url, String username, String password, String db, String moduleName) throws Exception {
        DBUtils dbUtils = new DBUtils(dbType, url, username, password);

        dbUtils.readTableMetaData(db);
        dbUtils.createEntityFile(moduleName);
    }
}
