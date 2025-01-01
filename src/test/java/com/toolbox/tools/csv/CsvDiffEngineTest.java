package com.toolbox.tools.csv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvDiffEngineTest {
    private CsvDiffEngine engine;
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        engine = new CsvDiffEngine();
    }

    private File createCsvFile(String filename, String... lines) throws IOException {
        File file = tempDir.resolve(filename).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
        return file;
    }

    @Test
    void testIdenticalFiles() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "id,name,value",
            "1,John,100",
            "2,Jane,200"
        );
        File file2 = createCsvFile("file2.csv",
            "id,name,value",
            "1,John,100",
            "2,Jane,200"
        );

        CsvDiffEngine.DiffResult result = engine.compareCsvFiles(
            file1, file2, Arrays.asList("id"), false, false
        );

        assertTrue(result.getModifiedRows().isEmpty(), "No modifications expected");
        assertTrue(result.getAddedRows().isEmpty(), "No additions expected");
        assertTrue(result.getRemovedRows().isEmpty(), "No removals expected");
    }

    @Test
    void testModifiedValues() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "id,name,value",
            "1,John,100",
            "2,Jane,200"
        );
        File file2 = createCsvFile("file2.csv",
            "id,name,value",
            "1,John,150",  // Modified value
            "2,Jane,200"
        );

        CsvDiffEngine.DiffResult result = engine.compareCsvFiles(
            file1, file2, Arrays.asList("id"), false, false
        );

        assertEquals(1, result.getModifiedRows().size(), "One modification expected");
        CsvDiffEngine.RowDiff diff = result.getModifiedRows().get(0);
        assertEquals("100", diff.getOldValues().get("value"));
        assertEquals("150", diff.getNewValues().get("value"));
        assertEquals(Arrays.asList("1"), diff.getKeyValues());
    }

    @Test
    void testAddedAndRemovedRows() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "id,name,value",
            "1,John,100",
            "2,Jane,200"
        );
        File file2 = createCsvFile("file2.csv",
            "id,name,value",
            "1,John,100",
            "3,Bob,300"  // Added row (2 removed, 3 added)
        );

        CsvDiffEngine.DiffResult result = engine.compareCsvFiles(
            file1, file2, Arrays.asList("id"), false, false
        );

        assertEquals(1, result.getAddedRows().size(), "One addition expected");
        assertEquals(1, result.getRemovedRows().size(), "One removal expected");
        
        CsvDiffEngine.RowDiff addedRow = result.getAddedRows().get(0);
        assertEquals(Arrays.asList("3"), addedRow.getKeyValues());
        assertEquals("Bob", addedRow.getNewValues().get("name"));
        
        CsvDiffEngine.RowDiff removedRow = result.getRemovedRows().get(0);
        assertEquals(Arrays.asList("2"), removedRow.getKeyValues());
        assertEquals("Jane", removedRow.getOldValues().get("name"));
    }

    @Test
    void testMultipleKeyColumns() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "region,id,name,value",
            "US,1,John,100",
            "UK,1,Jane,200"  // Different region, same id
        );
        File file2 = createCsvFile("file2.csv",
            "region,id,name,value",
            "US,1,John,150",  // Modified value
            "UK,1,Jane,200"
        );

        CsvDiffEngine.DiffResult result = engine.compareCsvFiles(
            file1, file2, Arrays.asList("region", "id"), false, false
        );

        assertEquals(1, result.getModifiedRows().size(), "One modification expected");
        CsvDiffEngine.RowDiff diff = result.getModifiedRows().get(0);
        assertEquals(Arrays.asList("US", "1"), diff.getKeyValues());
        assertEquals("100", diff.getOldValues().get("value"));
        assertEquals("150", diff.getNewValues().get("value"));
    }

    @Test
    void testIgnoreCaseAndWhitespace() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "id,name,value",
            "1,John  ,100",
            "2,JANE,200"
        );
        File file2 = createCsvFile("file2.csv",
            "id,name,value",
            "1,  John,100",
            "2,jane,200"
        );

        // Should find differences with no ignore options
        CsvDiffEngine.DiffResult result1 = engine.compareCsvFiles(
            file1, file2, Arrays.asList("id"), false, false
        );
        assertEquals(2, result1.getModifiedRows().size(), "Two modifications expected without ignore options");

        // Should find no differences with ignore options
        CsvDiffEngine.DiffResult result2 = engine.compareCsvFiles(
            file1, file2, Arrays.asList("id"), true, true
        );
        assertTrue(result2.getModifiedRows().isEmpty(), "No modifications expected with ignore options");
    }

    @Test
    void testEmptyFiles() {
        File file1 = tempDir.resolve("empty1.csv").toFile();
        File file2 = tempDir.resolve("empty2.csv").toFile();

        assertThrows(IOException.class, () -> {
            engine.compareCsvFiles(file1, file2, Arrays.asList("id"), false, false);
        }, "Should throw IOException for empty files");
    }

    @Test
    void testMissingKeyColumn() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "id,name,value",
            "1,John,100"
        );
        File file2 = createCsvFile("file2.csv",
            "id,name,value",
            "1,John,100"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            engine.compareCsvFiles(file1, file2, Arrays.asList("nonexistent"), false, false);
        }, "Should throw IllegalArgumentException for missing key column");
    }

    @Test
    void testEmptyCells() throws IOException {
        File file1 = createCsvFile("file1.csv",
            "id,name,value",
            "1,,100"
        );
        File file2 = createCsvFile("file2.csv",
            "id,name,value",
            "1,,100"
        );

        //assert identical values
        CsvDiffEngine.DiffResult result = engine.compareCsvFiles(
            file1, file2, Arrays.asList("id"), false, false
        );
        assertTrue(result.getModifiedRows().isEmpty(), "No modifications expected");
        assertTrue(result.getAddedRows().isEmpty(), "No additions expected");
        assertTrue(result.getRemovedRows().isEmpty(), "No removals expected");
    }
}
