package com.toolbox.tools.spreadsheet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConversionResult {
    private final File inputFile;
    private final List<File> outputFiles;
    private final boolean success;
    private final String error;

    public ConversionResult(File inputFile, List<File> outputFiles) {
        this.inputFile = inputFile;
        this.outputFiles = new ArrayList<>(outputFiles);
        this.success = true;
        this.error = null;
    }

    public ConversionResult(File inputFile, String error) {
        this.inputFile = inputFile;
        this.outputFiles = new ArrayList<>();
        this.success = false;
        this.error = error;
    }

    public File getInputFile() {
        return inputFile;
    }

    public List<File> getOutputFiles() {
        return new ArrayList<>(outputFiles);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
