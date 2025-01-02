package com.binarycarpenter.spreadsheet.tools;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import net.miginfocom.swing.MigLayout;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SpreadsheetSearchPanel extends JPanel {
    private JTextArea searchTextArea;
    private DefaultListModel<String> pathListModel;
    private JList<String> pathList;
    private JButton addDirectoryButton;
    private JButton addFilesButton;
    private JButton removeButton;
    private JButton searchButton;
    private JCheckBox exactMatchCheckBox;
    private JCheckBox regexCheckBox;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private SwingWorker<Void, SearchResult> currentSearch;
    private Set<Path> searchPaths;

    private static final String[] COLUMN_NAMES = {
        "File", "Sheet/Tab", "Row", "Column", "Cell Content", "Search Term"
    };

    public SpreadsheetSearchPanel() {
        setLayout(new MigLayout("fill, insets 5", "[grow]", "[][]10[]5[grow][]"));
        searchPaths = new HashSet<>();
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        // Initialize search controls
        searchTextArea = new JTextArea(5, 20);
        searchTextArea.setLineWrap(true);
        searchTextArea.setWrapStyleWord(true);
        
        // Initialize path list
        pathListModel = new DefaultListModel<>();
        pathList = new JList<>(pathListModel);
        pathList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // Initialize buttons
        addDirectoryButton = new JButton("Add Directory");
        addFilesButton = new JButton("Add Files");
        removeButton = new JButton("Remove Selected");
        searchButton = new JButton("Search");
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
        resultsTable.getColumnModel().getColumn(4).setPreferredWidth(300); // Content
        resultsTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Search Term

        // Initialize progress bar and status
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusLabel = new JLabel(" ");

        // Add listeners
        addDirectoryButton.addActionListener(e -> addDirectory());
        addFilesButton.addActionListener(e -> addFiles());
        removeButton.addActionListener(e -> removeSelected());
        searchButton.addActionListener(e -> startSearch());
        
        regexCheckBox.addActionListener(e -> {
            if (regexCheckBox.isSelected()) {
                exactMatchCheckBox.setSelected(false);
                exactMatchCheckBox.setEnabled(false);
            } else {
                exactMatchCheckBox.setEnabled(true);
            }
        });
        
        exactMatchCheckBox.addActionListener(e -> {
            if (exactMatchCheckBox.isSelected()) {
                regexCheckBox.setSelected(false);
                regexCheckBox.setEnabled(false);
            } else {
                regexCheckBox.setEnabled(true);
            }
        });
    }

    private void layoutComponents() {
        // Search panel
        JPanel searchPanel = new JPanel(new MigLayout("insets 0", "[grow]", "[]"));
        searchPanel.add(new JLabel("Search Terms (one per line):"), "wrap");
        searchPanel.add(new JScrollPane(searchTextArea), "grow, wrap");
        
        // Path list panel
        JPanel pathPanel = new JPanel(new MigLayout("insets 0", "[grow][]", "[][]"));
        pathPanel.add(new JLabel("Search Locations:"), "wrap");
        pathPanel.add(new JScrollPane(pathList), "grow, span 1 2");
        
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[]", "[][]"));
        buttonPanel.add(addDirectoryButton, "wrap");
        buttonPanel.add(addFilesButton, "wrap");
        buttonPanel.add(removeButton);
        pathPanel.add(buttonPanel, "top");

        // Options panel
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(searchButton);
        optionsPanel.add(exactMatchCheckBox);
        optionsPanel.add(regexCheckBox);

        // Add components to main panel
        add(searchPanel, "growx, wrap");
        add(pathPanel, "grow, wrap");
        add(optionsPanel, "growx, wrap");
        add(new JScrollPane(resultsTable), "grow, push, wrap");
        add(progressBar, "growx, wrap");
        add(statusLabel, "growx");
    }

    private void addDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Directory to Search");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = chooser.getSelectedFile();
            addSearchPath(dir.toPath());
        }
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".csv") || name.endsWith(".xlsx") || name.endsWith(".xls");
            }

            @Override
            public String getDescription() {
                return "Spreadsheet Files (*.csv, *.xlsx, *.xls)";
            }
        });
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                addSearchPath(file.toPath());
            }
        }
    }

    private void addSearchPath(Path path) {
        if (searchPaths.add(path)) {
            pathListModel.addElement(path.toString());
        }
    }

    private void removeSelected() {
        int[] indices = pathList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            searchPaths.remove(Paths.get(pathListModel.get(indices[i])));
            pathListModel.remove(indices[i]);
        }
    }

    private void startSearch() {
        String[] searchTerms = searchTextArea.getText().trim().split("\\n");
        if (searchTerms.length == 0 || searchTerms[0].isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter at least one search term", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (searchPaths.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one search location", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate regex patterns if selected
        if (regexCheckBox.isSelected()) {
            try {
                for (String term : searchTerms) {
                    Pattern.compile(term);
                }
            } catch (PatternSyntaxException e) {
                JOptionPane.showMessageDialog(this, "Invalid regular expression: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Clear previous results
        tableModel.setRowCount(0);
        statusLabel.setText(" ");
        
        // Disable controls during search
        setControlsEnabled(false);
        
        // Start search in background
        currentSearch = new SearchWorker(
            searchTerms,
            new ArrayList<>(searchPaths),
            exactMatchCheckBox.isSelected(),
            regexCheckBox.isSelected()
        );
        currentSearch.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        searchTextArea.setEnabled(enabled);
        pathList.setEnabled(enabled);
        addDirectoryButton.setEnabled(enabled);
        addFilesButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        exactMatchCheckBox.setEnabled(enabled && !regexCheckBox.isSelected());
        regexCheckBox.setEnabled(enabled && !exactMatchCheckBox.isSelected());
        progressBar.setIndeterminate(!enabled);
    }

    private class SearchWorker extends SwingWorker<Void, SearchResult> {
        private final String[] searchTerms;
        private final List<Path> searchPaths;
        private final boolean exactMatch;
        private final boolean useRegex;
        private int filesProcessed = 0;
        private int totalMatches = 0;
        private final Pattern[] patterns;

        public SearchWorker(String[] searchTerms, List<Path> searchPaths, 
                          boolean exactMatch, boolean useRegex) {
            this.searchTerms = searchTerms;
            this.searchPaths = searchPaths;
            this.exactMatch = exactMatch;
            this.useRegex = useRegex;
            
            if (useRegex) {
                patterns = new Pattern[searchTerms.length];
                for (int i = 0; i < searchTerms.length; i++) {
                    patterns[i] = Pattern.compile(searchTerms[i]);
                }
            } else {
                patterns = null;
            }
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                for (Path path : searchPaths) {
                    if (Files.isDirectory(path)) {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                String fileName = file.toString().toLowerCase();
                                if (fileName.endsWith(".csv") || fileName.endsWith(".xlsx") || 
                                    fileName.endsWith(".xls")) {
                                    searchFile(file.toFile());
                                    filesProcessed++;
                                    updateProgress();
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        searchFile(path.toFile());
                        filesProcessed++;
                        updateProgress();
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(SpreadsheetSearchPanel.this,
                        "Error searching files: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE));
            }
            return null;
        }

        private void searchFile(File file) {
            try {
                if (file.getName().toLowerCase().endsWith(".csv")) {
                    searchCsvFile(file);
                } else {
                    searchExcelFile(file);
                }
            } catch (Exception e) {
                System.err.println("Error processing file " + file + ": " + e.getMessage());
            }
        }

        private void searchCsvFile(File file) throws IOException {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] nextLine;
                int row = 0;
                while ((nextLine = reader.readNext()) != null) {
                    row++;
                    for (int col = 0; col < nextLine.length; col++) {
                        String cellContent = nextLine[col];
                        if (cellContent != null) {
                            checkMatches(file.getName(), "Sheet1", row, col + 1, cellContent);
                        }
                    }
                }
            } catch (CsvException e) {
                throw new IOException("Error reading CSV file: " + e.getMessage(), e);
            }
        }

        private void searchExcelFile(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = file.getName().toLowerCase().endsWith(".xlsx") ?
                     new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
                
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            String cellContent = getCellContentAsString(cell);
                            if (cellContent != null) {
                                checkMatches(file.getName(), sheet.getSheetName(),
                                    row.getRowNum() + 1, cell.getColumnIndex() + 1, cellContent);
                            }
                        }
                    }
                }
            }
        }

        private void checkMatches(String fileName, String sheetName, int row, int col, String cellContent) {
            for (int i = 0; i < searchTerms.length; i++) {
                if (matches(cellContent, i)) {
                    publish(new SearchResult(
                        fileName,
                        sheetName,
                        row,
                        col,
                        cellContent,
                        searchTerms[i]
                    ));
                    totalMatches++;
                }
            }
        }

        private String getCellContentAsString(Cell cell) {
            if (cell == null) return null;
            
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toString();
                    }
                    return String.valueOf(cell.getNumericCellValue());
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception ex) {
                            return cell.getCellFormula();
                        }
                    }
                default:
                    return null;
            }
        }

        private boolean matches(String cellContent, int termIndex) {
            if (useRegex) {
                return patterns[termIndex].matcher(cellContent).find();
            } else if (exactMatch) {
                return cellContent.equals(searchTerms[termIndex]);
            } else {
                return cellContent.contains(searchTerms[termIndex]);
            }
        }

        private void updateProgress() {
            SwingUtilities.invokeLater(() -> 
                statusLabel.setText(String.format("Processed %d files, found %d matches", 
                    filesProcessed, totalMatches)));
        }

        @Override
        protected void process(List<SearchResult> chunks) {
            for (SearchResult result : chunks) {
                tableModel.addRow(new Object[]{
                    result.fileName,
                    result.sheetName,
                    result.row,
                    result.column,
                    result.content,
                    result.searchTerm
                });
            }
        }

        @Override
        protected void done() {
            setControlsEnabled(true);
            progressBar.setIndeterminate(false);
            statusLabel.setText(String.format("Search complete. Processed %d files, found %d matches", 
                filesProcessed, totalMatches));
        }
    }

    private static class SearchResult {
        final String fileName;
        final String sheetName;
        final int row;
        final int column;
        final String content;
        final String searchTerm;

        SearchResult(String fileName, String sheetName, int row, int column, 
                    String content, String searchTerm) {
            this.fileName = fileName;
            this.sheetName = sheetName;
            this.row = row;
            this.column = column;
            this.content = content;
            this.searchTerm = searchTerm;
        }
    }
}
