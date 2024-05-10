package com.zj.codestaging.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RemoteApi {
    private String host;
    private String basePath;
    private Map<String,ApiDetail> paths;

    @Data
    public static class ApiDetail{
        private String url;
        private String method;
        private String bodyType;
        private String contentType;
        private List<Parameter> parameters;
        private Map<String,String> statusCode;
        private Responses responses;
        private String formType;
    }

    @Data
    public static class Parameter{
        private String in;
        private String name;
        private String description;
        private Boolean required;
        private String type;
        private Object defaultValue;
        private Integer length;
    }

    @Data
    public static class Responses{
        private Map<String,ItemData> data;
        private String message;
        private String status;
        private String primaryKey;
    }

    @Data
    public static class ItemData{
        private String description;
        private String type;
        private Integer length;
        private Map<String,ItemData> items;
    }
}
