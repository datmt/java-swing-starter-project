package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.exceptions.CsvException;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvMergePanel extends JPanel {
    private final List<File> selectedFiles = new ArrayList<>();
    private final DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(fileListModel);
    private final JTextArea logArea = new JTextArea();
    private final JButton csvMergeButton;
    private final JButton excelMergeButton;
    private final CsvMerger merger = new CsvMerger();

    public CsvMergePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Description panel
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("<html>CSV Merge Tool<br/>Merge multiple CSV files into one CSV file or Excel workbook.</html>");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descriptionLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        descriptionPanel.add(descriptionLabel, BorderLayout.CENTER);
        add(descriptionPanel, BorderLayout.NORTH);

        // Main content panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Merge to CSV", createCsvPanel());
        tabbedPane.addTab("Merge to Excel", createExcelPanel());
        add(tabbedPane, BorderLayout.CENTER);

        // File selection panel (shared between tabs)
        JPanel filePanel = new JPanel(new BorderLayout(0, 10));
        filePanel.setBorder(BorderFactory.createTitledBorder("Selected Files"));

        // File selection buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Files");
        addButton.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        addButton.addActionListener(e -> addFiles());

        JButton removeButton = new JButton("Remove Selected");
        removeButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        removeButton.addActionListener(e -> removeSelectedFiles());

        JButton clearButton = new JButton("Clear All");
        clearButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        clearButton.addActionListener(e -> clearFiles());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        filePanel.add(buttonPanel, BorderLayout.NORTH);

        // File list
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setPreferredSize(new Dimension(0, 200));
        filePanel.add(fileScrollPane, BorderLayout.CENTER);

        // Merge buttons panel
        JPanel mergePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        csvMergeButton = new JButton("Merge to CSV");
        csvMergeButton.setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
        csvMergeButton.setEnabled(false);
        csvMergeButton.addActionListener(e -> mergeToCsv());

        excelMergeButton = new JButton("Merge to Excel");
        excelMergeButton.setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
        excelMergeButton.setEnabled(false);
        excelMergeButton.addActionListener(e -> mergeToExcel());

        mergePanel.add(csvMergeButton);
        mergePanel.add(excelMergeButton);
        filePanel.add(mergePanel, BorderLayout.SOUTH);

        add(filePanel, BorderLayout.CENTER);

        // Log panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(0, 150));
        logPanel.add(logScrollPane);
        add(logPanel, BorderLayout.SOUTH);
    }

    private JPanel createCsvPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]"));
        infoPanel.add(new JLabel("<html><b>CSV Merge Options</b></html>"), "wrap");
        infoPanel.add(new JLabel("<html>Merge multiple CSV files into a single CSV file.<br/>" +
                "If headers match, data will be appended sequentially.<br/>" +
                "If headers differ, missing columns will be filled with empty values.</html>"), "wrap");

        panel.add(infoPanel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createExcelPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel infoPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]"));
        infoPanel.add(new JLabel("<html><b>Excel Merge Options</b></html>"), "wrap");
        infoPanel.add(new JLabel("<html>Merge multiple CSV files into a single Excel workbook.<br/>" +
                "Each CSV file will be placed in a separate worksheet.<br/>" +
                "Sheet names will be based on CSV file names.</html>"), "wrap");

        panel.add(infoPanel, BorderLayout.NORTH);
        return panel;
    }

    private void addFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }

            public String getDescription() {
                return "CSV Files (*.csv)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (!selectedFiles.contains(file)) {
                    selectedFiles.add(file);
                    fileListModel.addElement(file.getName());
                }
            }
            updateMergeButtonState();
        }
    }

    private void removeSelectedFiles() {
        int[] indices = fileList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            selectedFiles.remove(indices[i]);
            fileListModel.remove(indices[i]);
        }
        updateMergeButtonState();
    }

    private void clearFiles() {
        selectedFiles.clear();
        fileListModel.clear();
        updateMergeButtonState();
    }

    private void updateMergeButtonState() {
        boolean hasFiles = selectedFiles.size() >= 2;
        csvMergeButton.setEnabled(hasFiles);
        excelMergeButton.setEnabled(hasFiles);
    }

    private void mergeToCsv() {
        if (selectedFiles.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least two files to merge.",
                    "Not Enough Files",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Analyze headers first
            CsvMerger.MergeResult mergeResult = merger.analyzeHeaders(selectedFiles);

            // If headers don't match, show warning
            if (!mergeResult.isHeadersMatch()) {
                StringBuilder message = new StringBuilder();
                message.append("The selected files have different headers:\n\n");

                for (Map.Entry<File, List<String>> entry : mergeResult.getMissingHeadersByFile().entrySet()) {
                    message.append(entry.getKey().getName())
                            .append(" is missing headers: ")
                            .append(String.join(", ", entry.getValue()))
                            .append("\n");
                }

                message.append("\nDo you want to proceed? Missing values will be left empty.");

                int result = JOptionPane.showConfirmDialog(this,
                        message.toString(),
                        "Header Mismatch",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // Choose output file
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
                }

                public String getDescription() {
                    return "CSV Files (*.csv)";
                }
            });

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();
                if (!outputFile.getName().toLowerCase().endsWith(".csv")) {
                    outputFile = new File(outputFile.getAbsolutePath() + ".csv");
                }

                // Perform merge
                merger.mergeFiles(selectedFiles, outputFile, mergeResult);

                logArea.append("Successfully merged files to: " + outputFile.getName() + "\n");
                logArea.append("Total headers in output: " + mergeResult.getCombinedHeaders().size() + "\n");
                logArea.append("Headers: " + String.join(", ", mergeResult.getCombinedHeaders()) + "\n\n");

                // Show success message
                JOptionPane.showMessageDialog(this,
                        "Files merged successfully!\nOutput file: " + outputFile.getName(),
                        "Merge Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException | CsvException e) {
            logArea.append("Error: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(this,
                    "Error merging files: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mergeToExcel() {
        if (selectedFiles.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Please select at least two files to merge.",
                    "Not Enough Files",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Choose output file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".xlsx");
            }

            public String getDescription() {
                return "Excel Files (*.xlsx)";
            }
        });

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            if (!outputFile.getName().toLowerCase().endsWith(".xlsx")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".xlsx");
            }

            try {
                // Perform merge
                merger.mergeToExcel(selectedFiles, outputFile);

                logArea.append("Successfully merged files to Excel: " + outputFile.getName() + "\n");
                logArea.append("Created " + selectedFiles.size() + " worksheets:\n");
                for (File file : selectedFiles) {
                    logArea.append("- " + file.getName() + "\n");
                }
                logArea.append("\n");

                // Show success message
                JOptionPane.showMessageDialog(this,
                        "Files merged successfully!\nOutput file: " + outputFile.getName(),
                        "Merge Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | CsvException e) {
                logArea.append("Error: " + e.getMessage() + "\n");
                JOptionPane.showMessageDialog(this,
                        "Error merging files: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
