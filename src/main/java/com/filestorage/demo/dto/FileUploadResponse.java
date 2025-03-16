package com.filestorage.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.net.URI;

@Data
@Builder
public class FileUploadResponse {
    private URI fileDownloadUrl;
    private String fileId;
    private String message = "File uploaded successfully!";
}
