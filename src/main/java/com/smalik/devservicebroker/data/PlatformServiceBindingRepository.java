package com.smalik.devservicebroker.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlatformServiceBindingRepository extends MongoRepository<PlatformServiceBinding, PlatformServiceBindingId> {
  
}