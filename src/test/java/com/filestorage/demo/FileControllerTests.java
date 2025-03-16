package com.filestorage.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filestorage.demo.config.SecurityConfiguration;
import com.filestorage.demo.controller.FileStorageController;
import com.filestorage.demo.dto.FileInfoDTO;
import com.filestorage.demo.dto.FileUploadResponse;
import com.filestorage.demo.exception.FileDownloadDTO;
import com.filestorage.demo.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.Mockito.*;

@WebMvcTest(FileStorageController.class)  // Test only FileController
@Import(SecurityConfiguration.class)  // Use your actual security config
public class FileControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;  // To convert objects to JSON

    private static final String BASE_URL = "/files";

    @Test
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        FileUploadResponse mockResponse = FileUploadResponse.builder()
                .fileDownloadUrl( URI.create("http://localhost:8080/files/download/123"))
                .fileId("123")
                .message("File uploaded successfully")
                .build();

        when(fileService.uploadFile(any(), any(), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(multipart(BASE_URL + "/upload")
                        .file(file)
                        .param("email", "user@example.com")
                        .param("accessLevel", "PUBLIC")
                        .param("fileName", "test.txt")
                        .param("tags", "tag1", "tag2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.fileDownloadUrl").value("http://localhost:8080/files/download/123"))
                .andExpect(jsonPath("$.fileId").value("123"));

    }

    @Test
    void testDownloadFile() throws Exception {
        byte[] bytes = new byte[]{65, 66, 67};
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
        baos.write(bytes, 0, bytes.length);

        FileDownloadDTO mockDownload = new FileDownloadDTO( baos, "test.txt"); // "ABC" as byte array

        when(fileService.downloadFile("123")).thenReturn(mockDownload);

        mockMvc.perform(get(BASE_URL + "/download/123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""));
    }

    @Test
    void testUpdateFileContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "updated.txt", "text/plain", "Updated content".getBytes());

        doNothing().when(fileService).updateFile(any(), any(), any(), any());

        mockMvc.perform(multipart(BASE_URL + "/123")
                        .file(file)
                        .param("email", "user@example.com")
                        .param("fileName", "updated.txt")
                        .with(request -> {
                            request.setMethod("PATCH"); // Override to PATCH
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().string("File updated"));
    }

    @Test
    void testListFiles() throws Exception {
        FileInfoDTO mockFile1 = new FileInfoDTO("http://localhost:8080/files/download/123", "123",
                "test.doc", 4L, "text/plain", List.of("document"), LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 2), "user1");
        FileInfoDTO mockFile2 = new FileInfoDTO("http://localhost:8080/files/download/456", "456",
                "test1.xml", 4L, "text/plain", List.of("document"), LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 2, 2), "user1");

        when(fileService.getFileList(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(mockFile1, mockFile2));

        mockMvc.perform(get(BASE_URL + "/list")
                        .param("email", "user@example.com")
                        .param("accessLevel", "PUBLIC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").value("test.doc"))
                .andExpect(jsonPath("$[1].fileName").value("test1.xml"));
    }

    @Test
    void testDeleteFile() throws Exception {
        doNothing().when(fileService).deleteFile(any(), any());

        mockMvc.perform(delete(BASE_URL + "/123")
                        .param("email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("File deleted"));
    }
}
