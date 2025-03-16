package com.filestorage.demo.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.ByteArrayOutputStream;

@Data
@AllArgsConstructor
public class FileDownloadDTO {
    private ByteArrayOutputStream fileStream;
    private String fileName;
}
