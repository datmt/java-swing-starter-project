package com.toolbox.tools;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.toolbox.utils.Icons;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Arrays;
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
    private JCheckBox exactMatchCheckBox;
    private List<Point> matchingCells = new ArrayList<>();
    private int currentMatchIndex = -1;
    private JProgressBar progressBar;
    private JPanel overlayPanel;
    private JLayeredPane layeredPane;
    private JPanel mainPanel;
    private boolean isProcessing = false;

    public CsvEditorPanel() {
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // Initialize card layout and content panel first
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
        table.setFillsViewportHeight(true);
        
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Initialize text area
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane textScrollPane = new JScrollPane(textArea);

        // Add scroll panes to content panel
        JPanel spreadsheetPanel = new JPanel(new BorderLayout());
        spreadsheetPanel.add(tableScrollPane, BorderLayout.CENTER);
        contentPanel.add(spreadsheetPanel, "spreadsheet");
        
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.add(textScrollPane, BorderLayout.CENTER);
        contentPanel.add(textPanel, "text");

        // Create top panel with controls
        JPanel topPanel = new JPanel(new MigLayout("insets 0, gap 2", "[left]2[left]2[center,grow]0[right]", "[]0[]"));
        
        // File controls with small fixed size buttons
        JButton openButton = new JButton();
        openButton.setIcon(Icons.OPEN);
        openButton.setPreferredSize(new Dimension(28, 28));
        openButton.setToolTipText("Open CSV File");
        openButton.addActionListener(e -> openFile());
        
        JButton saveButton = new JButton();
        saveButton.setIcon(Icons.SAVE);
        saveButton.setPreferredSize(new Dimension(28, 28));
        saveButton.setToolTipText("Save CSV File");
        saveButton.addActionListener(e -> saveFile());
        
        // View mode toggle
        JToggleButton viewModeButton = new JToggleButton("Text");
        viewModeButton.setToolTipText("Switch between Spreadsheet and Text mode");
        viewModeButton.addActionListener(e -> toggleViewMode(viewModeButton));

        // Search and Replace Panel
        JPanel searchPanel = new JPanel(new MigLayout("insets 0, gap 2", "[][grow][]2[]", "[]2[]"));
        
        searchField = new JTextField(20);
        replaceField = new JTextField(20);
        
        findNextButton = new JButton();
        findNextButton.setIcon(Icons.FIND);
        findNextButton.setPreferredSize(new Dimension(28, 28));
        findNextButton.setToolTipText("Find Next");
        
        replaceButton = new JButton();
        replaceButton.setIcon(Icons.REPLACE);
        replaceButton.setPreferredSize(new Dimension(28, 28));
        replaceButton.setToolTipText("Replace");
        
        replaceAllButton = new JButton();
        replaceAllButton.setIcon(Icons.REPLACE_ALL);
        replaceAllButton.setPreferredSize(new Dimension(28, 28));
        replaceAllButton.setToolTipText("Replace All");
        
        matchCaseCheckBox = new JCheckBox("Aa");
        matchCaseCheckBox.setToolTipText("Match Case");
        
        exactMatchCheckBox = new JCheckBox("Exact");
        exactMatchCheckBox.setToolTipText("Match Entire Cell Content");
        exactMatchCheckBox.addActionListener(e -> updateSearch());

        // First row: file controls and view mode
        topPanel.add(openButton, "cell 0 0");
        topPanel.add(saveButton, "cell 1 0");
        topPanel.add(new JLabel(), "cell 2 0, growx"); // Spacer
        topPanel.add(viewModeButton, "cell 3 0");

        // Second row: search and replace
        searchPanel.add(new JLabel("Find:"), "");
        searchPanel.add(searchField, "growx");
        searchPanel.add(findNextButton, "");
        searchPanel.add(matchCaseCheckBox, "");
        searchPanel.add(exactMatchCheckBox, "wrap");
        
        searchPanel.add(new JLabel("Replace:"), "");
        searchPanel.add(replaceField, "growx");
        searchPanel.add(replaceButton, "split 2");
        searchPanel.add(replaceAllButton, "");
        
        topPanel.add(searchPanel, "cell 0 1 4 1, growx");

        // Create progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(0, 4));
        progressBar.setStringPainted(false);

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);

        // Add main panel to this panel
        add(mainPanel, BorderLayout.CENTER);

        // Set sizes
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(800, 600));

        // Initialize search
        setupSearchListeners();
    }

    private void setupSearchListeners() {
        // Search field listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearch();
            }
        });

        // Find next button
        findNextButton.addActionListener(e -> findNext());

        // Replace button
        replaceButton.addActionListener(e -> replaceSelected());

        // Replace all button
        replaceAllButton.addActionListener(e -> replaceAll());
    }

    private void updateSearch() {
        matchingCells.clear();
        currentMatchIndex = -1;
        String searchText = searchField.getText();
        
        if (searchText.isEmpty()) {
            table.repaint();
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        boolean caseSensitive = matchCaseCheckBox.isSelected();
        boolean exactMatch = exactMatchCheckBox.isSelected();
        
        if (!caseSensitive) {
            searchText = searchText.toLowerCase();
        }

        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    if (!caseSensitive) {
                        cellText = cellText.toLowerCase();
                    }
                    
                    boolean matches;
                    if (exactMatch) {
                        matches = cellText.equals(searchText);
                    } else {
                        matches = cellText.contains(searchText);
                    }
                    
                    if (matches) {
                        matchingCells.add(new Point(row, col));
                    }
                }
            }
        }
        table.repaint();
        if (!matchingCells.isEmpty()) {
            findNext(); // Move to first match
        }
    }

    private void findNext() {
        if (matchingCells.isEmpty()) {
            return;
        }
        
        currentMatchIndex = (currentMatchIndex + 1) % matchingCells.size();
        Point match = matchingCells.get(currentMatchIndex);
        
        // Select and scroll to the cell
        table.setRowSelectionInterval(match.x, match.x);
        table.setColumnSelectionInterval(match.y, match.y);
        table.scrollRectToVisible(table.getCellRect(match.x, match.y, true));
    }

    private void replaceSelected() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matchingCells.size()) {
            Point match = matchingCells.get(currentMatchIndex);
            String replaceText = replaceField.getText();
            
            // If in exact match mode, replace the entire cell
            if (exactMatchCheckBox.isSelected()) {
                table.setValueAt(replaceText, match.x, match.y);
            } else {
                // Otherwise, replace only the matching portion
                Object value = table.getValueAt(match.x, match.y);
                if (value != null) {
                    String cellText = value.toString();
                    String searchText = searchField.getText();
                    
                    if (!matchCaseCheckBox.isSelected()) {
                        String cellTextLower = cellText.toLowerCase();
                        String searchTextLower = searchText.toLowerCase();
                        int start = cellTextLower.indexOf(searchTextLower);
                        if (start >= 0) {
                            String newText = cellText.substring(0, start) + 
                                           replaceText + 
                                           cellText.substring(start + searchText.length());
                            table.setValueAt(newText, match.x, match.y);
                        }
                    } else {
                        String newText = cellText.replace(searchText, replaceText);
                        table.setValueAt(newText, match.x, match.y);
                    }
                }
            }
            
            // Update search to reflect changes
            updateSearch();
        }
    }

    private void replaceAll() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String searchText = searchField.getText();
        String replaceText = replaceField.getText();
        boolean caseSensitive = matchCaseCheckBox.isSelected();
        boolean exactMatch = exactMatchCheckBox.isSelected();
        
        if (searchText.isEmpty()) {
            return;
        }

        // Start background task
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private int totalReplacements = 0;

            @Override
            protected Void doInBackground() {
                try {
                    // Process each cell
                    for (int row = 0; row < model.getRowCount(); row++) {
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            Object value = model.getValueAt(row, col);
                            if (value != null) {
                                String cellText = value.toString();
                                String compareCellText = caseSensitive ? cellText : cellText.toLowerCase();
                                String compareSearchText = caseSensitive ? searchText : searchText.toLowerCase();

                                boolean matches;
                                if (exactMatch) {
                                    matches = compareCellText.equals(compareSearchText);
                                } else {
                                    matches = compareCellText.contains(compareSearchText);
                                }

                                if (matches) {
                                    if (exactMatch) {
                                        model.setValueAt(replaceText, row, col);
                                        totalReplacements++;
                                    } else {
                                        String newText = caseSensitive ?
                                            cellText.replace(searchText, replaceText) :
                                            cellText.replaceAll("(?i)" + Pattern.quote(searchText), replaceText);
                                        model.setValueAt(newText, row, col);
                                        totalReplacements++;
                                    }
                                }
                            }
                            
                            // Add a small delay to prevent UI from becoming completely unresponsive
                            if ((row * model.getColumnCount() + col) % 1000 == 0) {
                                Thread.sleep(1);
                            }
                        }
                    }
                    return null;
                } catch (InterruptedException e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                // Re-enable UI
                setProcessing(false);
                
                // Update search state
                clearSearchState();
                updateSearch();
                
                // Show completion message
                JOptionPane.showMessageDialog(CsvEditorPanel.this,
                    String.format("Replaced %d occurrence%s", 
                        totalReplacements,
                        totalReplacements == 1 ? "" : "s"),
                    "Replace Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };

        // Start processing
        setProcessing(true);
        worker.execute();
    }

    private void toggleViewMode(JToggleButton button) {
        isSpreadsheetMode = !button.isSelected();
        
        if (isSpreadsheetMode) {
            updateTableFromText();
            button.setText("Text");
            cardLayout.show(contentPanel, "spreadsheet");
        } else {
            updateTextFromTable();
            button.setText("Table");
            cardLayout.show(contentPanel, "text");
        }
        
        // Clear search state when switching views
        clearSearchState();
        searchField.setText("");
    }

    private void clearSearchState() {
        matchingCells.clear();
        currentMatchIndex = -1;
        if (table != null) {
            table.clearSelection();
            table.repaint();
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
                // Clear existing data
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                model.setColumnCount(0);
                
                // Read header
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    String[] headers = parseCsvLine(headerLine);
                    for (String header : headers) {
                        model.addColumn(header);
                    }
                }
                
                // Read data
                String line;
                while ((line = reader.readLine()) != null) {
                    model.addRow(parseCsvLine(line));
                }
                
                // Clear search state and update UI
                clearSearchState();
                searchField.setText("");
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error reading file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Double quotes inside quoted string = escaped quote
                    currentValue.append('"');
                    i++; // Skip the next quote
                } else {
                    // Toggle quote mode
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of value
                result.add(currentValue.toString().trim());
                currentValue.setLength(0);
            } else {
                currentValue.append(c);
            }
        }
        
        // Add the last value
        result.add(currentValue.toString().trim());
        
        return result.toArray(new String[0]);
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            
            try (PrintWriter writer = new PrintWriter(file)) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                
                // Write headers
                for (int col = 0; col < model.getColumnCount(); col++) {
                    if (col > 0) writer.print(",");
                    writeValue(writer, model.getColumnName(col));
                }
                writer.println();
                
                // Write data
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        if (col > 0) writer.print(",");
                        Object value = model.getValueAt(row, col);
                        writeValue(writer, value != null ? value.toString() : "");
                    }
                    writer.println();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error saving file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void writeValue(PrintWriter writer, String value) {
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        
        if (!needsQuotes) {
            writer.print(value);
            return;
        }
        
        writer.print('"');
        writer.print(value.replace("\"", "\"\""));
        writer.print('"');
    }

    private void updateTextFromTable() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        StringBuilder sb = new StringBuilder();
        
        // Headers
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (i > 0) sb.append(",");
            sb.append(model.getColumnName(i));
        }
        sb.append("\n");
        
        // Data
        for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                if (j > 0) sb.append(",");
                Object value = model.getValueAt(i, j);
                sb.append(value != null ? value.toString() : "");
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
                .map(line -> parseCsvLine(line))
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
            JOptionPane.showMessageDialog(this,
                "Error parsing CSV text: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
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

    private void setProcessing(boolean processing) {
        isProcessing = processing;
        progressBar.setVisible(processing);
        
        // Disable all interactive components
        searchField.setEnabled(!processing);
        replaceField.setEnabled(!processing);
        findNextButton.setEnabled(!processing);
        replaceButton.setEnabled(!processing);
        replaceAllButton.setEnabled(!processing);
        matchCaseCheckBox.setEnabled(!processing);
        exactMatchCheckBox.setEnabled(!processing);
        table.setEnabled(!processing);
        textArea.setEnabled(!processing);
    }
}
