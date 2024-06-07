package com.zj.codestaging.utils;

import com.zj.codestaging.model.Item;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * @Description: 数据库相关操作封装 CV战士 from https://github.com/hcitlife/DBUtilsGenerator
 * @Author: zhijian
 * @Date: 2024/6/6  16:44
 */
public class DBUtils {

    protected String type;
    protected String driver;
    protected String url;
    protected String username;
    protected String password;

    public DBUtils(EntityGenerator.DB dbtype, String url, String username, String password) {
        this.type = dbtype.name().toLowerCase(Locale.ROOT);
        switch (type) {
            case "mysql":
                this.driver = "com.mysql.cj.jdbc.Driver";
                break;
            case "postgresql":
                this.driver = "org.postgresql.Driver";
                break;
            case "oracle":
                this.driver = "com.p6spy.engine.spy.P6SpyDriver";
                break;
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public DBUtils(String driver, String url, String username, String password) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("未找到驱动类: " + driver, e);
        }

        return DriverManager.getConnection(url, username, password);
    }

    public void closeAll(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // 内部数据格式：表名 <对应Java类型 修正后的表字段名 对应Java类型 修正后的表字段名。。。>
    protected static Map<String, List<Item>> tableInfoList = new HashMap<>();// tb_dept <java.lang.Integer deptno>

    protected void readTableMetaData(String databaseName) throws Exception {
        List<String> tables = getAllTableNamesByDatabase(databaseName);

        // 遍历每一个表取出表中字段的名称、类型、对应的Java类型等信息
        for (String table : tables) {// 遍历每一个表取出表中字段的名称、类型、对应的Java类型等信息
            Connection conn = null;
            Statement stmt = null;
            ResultSet rs = null;

            try {
                conn = getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery("show full columns from " + table);// 得到表的描述信息（字段名称、数据类型等）

                List<Item> paramList = new ArrayList<>();
                while (rs.next()) {
                    String fieldName = rs.getString("field");
                    String propertyName = field2Property(fieldName);

                    String comment = rs.getString("Comment");

                    String sqlType = rs.getString("type").toUpperCase();
                    String javaType = getJavaTypeByDbType(sqlType);
                    if (sqlType.contains("(")) {
                        sqlType = sqlType.substring(0, sqlType.indexOf("("));
                    }

                    if (sqlType.equals("INT")) {// MyBatis中JdbcType的int类型的名称为INTEGER
                        sqlType = "INTEGER";
                    }
                    Item item = new Item(propertyName, comment, javaType, fieldName, sqlType);

                    if (getPK(table).size() > 0 && getPK(table).get(0).equalsIgnoreCase(fieldName)) {
                        item.setPk(true);
                    }

                    paramList.add(item);
                }
                tableInfoList.put(table, paramList);
            } catch (Exception e) {
                throw e;
            } finally {
                closeAll(conn, stmt, rs);
            }
        }

    }

    private List<String> getAllTableNamesByDatabase(String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();// 用来放置所有表的名字
        // 获取当前数据库所有的表名
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement("select table_name from information_schema.TABLES where TABLE_SCHEMA=?");
            ps.setString(1, databaseName);
            rs = ps.executeQuery();
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            closeAll(conn, ps, rs);
        }
        return tables;
    }

    private String getJavaTypeByDbType(String type) {
        String javaType = null;
        if (type.indexOf("CHAR") > -1 || type.indexOf("TEXT") > -1 || type.indexOf("ENUM") > -1
                || type.indexOf("SET") > -1) {
            javaType = "java.lang.String";
        } else if (type.indexOf("TIME") > -1 || type.indexOf("DATE") > -1 || type.indexOf("YEAR") > -1) {
            javaType = "java.util.Date";
        } else if (type.indexOf("BIGINT") > -1) {
            javaType = "java.lang.Long";
        } else if (type.indexOf("TINYINT") > -1) {
            javaType = "java.lang.Byte";
        } else if (type.indexOf("INT") > -1) {
            javaType = "java.lang.Integer";
        } else if (type.indexOf("BIT") > -1) {
            javaType = "java.lang.Boolean";
        } else if (type.indexOf("FLOAT") > -1 || type.indexOf("REAL") > -1) {
            javaType = "java.lang.Double";
        } else if (type.indexOf("DOUBLE") > -1 || type.indexOf("NUMERIC") > -1) {
            javaType = "java.lang.Double";
        } else if (type.indexOf("BLOB") > -1 || type.indexOf("BINARY") > -1) {
            javaType = "byte[]";
        } else if (type.indexOf("JSON") > -1) {
            javaType = "java.lang.String";
        } else if (type.indexOf("DECIMAL") > -1) {
            javaType = "java.math.BigDecimal";
        } else {
            System.out.println("type:" + type);
        }
        return javaType;
    }

    /**
     * 返回数据表中的主键
     *
     * @param table
     * @return
     * @throws SQLException
     */
    protected List<String> getPK(String table) throws SQLException {
        List<String> res = new ArrayList<>();
        Connection conn = getConnection();
        String catalog = conn.getCatalog(); // catalog 其实也就是数据库名
        DatabaseMetaData metaData = conn.getMetaData();

        ResultSet rs = null;
        rs = metaData.getPrimaryKeys(catalog, null, table);// 适用mysql
        while (rs.next()) {
            res.add(rs.getString("COLUMN_NAME"));
        }
        closeAll(conn, null, rs);
        return res;
    }

    /**
     * 返回数据表中的外键
     *
     * @param table
     * @return
     * @throws SQLException
     */
    protected List<String> getFk(String table) throws SQLException {
        List<String> res = new ArrayList<>();
        Connection conn = getConnection();
        String catalog = conn.getCatalog(); // catalog 其实也就是数据库名
        DatabaseMetaData metaData = conn.getMetaData();

        ResultSet rs = metaData.getImportedKeys(catalog, null, table);
        while (rs.next()) {
            res.add(rs.getString("FKCOLUMN_NAME"));
        }
        closeAll(conn, null, rs);
        return res;
    }

    protected Map<String, String> getFkNameType(String table) throws SQLException {
        Map<String, String> res = new HashMap<>();
        Connection conn = getConnection();
        String catalog = conn.getCatalog(); // catalog 其实也就是数据库名
        DatabaseMetaData metaData = conn.getMetaData();

        ResultSet rs = metaData.getImportedKeys(catalog, null, table);
        while (rs.next()) {
            String fkColumnName = rs.getString("FKCOLUMN_NAME");
            String fkName = null;
            int endIndex = fkColumnName.lastIndexOf('_');
            if (endIndex > 0) {
                fkName = fkColumnName.substring(0, endIndex);
            } else {
                fkName = fkColumnName;
            }

            String pkTablenName = rs.getString("PKTABLE_NAME");
            String fkType = getEntryName(pkTablenName);
            res.put(fkType, fkName);
        }
        closeAll(conn, null, rs);
        return res;
    }

    String field2Property(String columnName) {
        while (columnName.indexOf("_") > -1) {
            int index = columnName.indexOf("_");
            columnName = columnName.substring(0, index) + columnName.substring(index, index + 2).toUpperCase().substring(1)
                    + columnName.substring(index + 2, columnName.length());
        }
        return columnName;
    }

    //首字母转小写
    String First2LowerCase(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }

    //首字母转大写
    String First2UpperCase(String s) {
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }

    /**
     * 获取表名所对应的实体类的名称，默认每个表名都有一个前缀，将前缀给干掉
     * @param table
     * @return
     */
    String getEntryName(String table) {
        String tb = table.trim();
        String temp = First2UpperCase(tb.substring(tb.indexOf('_') + 1));
        return field2Property(temp);
    }

    void createEntityFile(String moduleName) throws Exception {
        Map<String, Object> data = new HashMap<>(); // 创建数据模型

        String rootPath = System.getProperty("user.dir");
        if(!rootPath.endsWith("base"))
            rootPath = rootPath + "/base";

        rootPath = rootPath + File.separator + ".." + File.separator;

        // 目标文件夹
        File targetDir =new File(rootPath + "project-"+ moduleName);
        if(!targetDir.exists())
            throw new FileNotFoundException("子模块["+moduleName+"]不存在,请先生成子模块");

        String packageName = "com.zj.codestaging.entity." + moduleName;

        for (String table : tableInfoList.keySet()) {
            String newEntity = getEntryName(table);

            data.put("clazzName", newEntity);
            data.put("pkg", packageName); // 包名

            Set<String> importList = new HashSet<>(); // 用来生成import语句
            List<Item> propertyList = new ArrayList<>(); // 用来生成各属性

            // 用来生成主键
            String pk = getPK(table).get(0);
            data.put("pk", field2Property(pk));

            List<Item> paramList = tableInfoList.get(table);
            for (Item item : paramList) {
                String javaType = item.getJavaType();
                String propertyName = item.getPropertyName();
                if (propertyName.equals(field2Property(pk))) {
                    data.put("pkType", javaType);// 主键的数据类型
                }
                importList.add(javaType);
                propertyList.add(new Item(javaType.substring(javaType.lastIndexOf(".") + 1),
                        field2Property(propertyName), item.getComment()));
            }

            Map<String, String> fkNameTypeList = getFkNameType(table);
            data.put("fkNameTypeList", fkNameTypeList);

            data.put("importList", importList);
            data.put("propertyList", propertyList);

            File path = new File(targetDir + File.separator + "src/main/java/" + packageName.replaceAll("[.]","/"));
            if(!path.exists()) path.mkdirs();
            String fileName = path.getPath() + "\\" + newEntity + ".java";
            Writer out = new FileWriter(new File(fileName));

            String resource = DBUtils.class.getResource("/ftl/entity.ftl").getPath();
            try {
                resource = URLDecoder.decode(resource, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            FreemarkerUtils.execute(resource, data, out);
        }

    }
}
