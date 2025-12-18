package rs.examproject.file_service.dto;

import java.io.Serializable;

public class FileProcessingMessage implements Serializable {
    private String fileName;
    private String operation;
    private String userId;

    public FileProcessingMessage() {
    }

    public FileProcessingMessage(String fileName, String operation, String userId) {
        this.fileName = fileName;
        this.operation = operation;
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

