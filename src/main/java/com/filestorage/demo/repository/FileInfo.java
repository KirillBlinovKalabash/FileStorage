package com.filestorage.demo.repository;

import com.filestorage.demo.dto.AccessLevel;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Getter
public class FileInfo {
    private String fileId;
    private String fileName;
    private Long size;
    private String contentType;
    private List<String> Tags;
    private LocalDate creationTime;
    private LocalDate modificationTime;
    private String owner;
    private AccessLevel accessLevel;
    private GridFSFile file;

    public static FileInfo fromFile(GridFSFile file) {
        Date rawCreateDate = (Date) file.getMetadata().get(FileMetaData.CREATE_TIME.getKey());
        LocalDate createDate = Instant.ofEpochMilli(rawCreateDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate uploadDate = Instant.ofEpochMilli(file.getUploadDate().getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
        return file == null ? null :
                new FileInfo(
                        (String) file.getMetadata().get(FileMetaData.ID.getKey()),
                        (String) file.getMetadata().get(FileMetaData.FILE_NAME.getKey()),
                        file.getLength(),
                        (String) file.getMetadata().get(FileMetaData.CONTENT_TYPE.getKey()),
                        (ArrayList<String>) file.getMetadata().get(FileMetaData.TAGS.getKey()),
                        createDate,
                        uploadDate,
                        (String) file.getMetadata().get(FileMetaData.OWNER.getKey()),
                        AccessLevel.valueOf((String) file.getMetadata().get(FileMetaData.ACCESS_LEVEL.getKey())),
                        file);
    }
}
