# File Server API

## Overview
This is a file server API that allows users to upload, download, update, delete, and list files. It also provides a tagging system to categorize files.

## Base URL
```
http://localhost:8080
```

## Installation
### Prerequisites
- Java 17+
- MongoDB
- Docker (optional)
- Maven

### Running with Docker
To start the application using Docker, execute:
```sh
docker-compose up --build
```

### Running Locally
1. Configure `application.properties`:
    ```properties
    spring.data.mongodb.uri=mongodb://mongo:27017/fileserver
    ```
2. Build and run the application:
    ```sh
    mvn clean install
    java -jar ./target/demo-0.0.1-SNAPSHOT.jar
    ```

## API Endpoints

### Upload File
`POST /files/upload`
#### Request Parameters
| Parameter     | Type    | Required | Description |
|--------------|--------|----------|-------------|
| email        | string | Yes      | User email  |
| accessLevel  | string | Yes      | `PUBLIC` or `PRIVATE` |
| fileName     | string | Yes      | Name of the file |
| tags         | array  | No       | List of tags |

#### Request Body (multipart/form-data)
```json
{
  "file": "binary data"
}
```
#### Response
```json
{
  "fileDownloadUrl": "http://localhost:8080/files/download/{fileId}",
  "fileId": "abc123",
  "message": "File uploaded successfully."
}
```

### Download File
`GET /files/download/{fileId}`
#### Path Parameter
| Parameter | Type   | Required | Description |
|----------|-------|----------|-------------|
| fileId   | string | Yes      | Unique identifier for the file |

#### Response
- **200 OK**: Returns binary file content
- **404 Not Found**: File not found

### Update File
`PATCH /files/{fileId}`
#### Request Parameters
| Parameter  | Type   | Required | Description |
|-----------|--------|----------|-------------|
| email     | string | Yes      | User email  |
| fileName  | string | No       | New file name |

#### Request Body (multipart/form-data)
```json
{
  "file": "binary data"
}
```
#### Response
```json
{
  "message": "File updated successfully."
}
```

### Delete File
`DELETE /files/{fileId}`
#### Request Parameters
| Parameter | Type   | Required | Description |
|----------|-------|----------|-------------|
| fileId   | string | Yes      | Unique identifier for the file |
| email    | string | Yes      | User email |

#### Response
```json
{
  "message": "File deleted successfully."
}
```

### List Files
`GET /files/list`
#### Request Parameters
| Parameter   | Type    | Required | Description |
|------------|--------|----------|-------------|
| email      | string | Yes      | User email  |
| accessLevel | string | Yes      | `PUBLIC` or `PRIVATE` |
| page       | int    | No       | Page number (default: 0) |
| size       | int    | No       | Number of items per page (default: 50) |
| sortBy     | string | No       | `FILE_NAME`, `UPLOAD_DATE`, etc. |
| order      | string | No       | `ASC` or `DESC` |

#### Response
```json
[
  {
    "fileDownloadUrl": "http://localhost:8080/files/download/abc123",
    "fileId": "abc123",
    "fileName": "example.txt",
    "size": 1024,
    "contentType": "text/plain",
    "creationTime": "2024-03-16",
    "modificationTime": "2024-03-16",
    "owner": "user@example.com",
    "tags": ["document"]
  }
]
```

### Get Allowed Tags
`GET /tags`
#### Response
```json
[
  "document",
  "image",
  "video",
  "backup"
]
```

## Error Handling
| Status Code | Meaning |
|------------|---------|
| 400        | Bad Request |
| 404        | Not Found |
| 409        | Conflict (duplicate file, etc.) |
| 500        | Internal Server Error |


