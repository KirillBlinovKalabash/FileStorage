package com.filestorage.demo;

import com.filestorage.demo.dto.*;
import com.filestorage.demo.exception.BadRequestException;
import com.filestorage.demo.exception.DuplicateEntryException;
import com.filestorage.demo.exception.NotFoundException;
import com.filestorage.demo.repository.FileRepository;
import com.filestorage.demo.repository.FileInfo;
import com.filestorage.demo.service.FileService;
import com.filestorage.demo.service.TagService;
import com.filestorage.demo.utils.Utils;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {
    @InjectMocks
    private FileService fileService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private TagService tagService;

    @Mock
    private Utils utils;

    private MultipartFile mockFile;
    private FileUploadDTO fileMeta;
    private FileInfo existingFile;
    private String userEmail = "user@example.com";
    private String downloadEndpoint = "/files/download";
    private String fileId = "123";
    private final AccessLevel accessLevel = AccessLevel.PRIVATE;
    private final Set<String> tags = Set.of("document", "video");
    private final FileOrderBy orderBy = FileOrderBy.FILE_NAME;
    private final Sort.Direction order = Sort.Direction.ASC;
    private final int page = 0;
    private final int size = 10;

    @BeforeEach
    void setUp() throws IOException {
        mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());

        fileMeta = new FileUploadDTO(AccessLevel.PUBLIC, "test.txt", new HashSet<>(Arrays.asList("document", "backup")));

        existingFile = new FileInfo(fileId, "oldFile.txt", 12345L, "text/plain",
                Collections.singletonList("backup"), LocalDate.now(), LocalDate.now(), userEmail, AccessLevel.PUBLIC, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setScheme("http");
        request.setServerPort(8080);

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void shouldThrowException_WhenFileIsEmpty() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        Exception exception = assertThrows(BadRequestException.class, () ->
                fileService.uploadFile(emptyFile, fileMeta, userEmail, downloadEndpoint)
        );

        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void shouldThrowException_WhenTooManyTags() {
        fileMeta.setTags(new HashSet<>(Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5", "tag6"))); // Exceed limit

        Exception exception = assertThrows(BadRequestException.class, () ->
                fileService.uploadFile(mockFile, fileMeta, userEmail, downloadEndpoint)
        );

        assertTrue(exception.getMessage().contains("Too many tags provided"));
    }

    @Test
    void shouldThrowException_WhenInvalidTagProvided() {
        //when(tagService.isValidTag("invalidTag")).thenReturn(false);

        fileMeta.setTags(new HashSet<>(Arrays.asList("document", "invalidTag")));

        Exception exception = assertThrows(BadRequestException.class, () ->
                fileService.uploadFile(mockFile, fileMeta, userEmail, downloadEndpoint)
        );

        assertTrue(exception.getMessage().contains("Invalid tag provided:"));
    }

    @Test
    void shouldThrowException_WhenDuplicateFileNameOrHash() throws IOException, NoSuchAlgorithmException {
        when(fileRepository.isFileNameExists("test.txt", userEmail)).thenReturn(true);
        when(fileRepository.isFileHashExists(anyString(), eq(userEmail))).thenReturn(true);
        when(tagService.isValidTag(any())).thenReturn(true);

        Exception exception = assertThrows(DuplicateEntryException.class, () ->
                fileService.uploadFile(mockFile, fileMeta, userEmail, downloadEndpoint)
        );

        assertTrue(exception.getMessage().contains("File with that name already exists"));
        assertTrue(exception.getMessage().contains("The same file is already uploaded"));
    }

    @Test
    void shouldUploadFileSuccessfully() throws IOException, NoSuchAlgorithmException {
        when(fileRepository.isFileNameExists("test.txt", userEmail)).thenReturn(false);
        when(fileRepository.isFileHashExists(anyString(), eq(userEmail))).thenReturn(false);
        when(tagService.isValidTag(any())).thenReturn(true);

        doNothing().when(fileRepository).uploadFile(any(), any(), anyString(), anyString());
        FileUploadResponse response = fileService.uploadFile(mockFile, fileMeta, userEmail, downloadEndpoint);

        assertNotNull(response);
        assertNotNull(response.getFileId());
        assertNotNull(response.getFileDownloadUrl());
    }

    @Test
    void shouldThrowException_WhenFileNotFound() {
        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(null);

        Exception exception = assertThrows(NotFoundException.class, () ->
                fileService.updateFile(userEmail, fileId, "newFile.txt", mockFile)
        );

        assertEquals("File to update not found", exception.getMessage());
    }

    @Test
    void shouldThrowException_WhenRenamingToExistingFileName() {
        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(existingFile);
        when(fileRepository.isFileNameExists("newFile.txt", userEmail)).thenReturn(true);

        Exception exception = assertThrows(DuplicateEntryException.class, () ->
                fileService.updateFile(userEmail, fileId, "newFile.txt", null) // Renaming only
        );

        assertEquals("File with the same name already exists.", exception.getMessage());
    }

    @Test
    void shouldThrowException_WhenNewFileIsEmpty() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(existingFile);

        Exception exception = assertThrows(BadRequestException.class, () ->
                fileService.updateFile(userEmail, fileId, null, emptyFile)
        );

        assertEquals("File is empty", exception.getMessage());
    }

    @Test
    void shouldThrowException_WhenNewFileContentIsDuplicate() throws IOException, NoSuchAlgorithmException {
        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(existingFile);
        when(fileRepository.isFileHashExists(any(), eq(userEmail))).thenReturn(true);

        Exception exception = assertThrows(DuplicateEntryException.class, () ->
                fileService.updateFile(userEmail, fileId, null, mockFile)
        );

        assertEquals("File with the same content already exists.", exception.getMessage());
    }

    @Test
    void shouldUpdateFile_WhenRenamingOnly() throws IOException, NoSuchAlgorithmException {
        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(existingFile);
        when(fileRepository.isFileNameExists("newFile.txt", userEmail)).thenReturn(false);

        doNothing().when(fileRepository).updateFile(any(), anyString(), any(), any());

        fileService.updateFile(userEmail, fileId, "newFile.txt", null);

        verify(fileRepository, times(1)).updateFile(existingFile.getFile(), "newFile.txt", null, null);
    }

 /*   @Test
    void shouldUpdateFile_WhenUpdatingContentOnly() throws IOException, NoSuchAlgorithmException {
        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(existingFile);
        when(fileRepository.isFileHashExists(any(), eq(userEmail))).thenReturn(false);

        doNothing().when(fileRepository).updateFile(any(), any(), any(), any());

        mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes(StandardCharsets.UTF_8));
        fileService.updateFile(userEmail, fileId, null, mockFile);

        verify(fileRepository, times(1)).updateFile(existingFile.getFile(), null, "newFileHash", mockFile);
    }*/

/*    @Test
    void shouldUpdateFile_WhenRenamingAndUpdatingContent() throws IOException, NoSuchAlgorithmException {
        when(fileRepository.findByIdAndOwner(fileId, userEmail)).thenReturn(existingFile);
        when(fileRepository.isFileNameExists("newFile.txt", userEmail)).thenReturn(false);
        when(fileRepository.isFileHashExists(any(), eq(userEmail))).thenReturn(false);

        doNothing().when(fileRepository).updateFile(any(), any(), any(), any());

        fileService.updateFile(userEmail, fileId, "newFile.txt", mockFile);

        verify(fileRepository, times(1)).updateFile(existingFile.getFile(), "newFile.txt", "newFileHash", mockFile);
    }*/

    @Test
    void shouldThrowException_WhenFileNotFound_OnDownload() {
        when(fileRepository.findById(fileId)).thenReturn(null);

        Exception exception = assertThrows(BadRequestException.class, () ->
                fileService.downloadFile(fileId)
        );

        assertEquals("File not found", exception.getMessage());
    }

    // Commented out because failed to mock GridFSFile
 /*   @Test
    void shouldReturnFileDownloadDTO_WhenFileExists() throws IOException {

        byte[] fileContent = "Hello, World!".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);  // Copy data

        when(fileRepository.findById(fileId)).thenReturn(existingFile);
        when(fileRepository.getOutputStreamById(any())).thenReturn(outputStream);

        FileDownloadDTO result = fileService.downloadFile(fileId);

        assertNotNull(result);
        assertEquals("oldFile.txt", result.getFileName());
        assertEquals(fileContent, result.getFileStream().toByteArray());

        verify(fileRepository, times(1)).findById(fileId);
        verify(fileRepository, times(1)).getOutputStreamById(existingFile.getFile().getObjectId());
    }*/

    @Test
    void shouldThrowException_WhenPageSizeExceedsLimit() {
        int invalidSize = 200; // Assuming MAX_PAGE_SIZE is less than 200

        Exception exception = assertThrows(BadRequestException.class, () ->
                fileService.getFileList(userEmail, accessLevel, tags, orderBy, order, page, invalidSize, downloadEndpoint)
        );

        assertEquals("Page size must be less than 100", exception.getMessage());
    }

    @Test
    void shouldReturnEmptyList_WhenNoFilesFound() {
        when(fileRepository.findFileInfoListPagenated(userEmail, accessLevel, tags, orderBy, order, page, size))
                .thenReturn(Collections.emptyList());

        List<FileInfoDTO> result = fileService.getFileList(userEmail, accessLevel, tags, orderBy, order, page, size, downloadEndpoint);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(fileRepository, times(1)).findFileInfoListPagenated(userEmail, accessLevel, tags, orderBy, order, page, size);
    }

    @Test
    void shouldReturnFileList_WhenFilesExist() {
        Document id = new Document();
        GridFSFile file = new GridFSFile(id.toBsonDocument(), "aaa", 4L, 4, Date.from(Instant.now()), null);
        List<FileInfo> fileInfos = List.of(
                new FileInfo("file1", "file1.txt", 1024L, "text/plain", Collections.singletonList("video"), LocalDate.now(), LocalDate.now(), userEmail, accessLevel, file),
                new FileInfo("file2", "file2.txt", 2048L, "text/plain", Collections.singletonList("document"), LocalDate.now(), LocalDate.now(), userEmail, accessLevel, file)
        );

        when(fileRepository.findFileInfoListPagenated(userEmail, accessLevel, tags, orderBy, order, page, size))
                .thenReturn(fileInfos);

        List<FileInfoDTO> result = fileService.getFileList(userEmail, accessLevel, tags, orderBy, order, page, size, downloadEndpoint);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0).getFileName());
        assertEquals("file2.txt", result.get(1).getFileName());
        assertTrue(result.get(0).getFileDownloadUrl().contains("/download/file1"));
        assertTrue(result.get(1).getFileDownloadUrl().contains("/download/file2"));

        verify(fileRepository, times(1)).findFileInfoListPagenated(userEmail, accessLevel, tags, orderBy, order, page, size);
    }

    @Test
    void shouldCallRepository_WhenDeletingFile() {
        fileService.deleteFile(userEmail, fileId);
        verify(fileRepository, times(1)).deleteFile(fileId, userEmail);
    }

    @Test
    void shouldThrowException_WhenFileNotFound_OnDelete() {
        // Simulate an exception in repository
        doThrow(new NotFoundException("File not found")).when(fileRepository).deleteFile(fileId, userEmail);

        Exception exception = assertThrows(NotFoundException.class, () ->
                fileService.deleteFile(userEmail, fileId)
        );

        assertEquals("File not found", exception.getMessage());

        verify(fileRepository, times(1)).deleteFile(fileId, userEmail);
    }
}
