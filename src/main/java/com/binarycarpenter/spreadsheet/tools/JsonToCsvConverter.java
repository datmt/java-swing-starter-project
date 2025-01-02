package com.binarycarpenter.spreadsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

public class JsonToCsvConverter {
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter dateFormatter;

    public JsonToCsvConverter() {
        this.objectMapper = new ObjectMapper();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    }

    public File convertJsonToCsv(File jsonFile) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonFile);
        Set<String> headers = extractHeaders(rootNode);
        File csvFile = createCsvFile(jsonFile);

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            writeHeaders(writer, headers);
            writeData(writer, rootNode, headers);
        }

        return csvFile;
    }

    private Set<String> extractHeaders(JsonNode rootNode) {
        Set<String> headers = new LinkedHashSet<>();
        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                node.fieldNames().forEachRemaining(headers::add);
            }
        } else {
            rootNode.fieldNames().forEachRemaining(headers::add);
        }
        return headers;
    }

    private File createCsvFile(File jsonFile) {
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String baseName = jsonFile.getName().replaceFirst("[.][^.]+$", "");
        String csvFileName = String.format("%s_%s.csv", baseName, timestamp);
        return new File(jsonFile.getParentFile(), csvFileName);
    }

    private void writeHeaders(CSVWriter writer, Set<String> headers) {
        writer.writeNext(headers.toArray(new String[0]), false);
    }

    private void writeData(CSVWriter writer, JsonNode rootNode, Set<String> headers) {
        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                writeJsonNodeToCsv(writer, node, headers);
            }
        } else {
            writeJsonNodeToCsv(writer, rootNode, headers);
        }
    }

    private void writeJsonNodeToCsv(CSVWriter writer, JsonNode node, Set<String> headers) {
        String[] row = new String[headers.size()];
        int i = 0;
        for (String header : headers) {
            JsonNode value = node.get(header);
            row[i++] = formatJsonValue(value);
        }
        writer.writeNext(row, false); // Set applyQuotesToAll to false
    }

    private String formatJsonValue(JsonNode value) {
        if (value == null) {
            return "";
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isNumber()) {
            return value.asText();
        }
        if (value.isBoolean()) {
            return value.asText();
        }
        if (value.isNull()) {
            return "";
        }
        // For objects and arrays, return the JSON string representation
        return value.toString();
    }
}
