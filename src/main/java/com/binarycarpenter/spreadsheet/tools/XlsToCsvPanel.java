package com.binarycarpenter.spreadsheet.tools;

import com.binarycarpenter.spreadsheet.tools.spreadsheet.ConversionResult;
import com.binarycarpenter.spreadsheet.tools.spreadsheet.SpreadsheetConverter;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class XlsToCsvPanel extends JPanel {
    private final DefaultListModel<File> inputFilesModel;
    private final JList<File> inputFilesList;
    private final ConversionResultsTableModel resultsModel;
    private final JTable resultsTable;
    private final JButton selectFilesButton;
    private final JButton selectFolderButton;
    private final JRadioButton sameLocationRadio;
    private final JRadioButton customLocationRadio;
    private final JButton selectOutputButton;
    private final JCheckBox includeSubfoldersCheckbox;
    private final JButton convertButton;
    private final JButton removeSelectedButton;
    private File outputDirectory;
    private final ExecutorService executor;

    private final Logger log = LoggerFactory.getLogger(XlsToCsvPanel.class);

    public XlsToCsvPanel() {
        setLayout(new MigLayout("fillx, insets 20", "[grow]", "[]10[]10[]10[]10[]"));
        executor = Executors.newSingleThreadExecutor();

        // Input section
        JPanel inputPanel = new JPanel(new MigLayout("fillx", "[grow][]", "[]5[]"));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input Files"));
        
        // File selection buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectFilesButton = new JButton("Select Files");
        selectFilesButton.addActionListener(e -> selectFiles());
        buttonPanel.add(selectFilesButton);
        
        selectFolderButton = new JButton("Select Folder");
        selectFolderButton.addActionListener(e -> selectFolder());
        buttonPanel.add(selectFolderButton);
        
        includeSubfoldersCheckbox = new JCheckBox("Include subfolders");
        buttonPanel.add(includeSubfoldersCheckbox);
        
        removeSelectedButton = new JButton("Remove Selected");
        removeSelectedButton.addActionListener(e -> removeSelectedFiles());
        buttonPanel.add(removeSelectedButton);
        
        inputPanel.add(buttonPanel, "wrap");

        // Files list
        inputFilesModel = new DefaultListModel<>();
        inputFilesList = new JList<>(inputFilesModel);
        inputFilesList.setCellRenderer(new FileListCellRenderer());
        JScrollPane filesScrollPane = new JScrollPane(inputFilesList);
        filesScrollPane.setPreferredSize(new Dimension(0, 150));
        inputPanel.add(filesScrollPane, "growx, wrap");
        
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

        // Results table
        resultsModel = new ConversionResultsTableModel();
        resultsTable = new JTable(resultsModel);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.rowAtPoint(e.getPoint());
                    int col = resultsTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        handleResultTableAction(row, col);
                    }
                }
            }
        });
        
        JScrollPane resultsScrollPane = new JScrollPane(resultsTable);
        resultsScrollPane.setPreferredSize(new Dimension(0, 200));
        add(resultsScrollPane, "grow");

        updateConvertButton();
    }

    private void selectFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Excel files (*.xlsx, *.xls)", "xlsx", "xls"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                if (!isFileAlreadyAdded(file)) {
                    inputFilesModel.addElement(file);
                }
            }
            updateConvertButton();
        }
    }

    private void selectFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            try {
                List<File> files = Files.walk(folder.toPath(), 
                        includeSubfoldersCheckbox.isSelected() ? Integer.MAX_VALUE : 1)
                    .filter(path -> {
                        String name = path.toString().toLowerCase();
                        return name.endsWith(".xlsx") || 
                               name.endsWith(".xls");
                    })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
                
                for (File file : files) {
                    if (!isFileAlreadyAdded(file)) {
                        inputFilesModel.addElement(file);
                    }
                }
                updateConvertButton();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Error scanning folder: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean isFileAlreadyAdded(File file) {
        for (int i = 0; i < inputFilesModel.size(); i++) {
            if (inputFilesModel.getElementAt(i).equals(file)) {
                return true;
            }
        }
        return false;
    }

    private void removeSelectedFiles() {
        int[] selectedIndices = inputFilesList.getSelectedIndices();
        for (int i = selectedIndices.length - 1; i >= 0; i--) {
            inputFilesModel.remove(selectedIndices[i]);
        }
        updateConvertButton();
    }

    private void selectOutputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirectory = chooser.getSelectedFile();
            updateConvertButton();
        }
    }

    private void updateConvertButton() {
        boolean hasFiles = inputFilesModel.size() > 0;
        boolean hasOutput = !customLocationRadio.isSelected() || outputDirectory != null;
        convertButton.setEnabled(hasFiles && hasOutput);
    }

    private void convertFiles() {
        setControlsEnabled(false);
        resultsModel.clear();

        executor.submit(() -> {
            try {
                List<ConversionResult> results = new ArrayList<>();
                int total = inputFilesModel.size();

                for (int i = 0; i < total; i++) {
                    File inputFile = inputFilesModel.getElementAt(i);
                    try {
                        File outputDir = customLocationRadio.isSelected() ? 
                            outputDirectory : inputFile.getParentFile();

                        ConversionResult result = SpreadsheetConverter.convertToCSV(inputFile, outputDir);
                        results.add(result);
                    } catch (Exception e) {
                        log.error("Error during conversion", e);
                        results.add(new ConversionResult(inputFile, e.getMessage()));
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    resultsModel.setResults(results);
                    setControlsEnabled(true);
                });
            } catch (Exception e) {
                log.error("Error during conversion", e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, 
                        "Error during conversion: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    setControlsEnabled(true);
                });
            }
        });
    }

    private void handleResultTableAction(int row, int column) {
        ConversionResult result = resultsModel.getResult(row);
        if (result != null) {
            if (column == 0) { // Input file column
                openFileOrFolder(result.getInputFile().getParentFile());
            } else if (column == 1 && result.isSuccess()) { // Output files column
                if (!result.getOutputFiles().isEmpty()) {
                    openFileOrFolder(result.getOutputFiles().get(0).getParentFile());
                }
            }
        }
    }

    private void openFileOrFolder(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Error opening file/folder: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        selectFilesButton.setEnabled(enabled);
        selectFolderButton.setEnabled(enabled);
        includeSubfoldersCheckbox.setEnabled(enabled);
        sameLocationRadio.setEnabled(enabled);
        customLocationRadio.setEnabled(enabled);
        selectOutputButton.setEnabled(enabled && customLocationRadio.isSelected());
        convertButton.setEnabled(enabled);
        removeSelectedButton.setEnabled(enabled);
        inputFilesList.setEnabled(enabled);
    }

    private static class FileListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File file = (File) value;
                setText(file.getName());
                setToolTipText(file.getAbsolutePath());
            }
            return this;
        }
    }

    private static class ConversionResultsTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Input File", "Status", "Details"};
        private final List<ConversionResult> results = new ArrayList<>();

        public void clear() {
            results.clear();
            fireTableDataChanged();
        }

        public void setResults(List<ConversionResult> newResults) {
            results.clear();
            results.addAll(newResults);
            fireTableDataChanged();
        }

        public ConversionResult getResult(int row) {
            return row >= 0 && row < results.size() ? results.get(row) : null;
        }

        @Override
        public int getRowCount() {
            return results.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            ConversionResult result = results.get(row);
            switch (column) {
                case 0:
                    return result.getInputFile().getName();
                case 1:
                    return result.isSuccess() ? "Success" : "Failed";
                case 2:
                    if (result.isSuccess()) {
                        return result.getOutputFiles().size() + " CSV file(s) created";
                    } else {
                        return result.getError();
                    }
                default:
                    return null;
            }
        }
    }
}
