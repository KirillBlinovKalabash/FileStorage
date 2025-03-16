package com.filestorage.demo.dto;

import com.filestorage.demo.repository.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class FileInfoDTO {
    private String fileDownloadUrl;
    private String fileId;
    private String fileName;
    private Long size;
    private String contentType;
    private List<String> Tags;
    private LocalDate creationTime;
    private LocalDate modificationTime;
    private String owner;

    public static FileInfoDTO fromFileInfo(FileInfo fileInfo, String fileDownloadUrl) {
        return new FileInfoDTO(fileDownloadUrl, fileInfo.getFileId(), fileInfo.getFileName(),
                fileInfo.getSize(), fileInfo.getContentType(), fileInfo.getTags(), fileInfo.getCreationTime(),
                fileInfo.getModificationTime(), fileInfo.getOwner());

    }
}
