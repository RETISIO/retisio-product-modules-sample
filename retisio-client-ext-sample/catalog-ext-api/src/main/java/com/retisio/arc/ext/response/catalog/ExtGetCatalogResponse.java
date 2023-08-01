package com.retisio.arc.ext.response.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtGetCatalogResponse {
    private String catalogId;
    private String catalogName;
    private Boolean active;
    private LinkedHashMap<String, Serializable> dynProps;
}