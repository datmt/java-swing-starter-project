package com.toolbox.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CsvEditorPanel extends JPanel {
    private JTable table;
    private JTextArea textArea;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private File currentFile;
    private boolean isSpreadsheetMode = true;
    private JTextField searchField;
    private JTextField replaceField;
    private JButton findNextButton;
    private JButton replaceButton;
    private JButton replaceAllButton;
    private JCheckBox matchCaseCheckBox;
    private int currentSearchRow = -1;
    private int currentSearchCol = -1;
    private List<Point> matchingCells = new ArrayList<>();

    public CsvEditorPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top panel with controls
        JPanel controlPanel = new JPanel(new MigLayout("fillx", "[left][center, grow][right]"));
        
        // File controls
        JButton openButton = new JButton("Open CSV");
        openButton.addActionListener(e -> openFile());
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveFile());
        
        // View mode toggle
        JToggleButton viewModeButton = new JToggleButton("Switch to Text Mode");
        viewModeButton.addActionListener(e -> toggleViewMode(viewModeButton));

        // Search and Replace Panel
        JPanel searchPanel = new JPanel(new MigLayout("fillx", "[][grow][]"));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search & Replace"));
        
        searchField = new JTextField(20);
        replaceField = new JTextField(20);
        findNextButton = new JButton("Find Next");
        replaceButton = new JButton("Replace");
        replaceAllButton = new JButton("Replace All");
        matchCaseCheckBox = new JCheckBox("Match Case");
        
        searchPanel.add(new JLabel("Find:"), "right");
        searchPanel.add(searchField, "growx");
        searchPanel.add(findNextButton, "wrap");
        searchPanel.add(new JLabel("Replace:"), "right");
        searchPanel.add(replaceField, "growx");
        searchPanel.add(replaceButton, "split 2");
        searchPanel.add(replaceAllButton, "wrap");
        searchPanel.add(matchCaseCheckBox, "skip 1");

        controlPanel.add(openButton);
        controlPanel.add(saveButton);
        controlPanel.add(viewModeButton, "right, wrap");
        controlPanel.add(searchPanel, "span, growx");
        
        add(controlPanel, BorderLayout.NORTH);

        // Content panel with card layout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Initialize table with custom renderer for highlighting
        table = new JTable() {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component comp = super.prepareRenderer(renderer, row, col);
                if (isCellSelected(row, col)) {
                    comp.setBackground(getSelectionBackground());
                    comp.setForeground(getSelectionForeground());
                } else if (matchingCells.contains(new Point(row, col))) {
                    comp.setBackground(new Color(255, 255, 0, 100)); // Light yellow highlight
                    comp.setForeground(getForeground());
                } else {
                    comp.setBackground(getBackground());
                    comp.setForeground(getForeground());
                }
                return comp;
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Initialize text area
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane textScrollPane = new JScrollPane(textArea);

        contentPanel.add(tableScrollPane, "spreadsheet");
        contentPanel.add(textScrollPane, "text");

        add(contentPanel, BorderLayout.CENTER);

        // Initialize search listeners
        setupSearchListeners();
    }

    private void setupSearchListeners() {
        findNextButton.addActionListener(e -> findNext());
        replaceButton.addActionListener(e -> replace());
        replaceAllButton.addActionListener(e -> replaceAll());
        
        // Reset search when search text changes
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetSearch();
            }
        });
    }

    private void resetSearch() {
        currentSearchRow = -1;
        currentSearchCol = -1;
        matchingCells.clear();
        table.repaint();
    }

    private void findNext() {
        String searchText = searchField.getText();
        if (searchText.isEmpty() || !isSpreadsheetMode) return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        boolean matchCase = matchCaseCheckBox.isSelected();
        
        // Start from current position or beginning
        int startRow = currentSearchRow;
        int startCol = currentSearchCol + 1;
        
        // Search through all cells
        boolean found = false;
        matchingCells.clear();
        
        // First, collect all matches
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    if (!matchCase) {
                        cellText = cellText.toLowerCase();
                        searchText = searchText.toLowerCase();
                    }
                    if (cellText.contains(searchText)) {
                        matchingCells.add(new Point(row, col));
                    }
                }
            }
        }
        
        // Find next match from current position
        for (Point match : matchingCells) {
            if ((match.x > startRow) || (match.x == startRow && match.y > startCol)) {
                currentSearchRow = match.x;
                currentSearchCol = match.y;
                found = true;
                break;
            }
        }
        
        // Wrap around if necessary
        if (!found && !matchingCells.isEmpty()) {
            Point firstMatch = matchingCells.get(0);
            currentSearchRow = firstMatch.x;
            currentSearchCol = firstMatch.y;
            found = true;
        }
        
        if (found) {
            // Select and scroll to the found cell
            table.setRowSelectionInterval(currentSearchRow, currentSearchRow);
            table.setColumnSelectionInterval(currentSearchCol, currentSearchCol);
            table.scrollRectToVisible(table.getCellRect(currentSearchRow, currentSearchCol, true));
        } else {
            JOptionPane.showMessageDialog(this,
                "No matches found for: " + searchText,
                "Search Result",
                JOptionPane.INFORMATION_MESSAGE);
            resetSearch();
        }
        
        table.repaint();
    }

    private void replace() {
        if (!isSpreadsheetMode || currentSearchRow < 0 || currentSearchCol < 0) return;
        
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String replaceText = replaceField.getText();
        
        model.setValueAt(replaceText, currentSearchRow, currentSearchCol);
        findNext(); // Move to next match
    }

    private void replaceAll() {
        String searchText = searchField.getText();
        String replaceText = replaceField.getText();
        if (searchText.isEmpty() || !isSpreadsheetMode) return;

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        boolean matchCase = matchCaseCheckBox.isSelected();
        int replacements = 0;
        
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    String compareCellText = matchCase ? cellText : cellText.toLowerCase();
                    String compareSearchText = matchCase ? searchText : searchText.toLowerCase();
                    
                    if (compareCellText.contains(compareSearchText)) {
                        String newText = cellText.replace(searchText, replaceText);
                        model.setValueAt(newText, row, col);
                        replacements++;
                    }
                }
            }
        }
        
        resetSearch();
        JOptionPane.showMessageDialog(this,
            String.format("Replaced %d occurrence%s", replacements, replacements == 1 ? "" : "s"),
            "Replace Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void toggleViewMode(JToggleButton button) {
        isSpreadsheetMode = !isSpreadsheetMode;
        if (isSpreadsheetMode) {
            button.setText("Switch to Text Mode");
            updateTableFromText();
            cardLayout.show(contentPanel, "spreadsheet");
        } else {
            button.setText("Switch to Spreadsheet Mode");
            updateTextFromTable();
            cardLayout.show(contentPanel, "text");
        }
        resetSearch(); // Clear search highlights when switching modes
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }
            public String getDescription() {
                return "CSV Files (*.csv)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            loadFile();
        }
    }

    private void loadFile() {
        try (CSVReader reader = new CSVReader(new FileReader(currentFile))) {
            List<String[]> allRows = reader.readAll();
            
            if (allRows.isEmpty()) {
                showError("Empty CSV file");
                return;
            }

            // Update table view
            String[] headers = allRows.get(0);
            DefaultTableModel model = new DefaultTableModel(headers, 0);
            allRows.stream().skip(1).forEach(model::addRow);
            table.setModel(model);

            // Update text view
            updateTextFromTable();

        } catch (IOException | CsvException e) {
            showError("Error loading file: " + e.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                if (!currentFile.getName().toLowerCase().endsWith(".csv")) {
                    currentFile = new File(currentFile.getAbsolutePath() + ".csv");
                }
            } else {
                return;
            }
        }

        try {
            if (!isSpreadsheetMode) {
                updateTableFromText();
            }
            
            try (CSVWriter writer = new CSVWriter(new FileWriter(currentFile))) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                
                // Write headers
                Vector<String> headers = new Vector<>();
                for (int i = 0; i < model.getColumnCount(); i++) {
                    headers.add(model.getColumnName(i));
                }
                writer.writeNext(headers.stream()
                    .map(Object::toString)
                    .toArray(String[]::new));
                
                // Write data
                for (int i = 0; i < model.getRowCount(); i++) {
                    String[] row = new String[model.getColumnCount()];
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object value = model.getValueAt(i, j);
                        row[j] = value != null ? value.toString() : "";
                    }
                    writer.writeNext(row);
                }
            }
            
            JOptionPane.showMessageDialog(this, 
                "File saved successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (IOException e) {
            showError("Error saving file: " + e.getMessage());
        }
    }

    private void updateTextFromTable() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        StringBuilder sb = new StringBuilder();
        
        // Headers
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCSV(model.getColumnName(i)));
        }
        sb.append("\n");
        
        // Data
        for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                if (j > 0) sb.append(",");
                Object value = model.getValueAt(i, j);
                sb.append(escapeCSV(value != null ? value.toString() : ""));
            }
            sb.append("\n");
        }
        
        textArea.setText(sb.toString());
    }

    private void updateTableFromText() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            table.setModel(new DefaultTableModel());
            return;
        }

        try {
            List<String[]> rows = Arrays.stream(text.split("\n"))
                .map(line -> line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(row -> Arrays.stream(row)
                    .map(this::unescapeCSV)
                    .toArray(String[]::new))
                .collect(Collectors.toList());

            if (rows.isEmpty()) {
                table.setModel(new DefaultTableModel());
                return;
            }

            String[] headers = rows.get(0);
            DefaultTableModel model = new DefaultTableModel(headers, 0);
            rows.stream().skip(1).forEach(model::addRow);
            table.setModel(model);

        } catch (Exception e) {
            showError("Error parsing CSV text: " + e.getMessage());
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String unescapeCSV(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
            value = value.replace("\"\"", "\"");
        }
        return value;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}
