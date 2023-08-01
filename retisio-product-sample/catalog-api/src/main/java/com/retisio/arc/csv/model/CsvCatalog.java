package com.retisio.arc.csv.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CsvCatalog {
    @JsonProperty("Catalog Id")
    private String catalogId;
    @JsonProperty("Catalog Name")
    private String catalogName;
    @JsonProperty("Active")
    private Boolean active;
}