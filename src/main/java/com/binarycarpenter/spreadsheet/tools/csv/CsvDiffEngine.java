package com.binarycarpenter.spreadsheet.tools.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

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

    private List<Map<String, String>> records1;
    private List<Map<String, String>> records2;

    public DiffResult compareFiles(File file1, File file2, List<String> keyColumns,
                                 boolean ignoreCase, boolean ignoreWhitespace,
                                 boolean strictRowOrder, boolean ignoreDuplicates) throws IOException, CsvValidationException {
        try (CSVReader reader1 = new CSVReader(new FileReader(file1));
             CSVReader reader2 = new CSVReader(new FileReader(file2))) {

            // Read headers
            String[] headers1 = reader1.readNext();
            String[] headers2 = reader2.readNext();
            if (headers1 == null || headers2 == null) {
                throw new IOException("One or both files are empty");
            }

            // Convert headers to lists for easier manipulation
            List<String> headersList1 = Arrays.asList(headers1);
            List<String> headersList2 = Arrays.asList(headers2);

            // Find column differences
            Set<String> addedColumns = new HashSet<>(headersList2);
            addedColumns.removeAll(headersList1);
            Set<String> removedColumns = new HashSet<>(headersList1);
            removedColumns.removeAll(headersList2);

            // Read all rows
            List<String[]> rows1 = reader1.readAll();
            List<String[]> rows2 = reader2.readAll();

            // Convert rows to maps for easier comparison
            List<Map<String, String>> maps1 = convertRowsToMaps(rows1, headersList1);
            List<Map<String, String>> maps2 = convertRowsToMaps(rows2, headersList2);

            // If not ignoring duplicates, we should count them
            if (!ignoreDuplicates) {
                // Count occurrences in both files
                Map<String, Integer> occurrences1 = countOccurrences(maps1, headersList1, keyColumns, ignoreCase, ignoreWhitespace);
                Map<String, Integer> occurrences2 = countOccurrences(maps2, headersList2, keyColumns, ignoreCase, ignoreWhitespace);

                // If the counts are different for any row, add them to modified rows
                List<RowDiff> modifiedRows = new ArrayList<>();
                for (String key : occurrences1.keySet()) {
                    int count1 = occurrences1.get(key);
                    int count2 = occurrences2.getOrDefault(key, 0);
                    if (count1 != count2) {
                        // Find the original row data
                        Map<String, String> rowData = findRowByKey(maps1, headersList1, keyColumns, key, ignoreCase, ignoreWhitespace);
                        if (rowData != null) {
                            modifiedRows.add(new RowDiff(
                                rowData,
                                rowData,
                                new HashSet<>(),
                                extractKeyValues(rowData, keyColumns)
                            ));
                        }
                    }
                }

                // If we found any count differences, return them
                if (!modifiedRows.isEmpty()) {
                    return new DiffResult(
                        headersList1,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        modifiedRows,
                        addedColumns,
                        removedColumns
                    );
                }
            }

            // Continue with normal comparison if no duplicate differences found or if ignoring duplicates
            return compareRows(maps1, maps2, headersList1, headersList2, keyColumns,
                             ignoreCase, ignoreWhitespace, strictRowOrder);
        } catch (Exception e) {
            return new DiffResult(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new HashSet<>(),
                new HashSet<>()
            );
        }
    }

    private void validateKeyColumns(List<String> headers, List<String> keyColumns) {
        for (String keyColumn : keyColumns) {
            if (!headers.contains(keyColumn)) {
                throw new IllegalArgumentException("Key column not found: " + keyColumn);
            }
        }
    }

    private List<Map<String, String>> readCsv(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            List<String[]> rows = reader.readAll();
            List<String> headers = Arrays.asList(reader.readNext());
            return convertRowsToMaps(rows, headers);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Map<String, String>> createRowMap(List<Map<String, String>> records, List<String> keyColumns, boolean ignoreCase) {
        Map<String, Map<String, String>> map = new HashMap<>();
        for (Map<String, String> record : records) {
            String key = createRowKey(record, keyColumns, ignoreCase, false);
            map.put(key, record);
        }
        return map;
    }

    private String createRowKey(Map<String, String> record, List<String> keyColumns, boolean ignoreCase, boolean ignoreWhitespace) {
        StringBuilder key = new StringBuilder();
        for (String column : keyColumns) {
            String value = record.get(column);
            if (value == null) value = "";
            if (ignoreCase) value = value.toLowerCase();
            if (ignoreWhitespace) value = value.trim();
            key.append(value).append("|");
        }
        return key.toString();
    }

    private void compareRowsInOrder(List<Map<String, String>> rows1, List<Map<String, String>> rows2,
                                  List<String> headers1, List<String> headers2,
                                  List<String> keyColumns, boolean ignoreCase, boolean ignoreWhitespace,
                                  List<RowDiff> addedRows, List<RowDiff> removedRows, List<RowDiff> modifiedRows) {
        int maxLen = Math.max(rows1.size(), rows2.size());
        for (int i = 0; i < maxLen; i++) {
            Map<String, String> row1 = i < rows1.size() ? rows1.get(i) : null;
            Map<String, String> row2 = i < rows2.size() ? rows2.get(i) : null;

            if (row1 == null) {
                // Row was added
                addedRows.add(createRowDiff(row2, headers2, keyColumns, false));
            } else if (row2 == null) {
                // Row was removed
                removedRows.add(createRowDiff(row1, headers1, keyColumns, true));
            } else {
                // Compare rows for modifications
                RowDiff diff = compareRows(row1, row2, headers1, headers2, keyColumns, ignoreCase, ignoreWhitespace);
                if (diff != null) {
                    modifiedRows.add(diff);
                }
            }
        }
    }

    private Map<String, Integer> countOccurrences(List<Map<String, String>> rows,
                                                List<String> headers,
                                                List<String> keyColumns,
                                                boolean ignoreCase,
                                                boolean ignoreWhitespace) {
        Map<String, Integer> occurrences = new HashMap<>();
        for (Map<String, String> row : rows) {
            String key = createRowKey(row, headers, keyColumns, ignoreCase, ignoreWhitespace);
            occurrences.merge(key, 1, Integer::sum);
        }
        return occurrences;
    }

    private Map<String, String> findRowByKey(List<Map<String, String>> rows,
                                           List<String> headers,
                                           List<String> keyColumns,
                                           String targetKey,
                                           boolean ignoreCase,
                                           boolean ignoreWhitespace) {
        for (Map<String, String> row : rows) {
            String key = createRowKey(row, headers, keyColumns, ignoreCase, ignoreWhitespace);
            if (key.equals(targetKey)) {
                return row;
            }
        }
        return null;
    }

    private String createRowKey(Map<String, String> row,
                              List<String> headers,
                              List<String> keyColumns,
                              boolean ignoreCase,
                              boolean ignoreWhitespace) {
        StringBuilder key = new StringBuilder();
        List<String> columnsToUse = keyColumns.isEmpty() ? headers : keyColumns;
        for (String column : columnsToUse) {
            String value = row.get(column);
            if (value == null) value = "";
            if (ignoreCase) value = value.toLowerCase();
            if (ignoreWhitespace) value = value.trim();
            key.append(value).append("|");
        }
        return key.toString();
    }

    private List<Map<String, String>> convertRowsToMaps(List<String[]> rows, List<String> headers) {
        List<Map<String, String>> maps = new ArrayList<>();
        for (String[] row : rows) {
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < headers.size() && i < row.length; i++) {
                map.put(headers.get(i), row[i]);
            }
            maps.add(map);
        }
        return maps;
    }

    private DiffResult compareRows(List<Map<String, String>> rows1, List<Map<String, String>> rows2,
                                 List<String> headers1, List<String> headers2, List<String> keyColumns,
                                 boolean ignoreCase, boolean ignoreWhitespace, boolean strictRowOrder) {
        List<RowDiff> addedRows = new ArrayList<>();
        List<RowDiff> removedRows = new ArrayList<>();
        List<RowDiff> modifiedRows = new ArrayList<>();

        if (strictRowOrder) {
            compareRowsInOrder(rows1, rows2, headers1, headers2, keyColumns, 
                    ignoreCase, ignoreWhitespace, addedRows, removedRows, modifiedRows);
        } else {
            // Create maps for both files using key columns
            Map<String, Map<String, String>> map1 = createRowMap(rows1, keyColumns, ignoreCase);
            Map<String, Map<String, String>> map2 = createRowMap(rows2, keyColumns, ignoreCase);

            // Find removed and modified rows
            for (Map.Entry<String, Map<String, String>> entry : map1.entrySet()) {
                String key = entry.getKey();
                Map<String, String> row1 = entry.getValue();
                Map<String, String> row2 = map2.get(key);

                if (row2 == null) {
                    // Row was removed
                    removedRows.add(createRowDiff(row1, headers1, keyColumns, true));
                } else {
                    // Compare rows for modifications
                    RowDiff diff = compareRows(row1, row2, headers1, headers2, keyColumns, ignoreCase, ignoreWhitespace);
                    if (diff != null) {
                        modifiedRows.add(diff);
                    }
                }
            }

            // Find added rows
            for (Map.Entry<String, Map<String, String>> entry : map2.entrySet()) {
                String key = entry.getKey();
                if (!map1.containsKey(key)) {
                    addedRows.add(createRowDiff(entry.getValue(), headers2, keyColumns, false));
                }
            }
        }

        return new DiffResult(
            headers2,
            addedRows,
            removedRows,
            modifiedRows,
            new HashSet<>(),
            new HashSet<>()
        );
    }

    private RowDiff compareRows(Map<String, String> row1, Map<String, String> row2, List<String> headers1, List<String> headers2, 
                              List<String> keyColumns, boolean ignoreCase, boolean ignoreWhitespace) {
        Set<String> modifiedColumns = new HashSet<>();

        // Compare all columns that exist in both files
        Set<String> allColumns = new HashSet<>();
        allColumns.addAll(headers1);
        allColumns.addAll(headers2);

        for (String column : allColumns) {
            String value1 = row1.get(column);
            String value2 = row2.get(column);

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
            return new RowDiff(row1, row2, modifiedColumns, extractKeyValues(row1, keyColumns));
        }
        return null;
    }

    private List<String> extractKeyValues(Map<String, String> row, List<String> keyColumns) {
        List<String> keyValues = new ArrayList<>();
        for (String keyColumn : keyColumns) {
            keyValues.add(row.get(keyColumn));
        }
        return keyValues;
    }

    private RowDiff createRowDiff(Map<String, String> row, List<String> headers, List<String> keyColumns, boolean isOld) {
        Map<String, String> values = new HashMap<>();
        List<String> keyValues = new ArrayList<>();

        if (row != null && headers != null) {
            // Store all values
            for (String header : headers) {
                values.put(header, row.get(header));
            }

            // Extract key values
            keyValues = extractKeyValues(row, keyColumns);
        }

        return new RowDiff(
            isOld ? values : new HashMap<>(),  // oldValues
            isOld ? new HashMap<>() : values,  // newValues
            new HashSet<>(),                   // no modified columns for added/removed rows
            keyValues
        );
    }
}
