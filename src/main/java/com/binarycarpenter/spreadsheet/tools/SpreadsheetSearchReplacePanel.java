package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import net.miginfocom.swing.MigLayout;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SpreadsheetSearchReplacePanel extends JPanel {
    // File selection components
    private DefaultListModel<String> pathListModel;
    private JList<String> pathList;
    private Set<Path> searchPaths;
    private JButton addDirectoryButton;
    private JButton addFilesButton;
    private JButton removeButton;

    // Search/Replace components
    private JTextField searchField;
    private JTextField replaceField;
    private JCheckBox matchCaseCheckBox;
    private JCheckBox exactMatchCheckBox;
    private JCheckBox regexCheckBox;
    private JButton previewButton;
    private JButton executeButton;

    // Results table
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private SwingWorker<Void, SearchReplaceResult> currentOperation;

    private static final String[] COLUMN_NAMES = {
        "File", "Sheet/Tab", "Row", "Column", "Current Content", "Will Replace With"
    };

    public SpreadsheetSearchReplacePanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow]", "[][]10[]5[grow][]"));
        searchPaths = new HashSet<>();
        initializeComponents();
        layoutComponents();
        setupListeners();
    }

    private void initializeComponents() {
        // Initialize path list
        pathListModel = new DefaultListModel<>();
        pathList = new JList<>(pathListModel);
        pathList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Initialize search/replace fields
        searchField = new JTextField(20);
        replaceField = new JTextField(20);
        matchCaseCheckBox = new JCheckBox("Match Case");
        exactMatchCheckBox = new JCheckBox("Exact Match");
        regexCheckBox = new JCheckBox("Regex");

        // Initialize results table
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // File
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Sheet
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Row
        resultsTable.getColumnModel().getColumn(3).setPreferredWidth(60);  // Column
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(200); // Current Content
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(200); // Will Replace With

        // Initialize progress and status
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusLabel = new JLabel(" ");
    }

    private void layoutComponents() {
        // File selection panel
        JPanel filePanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]unrel[]"));
        filePanel.setBorder(BorderFactory.createTitledBorder("Files and Folders"));

        addDirectoryButton = new JButton("Add Directory");
        addFilesButton = new JButton("Add Files");
        removeButton = new JButton("Remove Selected");

        filePanel.add(new JScrollPane(pathList), "grow, span 1 2");
        filePanel.add(addDirectoryButton, "wrap");
        filePanel.add(addFilesButton, "split 2");
        filePanel.add(removeButton, "wrap");

        // Search/Replace panel
        JPanel searchReplacePanel = new JPanel(new MigLayout("fillx, insets 0", "[][grow][]", "[][]"));
        searchReplacePanel.setBorder(BorderFactory.createTitledBorder("Search and Replace"));

        searchReplacePanel.add(new JLabel("Search:"));
        searchReplacePanel.add(searchField, "grow");
        searchReplacePanel.add(matchCaseCheckBox);
        searchReplacePanel.add(exactMatchCheckBox);
        searchReplacePanel.add(regexCheckBox, "wrap");

        searchReplacePanel.add(new JLabel("Replace:"));
        searchReplacePanel.add(replaceField, "grow");

        previewButton = new JButton("Preview Changes");
        executeButton = new JButton("Execute Replace");
        searchReplacePanel.add(previewButton, "split 2");
        searchReplacePanel.add(executeButton, "wrap");

        // Results panel
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Preview"));
        resultsPanel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]"));
        statusPanel.add(progressBar, "grow");
        statusPanel.add(statusLabel);

        // Add all panels to main panel
        add(filePanel, "grow, wrap");
        add(searchReplacePanel, "grow, wrap");
        add(resultsPanel, "grow, wrap");
        add(statusPanel, "grow");

        // Set minimum size
        setPreferredSize(new Dimension(800, 600));
    }

    private void setupListeners() {
        // File selection listeners
        addDirectoryButton.addActionListener(e -> addDirectory());
        addFilesButton.addActionListener(e -> addFiles());
        removeButton.addActionListener(e -> removeSelected());

        // Search/Replace listeners
        previewButton.addActionListener(e -> previewChanges());
        executeButton.addActionListener(e -> executeReplace());

        // Option checkboxes listeners
        regexCheckBox.addActionListener(e -> {
            if (regexCheckBox.isSelected()) {
                exactMatchCheckBox.setSelected(false);
                exactMatchCheckBox.setEnabled(false);
            } else {
                exactMatchCheckBox.setEnabled(true);
            }
        });

        // Search field validation
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { validateSearch(); }
            @Override
            public void removeUpdate(DocumentEvent e) { validateSearch(); }
            @Override
            public void changedUpdate(DocumentEvent e) { validateSearch(); }
        });
    }

    private void validateSearch() {
        boolean validSearch = !searchField.getText().trim().isEmpty();
        previewButton.setEnabled(validSearch);
        executeButton.setEnabled(validSearch);

        if (regexCheckBox.isSelected()) {
            try {
                Pattern.compile(searchField.getText());
            } catch (PatternSyntaxException e) {
                validSearch = false;
                statusLabel.setText("Invalid regex pattern");
                previewButton.setEnabled(false);
                executeButton.setEnabled(false);
            }
        }
    }

    private void addDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            addPath(dir.toPath());
        }
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Spreadsheet files (*.csv, *.xlsx, *.xls)", "csv", "xlsx", "xls");
        chooser.setFileFilter(filter);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                addPath(file.toPath());
            }
        }
    }

    private void addPath(Path path) {
        if (!searchPaths.contains(path)) {
            searchPaths.add(path);
            pathListModel.addElement(path.toString());
        }
    }

    private void removeSelected() {
        List<String> selectedPaths = pathList.getSelectedValuesList();
        for (String pathStr : selectedPaths) {
            searchPaths.remove(Path.of(pathStr));
            pathListModel.removeElement(pathStr);
        }
    }

    private void previewChanges() {
        if (currentOperation != null && !currentOperation.isDone()) {
            currentOperation.cancel(true);
        }

        tableModel.setRowCount(0);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        statusLabel.setText("Searching...");

        currentOperation = new SwingWorker<Void, SearchReplaceResult>() {
            @Override
            protected Void doInBackground() throws Exception {
                processFiles(true);
                return null;
            }

            @Override
            protected void process(List<SearchReplaceResult> results) {
                SwingUtilities.invokeLater(() -> {
                    for (SearchReplaceResult result : results) {
                        tableModel.addRow(new Object[]{
                            result.filePath,
                            result.sheet,
                            result.row + 1, // Convert to 1-based for display
                            result.column + 1,
                            result.currentContent,
                            result.replacement
                        });
                    }
                });
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                statusLabel.setText(String.format("Found %d matches", tableModel.getRowCount()));
            }
        };

        currentOperation.execute();
    }

    private void executeReplace() {
        int option = JOptionPane.showConfirmDialog(
            this,
            String.format("Are you sure you want to replace %d occurrences?", tableModel.getRowCount()),
            "Confirm Replace",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            if (currentOperation != null && !currentOperation.isDone()) {
                currentOperation.cancel(true);
            }

            progressBar.setIndeterminate(true);
            progressBar.setVisible(true);
            statusLabel.setText("Replacing...");

            currentOperation = new SwingWorker<Void, SearchReplaceResult>() {
                @Override
                protected Void doInBackground() throws Exception {
                    processFiles(false);
                    return null;
                }

                @Override
                protected void done() {
                    progressBar.setVisible(false);
                    statusLabel.setText("Replace operation completed");
                    tableModel.setRowCount(0); // Clear preview
                }
            };

            currentOperation.execute();
        }
    }

    private void processFiles(boolean previewOnly) throws IOException {
        for (Path path : searchPaths) {
            if (Files.isDirectory(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.filter(Files::isRegularFile)
                        .filter(this::isSpreadsheetFile)
                        .forEach(file -> processFile(file, previewOnly));
                }
            } else if (Files.isRegularFile(path) && isSpreadsheetFile(path)) {
                processFile(path, previewOnly);
            }
        }
    }

    private boolean isSpreadsheetFile(Path path) {
        String name = path.toString().toLowerCase();
        return name.endsWith(".csv") || name.endsWith(".xlsx") || name.endsWith(".xls");
    }

    private void processFile(Path file, boolean previewOnly) {
        try {
            String fileName = file.toString().toLowerCase();
            if (fileName.endsWith(".csv")) {
                processCsvFile(file, previewOnly);
            } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                processExcelFile(file, previewOnly);
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Error processing file: " + file.getFileName());
                e.printStackTrace();
            });
        }
    }

    private void processCsvFile(Path file, boolean previewOnly) throws IOException, CsvException {
        String[][] rows;
        try (CSVReader reader = new CSVReader(new FileReader(file.toFile()))) {
            List<String[]> rowsList = reader.readAll();
            rows = rowsList.toArray(new String[0][]);
        }

        boolean modified = false;
        List<SearchReplaceResult> changes = new ArrayList<>();

        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            for (int j = 0; j < row.length; j++) {
                String cell = row[j];
                String replacement = matchAndReplace(cell);
                
                if (replacement != null) {
                    changes.add(new SearchReplaceResult(
                        file.toString(), "Sheet1", i, j, cell, replacement
                    ));
                    if (!previewOnly) {
                        row[j] = replacement;
                        modified = true;
                    }
                }
            }
        }

        if (!previewOnly && modified) {
            try (CSVWriter writer = new CSVWriter(new FileWriter(file.toFile()))) {
                writer.writeAll(Arrays.asList(rows));
            }
        }

        if (!changes.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                for (SearchReplaceResult result : changes) {
                    tableModel.addRow(new Object[]{
                        result.filePath,
                        result.sheet,
                        result.row + 1,
                        result.column + 1,
                        result.currentContent,
                        result.replacement
                    });
                }
            });
        }
    }

    private void processExcelFile(Path file, boolean previewOnly) throws IOException {
        try (FileInputStream fis = new FileInputStream(file.toFile());
             Workbook workbook = file.toString().toLowerCase().endsWith(".xlsx") ?
                                new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            
            boolean modified = false;
            List<SearchReplaceResult> changes = new ArrayList<>();

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING) {
                            String cellValue = cell.getStringCellValue();
                            String replacement = matchAndReplace(cellValue);
                            
                            if (replacement != null) {
                                changes.add(new SearchReplaceResult(
                                    file.toString(),
                                    sheet.getSheetName(),
                                    row.getRowNum(),
                                    cell.getColumnIndex(),
                                    cellValue,
                                    replacement
                                ));
                                
                                if (!previewOnly) {
                                    cell.setCellValue(replacement);
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }

            if (!previewOnly && modified) {
                try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                    workbook.write(fos);
                }
            }

            if (!changes.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    for (SearchReplaceResult result : changes) {
                        tableModel.addRow(new Object[]{
                            result.filePath,
                            result.sheet,
                            result.row + 1,
                            result.column + 1,
                            result.currentContent,
                            result.replacement
                        });
                    }
                });
            }
        }
    }

    private String matchAndReplace(String text) {
        if (text == null) return null;

        String searchText = searchField.getText();
        String replaceText = replaceField.getText();
        
        if (regexCheckBox.isSelected()) {
            try {
                Pattern pattern = matchCaseCheckBox.isSelected() ? 
                    Pattern.compile(searchText) :
                    Pattern.compile(searchText, Pattern.CASE_INSENSITIVE);
                
                if (pattern.matcher(text).find()) {
                    return text.replaceAll(searchText, replaceText);
                }
            } catch (PatternSyntaxException e) {
                return null;
            }
        } else if (exactMatchCheckBox.isSelected()) {
            if (matchCaseCheckBox.isSelected() ? 
                text.equals(searchText) : 
                text.equalsIgnoreCase(searchText)) {
                return replaceText;
            }
        } else {
            String textToSearch = matchCaseCheckBox.isSelected() ? 
                text : text.toLowerCase();
            searchText = matchCaseCheckBox.isSelected() ? 
                searchText : searchText.toLowerCase();
            
            if (textToSearch.contains(searchText)) {
                return text.replace(
                    text.substring(
                        textToSearch.indexOf(searchText),
                        textToSearch.indexOf(searchText) + searchText.length()
                    ),
                    replaceText
                );
            }
        }
        
        return null;
    }

    private static class SearchReplaceResult {
        final String filePath;
        final String sheet;
        final int row;
        final int column;
        final String currentContent;
        final String replacement;

        SearchReplaceResult(String filePath, String sheet, int row, int column, 
                          String currentContent, String replacement) {
            this.filePath = filePath;
            this.sheet = sheet;
            this.row = row;
            this.column = column;
            this.currentContent = currentContent;
            this.replacement = replacement;
        }
    }
}
