package com.binarycarpenter.spreadsheet.tools;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvMapperTest {
    @TempDir
    Path tempDir;

    private File sourceFile;
    private File targetFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create source file
        sourceFile = new File(tempDir.toFile(), "source.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(sourceFile))) {
            writer.writeNext(new String[]{"ID", "Name", "Value"});
            writer.writeNext(new String[]{"1", "John", "100"});
            writer.writeNext(new String[]{"2", "Jane", "200"});
            writer.writeNext(new String[]{"3", "Bob", "300"});
        }

        // Create target file
        targetFile = new File(tempDir.toFile(), "target.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(targetFile))) {
            writer.writeNext(new String[]{"OrderID", "CustomerID", "Amount", "MappedValue"});
            writer.writeNext(new String[]{"O1", "2", "50", ""});
            writer.writeNext(new String[]{"O2", "1", "75", ""});
            writer.writeNext(new String[]{"O3", "4", "100", ""});
        }
    }

    @Test
    void testSuccessfulMapping() throws IOException, CsvException {
        // Setup mapper: Map customer IDs to their values
        CsvMapper mapper = new CsvMapper(
            sourceFile,
            targetFile,
            0,  // source lookup column (ID)
            2,  // source value column (Value)
            1,  // target lookup column (CustomerID)
            3,  // target output column (MappedValue)
            "N/A"
        );

        // Perform mapping
        File outputFile = mapper.performMapping();
        assertTrue(outputFile.exists());

        // Verify results
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(4, rows.size()); // Header + 3 data rows

            // Verify header
            assertArrayEquals(
                new String[]{"OrderID", "CustomerID", "Amount", "MappedValue"},
                rows.get(0)
            );

            // Verify mapped values
            assertEquals("200", rows.get(1)[3]); // CustomerID 2 -> Value 200
            assertEquals("100", rows.get(2)[3]); // CustomerID 1 -> Value 100
            assertEquals("N/A", rows.get(3)[3]); // CustomerID 4 -> Default value
        }
    }

    @Test
    void testMappingWithAllDefaultValues() throws IOException, CsvException {
        // Setup mapper with non-existent lookup values
        CsvMapper mapper = new CsvMapper(
            sourceFile,
            targetFile,
            0,  // source lookup column (ID)
            2,  // source value column (Value)
            0,  // target lookup column (OrderID) - No matches will be found
            3,  // target output column (MappedValue)
            "NOT_FOUND"
        );

        // Perform mapping
        File outputFile = mapper.performMapping();

        // Verify all values are default
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            for (int i = 1; i < rows.size(); i++) {
                assertEquals("NOT_FOUND", rows.get(i)[3]);
            }
        }
    }

    @Test
    void testMappingWithEmptySourceFile() throws IOException, CsvException {
        // Create empty source file (only header)
        File emptySource = new File(tempDir.toFile(), "empty_source.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(emptySource))) {
            writer.writeNext(new String[]{"ID", "Name", "Value"});
        }

        CsvMapper mapper = new CsvMapper(
            emptySource,
            targetFile,
            0, 2, 1, 3,
            "EMPTY"
        );

        File outputFile = mapper.performMapping();

        // Verify all values are default
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            for (int i = 1; i < rows.size(); i++) {
                assertEquals("EMPTY", rows.get(i)[3]);
            }
        }
    }

    @Test
    void testMappingWithEmptyTargetFile() throws IOException, CsvException {
        // Create empty target file (only header)
        File emptyTarget = new File(tempDir.toFile(), "empty_target.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(emptyTarget))) {
            writer.writeNext(new String[]{"OrderID", "CustomerID", "Amount", "MappedValue"});
        }

        CsvMapper mapper = new CsvMapper(
            sourceFile,
            emptyTarget,
            0, 2, 1, 3,
            "DEFAULT"
        );

        File outputFile = mapper.performMapping();

        // Verify output has only header
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals(1, rows.size());
            assertArrayEquals(
                new String[]{"OrderID", "CustomerID", "Amount", "MappedValue"},
                rows.get(0)
            );
        }
    }

    @Test
    void testMappingWithDuplicateSourceKeys() throws IOException, CsvException {
        // Create source file with duplicate keys
        File duplicateSource = new File(tempDir.toFile(), "duplicate_source.csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(duplicateSource))) {
            writer.writeNext(new String[]{"ID", "Name", "Value"});
            writer.writeNext(new String[]{"1", "John", "100"});
            writer.writeNext(new String[]{"1", "John2", "150"}); // Duplicate key
            writer.writeNext(new String[]{"2", "Jane", "200"});
        }

        CsvMapper mapper = new CsvMapper(
            duplicateSource,
            targetFile,
            0, 2, 1, 3,
            "N/A"
        );

        File outputFile = mapper.performMapping();

        // Verify that last value for duplicate key is used
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            String mappedValue = rows.get(2)[3]; // Row with CustomerID 1
            assertEquals("150", mappedValue); // Should get the last value for ID 1
        }
    }

    @Test
    void testGetHeaders() throws IOException, CsvException {
        String[] headers = CsvMapper.getHeaders(sourceFile);
        assertArrayEquals(new String[]{"ID", "Name", "Value"}, headers);
    }

    @Test
    void testInvalidColumnIndexes() {
        // Test with column index out of bounds
        CsvMapper mapper = new CsvMapper(
            sourceFile,
            targetFile,
            99, // Invalid source lookup column
            2,
            1,
            3,
            "ERROR"
        );

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
            mapper.performMapping();
        });
    }

    @Test
    void testWithNullDefaultValue() throws IOException, CsvException {
        CsvMapper mapper = new CsvMapper(
            sourceFile,
            targetFile,
            0, 2, 1, 3,
            null
        );

        File outputFile = mapper.performMapping();

        // Verify unmatched values are empty strings
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> rows = reader.readAll();
            assertEquals("", rows.get(3)[3]); // Unmatched row should have empty string
        }
    }
}
