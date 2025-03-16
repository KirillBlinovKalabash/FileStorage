package com.filestorage.demo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUpdateDTO {
    private MultipartFile file;
    private String fileName;
}
