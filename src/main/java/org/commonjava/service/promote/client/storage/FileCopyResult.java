package org.commonjava.service.promote.client.storage;

public class FileCopyResult
{
    private boolean success;

    private String message;

    public FileCopyResult( boolean success ) {
        this.success = success;
    }

    public FileCopyResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
