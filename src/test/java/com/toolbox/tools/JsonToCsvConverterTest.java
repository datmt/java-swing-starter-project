package com.toolbox.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonToCsvConverterTest {
    private JsonToCsvConverter converter;
    private ObjectMapper objectMapper;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        converter = new JsonToCsvConverter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldConvertSingleObjectJsonToCsv() throws IOException {
        // Given
        Map<String, Object> jsonData = Map.of(
            "name", "John",
            "age", 30,
            "city", "New York"
        );
        File jsonFile = createJsonFile(jsonData, "single.json");

        // When
        File csvFile = converter.convertJsonToCsv(jsonFile);

        // Then
        assertTrue(csvFile.exists());
        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertThat(lines).hasSize(2); // header + 1 data row
        assertThat(lines.get(0)).contains("name", "age", "city");
        assertThat(lines.get(1)).contains("John", "30", "New York");
    }

    @Test
    void shouldConvertJsonArrayToCsv() throws IOException {
        // Given
        List<Map<String, Object>> jsonData = List.of(
            Map.of("name", "John", "age", 30),
            Map.of("name", "Jane", "age", 25)
        );
        File jsonFile = createJsonFile(jsonData, "array.json");

        // When
        File csvFile = converter.convertJsonToCsv(jsonFile);

        // Then
        assertTrue(csvFile.exists());
        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertThat(lines).hasSize(3); // header + 2 data rows
        assertThat(lines.get(0)).contains("name", "age");
        assertThat(lines.get(1)).contains("John", "30");
        assertThat(lines.get(2)).contains("Jane", "25");
    }

    @Test
    void shouldHandleNestedObjects() throws IOException {
        // Given
        Map<String, Object> jsonData = Map.of(
            "name", "John",
            "address", Map.of(
                "street", "123 Main St",
                "city", "New York"
            )
        );
        File jsonFile = createJsonFile(jsonData, "nested.json");

        // When
        File csvFile = converter.convertJsonToCsv(jsonFile);

        // Then
        assertTrue(csvFile.exists());
        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertThat(lines).hasSize(2); // header + 1 data row
        assertThat(lines.get(0)).contains("name", "address");
        assertThat(lines.get(1)).contains("John");
        // The nested object is stored as a JSON string
        String line = lines.get(1);
        assertThat(line).contains("123 Main St");
        assertThat(line).contains("New York");
    }

    private File createJsonFile(Object content, String fileName) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        objectMapper.writeValue(file, content);
        return file;
    }
}
