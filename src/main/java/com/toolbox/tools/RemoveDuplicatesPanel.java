package com.toolbox.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RemoveDuplicatesPanel extends JPanel {
    private JTextField filePathField;
    private JComboBox<String> columnComboBox;
    private JRadioButton entireRowRadio;
    private JRadioButton selectedColumnRadio;
    private JButton browseButton;
    private JButton removeButton;
    private JLabel statusLabel;
    private List<String> headers;
    private String selectedFilePath;

    public RemoveDuplicatesPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // File selection panel
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePathField = new JTextField(30);
        filePathField.setEditable(false);
        browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> browseFile());
        filePanel.add(new JLabel("File:"));
        filePanel.add(filePathField);
        filePanel.add(browseButton);

        // Duplicate removal options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup group = new ButtonGroup();
        entireRowRadio = new JRadioButton("Entire Row", true);
        selectedColumnRadio = new JRadioButton("Selected Column");
        group.add(entireRowRadio);
        group.add(selectedColumnRadio);

        columnComboBox = new JComboBox<>();
        columnComboBox.setEnabled(false);
        columnComboBox.setPreferredSize(new Dimension(200, columnComboBox.getPreferredSize().height));

        selectedColumnRadio.addActionListener(e -> columnComboBox.setEnabled(selectedColumnRadio.isSelected()));
        entireRowRadio.addActionListener(e -> columnComboBox.setEnabled(selectedColumnRadio.isSelected()));

        optionsPanel.add(entireRowRadio);
        optionsPanel.add(selectedColumnRadio);
        optionsPanel.add(columnComboBox);

        // Action panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        removeButton = new JButton("Remove Duplicates");
        removeButton.setEnabled(false);
        removeButton.addActionListener(e -> removeDuplicates());
        actionPanel.add(removeButton);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel(" ");
        statusPanel.add(statusLabel);

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(filePanel);
        mainPanel.add(optionsPanel);
        mainPanel.add(actionPanel);
        mainPanel.add(statusPanel);

        add(mainPanel, BorderLayout.NORTH);
    }

    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".csv") || name.endsWith(".xls") || name.endsWith(".xlsx");
            }

            public String getDescription() {
                return "CSV and Excel files (*.csv, *.xls, *.xlsx)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            selectedFilePath = file.getAbsolutePath();
            filePathField.setText(selectedFilePath);
            loadHeaders(file);
            removeButton.setEnabled(true);
        }
    }

    private void loadHeaders(File file) {
        headers = new ArrayList<>();
        columnComboBox.removeAllItems();

        try {
            if (file.getName().toLowerCase().endsWith(".csv")) {
                try (CSVReader reader = new CSVReader(new FileReader(file))) {
                    String[] headerRow = reader.readNext();
                    if (headerRow != null) {
                        headers = Arrays.asList(headerRow);
                    }
                }
            } else {
                try (Workbook workbook = WorkbookFactory.create(file)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    Row headerRow = sheet.getRow(0);
                    if (headerRow != null) {
                        headers = IntStream.range(0, headerRow.getLastCellNum())
                                .mapToObj(i -> {
                                    Cell cell = headerRow.getCell(i);
                                    return cell == null ? "" : cell.toString();
                                })
                                .collect(Collectors.toList());
                    }
                }
            }

            headers.forEach(columnComboBox::addItem);
        } catch (IOException e) {
            showError("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            showError("Error processing file: " + e.getMessage());
        }
    }

    private void removeDuplicates() {
        if (selectedFilePath == null) {
            showError("Please select a file first");
            return;
        }

        File inputFile = new File(selectedFilePath);
        String outputPath = createOutputPath(selectedFilePath);
        boolean entireRow = entireRowRadio.isSelected();
        int selectedColumnIndex = entireRow ? -1 : columnComboBox.getSelectedIndex();

        try {
            if (inputFile.getName().toLowerCase().endsWith(".csv")) {
                removeDuplicatesFromCsv(inputFile, new File(outputPath), selectedColumnIndex);
            } else {
                removeDuplicatesFromExcel(inputFile, new File(outputPath), selectedColumnIndex);
            }
            showSuccess("Duplicates removed successfully. Output saved to: " + outputPath);
        } catch (Exception e) {
            showError("Error removing duplicates: " + e.getMessage());
        }
    }

    void removeDuplicatesFromCsv(File input, File output, int selectedColumn) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(input));
             CSVWriter writer = new CSVWriter(new FileWriter(output))) {
            
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                return;
            }

            // Write header
            writer.writeNext(allRows.get(0));

            // Process data rows
            Set<String> seen = new HashSet<>();
            List<String[]> uniqueRows = new ArrayList<>();

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                String key = selectedColumn == -1 ? 
                        String.join(",", row) : // Entire row
                        row[selectedColumn];    // Selected column

                if (seen.add(key)) {
                    uniqueRows.add(row);
                }
            }

            writer.writeAll(uniqueRows);
        }
    }

    void removeDuplicatesFromExcel(File input, File output, int selectedColumn) throws IOException {
        try (Workbook inputWorkbook = WorkbookFactory.create(input);
             Workbook outputWorkbook = new XSSFWorkbook()) {
            
            Sheet inputSheet = inputWorkbook.getSheetAt(0);
            Sheet outputSheet = outputWorkbook.createSheet();

            // Copy header row
            Row headerRow = inputSheet.getRow(0);
            if (headerRow == null) {
                return;
            }
            copyRow(headerRow, outputSheet.createRow(0));

            // Process data rows
            Set<String> seen = new HashSet<>();
            int outputRowNum = 1;

            for (int i = 1; i <= inputSheet.getLastRowNum(); i++) {
                Row row = inputSheet.getRow(i);
                if (row == null) continue;

                String key = selectedColumn == -1 ?
                        getRowKey(row) :                  // Entire row
                        getCellValue(row.getCell(selectedColumn)); // Selected column

                if (seen.add(key)) {
                    copyRow(row, outputSheet.createRow(outputRowNum++));
                }
            }

            try (var fileOut = new java.io.FileOutputStream(output)) {
                outputWorkbook.write(fileOut);
            }
        }
    }

    private String getRowKey(Row row) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (i > 0) key.append(",");
            key.append(getCellValue(row.getCell(i)));
        }
        return key.toString();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    private void copyRow(Row source, Row target) {
        for (int i = 0; i < source.getLastCellNum(); i++) {
            Cell sourceCell = source.getCell(i);
            if (sourceCell != null) {
                Cell targetCell = target.createCell(i);
                copyCellValue(sourceCell, targetCell);
            }
        }
    }

    private void copyCellValue(Cell source, Cell target) {
        switch (source.getCellType()) {
            case STRING:
                target.setCellValue(source.getStringCellValue());
                break;
            case NUMERIC:
                target.setCellValue(source.getNumericCellValue());
                break;
            case BOOLEAN:
                target.setCellValue(source.getBooleanCellValue());
                break;
            case FORMULA:
                target.setCellFormula(source.getCellFormula());
                break;
            default:
                target.setCellValue("");
        }
    }

    private String createOutputPath(String inputPath) {
        int lastDot = inputPath.lastIndexOf('.');
        String basePath = lastDot > 0 ? inputPath.substring(0, lastDot) : inputPath;
        String extension = lastDot > 0 ? inputPath.substring(lastDot) : "";
        return basePath + "_no_duplicates" + extension;
    }

    private void showError(String message) {
        statusLabel.setForeground(java.awt.Color.RED);
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.setForeground(new java.awt.Color(0, 128, 0));
        statusLabel.setText(message);
    }
}
