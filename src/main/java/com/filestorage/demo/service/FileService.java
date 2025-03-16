package com.filestorage.demo.service;

import com.filestorage.demo.dto.*;
import com.filestorage.demo.exception.*;
import com.filestorage.demo.repository.FileInfo;
import com.filestorage.demo.repository.FileRepository;
import com.filestorage.demo.utils.Utils;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.apache.tika.Tika;

@Service
@AllArgsConstructor
public class FileService {

    private static Logger logger = LoggerFactory.getLogger(FileService.class);

    private static final int MAX_ALLOWED_TAGS = 5;
    private static final int MAX_PAGE_SIZE = 100;

    FileRepository fileRepository;
    TagService tagService;

    private URI generateDownloadURL(String downloadEndpoint, String fileId){
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path( downloadEndpoint + "/{fileId}")
                .buildAndExpand(fileId)
                .toUri(); // TODO: Add key with expiration
    }

    private boolean isValidFileNameFormat(String input) {
        return input.matches("^[a-zA-Z_0-9]+\\.[a-zA-Z_0-9]+$");
    }


    public FileUploadResponse uploadFile(MultipartFile file, FileUploadDTO fileMeta, String userEmail, String downloadEndpoint) throws IOException, NoSuchAlgorithmException {
        if (file.isEmpty()){
            throw new BadRequestException("File is empty");
        }

        if (!isValidFileNameFormat(fileMeta.getFileName())){
            throw new BadRequestException("File name should follow the format file_name.extension");
        }

        if (fileMeta.getTags() != null) {
            if (fileMeta.getTags().size() > MAX_ALLOWED_TAGS){
                throw new BadRequestException("Too many tags provided. Max allowed is " + MAX_ALLOWED_TAGS);
            }

            for (String tag : fileMeta.getTags()){
                if (!tagService.isValidTag(tag)){
                    throw new BadRequestException("Invalid tag provided: " + tag);
                }
            }
        }

        InputStream inputStream = file.getInputStream();
        String fileHash = Utils.computeSHA256(inputStream);

        String fileNameDuplication = fileRepository.isFileNameExists(fileMeta.getFileName(), userEmail) ? "File with that name already exists. " : " ";
        String fileHashDuplication = fileRepository.isFileHashExists(fileHash, userEmail) ? "The same file is already uploaded. " : " ";
        if (StringUtils.isNotBlank(fileNameDuplication) || StringUtils.isNotBlank(fileHashDuplication)) {
            throw new DuplicateEntryException(fileNameDuplication + fileHashDuplication);
        }

        LocalDate time = LocalDate.now();
        String fileId = UUID.randomUUID().toString();
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType)){
            if (StringUtils.isBlank(file.getOriginalFilename())) {
                contentType = Files.probeContentType(Path.of(Objects.requireNonNull(file.getOriginalFilename())));
            } else {
                Tika tika = new Tika();
                contentType = tika.detect(file.getInputStream());
            }
        }
        FileInfo fileInfo = new FileInfo(fileId, fileMeta.getFileName(), file.getSize(),
                contentType,
                fileMeta.getTags() != null ? fileMeta.getTags().stream().map(String::toLowerCase).collect(Collectors.toList()) : null,
                time, time, userEmail, fileMeta.getAccessLevel(), null);

        fileRepository.uploadFile(fileInfo, file, fileHash, userEmail);

        URI downloadUrl = generateDownloadURL(downloadEndpoint, fileId);
        return FileUploadResponse.builder().fileDownloadUrl(downloadUrl).fileId(fileId).build();
    }

    public void updateFile(String userEmail, String fileId, String newFileName, MultipartFile newFile) throws IOException, NoSuchAlgorithmException {

        FileInfo existingFileInfo = fileRepository.findByIdAndOwner(fileId, userEmail);
        if (existingFileInfo == null) {
            throw new NotFoundException("File to update not found");
        }

        String currentFileName = existingFileInfo.getFileName();
        boolean isNameChanged = StringUtils.isNotBlank(newFileName) && !newFileName.equals(currentFileName);

        if (isNameChanged) {
            if (!isValidFileNameFormat(newFileName)) {
                throw new BadRequestException("File name should follow the format file_name.extension");
            }

            if (fileRepository.isFileNameExists(newFileName, userEmail)) {
                throw new DuplicateEntryException("File with the same name already exists.");
            }
        }

        String newFileHash = null;
        if (newFile != null) {
            if (newFile.isEmpty()){
                throw new BadRequestException("File is empty");
            }

            newFileHash = Utils.computeSHA256(newFile.getInputStream());
            if (fileRepository.isFileHashExists(newFileHash, userEmail)) {
                throw new DuplicateEntryException("File with the same content already exists.");
            }
        }

        fileRepository.updateFile(existingFileInfo.getFile(), newFileName, newFileHash, newFile);
    }

    public FileDownloadDTO downloadFile(String fileId) {
        logger.info("Got download request for " + fileId);
        FileInfo fileInfo = fileRepository.findById(fileId);

        if (fileInfo == null) {
            throw new BadRequestException("File not found");
        }

        return new FileDownloadDTO(fileRepository.getOutputStreamById(fileInfo.getFile().getObjectId()), fileInfo.getFileName());
    }

    public List<FileInfoDTO> getFileList(String userMail, AccessLevel accessLevel, Set<String> tags, FileOrderBy orderBy,
                                         Sort.Direction order, int page, int size, String downloadEndpoint) {

        if (size > MAX_PAGE_SIZE){
            throw new BadRequestException("Page size must be less than " + MAX_PAGE_SIZE);
        }

        List<FileInfoDTO> result = fileRepository.findFileInfoListPagenated(userMail, accessLevel, tags, orderBy, order, page, size).stream().map(
                info -> {
                    String downloadUrl = generateDownloadURL(downloadEndpoint, info.getFileId()).toString();
                    return FileInfoDTO.fromFileInfo(info, downloadUrl);
                }).collect(Collectors.toList());
        return result;
    }

    public void deleteFile(String userEmail, String fileId) {
        fileRepository.deleteFile(fileId, userEmail);
    }
}
