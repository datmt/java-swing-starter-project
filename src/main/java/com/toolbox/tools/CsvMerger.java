package com.toolbox.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CsvMerger {
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
}
