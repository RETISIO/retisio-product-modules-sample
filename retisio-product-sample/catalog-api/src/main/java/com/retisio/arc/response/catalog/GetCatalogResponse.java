package com.retisio.arc.response.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetCatalogResponse {
    private String catalogId;
    private String catalogName;
    private Boolean active;
    private LinkedHashMap<String, Serializable> dynProps;
    private Boolean deleted;
}
