package com.filestorage.demo.controller;

import com.filestorage.demo.dto.*;
import com.filestorage.demo.exception.FileDownloadDTO;
import com.filestorage.demo.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/files")
@AllArgsConstructor
public class FileStorageController {
    FileService fileService;
    private static final String DOWNLOAD_ENDPOINT = "/download";
    private static final String CONTROLLER_NAME = "/files";

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("email") String userEmail,
            @RequestParam("accessLevel") AccessLevel accessLevel,
            @RequestParam("fileName") String fileName,
            @RequestParam(required = false, value = "tags") Set<String> tags,
            @RequestPart("file") MultipartFile file) throws IOException, NoSuchAlgorithmException {

        FileUploadDTO fileMeta = new FileUploadDTO(accessLevel, fileName, tags);
        FileUploadResponse response = fileService.uploadFile(file, fileMeta, userEmail, CONTROLLER_NAME + DOWNLOAD_ENDPOINT);
        return ResponseEntity.created(response.getFileDownloadUrl()).body(response);
    }

    @GetMapping(DOWNLOAD_ENDPOINT + "/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) {
        FileDownloadDTO fileDownloadDTO = fileService.downloadFile(fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileDownloadDTO.getFileName() + "\"")
                .body(new InputStreamResource(new java.io.ByteArrayInputStream(fileDownloadDTO.getFileStream().toByteArray())));
    }

    @PatchMapping(
            path = "/{fileId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateFileContent(
            @PathVariable String fileId,
            @RequestParam("email") String userEmail,
            @RequestParam(required = false, value = "fileName") String fileName,
            @RequestPart(required = false, value = "file") MultipartFile file
    ) throws IOException, NoSuchAlgorithmException {
        fileService.updateFile(userEmail, fileId, fileName, file);
        return ResponseEntity.ok("File updated");
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileInfoDTO>> listFiles(
            @RequestParam(required = true, value = "email") String userEmail,
            @RequestParam(required = true, value = "accessLevel") AccessLevel accessLevel,
            @RequestParam(required = true, value = "page", defaultValue = "0") int page,
            @RequestParam(required = true, value = "size", defaultValue = "50") int size,
            @RequestParam(required = false, value = "sortBy") FileOrderBy sortBy,
            @RequestParam(required = false, value = "order") Sort.Direction order,
            @RequestParam(required = false, value = "tag") Set<String> tags
    ) {

        return ResponseEntity.ok(fileService.getFileList(userEmail, accessLevel, tags, sortBy, order, page, size, DOWNLOAD_ENDPOINT));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String fileId,
            @RequestParam(required = true, value = "email") String userEmail) {
        fileService.deleteFile(userEmail, fileId);
        return ResponseEntity.ok("File deleted");
    }
}

