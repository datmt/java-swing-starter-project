package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RemoveDuplicatesPanelTest {
    @TempDir
    Path tempDir;
    private RemoveDuplicatesPanel panel;

    @BeforeEach
    void setUp() {
        panel = new RemoveDuplicatesPanel();
    }

    @Test
    void testRemoveDuplicatesFromCsvEntireRow() throws Exception {
        // Create a test CSV file
        File inputFile = tempDir.resolve("test.csv").toFile();
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputFile))) {
            writer.writeNext(new String[]{"Name", "Age", "City"});
            writer.writeNext(new String[]{"John", "25", "New York"});
            writer.writeNext(new String[]{"Jane", "30", "London"});
            writer.writeNext(new String[]{"John", "25", "New York"}); // Duplicate
            writer.writeNext(new String[]{"Bob", "35", "Paris"});
        }

        // Process the file
        File outputFile = new File(inputFile.getAbsolutePath().replace(".csv", "_no_duplicates.csv"));
        panel.removeDuplicatesFromCsv(inputFile, outputFile, -1);

        // Verify results
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(4, rows.size()); // Header + 3 unique rows
            assertEquals("John", rows.get(1)[0]);
            assertEquals("Jane", rows.get(2)[0]);
            assertEquals("Bob", rows.get(3)[0]);
        }
    }

    @Test
    void testRemoveDuplicatesFromCsvSelectedColumn() throws Exception {
        // Create a test CSV file
        File inputFile = tempDir.resolve("test.csv").toFile();
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputFile))) {
            writer.writeNext(new String[]{"Name", "Age", "City"});
            writer.writeNext(new String[]{"John", "25", "New York"});
            writer.writeNext(new String[]{"John", "30", "London"}); // Duplicate name
            writer.writeNext(new String[]{"Jane", "25", "Paris"}); // Duplicate age
            writer.writeNext(new String[]{"Bob", "35", "Tokyo"});
        }

        // Process the file - remove duplicates based on Name column (index 0)
        File outputFile = new File(inputFile.getAbsolutePath().replace(".csv", "_no_duplicates.csv"));
        panel.removeDuplicatesFromCsv(inputFile, outputFile, 0);

        // Verify results
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(4, rows.size()); // Header + 3 unique names
            assertTrue(Arrays.asList(rows.get(1)[0], rows.get(2)[0], rows.get(3)[0])
                    .containsAll(Arrays.asList("John", "Jane", "Bob")));
        }
    }

    @Test
    void testRemoveDuplicatesFromExcelEntireRow() throws Exception {
        // Create a test Excel file
        File inputFile = tempDir.resolve("test.xlsx").toFile();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            createRow(sheet, 0, "Name", "Age", "City");
            createRow(sheet, 1, "John", "25", "New York");
            createRow(sheet, 2, "Jane", "30", "London");
            createRow(sheet, 3, "John", "25", "New York"); // Duplicate
            createRow(sheet, 4, "Bob", "35", "Paris");

            try (var fileOut = new java.io.FileOutputStream(inputFile)) {
                workbook.write(fileOut);
            }
        }

        // Process the file
        File outputFile = new File(inputFile.getAbsolutePath().replace(".xlsx", "_no_duplicates.xlsx"));
        panel.removeDuplicatesFromExcel(inputFile, outputFile, -1);

        // Verify results
        try (Workbook workbook = WorkbookFactory.create(outputFile)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(4, sheet.getLastRowNum() + 1); // Header + 3 unique rows
            assertEquals("John", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Jane", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("Bob", sheet.getRow(3).getCell(0).getStringCellValue());
        }
    }

    @Test
    void testRemoveDuplicatesFromCsvWithConsecutiveDuplicates() throws Exception {
        // Create a test CSV file with consecutive duplicate rows
        File inputFile = tempDir.resolve("test_consecutive.csv").toFile();
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputFile))) {
            writer.writeNext(new String[]{"Code", "Date", "Value", "Status", "Type", "Unit", "Version", "Collection", "Variable", "Measure", "Industry", "State", "Empty1", "Empty2"});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""});
        }

        // Process the file
        File outputFile = new File(inputFile.getAbsolutePath().replace(".csv", "_no_duplicates.csv"));
        panel.removeDuplicatesFromCsv(inputFile, outputFile, -1);

        // Verify results
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(2, rows.size()); // Header + 1 unique row
            assertArrayEquals(
                    new String[]{"Code", "Date", "Value", "Status", "Type", "Unit", "Version", "Collection", "Variable", "Measure", "Industry", "State", "Empty1", "Empty2"},
                    rows.get(0)
            );
            assertArrayEquals(
                    new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""},
                    rows.get(1)
            );
        }
    }

    @Test
    void testRemoveDuplicatesFromCsvByDateColumn() throws Exception {
        // Create a test CSV file with duplicate dates but different values
        File inputFile = tempDir.resolve("test_date_column.csv").toFile();
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputFile))) {
            writer.writeNext(new String[]{"Code", "Date", "Value", "Status", "Type", "Unit", "Version", "Collection", "Variable", "Measure", "Industry", "State", "Empty1", "Empty2"});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""});
            writer.writeNext(new String[]{"BDCQ.SEA1BB", "2013.09", "82000", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Mining", "Actual", "", ""});
            writer.writeNext(new String[]{"BDCQ.SEA1CC", "2013.12", "93950", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Manufacturing", "Actual", "", ""});
        }

        // Process the file - remove duplicates based on Date column (index 1)
        File outputFile = new File(inputFile.getAbsolutePath().replace(".csv", "_no_duplicates.csv"));
        panel.removeDuplicatesFromCsv(inputFile, outputFile, 1);

        // Verify results
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(3, rows.size()); // Header + 2 unique dates (2013.09 and 2013.12)
            assertEquals("2013.09", rows.get(1)[1]); // First row after header should have first date
            assertEquals("2013.12", rows.get(2)[1]); // Second row should have second date
        }
    }

    @Test
    void testRemoveDuplicatesFromCsvWithEmptyFields() throws Exception {
        // Create a test CSV file with empty fields
        File inputFile = tempDir.resolve("test_empty_fields.csv").toFile();
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputFile))) {
            writer.writeNext(new String[]{"Code", "Date", "Value", "Status", "Type", "Unit", "Version", "Collection", "Variable", "Measure", "Industry", "State", "Empty1", "Empty2"});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""});
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", "", "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", "", ""}); // Duplicate with empty fields
            writer.writeNext(new String[]{"BDCQ.SEA1AA", "2013.09", "81471", null, "F", "Number", "0", "Business Data Collection - BDC", "Industry by employment variable", "Filled jobs", "Agriculture, Forestry and Fishing", "Actual", null, null}); // Duplicate with null fields
        }

        // Process the file
        File outputFile = new File(inputFile.getAbsolutePath().replace(".csv", "_no_duplicates.csv"));
        panel.removeDuplicatesFromCsv(inputFile, outputFile, -1);

        // Verify results
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(2, rows.size()); // Header + 1 unique row (empty and null fields should be treated as equal)
        }
    }

    private void createRow(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
        }
    }
}
