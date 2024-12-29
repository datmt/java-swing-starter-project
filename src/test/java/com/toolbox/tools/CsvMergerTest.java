package com.toolbox.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
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
    void mergeFiles_WithEmptyFiles_ShouldHandleGracefully() throws IOException, CsvException {
        // Arrange
        String[] headers = {"id", "name"};
        String[][] data = new String[0][0];
        
        File file1 = createCsvFile("file1.csv", headers, data);
        File file2 = createCsvFile("file2.csv", headers, data);
        File outputFile = tempDir.resolve("output.csv").toFile();

        // Act
        CsvMerger.MergeResult result = merger.analyzeHeaders(Arrays.asList(file1, file2));
        merger.mergeFiles(Arrays.asList(file1, file2), outputFile, result);

        // Assert
        List<String[]> mergedData = readCsvFile(outputFile);
        assertEquals(1, mergedData.size()); // only header
        assertArrayEquals(headers, mergedData.get(0));
    }
}
