package com.binarycarpenter.spreadsheet.tools;

import java.io.File;
import java.time.LocalDateTime;

public class ConversionRecord {
    private final File sourceFile;
    private File targetFile;
    private LocalDateTime timestamp;
    private ConversionStatus status;
    private String errorMessage;

    public ConversionRecord(File sourceFile) {
        this.sourceFile = sourceFile;
        this.timestamp = LocalDateTime.now();
        this.status = ConversionStatus.IN_PROGRESS;
    }

    public void setCompleted(File targetFile) {
        this.targetFile = targetFile;
        this.status = ConversionStatus.COMPLETED;
        this.timestamp = LocalDateTime.now();
    }

    public void setFailed(String errorMessage) {
        this.status = ConversionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now();
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ConversionStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public enum ConversionStatus {
        IN_PROGRESS("Converting...", "⏳"),
        COMPLETED("Completed", "✓"),
        FAILED("Failed", "❌");

        private final String label;
        private final String icon;

        ConversionStatus(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return icon + " " + label;
        }
    }
}
