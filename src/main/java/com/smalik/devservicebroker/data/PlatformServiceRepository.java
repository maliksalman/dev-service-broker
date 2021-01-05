package com.smalik.devservicebroker.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlatformServiceRepository extends MongoRepository<PlatformService, String> {

}