package com.filestorage.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class FileUploadDTO {
    @NotBlank
    private AccessLevel accessLevel;
    @NotBlank
    private String fileName;
    private Set<String> tags;
}
