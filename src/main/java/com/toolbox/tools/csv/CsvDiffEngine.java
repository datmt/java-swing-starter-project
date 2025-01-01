package com.toolbox.tools.csv;

import com.opencsv.CSVReader;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.*;

@Slf4j
public class CsvDiffEngine {
    public static class DiffResult {
        private final List<String> headers;
        private final List<RowDiff> addedRows;
        private final List<RowDiff> removedRows;
        private final List<RowDiff> modifiedRows;
        private final Set<String> addedColumns;
        private final Set<String> removedColumns;

        public DiffResult(List<String> headers, List<RowDiff> addedRows, 
                         List<RowDiff> removedRows, List<RowDiff> modifiedRows,
                         Set<String> addedColumns, Set<String> removedColumns) {
            this.headers = headers;
            this.addedRows = addedRows;
            this.removedRows = removedRows;
            this.modifiedRows = modifiedRows;
            this.addedColumns = addedColumns;
            this.removedColumns = removedColumns;
        }

        public List<String> getHeaders() { return headers; }
        public List<RowDiff> getAddedRows() { return addedRows; }
        public List<RowDiff> getRemovedRows() { return removedRows; }
        public List<RowDiff> getModifiedRows() { return modifiedRows; }
        public Set<String> getAddedColumns() { return addedColumns; }
        public Set<String> getRemovedColumns() { return removedColumns; }
    }

    public static class RowDiff {
        private final Map<String, String> oldValues;
        private final Map<String, String> newValues;
        private final Set<String> modifiedColumns;
        private final List<String> keyValues;

        public RowDiff(Map<String, String> oldValues, Map<String, String> newValues, 
                      Set<String> modifiedColumns, List<String> keyValues) {
            this.oldValues = oldValues;
            this.newValues = newValues;
            this.modifiedColumns = modifiedColumns;
            this.keyValues = keyValues;
        }

        public Map<String, String> getOldValues() { return oldValues; }
        public Map<String, String> getNewValues() { return newValues; }
        public Set<String> getModifiedColumns() { return modifiedColumns; }
        public List<String> getKeyValues() { return keyValues; }
    }

    private List<String[]> records1;
    private List<String[]> records2;

    public DiffResult compareCsvFiles(File file1, File file2, List<String> keyColumns, 
                                    boolean ignoreCase, boolean ignoreWhitespace) throws IOException {
        // Parse both files
        records1 = readCsv(file1);
        records2 = readCsv(file2);

        if (records1.isEmpty() || records2.isEmpty()) {
            throw new IOException("One or both files are empty or could not be read");
        }

        // Get headers
        String[] headers1 = records1.get(0);
        String[] headers2 = records2.get(0);

        // Validate key columns exist in both files
        validateKeyColumns(headers1, keyColumns);
        validateKeyColumns(headers2, keyColumns);

        // Find column differences
        Set<String> addedColumns = new HashSet<>(Arrays.asList(headers2));
        addedColumns.removeAll(Arrays.asList(headers1));
        Set<String> removedColumns = new HashSet<>(Arrays.asList(headers1));
        removedColumns.removeAll(Arrays.asList(headers2));

        // Create maps for both files using key columns
        Map<String, String[]> map1 = createRecordMap(records1, keyColumns, ignoreCase);
        Map<String, String[]> map2 = createRecordMap(records2, keyColumns, ignoreCase);

        // Compare records
        List<RowDiff> addedRows = new ArrayList<>();
        List<RowDiff> removedRows = new ArrayList<>();
        List<RowDiff> modifiedRows = new ArrayList<>();

        // Find removed and modified rows
        for (Map.Entry<String, String[]> entry : map1.entrySet()) {
            String key = entry.getKey();
            String[] record1 = entry.getValue();
            String[] record2 = map2.get(key);

            if (record2 == null) {
                // Row was removed
                removedRows.add(createRowDiff(record1, headers1, keyColumns, true));
            } else {
                // Compare rows for modifications
                RowDiff diff = compareRows(record1, record2, headers1, headers2, keyColumns, ignoreCase, ignoreWhitespace);
                if (diff != null) {
                    modifiedRows.add(diff);
                }
            }
        }

        // Find added rows
        for (Map.Entry<String, String[]> entry : map2.entrySet()) {
            String key = entry.getKey();
            if (!map1.containsKey(key)) {
                addedRows.add(createRowDiff(entry.getValue(), headers2, keyColumns, false));
            }
        }

        return new DiffResult(
            Arrays.asList(headers2),
            addedRows,
            removedRows,
            modifiedRows,
            addedColumns,
            removedColumns
        );
    }

    private void validateKeyColumns(String[] headers, List<String> keyColumns) {
        for (String keyColumn : keyColumns) {
            if (Arrays.asList(headers).indexOf(keyColumn) == -1) {
                throw new IllegalArgumentException("Key column not found: " + keyColumn);
            }
        }
    }

    private List<String[]> readCsv(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            return reader.readAll();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, String[]> createRecordMap(List<String[]> records, List<String> keyColumns, boolean ignoreCase) {
        Map<String, String[]> map = new HashMap<>();
        String[] headers = records.get(0);
        for (int i = 1; i < records.size(); i++) {
            String[] record = records.get(i);
            String key = generateKey(record, headers, keyColumns, ignoreCase);
            map.put(key, record);
        }
        return map;
    }

    private String generateKey(String[] record, String[] headers, List<String> keyColumns, boolean ignoreCase) {
        StringBuilder key = new StringBuilder();
        for (String column : keyColumns) {
            int index = Arrays.asList(headers).indexOf(column);
            String value = record[index];
            key.append(ignoreCase ? value.toLowerCase() : value).append("|");
        }
        return key.toString();
    }

    private RowDiff compareRows(String[] record1, String[] record2, String[] headers1, String[] headers2, 
                              List<String> keyColumns, boolean ignoreCase, boolean ignoreWhitespace) {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();
        Set<String> modifiedColumns = new HashSet<>();
        List<String> keyValues = new ArrayList<>();

        // Store all values in maps
        for (int i = 0; i < headers1.length && i < record1.length; i++) {
            map1.put(headers1[i], record1[i]);
        }
        for (int i = 0; i < headers2.length && i < record2.length; i++) {
            map2.put(headers2[i], record2[i]);
        }

        // Compare all columns that exist in both files
        Set<String> allColumns = new HashSet<>();
        allColumns.addAll(Arrays.asList(headers1));
        allColumns.addAll(Arrays.asList(headers2));

        for (String column : allColumns) {
            String value1 = map1.get(column);
            String value2 = map2.get(column);

            // Skip if column doesn't exist in one of the files
            if (value1 == null || value2 == null) {
                continue;
            }

            if (ignoreWhitespace) {
                value1 = value1.trim();
                value2 = value2.trim();
            }
            if (ignoreCase) {
                value1 = value1.toLowerCase();
                value2 = value2.toLowerCase();
            }

            if (!value1.equals(value2)) {
                modifiedColumns.add(column);
            }
        }

        // If there are modifications, return a diff with all values
        if (!modifiedColumns.isEmpty()) {
            return new RowDiff(map1, map2, modifiedColumns, extractKeyValues(record1, headers1, keyColumns));
        }
        return null;
    }

    private List<String> extractKeyValues(String[] record, String[] headers, List<String> keyColumns) {
        List<String> keyValues = new ArrayList<>();
        for (String keyColumn : keyColumns) {
            int index = Arrays.asList(headers).indexOf(keyColumn);
            if (index >= 0 && index < record.length) {
                keyValues.add(record[index]);
            }
        }
        return keyValues;
    }

    private RowDiff createRowDiff(String[] record, String[] headers, List<String> keyColumns, boolean isOld) {
        Map<String, String> values = new HashMap<>();
        List<String> keyValues = new ArrayList<>();

        if (record != null && headers != null) {
            // Store all values
            for (int i = 0; i < headers.length && i < record.length; i++) {
                values.put(headers[i], record[i]);
            }

            // Extract key values
            keyValues = extractKeyValues(record, headers, keyColumns);
        }

        return new RowDiff(
            isOld ? values : new HashMap<>(),  // oldValues
            isOld ? new HashMap<>() : values,  // newValues
            new HashSet<>(),                   // no modified columns for added/removed rows
            keyValues
        );
    }
}
