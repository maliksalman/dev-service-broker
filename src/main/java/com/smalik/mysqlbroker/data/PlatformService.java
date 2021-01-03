package com.smalik.mysqlbroker.data;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@Document
public class PlatformService {
  
  @Id
  private String id;

  private String planDefinitionId;
  private String serviceDefinitionId;

  @Singular
  private Map<String, Object> properties;
}