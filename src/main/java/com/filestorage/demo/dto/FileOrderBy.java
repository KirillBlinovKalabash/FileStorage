package com.filestorage.demo.dto;

import com.filestorage.demo.repository.FileMetaData;
import lombok.Getter;

@Getter
public enum FileOrderBy {
    FILE_NAME(FileMetaData.FILE_NAME.getKey()),
    UPLOAD_DATE(FileMetaData.CREATE_TIME.getKey()),
    TAG(FileMetaData.TAGS.getKey()),
    CONTENT_TYPE(FileMetaData.CONTENT_TYPE.getKey()),
    FILE_SIZE(FileMetaData.FILE_SIZE.getKey());

    private final String searchKey;

    FileOrderBy(String searchKey) {
        this.searchKey = searchKey;
    }
}
