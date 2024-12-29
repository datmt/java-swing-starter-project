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
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowFilter.Entry;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Comparator;
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
    private JCheckBox regexCheckBox;
    private List<Point> matchingCells = new ArrayList<>();
    private int currentMatchIndex = -1;
    private JProgressBar progressBar;
    private JPanel overlayPanel;
    private JLayeredPane layeredPane;
    private JPanel mainPanel;
    private boolean isProcessing = false;
    private TableRowSorter<DefaultTableModel> sorter;

    public CsvEditorPanel() {
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // Initialize card layout and content panel first
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Initialize table with custom renderer for highlighting
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                return String.class; // Treat all columns as strings for display
            }
        };

        // Create custom cell renderer
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value,
                    boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component comp = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    if (matchingCells.contains(new Point(row, column))) {
                        comp.setBackground(new Color(255, 255, 0, 100)); // Light yellow highlight
                        comp.setForeground(table.getForeground());
                    } else {
                        comp.setBackground(table.getBackground());
                        comp.setForeground(table.getForeground());
                    }
                }

                // Set tooltip if cell content is truncated
                if (comp instanceof JComponent) {
                    JComponent jc = (JComponent)comp;
                    if (value != null) {
                        String text = value.toString();
                        FontMetrics fm = comp.getFontMetrics(comp.getFont());
                        int textWidth = fm.stringWidth(text);
                        int cellWidth = table.getColumnModel().getColumn(column).getWidth();

                        // Check if text is truncated
                        if (textWidth > cellWidth - 4) { // 4 pixels for padding
                            // Format tooltip text with line wrapping
                            String tooltipText = "<html><body style='width: 300px'>" +
                                               text.replace("<", "&lt;")
                                                   .replace(">", "&gt;")
                                                   .replace("\n", "<br>") +
                                               "</body></html>";
                            jc.setToolTipText(tooltipText);
                        } else {
                            jc.setToolTipText(null);
                        }
                    }
                }

                return comp;
            }
        };

        // Configure tooltip display
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE); // Keep tooltip visible
        ToolTipManager.sharedInstance().setInitialDelay(500); // Show after 500ms

        table = new JTable(model);
        table.setDefaultRenderer(Object.class, customRenderer);
        table.setDefaultRenderer(String.class, customRenderer);

        // Initialize the sorter with custom comparator
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Disable default sorting behavior
        table.getTableHeader().setReorderingAllowed(false);
        sorter.setSortsOnUpdates(false);

        // Remove all existing mouse listeners from header
        for (MouseListener listener : table.getTableHeader().getMouseListeners()) {
            table.getTableHeader().removeMouseListener(listener);
        }

        // Add our custom double-click listener
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            private long lastClick = 0;
            private int lastColumn = -1;

            @Override
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column == -1) return;

                long clickTime = System.currentTimeMillis();

                if (column == lastColumn && clickTime - lastClick < 500) {  // Double click within 500ms
                    e.consume();  // Prevent event propagation
                    sortColumn(column);
                }

                lastColumn = column;
                lastClick = clickTime;
            }
        });

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

        JButton filterButton = new JButton();
        filterButton.setIcon(Icons.FILTER);
        filterButton.setPreferredSize(new Dimension(28, 28));
        filterButton.setToolTipText("Filter Data");
        filterButton.addActionListener(e -> showFilterDialog());

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

        regexCheckBox = new JCheckBox(".*");
        regexCheckBox.setToolTipText("Use Regular Expression");
        regexCheckBox.addActionListener(e -> updateSearch());

        // First row: file controls and view mode
        topPanel.add(openButton, "cell 0 0");
        topPanel.add(saveButton, "cell 1 0");
        topPanel.add(filterButton, "cell 2 0");
        topPanel.add(new JLabel(), "cell 3 0, growx"); // Spacer
        topPanel.add(viewModeButton, "cell 4 0");

        // Second row: search and replace
        searchPanel.add(new JLabel("Find:"), "");
        searchPanel.add(searchField, "growx");
        searchPanel.add(findNextButton, "");
        searchPanel.add(matchCaseCheckBox, "");
        searchPanel.add(exactMatchCheckBox, "");
        searchPanel.add(regexCheckBox, "wrap");

        searchPanel.add(new JLabel("Replace:"), "");
        searchPanel.add(replaceField, "growx");
        searchPanel.add(replaceButton, "split 2");
        searchPanel.add(replaceAllButton, "");

        topPanel.add(searchPanel, "cell 0 1 5 1, growx");

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
        boolean useRegex = regexCheckBox.isSelected();

        Pattern pattern = null;
        if (useRegex) {
            try {
                pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                showError("Invalid regular expression: " + e.getMessage());
                return;
            }
        } else if (!caseSensitive) {
            searchText = searchText.toLowerCase();
        }

        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    boolean matches = false;

                    if (useRegex) {
                        matches = pattern.matcher(cellText).find();
                    } else {
                        String compareText = caseSensitive ? cellText : cellText.toLowerCase();
                        if (exactMatch) {
                            matches = compareText.equals(searchText);
                        } else {
                            matches = compareText.contains(searchText);
                        }
                    }

                    if (matches) {
                        matchingCells.add(new Point(row, col));
                    }
                }
            }
        }

        table.repaint();
        if (!matchingCells.isEmpty()) {
            findNext();
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
            boolean useRegex = regexCheckBox.isSelected();

            Object value = table.getValueAt(match.x, match.y);
            if (value != null) {
                String cellText = value.toString();
                String searchText = searchField.getText();
                String newText;

                if (useRegex) {
                    try {
                        Pattern pattern = Pattern.compile(searchText,
                                matchCaseCheckBox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE);
                        newText = pattern.matcher(cellText).replaceAll(replaceText);
                    } catch (Exception e) {
                        showError("Invalid regular expression or replacement: " + e.getMessage());
                        return;
                    }
                } else if (exactMatchCheckBox.isSelected()) {
                    newText = replaceText;
                } else {
                    if (!matchCaseCheckBox.isSelected()) {
                        String cellTextLower = cellText.toLowerCase();
                        String searchTextLower = searchText.toLowerCase();
                        int start = cellTextLower.indexOf(searchTextLower);
                        if (start >= 0) {
                            newText = cellText.substring(0, start) +
                                    replaceText +
                                    cellText.substring(start + searchText.length());
                        } else {
                            return;
                        }
                    } else {
                        newText = cellText.replace(searchText, replaceText);
                    }
                }

                table.setValueAt(newText, match.x, match.y);
                updateSearch();
            }
        }
    }

    private void replaceAll() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String searchText = searchField.getText();
        String replaceText = replaceField.getText();
        boolean caseSensitive = matchCaseCheckBox.isSelected();
        boolean exactMatch = exactMatchCheckBox.isSelected();
        boolean useRegex = regexCheckBox.isSelected();

        if (searchText.isEmpty()) {
            return;
        }

        Pattern pattern = null;
        if (useRegex) {
            try {
                pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                showError("Invalid regular expression: " + e.getMessage());
                return;
            }
        }

        final Pattern finalPattern = pattern;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private int totalReplacements = 0;

            @Override
            protected Void doInBackground() {
                try {
                    for (int row = 0; row < model.getRowCount(); row++) {
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            Object value = model.getValueAt(row, col);
                            if (value != null) {
                                String cellText = value.toString();
                                String newText = null;

                                if (useRegex) {
                                    if (finalPattern.matcher(cellText).find()) {
                                        newText = finalPattern.matcher(cellText).replaceAll(replaceText);
                                        totalReplacements++;
                                    }
                                } else {
                                    String compareCellText = caseSensitive ? cellText : cellText.toLowerCase();
                                    String compareSearchText = caseSensitive ? searchText : searchText.toLowerCase();

                                    if (exactMatch) {
                                        if (compareCellText.equals(compareSearchText)) {
                                            newText = replaceText;
                                            totalReplacements++;
                                        }
                                    } else if (compareCellText.contains(compareSearchText)) {
                                        newText = caseSensitive ?
                                                cellText.replace(searchText, replaceText) :
                                                cellText.replaceAll("(?i)" + Pattern.quote(searchText), replaceText);
                                        totalReplacements++;
                                    }
                                }

                                if (newText != null) {
                                    model.setValueAt(newText, row, col);
                                }

                            }

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
                setProcessing(false);
                clearSearchState();
                updateSearch();

                JOptionPane.showMessageDialog(CsvEditorPanel.this,
                        String.format("Replaced %d occurrence%s",
                                totalReplacements,
                                totalReplacements == 1 ? "" : "s"),
                        "Replace Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

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

                    // Reset sorter with custom comparator
                    sorter = new TableRowSorter<>(model);
                    table.setRowSorter(sorter);
                    sorter.setSortsOnUpdates(false);

                    // Initially disable sorting for all columns
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        sorter.setSortable(i, false);
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

        searchField.setEnabled(!processing);
        replaceField.setEnabled(!processing);
        findNextButton.setEnabled(!processing);
        replaceButton.setEnabled(!processing);
        replaceAllButton.setEnabled(!processing);
        matchCaseCheckBox.setEnabled(!processing);
        exactMatchCheckBox.setEnabled(!processing);
        regexCheckBox.setEnabled(!processing);
        table.setEnabled(!processing);
        textArea.setEnabled(!processing);
    }

    private void updateSearchHighlights() {
        // Convert view indices to model indices for all matching cells
        List<Point> newMatchingCells = new ArrayList<>();
        for (Point p : matchingCells) {
            int viewRow = table.convertRowIndexToView(p.x);
            if (viewRow != -1) {
                newMatchingCells.add(new Point(viewRow, p.y));
            }
        }
        matchingCells = newMatchingCells;
        table.repaint();
    }

    private void sortColumn(int column) {
        setProcessing(true);
        progressBar.setValue(0);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                int rowCount = model.getRowCount();

                // Get current sort order
                List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
                SortOrder currentOrder = SortOrder.UNSORTED;
                if (!sortKeys.isEmpty() && sortKeys.get(0).getColumn() == column) {
                    currentOrder = sortKeys.get(0).getSortOrder();
                }

                // Determine next sort order
                SortOrder nextOrder;
                switch (currentOrder) {
                    case ASCENDING:
                        nextOrder = SortOrder.DESCENDING;
                        break;
                    case DESCENDING:
                        nextOrder = SortOrder.UNSORTED;
                        break;
                    default:
                        nextOrder = SortOrder.ASCENDING;
                }

                // Enable sorting just for this column
                for (int i = 0; i < model.getColumnCount(); i++) {
                    sorter.setSortable(i, i == column);
                }

                // Prepare data for sorting in chunks
                int chunkSize = 1000;
                int totalChunks = (rowCount + chunkSize - 1) / chunkSize;

                for (int chunk = 0; chunk < totalChunks; chunk++) {
                    if (isCancelled()) {
                        break;
                    }

                    int progress = (chunk * 100) / totalChunks;
                    publish(progress);

                    // Process a chunk of rows
                    int start = chunk * chunkSize;
                    int end = Math.min(start + chunkSize, rowCount);

                    // Allow UI to update
                    Thread.sleep(1);
                }

                // Set new sort order on EDT
                SwingUtilities.invokeLater(() -> {
                    if (nextOrder == SortOrder.UNSORTED) {
                        sorter.setSortKeys(null);
                    } else {
                        sorter.setSortKeys(List.of(new RowSorter.SortKey(column, nextOrder)));
                    }
                });

                publish(100);
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    progressBar.setValue(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    get(); // Check for exceptions
                    progressBar.setValue(100);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setProcessing(false);
                    table.repaint();
                }
            }
        };

        worker.execute();
    }

    private void showFilterDialog() {
        // Find the frame owner
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);

        JDialog dialog = new JDialog(owner, "Filter Data", true);
        dialog.setLayout(new BorderLayout());

        // Create main panel with filter conditions
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Scrollable container for filter conditions
        JPanel conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(conditionsPanel);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        // List to keep track of filter conditions
        List<FilterCondition> filterConditions = new ArrayList<>();

        // Add condition button
        JButton addConditionButton = new JButton("Add Condition");
        addConditionButton.addActionListener(e -> {
            FilterCondition condition = new FilterCondition(getColumnNames());
            filterConditions.add(condition);
            conditionsPanel.add(condition);
            conditionsPanel.revalidate();
            conditionsPanel.repaint();
        });

        // Control buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");
        JButton cancelButton = new JButton("Cancel");

        applyButton.addActionListener(e -> {
            applyFilters(filterConditions);
            dialog.dispose();
        });

        clearButton.addActionListener(e -> {
            filterConditions.clear();
            conditionsPanel.removeAll();
            conditionsPanel.revalidate();
            conditionsPanel.repaint();
            sorter.setRowFilter(null);
            table.repaint();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(applyButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(cancelButton);

        // Add components to dialog
        dialog.add(addConditionButton, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String[] getColumnNames() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        String[] columns = new String[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            columns[i] = model.getColumnName(i);
        }
        return columns;
    }

    private void applyFilters(List<FilterCondition> conditions) {
        if (conditions.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        RowFilter<DefaultTableModel, Integer> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                for (FilterCondition condition : conditions) {
                    if (!condition.evaluate(entry)) {
                        return false;
                    }
                }
                return true;
            }
        };

        sorter.setRowFilter(filter);
    }

    // Inner class for filter condition UI and logic
    private class FilterCondition extends JPanel {
        private JComboBox<String> columnCombo;
        private JComboBox<String> operatorCombo;
        private JTextField valueField;
        private JButton removeButton;

        public FilterCondition(String[] columns) {
            setLayout(new FlowLayout(FlowLayout.LEFT));

            columnCombo = new JComboBox<>(columns);
            operatorCombo = new JComboBox<>(new String[]{"Equals", "Contains", "In List"});
            valueField = new JTextField(20);
            removeButton = new JButton("×");

            removeButton.addActionListener(e -> {
                Container parent = getParent();
                parent.remove(this);
                parent.revalidate();
                parent.repaint();
            });

            add(columnCombo);
            add(operatorCombo);
            add(valueField);
            add(removeButton);
        }

        public boolean evaluate(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
            int columnIndex = columnCombo.getSelectedIndex();
            String cellValue = String.valueOf(entry.getValue(columnIndex));
            String filterValue = valueField.getText().trim();
            String operator = (String) operatorCombo.getSelectedItem();

            if (filterValue.isEmpty()) {
                return true;
            }

            switch (operator) {
                case "Equals":
                    return cellValue.equalsIgnoreCase(filterValue);

                case "Contains":
                    return cellValue.toLowerCase().contains(filterValue.toLowerCase());

                case "In List":
                    String[] items = filterValue.split(",");
                    for (String item : items) {
                        if (cellValue.equalsIgnoreCase(item.trim())) {
                            return true;
                        }
                    }
                    return false;

                default:
                    return true;
            }
        }
    }
}
