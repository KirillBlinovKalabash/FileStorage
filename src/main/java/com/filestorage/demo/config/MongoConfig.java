package com.filestorage.demo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {
    private static final String DATABASE_NAME = "fileserver";

    @Bean
    public MongoDatabase mongoDatabase() {
        // Get database reference
        MongoClient client = MongoClients.create("mongodb://localhost:27017");
        return client.getDatabase(DATABASE_NAME);
    }

    @Bean
    public GridFSBucket gridFSBucket(MongoDatabase database) {
        // Create and return GridFSBucket instance
        return GridFSBuckets.create(database);
    }
}
