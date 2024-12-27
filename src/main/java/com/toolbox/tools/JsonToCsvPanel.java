package com.toolbox.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class JsonToCsvPanel extends JPanel {
    private final JTextArea logArea;
    private final JButton selectFilesButton;
    private final JButton convertButton;
    private final ObjectMapper objectMapper;
    private File[] selectedFiles;
    private final DateTimeFormatter dateFormatter;

    public JsonToCsvPanel() {
        setLayout(new MigLayout("fill"));
        objectMapper = new ObjectMapper();
        dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        // Create components
        selectFilesButton = new JButton("Select JSON Files");
        convertButton = new JButton("Convert to CSV");
        convertButton.setEnabled(false);
        logArea = new JTextArea();
        logArea.setEditable(false);

        // Setup UI
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0"));
        buttonPanel.add(selectFilesButton);
        buttonPanel.add(convertButton);

        add(buttonPanel, "wrap");
        add(new JScrollPane(logArea), "grow");

        // Add listeners
        setupListeners();
    }

    private void setupListeners() {
        selectFilesButton.addActionListener(e -> selectFiles());
        convertButton.addActionListener(e -> convertFiles());
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFiles = fileChooser.getSelectedFiles();
            convertButton.setEnabled(selectedFiles != null && selectedFiles.length > 0);
            logArea.setText("Selected " + selectedFiles.length + " file(s)\n");
        }
    }

    private void convertFiles() {
        if (selectedFiles == null || selectedFiles.length == 0) {
            return;
        }

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (File jsonFile : selectedFiles) {
                    try {
                        publish("Processing " + jsonFile.getName() + "...");
                        File csvFile = convertJsonToCsv(jsonFile);
                        publish("Successfully converted " + jsonFile.getName());
                        publish("CSV file created: " + csvFile.getName());
                    } catch (Exception e) {
                        publish("Error converting " + jsonFile.getName() + ": " + e.getMessage());
                    }
                    publish("------------------------");
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    logArea.append(message + "\n");
                }
            }

            @Override
            protected void done() {
                convertButton.setEnabled(true);
            }
        };

        convertButton.setEnabled(false);
        worker.execute();
    }

    private File convertJsonToCsv(File jsonFile) throws IOException {
        // Read JSON
        JsonNode rootNode = objectMapper.readTree(jsonFile);
        
        // Get all unique fields from all objects
        Set<String> headers = new LinkedHashSet<>();
        if (rootNode.isArray()) {
            for (JsonNode node : rootNode) {
                node.fieldNames().forEachRemaining(headers::add);
            }
        } else {
            rootNode.fieldNames().forEachRemaining(headers::add);
        }

        // Create CSV file name with timestamp
        String timestamp = LocalDateTime.now().format(dateFormatter);
        String baseName = jsonFile.getName().replaceFirst("[.][^.]+$", "");
        String csvFileName = String.format("%s_%s.csv", baseName, timestamp);
        File csvFile = new File(jsonFile.getParentFile(), csvFileName);

        // Write CSV
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            // Write headers
            writer.writeNext(headers.toArray(new String[0]));

            // Write data
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    writeJsonNodeToCsv(node, headers, writer);
                }
            } else {
                writeJsonNodeToCsv(rootNode, headers, writer);
            }
        }
        
        return csvFile;
    }

    private void writeJsonNodeToCsv(JsonNode node, Set<String> headers, CSVWriter writer) {
        String[] row = new String[headers.size()];
        int i = 0;
        for (String header : headers) {
            JsonNode value = node.get(header);
            row[i++] = formatJsonValue(value);
        }
        writer.writeNext(row);
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
