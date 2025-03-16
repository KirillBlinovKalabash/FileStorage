package com.filestorage.demo.repository;

import lombok.Getter;

@Getter
public enum FileMetaData {
    CONTENT_TYPE("contentType"),
    OWNER("owner"),
    ACCESS_LEVEL("accessLevel"),
    CREATE_TIME("createTime"),
    TAGS("tags"),
    FILE_HASH("fileHash"),
    ID("id"),
    FILE_NAME("fileName"),
    FILE_SIZE("fileSize");

    private final String key;

    FileMetaData(String key) {
        this.key = key;
    }
}
