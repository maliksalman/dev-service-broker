package com.smalik.devservicebroker.data;

import lombok.Data;

import java.util.List;

@Data
public class CatalogService {
    private String id;
    private String description;
    private List<CatalogServicePlan> plans;
    private List<String> tags;
}
