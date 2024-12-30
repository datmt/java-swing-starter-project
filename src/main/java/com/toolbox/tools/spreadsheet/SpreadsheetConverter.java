package com.toolbox.tools.spreadsheet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jopendocument.dom.spreadsheet.SpreadSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class SpreadsheetConverter {
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9-_.]");
    private static final Logger log = LoggerFactory.getLogger(SpreadsheetConverter.class);    
    public static ConversionResult convertToCSV(File inputFile, File outputDir) throws IOException {
        List<File> outputFiles = new ArrayList<>();
        try {
            String extension = getFileExtension(inputFile).toLowerCase();
            switch (extension) {
                case "xlsx":
                case "xls":
                    outputFiles.addAll(convertExcelToCSV(inputFile, outputDir));
                    break;
                case "ods":
                    outputFiles.addAll(convertOdsToCSV(inputFile, outputDir));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported file format: " + extension);
            }
            return new ConversionResult(inputFile, outputFiles);
        } catch (Exception e) {
            log.error("Error during conversion", e);
            return new ConversionResult(inputFile, e.getMessage());
        }
    }

    private static List<File> convertExcelToCSV(File inputFile, File outputDir) throws IOException {
        List<File> outputFiles = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputFile)) {
            // Create formula evaluator
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                String baseFileName = getBaseFileName(inputFile.getName());
                String csvFileName = normalizeFileName(baseFileName + "_" + sheetName + ".csv");
                File csvFile = new File(outputDir, csvFileName);
                
                try (FileWriter fw = new FileWriter(csvFile);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    
                    // Convert each row
                    for (Row row : sheet) {
                        StringBuilder line = new StringBuilder();
                        for (Cell cell : row) {
                            if (line.length() > 0) {
                                line.append(",");
                            }
                            line.append(getCellValueAsString(cell, evaluator));
                        }
                        bw.write(line.toString());
                        bw.newLine();
                    }
                }
                outputFiles.add(csvFile);
            }
        }
        return outputFiles;
    }

    private static List<File> convertOdsToCSV(File inputFile, File outputDir) throws IOException {
        List<File> outputFiles = new ArrayList<>();
        try {
            SpreadSheet spreadSheet = SpreadSheet.createFromFile(inputFile);
            int sheetCount = spreadSheet.getSheetCount();
            
            for (int i = 0; i < sheetCount; i++) {
                org.jopendocument.dom.spreadsheet.Sheet sheet = spreadSheet.getSheet(i);
                String sheetName = sheet.getName();
                
                String baseFileName = getBaseFileName(inputFile.getName());
                String csvFileName = normalizeFileName(baseFileName + "_" + sheetName + ".csv");
                File csvFile = new File(outputDir, csvFileName);
                
                try (FileWriter fw = new FileWriter(csvFile);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    
                    // Get sheet dimensions
                    int rowCount = sheet.getRowCount();
                    int colCount = sheet.getColumnCount();
                    
                    // Convert each row
                    for (int row = 0; row < rowCount; row++) {
                        StringBuilder line = new StringBuilder();
                        for (int col = 0; col < colCount; col++) {
                            if (col > 0) {
                                line.append(",");
                            }
                            Object value = sheet.getCellAt(col, row).getValue();
                            line.append(value != null ? escapeCSV(value.toString()) : "");
                        }
                        bw.write(line.toString());
                        bw.newLine();
                    }
                }
                outputFiles.add(csvFile);
            }
        } catch (Exception e) {
            throw new IOException("Error converting ODS file: " + e.getMessage(), e);
        }
        return outputFiles;
    }

    private static String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        
        String value;
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getLocalDateTimeCellValue().toString();
                } else {
                    value = String.valueOf(cell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                CellValue cellValue = evaluator.evaluate(cell);
                switch (cellValue.getCellType()) {
                    case STRING:
                        value = cellValue.getStringValue();
                        break;
                    case NUMERIC:
                        value = String.valueOf(cellValue.getNumberValue());
                        break;
                    case BOOLEAN:
                        value = String.valueOf(cellValue.getBooleanValue());
                        break;
                    default:
                        value = "";
                }
                break;
            default:
                value = "";
        }
        return escapeCSV(value);
    }

    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        if (!needsQuotes) {
            return value;
        }
        
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex > 0 ? name.substring(lastDotIndex + 1) : "";
    }

    private static String getBaseFileName(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    public static String normalizeFileName(String fileName) {
        // Remove extension
        String baseName = getBaseFileName(fileName);
        String extension = getFileExtension(new File(fileName));
        
        // Normalize base name
        String normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "") // Remove non-ASCII
                .replaceAll("\\s+", "_")         // Replace spaces with underscore
                .replaceAll("[^a-zA-Z0-9-_]", "") // Remove other invalid chars
                .toLowerCase();
        
        // Ensure the name is not empty and add extension
        normalized = (normalized.isEmpty() ? "file" : normalized) + 
                    (extension.isEmpty() ? "" : "." + extension);
        
        return normalized;
    }
}
