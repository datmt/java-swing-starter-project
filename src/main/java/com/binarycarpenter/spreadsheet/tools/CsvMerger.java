package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class CsvMerger {
    public MergeResult analyzeHeaders(List<File> files) throws IOException, CsvException {
        Set<String> allHeaders = new LinkedHashSet<>();
        Map<File, List<String>> headersByFile = new HashMap<>();

        // Read headers from all files
        for (File file : files) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] headers = reader.readNext();
                if (headers != null) {
                    List<String> headerList = Arrays.asList(headers);
                    headersByFile.put(file, headerList);
                    allHeaders.addAll(headerList);
                }
            }
        }

        // Check if all files have the same headers
        boolean headersMatch = true;
        List<String> firstFileHeaders = headersByFile.values().iterator().next();
        for (List<String> headers : headersByFile.values()) {
            if (!headers.equals(firstFileHeaders)) {
                headersMatch = false;
                break;
            }
        }

        // If headers don't match, find missing headers for each file
        Map<File, List<String>> missingHeadersByFile = new HashMap<>();
        if (!headersMatch) {
            for (Map.Entry<File, List<String>> entry : headersByFile.entrySet()) {
                List<String> missingHeaders = new ArrayList<>(allHeaders);
                missingHeaders.removeAll(entry.getValue());
                if (!missingHeaders.isEmpty()) {
                    missingHeadersByFile.put(entry.getKey(), missingHeaders);
                }
            }
        }

        return new MergeResult(headersMatch, new ArrayList<>(allHeaders), missingHeadersByFile);
    }

    public void mergeFiles(List<File> files, File outputFile, MergeResult mergeResult) throws IOException, CsvException {
        List<String> combinedHeaders = mergeResult.getCombinedHeaders();

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Write combined headers
            writer.writeNext(combinedHeaders.toArray(new String[0]));

            // Process each file
            for (File file : files) {
                try (CSVReader reader = new CSVReader(new FileReader(file))) {
                    String[] headers = reader.readNext(); // Skip header row
                    if (headers == null) continue;

                    // Create a map of header index to combined headers index
                    Map<Integer, Integer> headerMapping = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        headerMapping.put(i, combinedHeaders.indexOf(headers[i]));
                    }

                    // Read and write data rows
                    String[] nextLine;
                    while ((nextLine = reader.readNext()) != null) {
                        String[] newLine = new String[combinedHeaders.size()];
                        Arrays.fill(newLine, ""); // Fill with empty strings

                        // Map values to their correct positions
                        for (int i = 0; i < nextLine.length; i++) {
                            Integer newIndex = headerMapping.get(i);
                            if (newIndex != null && i < nextLine.length) {
                                newLine[newIndex] = nextLine[i];
                            }
                        }

                        writer.writeNext(newLine);
                    }
                }
            }
        }
    }

    public void mergeToExcel(List<File> files, File outputFile) throws IOException, CsvException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create cell style for headers
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Keep track of used sheet names
            Set<String> usedSheetNames = new HashSet<>();

            // Process each file into a separate sheet
            for (File file : files) {
                String baseSheetName = file.getName().replaceAll("\\.csv$", "");
                if (baseSheetName.length() > 25) { // Leave room for random suffix
                    baseSheetName = baseSheetName.substring(0, 25);
                }

                // Generate unique sheet name
                String sheetName = baseSheetName;
                int attempt = 1;
                while (usedSheetNames.contains(sheetName)) {
                    String suffix = String.format("_%04d", (int) (Math.random() * 10000));
                    sheetName = baseSheetName + suffix;
                    if (sheetName.length() > 31) { // Excel sheet name length limit
                        sheetName = baseSheetName.substring(0, 31 - suffix.length()) + suffix;
                    }
                    attempt++;
                    if (attempt > 1000) { // Prevent infinite loop
                        throw new IOException("Unable to generate unique sheet name after 1000 attempts");
                    }
                }
                usedSheetNames.add(sheetName);

                Sheet sheet = workbook.createSheet(sheetName);

                // Read CSV data
                List<String[]> csvData;
                try (CSVReader reader = new CSVReader(new FileReader(file))) {
                    csvData = reader.readAll();
                }

                if (csvData.isEmpty()) continue;

                // Write headers
                Row headerRow = sheet.createRow(0);
                String[] headers = csvData.get(0);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Write data rows
                for (int i = 1; i < csvData.size(); i++) {
                    Row row = sheet.createRow(i);
                    String[] rowData = csvData.get(i);
                    for (int j = 0; j < rowData.length; j++) {
                        Cell cell = row.createCell(j);
                        cell.setCellValue(rowData[j]);
                    }
                }

                // Auto-size columns
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Write the workbook to file
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }
        }
    }

    public static class MergeResult {
        private final boolean headersMatch;
        private final List<String> combinedHeaders;
        private final Map<File, List<String>> missingHeadersByFile;

        public MergeResult(boolean headersMatch, List<String> combinedHeaders, Map<File, List<String>> missingHeadersByFile) {
            this.headersMatch = headersMatch;
            this.combinedHeaders = combinedHeaders;
            this.missingHeadersByFile = missingHeadersByFile;
        }

        public boolean isHeadersMatch() {
            return headersMatch;
        }

        public List<String> getCombinedHeaders() {
            return combinedHeaders;
        }

        public Map<File, List<String>> getMissingHeadersByFile() {
            return missingHeadersByFile;
        }
    }
}
