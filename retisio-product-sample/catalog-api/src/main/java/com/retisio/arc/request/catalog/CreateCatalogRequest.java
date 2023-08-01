package com.retisio.arc.request.catalog;

import lombok.Value;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Value
public class CreateCatalogRequest {
    private String catalogId;
    private String catalogName;
    private Boolean active;
    private LinkedHashMap<String, Serializable> dynProps;
}
