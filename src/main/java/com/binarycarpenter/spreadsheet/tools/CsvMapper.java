package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvMapper {
    private final File sourceFile;
    private final File targetFile;
    private final int sourceLookupColumn;
    private final int sourceValueColumn;
    private final int targetLookupColumn;
    private final int targetOutputColumn;
    private final String defaultValue;
    private Map<String, String> lookupMap;

    public CsvMapper(File sourceFile, File targetFile, 
                    int sourceLookupColumn, int sourceValueColumn,
                    int targetLookupColumn, int targetOutputColumn,
                    String defaultValue) {
        this.sourceFile = sourceFile;
        this.targetFile = targetFile;
        this.sourceLookupColumn = sourceLookupColumn;
        this.sourceValueColumn = sourceValueColumn;
        this.targetLookupColumn = targetLookupColumn;
        this.targetOutputColumn = targetOutputColumn;
        this.defaultValue = defaultValue;
    }

    public File performMapping() throws IOException, CsvException {
        // Build lookup map from source file
        buildLookupMap();

        // Create output file
        File outputFile = new File(targetFile.getParent(), 
            "mapped_" + targetFile.getName());

        // Read target file and perform mapping
        try (CSVReader reader = new CSVReader(new FileReader(targetFile));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            
            List<String[]> allRows = reader.readAll();
            List<String[]> outputRows = new ArrayList<>();

            // Process header
            if (!allRows.isEmpty()) {
                outputRows.add(allRows.get(0));
            }

            // Process data rows
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                String lookupValue = row[targetLookupColumn];
                String mappedValue = lookupMap.getOrDefault(lookupValue, defaultValue);
                
                // Create new row with mapped value
                String[] newRow = row.clone();
                newRow[targetOutputColumn] = mappedValue;
                outputRows.add(newRow);
            }

            // Write all rows to output file
            writer.writeAll(outputRows);
        }

        return outputFile;
    }

    private void buildLookupMap() throws IOException, CsvException {
        lookupMap = new HashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(sourceFile))) {
            List<String[]> allRows = reader.readAll();
            
            // Skip header row
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                String key = row[sourceLookupColumn];
                String value = row[sourceValueColumn];
                lookupMap.put(key, value);
            }
        }
    }

    public static String[] getHeaders(File csvFile) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            return reader.readNext();
        }
    }
}
