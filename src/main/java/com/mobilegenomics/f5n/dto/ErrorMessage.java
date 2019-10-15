package com.mobilegenomics.f5n.dto;

public enum ErrorMessage {
    COMM_FAIL("Communication with client failed"),
    EMPTY_FILE_DIRECTORY("File directory is empty"),
    NULL("Null value");

    private String errorMessage;

    ErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
