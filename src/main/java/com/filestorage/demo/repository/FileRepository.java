package com.filestorage.demo.repository;

import com.filestorage.demo.dto.*;
import com.filestorage.demo.exception.BadRequestException;
import com.filestorage.demo.exception.InternalServerError;
import com.filestorage.demo.exception.NotFoundException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

@Repository
@AllArgsConstructor
public class FileRepository {

    private static Logger logger = LoggerFactory.getLogger(FileRepository.class);

    private GridFSBucket gridFSBucket;
    private MongoDatabase mongoDatabase;
    private static final String META = "metadata.";

    public boolean isFileNameExists(String fileName, String userEmail) {
        Document filter = new Document();
        filter.append(META + FileMetaData.FILE_NAME.getKey(), fileName);
        filter.append(META + FileMetaData.OWNER.getKey(), userEmail);

        GridFSFindIterable files = gridFSBucket.find(filter);
        return files.iterator().hasNext();
    }

    public boolean isFileHashExists(String fileHash, String userEmail) {
        Document filter = new Document();
        filter.append(META + FileMetaData.FILE_HASH.getKey(), fileHash);
        filter.append(META + FileMetaData.OWNER.getKey(), userEmail);

        GridFSFindIterable files = gridFSBucket.find(filter);
        return files.iterator().hasNext();
    }

    public void uploadFile(FileInfo fileInfo, MultipartFile file, String fileHash, String ownerEmail) throws IOException {
        InputStream inputStream = file.getInputStream();

        Document metadata = new Document()
                .append(FileMetaData.CONTENT_TYPE.getKey(), fileInfo.getContentType())
                .append(FileMetaData.OWNER.getKey(), ownerEmail)
                .append(FileMetaData.TAGS.getKey(), fileInfo.getTags())
                .append(FileMetaData.ACCESS_LEVEL.getKey(), fileInfo.getAccessLevel())
                .append(FileMetaData.CREATE_TIME.getKey(), fileInfo.getCreationTime())
                .append(FileMetaData.FILE_HASH.getKey(), fileHash)
                .append(FileMetaData.ID.getKey(), fileInfo.getFileId())
                .append(FileMetaData.FILE_NAME.getKey(), fileInfo.getFileName())
                .append(FileMetaData.FILE_SIZE.getKey(), file.getSize());
        GridFSUploadOptions options = new GridFSUploadOptions().metadata(metadata);

        gridFSBucket.uploadFromStream(fileInfo.getFileName(), inputStream, options);
    }

    public FileInfo findByIdAndOwner(String fileId, String ownerEmail) {
        Document filter = new Document();
        filter.append(META + FileMetaData.ID.getKey(), fileId);
        filter.append(META + FileMetaData.OWNER.getKey(), ownerEmail);

        GridFSFindIterable files = gridFSBucket.find(filter);
        return files.iterator().hasNext() ? FileInfo.fromFile(files.first()) : null;
    }

    public FileInfo findById(String fileId) {
        Document filter = new Document();
        filter.append(META + FileMetaData.ID.getKey(), fileId);

        GridFSFindIterable files = gridFSBucket.find(filter);
        return files.iterator().hasNext() ? FileInfo.fromFile(files.first()) : null;
    }

    public ByteArrayOutputStream getOutputStreamById(ObjectId fileId) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        gridFSBucket.downloadToStream(fileId, outputStream);
        return outputStream;
    }

    public void updateFile(GridFSFile existingFile, String newFileName, String newFileHash, MultipartFile newFile) throws IOException {
        Document metadata = existingFile.getMetadata();
        String currentFileName = metadata.getString(FileMetaData.FILE_NAME.getKey());

        boolean isNameChanged = StringUtils.isNotBlank(newFileName) && !newFileName.equals(currentFileName);
        boolean isContentChanged = newFile != null;

        if (isNameChanged) {
            metadata.put(FileMetaData.FILE_NAME.getKey(), newFileName);
        }

        if (isContentChanged) {
            // Upload new content with same UUID
            metadata.put(FileMetaData.FILE_HASH.getKey(), newFileHash);
            metadata.put(FileMetaData.FILE_SIZE.getKey(), newFile.getSize());

            updateFileContent(existingFile.getObjectId(), newFile, metadata.getString(FileMetaData.FILE_NAME.getKey()), metadata);
        } else {
            updateMetadata(existingFile.getObjectId(), metadata);
        }
    }

    private void updateFileContent(ObjectId objectId, MultipartFile file, String fileName, Document metadata) throws IOException {
        GridFSUploadOptions options = new GridFSUploadOptions().metadata(metadata);
        InputStream inputStream = file.getInputStream();
        ObjectId newFileObjectId = gridFSBucket.uploadFromStream(fileName, inputStream, options);

        // Delete the old file content
        try {
            gridFSBucket.delete(objectId);
        } catch (Exception ex) {
            logger.error("Failed to delete old file");
            gridFSBucket.delete(newFileObjectId);
            throw new InternalServerError("Failed to delete old file");
        }
    }

    private void updateMetadata(ObjectId objectId, Document metadata) throws IOException {
        MongoCollection<Document> filesCollection = mongoDatabase.getCollection("fs.files");

        long updatedCount = filesCollection.updateOne(
                Filters.eq("_id", objectId),
                Updates.set("metadata", metadata)
        ).getModifiedCount();

        if (updatedCount < 1) {
            throw new IOException("Metadata update failed");
        }
    }

    public List<FileInfo> findFileInfoListPagenated(String userMail, AccessLevel accessLevel, Set<String> tags, FileOrderBy orderBy,
                                                    Sort.Direction order, int page, int size) {
        Document filter = new Document();
        if (accessLevel == AccessLevel.PUBLIC) {
            filter.append(META + FileMetaData.ACCESS_LEVEL.getKey(), AccessLevel.PUBLIC);
        } else {
            filter.append(META + FileMetaData.OWNER.getKey(), userMail);
        }
        if (tags != null && !tags.isEmpty()) {
            filter.append(META + FileMetaData.TAGS.getKey(), new Document("$in", tags.stream().map(String::toLowerCase).collect(Collectors.toSet()))); // Matches any of the tags
        }

        Document sorting = new Document();
        if (orderBy != null) {
            if (order == null) {
                throw new BadRequestException("Order must be provided together with orderBy");
            }

            // FIXME: Support ordering by tags
            sorting.append(META + orderBy.getSearchKey(), order == Sort.Direction.ASC ? 1 : -1);
        }

        List<FileInfo> filesList = new ArrayList<>();
        gridFSBucket.find(filter)
                .sort(sorting)
                .skip(page * size)
                .limit(size)
                .forEach(file -> {
                    FileInfo fileInfo = FileInfo.fromFile(file);
                    filesList.add(fileInfo);
                });

        return filesList;
    }

    public void deleteFile(String fileId, String userEmail) {
        Document filter = new Document();
        filter.append(META + FileMetaData.OWNER.getKey(), userEmail);
        filter.append(META + FileMetaData.ID.getKey(), fileId);

        GridFSFile file = gridFSBucket.find(filter).first();
        if (file == null) {
            throw new NotFoundException("File not found");
        }

        // TODO: May be soft delete?
        gridFSBucket.delete(file.getObjectId());
    }
}
