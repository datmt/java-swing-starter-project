package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CsvMergerTest {
    private CsvMerger merger;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        merger = new CsvMerger();
    }

    private File createCsvFile(String filename, String[] headers, String[][] data) throws IOException {
        File file = tempDir.resolve(filename).toFile();
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers);
            writer.writeAll(Arrays.asList(data));
        }
        return file;
    }

    private List<String[]> readCsvFile(File file) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            return reader.readAll();
        }
    }

    private List<List<String>> readExcelSheet(Sheet sheet) {
        List<List<String>> data = new ArrayList<>();
        for (Row row : sheet) {
            List<String> rowData = new ArrayList<>();
            for (Cell cell : row) {
                rowData.add(cell.getStringCellValue());
            }
            data.add(rowData);
        }
        return data;
    }

    @Test
    void analyzeHeaders_WithMatchingHeaders_ShouldReturnTrue() throws IOException, CsvException {
        // Arrange
        String[] headers = {"id", "name", "value"};
        String[][] data1 = {{"1", "John", "100"}, {"2", "Jane", "200"}};
        String[][] data2 = {{"3", "Bob", "300"}, {"4", "Alice", "400"}};
        
        File file1 = createCsvFile("file1.csv", headers, data1);
        File file2 = createCsvFile("file2.csv", headers, data2);

        // Act
        CsvMerger.MergeResult result = merger.analyzeHeaders(Arrays.asList(file1, file2));

        // Assert
        assertTrue(result.isHeadersMatch());
        assertEquals(Arrays.asList(headers), result.getCombinedHeaders());
        assertTrue(result.getMissingHeadersByFile().isEmpty());
    }

    @Test
    void analyzeHeaders_WithDifferentHeaders_ShouldReturnFalse() throws IOException, CsvException {
        // Arrange
        String[] headers1 = {"id", "name", "value"};
        String[] headers2 = {"id", "age", "city"};
        String[][] data1 = {{"1", "John", "100"}};
        String[][] data2 = {{"1", "25", "NYC"}};
        
        File file1 = createCsvFile("file1.csv", headers1, data1);
        File file2 = createCsvFile("file2.csv", headers2, data2);

        // Act
        CsvMerger.MergeResult result = merger.analyzeHeaders(Arrays.asList(file1, file2));

        // Assert
        assertFalse(result.isHeadersMatch());
        assertThat(result.getCombinedHeaders())
            .containsExactlyInAnyOrder("id", "name", "value", "age", "city");
        
        Map<File, List<String>> missingHeaders = result.getMissingHeadersByFile();
        assertThat(missingHeaders.get(file1)).containsExactlyInAnyOrder("age", "city");
        assertThat(missingHeaders.get(file2)).containsExactlyInAnyOrder("name", "value");
    }

    @Test
    void mergeFiles_WithMatchingHeaders_ShouldMergeCorrectly() throws IOException, CsvException {
        // Arrange
        String[] headers = {"id", "name", "value"};
        String[][] data1 = {{"1", "John", "100"}, {"2", "Jane", "200"}};
        String[][] data2 = {{"3", "Bob", "300"}, {"4", "Alice", "400"}};
        
        File file1 = createCsvFile("file1.csv", headers, data1);
        File file2 = createCsvFile("file2.csv", headers, data2);
        File outputFile = tempDir.resolve("output.csv").toFile();

        // Act
        CsvMerger.MergeResult result = merger.analyzeHeaders(Arrays.asList(file1, file2));
        merger.mergeFiles(Arrays.asList(file1, file2), outputFile, result);

        // Assert
        List<String[]> mergedData = readCsvFile(outputFile);
        assertEquals(5, mergedData.size()); // header + 4 data rows
        
        // Check headers
        assertArrayEquals(headers, mergedData.get(0));
        
        // Check data
        assertArrayEquals(data1[0], mergedData.get(1));
        assertArrayEquals(data1[1], mergedData.get(2));
        assertArrayEquals(data2[0], mergedData.get(3));
        assertArrayEquals(data2[1], mergedData.get(4));
    }

    @Test
    void mergeFiles_WithDifferentHeaders_ShouldMergeAndAlignColumns() throws IOException, CsvException {
        // Arrange
        String[] headers1 = {"id", "name"};
        String[] headers2 = {"id", "age"};
        String[][] data1 = {{"1", "John"}, {"2", "Jane"}};
        String[][] data2 = {{"3", "25"}, {"4", "30"}};
        
        File file1 = createCsvFile("file1.csv", headers1, data1);
        File file2 = createCsvFile("file2.csv", headers2, data2);
        File outputFile = tempDir.resolve("output.csv").toFile();

        // Act
        CsvMerger.MergeResult result = merger.analyzeHeaders(Arrays.asList(file1, file2));
        merger.mergeFiles(Arrays.asList(file1, file2), outputFile, result);

        // Assert
        List<String[]> mergedData = readCsvFile(outputFile);
        assertEquals(5, mergedData.size()); // header + 4 data rows
        
        // Check headers contain all columns
        assertThat(mergedData.get(0)).containsExactlyInAnyOrder("id", "name", "age");
        
        // Check data from first file (should have empty age)
        assertThat(mergedData.get(1)).containsExactly("1", "John", "");
        assertThat(mergedData.get(2)).containsExactly("2", "Jane", "");
        
        // Check data from second file (should have empty name)
        assertThat(mergedData.get(3)).containsExactly("3", "", "25");
        assertThat(mergedData.get(4)).containsExactly("4", "", "30");
    }

    @Test
    void mergeToExcel_ShouldCreateWorkbookWithMultipleSheets() throws IOException, CsvException {
        // Arrange
        String[] headers1 = {"id", "name", "value"};
        String[] headers2 = {"id", "age", "city"};
        String[][] data1 = {{"1", "John", "100"}, {"2", "Jane", "200"}};
        String[][] data2 = {{"3", "25", "NYC"}, {"4", "30", "LA"}};
        
        File file1 = createCsvFile("file1.csv", headers1, data1);
        File file2 = createCsvFile("file2.csv", headers2, data2);
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        // Act
        merger.mergeToExcel(Arrays.asList(file1, file2), outputFile);

        // Assert
        try (Workbook workbook = WorkbookFactory.create(outputFile)) {
            assertEquals(2, workbook.getNumberOfSheets());
            
            // Check first sheet
            Sheet sheet1 = workbook.getSheetAt(0);
            assertEquals("file1", sheet1.getSheetName());
            List<List<String>> sheet1Data = readExcelSheet(sheet1);
            
            assertEquals(3, sheet1Data.size()); // header + 2 data rows
            assertThat(sheet1Data.get(0)).containsExactly("id", "name", "value");
            assertThat(sheet1Data.get(1)).containsExactly("1", "John", "100");
            assertThat(sheet1Data.get(2)).containsExactly("2", "Jane", "200");
            
            // Check second sheet
            Sheet sheet2 = workbook.getSheetAt(1);
            assertEquals("file2", sheet2.getSheetName());
            List<List<String>> sheet2Data = readExcelSheet(sheet2);
            
            assertEquals(3, sheet2Data.size()); // header + 2 data rows
            assertThat(sheet2Data.get(0)).containsExactly("id", "age", "city");
            assertThat(sheet2Data.get(1)).containsExactly("3", "25", "NYC");
            assertThat(sheet2Data.get(2)).containsExactly("4", "30", "LA");
            
            // Check header styling
            Row headerRow1 = sheet1.getRow(0);
            CellStyle headerStyle = headerRow1.getCell(0).getCellStyle();
            assertEquals(FillPatternType.SOLID_FOREGROUND, headerStyle.getFillPattern());
            assertEquals(IndexedColors.GREY_25_PERCENT.getIndex(), headerStyle.getFillForegroundColor());
            Font font = workbook.getFontAt(headerStyle.getFontIndex());
            assertTrue(font.getBold());
        }
    }

    @Test
    void mergeToExcel_WithEmptyFiles_ShouldHandleGracefully() throws IOException, CsvException {
        // Arrange
        String[] headers = {"id", "name"};
        String[][] data = new String[0][0];
        
        File file1 = createCsvFile("file1.csv", headers, data);
        File file2 = createCsvFile("file2.csv", headers, data);
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        // Act
        merger.mergeToExcel(Arrays.asList(file1, file2), outputFile);

        // Assert
        try (Workbook workbook = WorkbookFactory.create(outputFile)) {
            assertEquals(2, workbook.getNumberOfSheets());
            
            // Check both sheets have only headers
            for (int i = 0; i < 2; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String>> sheetData = readExcelSheet(sheet);
                assertEquals(1, sheetData.size()); // only header row
                assertThat(sheetData.get(0)).containsExactly("id", "name");
            }
        }
    }

    @Test
    void mergeToExcel_WithDuplicateSheetNames_ShouldCreateUniqueNames() throws IOException, CsvException {
        // Arrange
        String[] headers = {"id", "name", "value"};
        String[][] data = {{"1", "John", "100"}};
        
        // Create two files with the same name in different directories
        File file1 = createCsvFile("data.csv", headers, data);
        File file2 = createCsvFile("data.csv", headers, data);
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        // Act
        merger.mergeToExcel(Arrays.asList(file1, file2), outputFile);

        // Assert
        try (Workbook workbook = WorkbookFactory.create(outputFile)) {
            assertEquals(2, workbook.getNumberOfSheets());
            
            // Get sheet names
            String sheet1Name = workbook.getSheetName(0);
            String sheet2Name = workbook.getSheetName(1);
            
            // Verify sheet names are different
            assertNotEquals(sheet1Name, sheet2Name);
            
            // Verify both sheets start with "data"
            assertTrue(sheet1Name.startsWith("data"));
            assertTrue(sheet2Name.startsWith("data"));
            
            // Verify sheet names are not longer than 31 characters (Excel limit)
            assertTrue(sheet1Name.length() <= 31);
            assertTrue(sheet2Name.length() <= 31);
            
            // Verify data in both sheets
            for (int i = 0; i < 2; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String>> sheetData = readExcelSheet(sheet);
                assertEquals(2, sheetData.size()); // header + 1 data row
                assertThat(sheetData.get(0)).containsExactly("id", "name", "value");
                assertThat(sheetData.get(1)).containsExactly("1", "John", "100");
            }
        }
    }

    @Test
    void mergeToExcel_WithLongSheetNames_ShouldTruncateAndAddSuffix() throws IOException, CsvException {
        // Arrange
        String[] headers = {"id", "name"};
        String[][] data = {{"1", "John"}};
        String longFileName = "very_long_file_name_that_exceeds_excel_limit.csv";
        
        File file1 = createCsvFile(longFileName, headers, data);
        File file2 = createCsvFile(longFileName, headers, data);
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        // Act
        merger.mergeToExcel(Arrays.asList(file1, file2), outputFile);

        // Assert
        try (Workbook workbook = WorkbookFactory.create(outputFile)) {
            assertEquals(2, workbook.getNumberOfSheets());
            
            // Get sheet names
            String sheet1Name = workbook.getSheetName(0);
            String sheet2Name = workbook.getSheetName(1);
            
            // Verify sheet names are different and within length limit
            assertNotEquals(sheet1Name, sheet2Name);
            assertTrue(sheet1Name.length() <= 31);
            assertTrue(sheet2Name.length() <= 31);
            
            // Verify both sheets contain the data
            for (int i = 0; i < 2; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<List<String>> sheetData = readExcelSheet(sheet);
                assertEquals(2, sheetData.size());
                assertThat(sheetData.get(0)).containsExactly("id", "name");
                assertThat(sheetData.get(1)).containsExactly("1", "John");
            }
        }
    }
}
