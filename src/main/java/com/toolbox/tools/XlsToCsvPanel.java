package com.toolbox.tools;

import com.toolbox.tools.spreadsheet.SpreadsheetConverter;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class XlsToCsvPanel extends JPanel {
    private final JTextArea logArea;
    private final JButton selectFilesButton;
    private final JButton selectFolderButton;
    private final JRadioButton sameLocationRadio;
    private final JRadioButton customLocationRadio;
    private final JButton selectOutputButton;
    private final JCheckBox includeSubfoldersCheckbox;
    private final JButton convertButton;
    private File outputDirectory;
    private final List<File> selectedFiles;
    private final ExecutorService executor;

    public XlsToCsvPanel() {
        setLayout(new MigLayout("fillx, insets 20", "[grow]", "[]10[]10[]10[]10[]"));
        selectedFiles = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();

        // Input section
        JPanel inputPanel = new JPanel(new MigLayout("fillx", "[grow][]", "[]5[]"));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input"));
        
        selectFilesButton = new JButton("Select Files");
        selectFilesButton.addActionListener(e -> selectFiles());
        inputPanel.add(selectFilesButton, "split 2");
        
        selectFolderButton = new JButton("Select Folder");
        selectFolderButton.addActionListener(e -> selectFolder());
        inputPanel.add(selectFolderButton, "wrap");
        
        includeSubfoldersCheckbox = new JCheckBox("Include subfolders");
        inputPanel.add(includeSubfoldersCheckbox, "wrap");
        
        add(inputPanel, "growx, wrap");

        // Output location section
        JPanel outputPanel = new JPanel(new MigLayout("fillx", "[grow][]", "[]5[]"));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output Location"));
        
        ButtonGroup locationGroup = new ButtonGroup();
        sameLocationRadio = new JRadioButton("Same as input files");
        customLocationRadio = new JRadioButton("Custom location");
        locationGroup.add(sameLocationRadio);
        locationGroup.add(customLocationRadio);
        sameLocationRadio.setSelected(true);
        
        outputPanel.add(sameLocationRadio, "wrap");
        outputPanel.add(customLocationRadio, "split 2");
        
        selectOutputButton = new JButton("Select Output Folder");
        selectOutputButton.setEnabled(false);
        selectOutputButton.addActionListener(e -> selectOutputFolder());
        outputPanel.add(selectOutputButton, "wrap");
        
        customLocationRadio.addActionListener(e -> selectOutputButton.setEnabled(customLocationRadio.isSelected()));
        
        add(outputPanel, "growx, wrap");

        // Convert button
        convertButton = new JButton("Convert to CSV");
        convertButton.setEnabled(false);
        convertButton.addActionListener(e -> convertFiles());
        add(convertButton, "growx, wrap");

        // Log area
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, "grow");

        updateConvertButton();
    }

    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Spreadsheet files (*.xlsx, *.xls, *.ods)", "xlsx", "xls", "ods"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFiles.clear();
            selectedFiles.addAll(List.of(chooser.getSelectedFiles()));
            logArea.append("Selected " + selectedFiles.size() + " files\n");
            updateConvertButton();
        }
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            try {
                selectedFiles.clear();
                List<File> files = Files.walk(folder.toPath(), 
                        includeSubfoldersCheckbox.isSelected() ? Integer.MAX_VALUE : 1)
                    .filter(path -> {
                        String name = path.toString().toLowerCase();
                        return name.endsWith(".xlsx") || 
                               name.endsWith(".xls") || 
                               name.endsWith(".ods");
                    })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
                selectedFiles.addAll(files);
                logArea.append("Found " + selectedFiles.size() + " spreadsheet files in folder\n");
                updateConvertButton();
            } catch (Exception e) {
                logArea.append("Error scanning folder: " + e.getMessage() + "\n");
            }
        }
    }

    private void selectOutputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirectory = chooser.getSelectedFile();
            logArea.append("Selected output directory: " + outputDirectory + "\n");
            updateConvertButton();
        }
    }

    private void updateConvertButton() {
        boolean hasFiles = !selectedFiles.isEmpty();
        boolean hasOutput = !customLocationRadio.isSelected() || outputDirectory != null;
        convertButton.setEnabled(hasFiles && hasOutput);
    }

    private void convertFiles() {
        // Disable UI during conversion
        setControlsEnabled(false);
        logArea.append("\nStarting conversion...\n");

        executor.submit(() -> {
            try {
                int total = selectedFiles.size();
                int current = 0;

                for (File file : selectedFiles) {
                    current++;
                    logArea.append(String.format("\nProcessing file %d/%d: %s\n", 
                        current, total, file.getName()));

                    try {
                        File outputDir = customLocationRadio.isSelected() ? 
                            outputDirectory : file.getParentFile();

                        SpreadsheetConverter.convertToCSV(
                            file, 
                            outputDir,
                            message -> SwingUtilities.invokeLater(() -> 
                                logArea.append(message + "\n"))
                        );
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> 
                            logArea.append("Error converting " + file.getName() + 
                                ": " + e.getMessage() + "\n"));
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    logArea.append("\nConversion completed!\n");
                    setControlsEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append("Error during conversion: " + e.getMessage() + "\n");
                    setControlsEnabled(true);
                });
            }
        });
    }

    private void setControlsEnabled(boolean enabled) {
        selectFilesButton.setEnabled(enabled);
        selectFolderButton.setEnabled(enabled);
        includeSubfoldersCheckbox.setEnabled(enabled);
        sameLocationRadio.setEnabled(enabled);
        customLocationRadio.setEnabled(enabled);
        selectOutputButton.setEnabled(enabled && customLocationRadio.isSelected());
        convertButton.setEnabled(enabled);
    }
}
