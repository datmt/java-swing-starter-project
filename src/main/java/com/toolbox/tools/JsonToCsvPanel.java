package com.toolbox.tools;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonToCsvPanel extends JPanel {
    private final JTextArea logArea;
    private final JButton selectFilesButton;
    private final JButton convertButton;
    private final JButton openFileButton;
    private final JsonToCsvConverter converter;
    private File[] selectedFiles;
    private File lastConvertedFile;

    public JsonToCsvPanel() {
        setLayout(new MigLayout("fill"));
        converter = new JsonToCsvConverter();

        // Create components
        selectFilesButton = new JButton("Select JSON Files");
        convertButton = new JButton("Convert to CSV");
        openFileButton = new JButton("Open Last Converted File");
        convertButton.setEnabled(false);
        openFileButton.setEnabled(false);
        logArea = new JTextArea();
        logArea.setEditable(false);

        // Setup UI
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0"));
        buttonPanel.add(selectFilesButton);
        buttonPanel.add(convertButton);
        buttonPanel.add(openFileButton);

        add(buttonPanel, "wrap");
        add(new JScrollPane(logArea), "grow");

        // Add listeners
        setupListeners();
    }

    private void setupListeners() {
        selectFilesButton.addActionListener(e -> selectFiles());
        convertButton.addActionListener(e -> convertFiles());
        openFileButton.addActionListener(e -> openLastConvertedFile());
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
                        File csvFile = converter.convertJsonToCsv(jsonFile);
                        lastConvertedFile = csvFile;
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
                openFileButton.setEnabled(lastConvertedFile != null);
            }
        };

        convertButton.setEnabled(false);
        worker.execute();
    }

    private void openLastConvertedFile() {
        if (lastConvertedFile != null && lastConvertedFile.exists()) {
            try {
                Desktop.getDesktop().open(lastConvertedFile);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error opening file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
