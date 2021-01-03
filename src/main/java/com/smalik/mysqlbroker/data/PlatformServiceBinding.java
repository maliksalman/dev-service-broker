package com.smalik.mysqlbroker.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Document
public class PlatformServiceBinding {
 
  @Id
  private String id;

  private String platformServiceId;
  private String username;
  private String password;
  private String url; 
}