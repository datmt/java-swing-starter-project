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
import javax.swing.table.TableModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.RowFilter.Entry;
import javax.swing.filechooser.FileNameExtensionFilter;
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
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

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
    private List<FilterState> currentFilters = new ArrayList<>();  // Store current filters

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
                            // Calculate optimal width for tooltip (max 300px, min 100px)
                            int tooltipWidth = Math.min(300, Math.max(100, textWidth / 2));

                            // Format tooltip text with dynamic width and proper wrapping
                            String tooltipText = "<html><div style='width: " + tooltipWidth + "px; padding: 5px;'>" + 
                                               text.replace("<", "&lt;")
                                                   .replace(">", "&gt;")
                                                   .replace("\n", "<br>")
                                                   .replace(" ", "&nbsp;") + 
                                               "</div></html>";
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

        setupTable(model);

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

        JButton exportButton = new JButton();
        exportButton.setIcon(Icons.EXPORT);
        exportButton.setPreferredSize(new Dimension(28, 28));
        exportButton.setToolTipText("Export Filtered Results");
        exportButton.addActionListener(e -> exportFilteredResults());

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
        topPanel.add(exportButton, "cell 3 0");
        topPanel.add(new JLabel(), "cell 4 0, growx"); // Spacer
        topPanel.add(viewModeButton, "cell 5 0");

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

        topPanel.add(searchPanel, "cell 0 1 6 1, growx");

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

    private void setupTable(DefaultTableModel model) {
        table.setModel(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        
        // Set up column model with proper resize behavior
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(150); // Default width
            
            // Add column resize listener
            column.addPropertyChangeListener(e -> {
                if ("width".equals(e.getPropertyName())) {
                    table.repaint();
                }
            });
        }

        // Add mouse motion listener for resize cursor
        table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int column = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (column >= 0) {
                    Rectangle r = table.getTableHeader().getHeaderRect(column);
                    r.grow(-3, 0);
                    if (r.contains(e.getPoint())) {
                        table.getTableHeader().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    } else {
                        table.getTableHeader().setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    }
                }
            }
        });

        // Enable column reordering
        table.getTableHeader().setReorderingAllowed(true);
        
        // Set row height
        table.setRowHeight(24);

        // Add double-click listener for sorting
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
        }

        // Convert search text to lowercase if case insensitive
        if (!caseSensitive) {
            searchText = searchText.toLowerCase();
        }

        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    String compareText = caseSensitive ? cellText : cellText.toLowerCase();
                    boolean matches = false;

                    if (useRegex) {
                        matches = pattern.matcher(cellText).find();
                    } else if (exactMatch) {
                        matches = compareText.equals(searchText);
                    } else {
                        matches = compareText.contains(searchText);
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

                    // Set up sorter
                    sorter = new TableRowSorter<>(model);
                    table.setRowSorter(sorter);
                    sorter.setSortsOnUpdates(false);

                    // Add numeric comparator for the index column
                    if (model.getColumnCount() > 0 && "Index".equals(model.getColumnName(0))) {
                        sorter.setComparator(0, new Comparator<String>() {
                            @Override
                            public int compare(String s1, String s2) {
                                try {
                                    long n1 = Long.parseLong(s1);
                                    long n2 = Long.parseLong(s2);
                                    return Long.compare(n1, n2);
                                } catch (NumberFormatException e) {
                                    return s1.compareTo(s2); // fallback to string comparison
                                }
                            }
                        });
                    }

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

                // Auto-size columns based on content
                for (int column = 0; column < table.getColumnCount(); column++) {
                    int width = 50; // minimum width
                    TableColumn tableColumn = table.getColumnModel().getColumn(column);

                    // Check header width
                    TableCellRenderer headerRenderer = tableColumn.getHeaderRenderer();
                    if (headerRenderer == null) {
                        headerRenderer = table.getTableHeader().getDefaultRenderer();
                    }
                    Component headerComp = headerRenderer.getTableCellRendererComponent(
                        table, tableColumn.getHeaderValue(), false, false, 0, column);
                    width = Math.max(width, headerComp.getPreferredSize().width + 10);

                    // Check data width (sample first 100 rows)
                    int rowsToCheck = Math.min(100, table.getRowCount());
                    for (int row = 0; row < rowsToCheck; row++) {
                        TableCellRenderer renderer = table.getCellRenderer(row, column);
                        Component comp = table.prepareRenderer(renderer, row, column);
                        width = Math.max(width, comp.getPreferredSize().width + 10);
                    }

                    // Cap width at 300 pixels
                    width = Math.min(width, 300);
                    tableColumn.setPreferredWidth(width);
                }

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
        List<? extends SortKey> sortKeys = sorter.getSortKeys();
        SortOrder currentOrder = SortOrder.ASCENDING;
        
        if (!sortKeys.isEmpty()) {
            SortKey currentKey = sortKeys.get(0);
            if (currentKey.getColumn() == column) {
                currentOrder = currentKey.getSortOrder() == SortOrder.ASCENDING ? 
                    SortOrder.DESCENDING : SortOrder.ASCENDING;
            }
        }
        
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(column, currentOrder)));
    }

    private void showFilterDialog() {
        // Find the frame owner
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);

        JDialog dialog = new JDialog(owner, "Filter Data", true);
        dialog.setLayout(new BorderLayout(0, 5));  // 5px vertical gap

        // Create main panel with filter conditions
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Scrollable container for filter conditions
        JPanel conditionsPanel = new JPanel();
        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));
        conditionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Wrap conditionsPanel in another panel to prevent vertical centering
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(conditionsPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // List to keep track of filter conditions
        List<FilterCondition> filterConditions = new ArrayList<>();

        // Restore previous filters if any
        for (FilterState state : currentFilters) {
            FilterCondition condition = new FilterCondition(getColumnNames());
            condition.setValues(state.column, state.operator, state.value);
            filterConditions.add(condition);
            conditionsPanel.add(condition);
            condition.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        // Add condition button panel
        JPanel addButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton addConditionButton = new JButton("Add Condition");
        addConditionButton.addActionListener(e -> {
            FilterCondition condition = new FilterCondition(getColumnNames());
            filterConditions.add(condition);
            conditionsPanel.add(condition);
            condition.setAlignmentX(Component.LEFT_ALIGNMENT);
            conditionsPanel.revalidate();
            conditionsPanel.repaint();

            // Scroll to bottom to show new condition
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
        addButtonPanel.add(addConditionButton);

        // Control buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");
        JButton cancelButton = new JButton("Cancel");

        applyButton.addActionListener(e -> {
            // Save current filters
            currentFilters.clear();
            for (FilterCondition condition : filterConditions) {
                currentFilters.add(new FilterState(
                    (String) condition.columnCombo.getSelectedItem(),
                    (String) condition.operatorCombo.getSelectedItem(),
                    condition.valueField.getText()
                ));
            }
            applyFilters(filterConditions);
            dialog.dispose();
        });

        clearButton.addActionListener(e -> {
            filterConditions.clear();
            currentFilters.clear();
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
        dialog.add(addButtonPanel, BorderLayout.NORTH);
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
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));  // 5px horizontal gap, 0px vertical gap
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));  // Small vertical padding

            columnCombo = new JComboBox<>(columns);
            operatorCombo = new JComboBox<>(new String[]{
                "Equals", "Contains", "In List", 
                "Greater Than", "Less Than", "Regex Match"
            });
            valueField = new JTextField(20);
            removeButton = new JButton("×");

            // Make all components the same height
            Dimension buttonSize = new Dimension(28, 28);
            Dimension comboSize = new Dimension(120, 28);

            columnCombo.setPreferredSize(comboSize);
            operatorCombo.setPreferredSize(comboSize);
            valueField.setPreferredSize(new Dimension(200, 28));
            removeButton.setPreferredSize(buttonSize);

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

        public void setValues(String column, String operator, String value) {
            columnCombo.setSelectedItem(column);
            operatorCombo.setSelectedItem(operator);
            valueField.setText(value);
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

                case "Greater Than":
                    try {
                        double cellNum = Double.parseDouble(cellValue.replaceAll("[^\\d.-]", ""));
                        double filterNum = Double.parseDouble(filterValue.replaceAll("[^\\d.-]", ""));
                        return cellNum > filterNum;
                    } catch (NumberFormatException e) {
                        // If not numeric, do string comparison
                        return cellValue.compareToIgnoreCase(filterValue) > 0;
                    }

                case "Less Than":
                    try {
                        double cellNum = Double.parseDouble(cellValue.replaceAll("[^\\d.-]", ""));
                        double filterNum = Double.parseDouble(filterValue.replaceAll("[^\\d.-]", ""));
                        return cellNum < filterNum;
                    } catch (NumberFormatException e) {
                        // If not numeric, do string comparison
                        return cellValue.compareToIgnoreCase(filterValue) < 0;
                    }

                case "Regex Match":
                    try {
                        return cellValue.matches(filterValue);
                    } catch (java.util.regex.PatternSyntaxException e) {
                        // If invalid regex, treat as no match
                        return false;
                    }
                    
                default:
                    return true;
            }
        }
    }

    private static class FilterState {
        String column;
        String operator;
        String value;

        FilterState(String column, String operator, String value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
        }
    }

    private void exportFilteredResults() {
        if (table == null || table.getModel().getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "No data to export", 
                "Export Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this,
                    "File already exists. Do you want to overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                TableRowSorter<? extends TableModel> sorter = 
                    (TableRowSorter<? extends TableModel>) table.getRowSorter();
                
                // Write headers
                String[] headers = new String[model.getColumnCount()];
                for (int i = 0; i < model.getColumnCount(); i++) {
                    headers[i] = model.getColumnName(i);
                }
                writer.writeNext(headers);
                
                // Write filtered data
                for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
                    String[] rowData = new String[model.getColumnCount()];
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        Object value = model.getValueAt(modelRow, col);
                        rowData[col] = value != null ? value.toString() : "";
                    }
                    writer.writeNext(rowData);
                }
                
                JOptionPane.showMessageDialog(this,
                    "Export completed successfully",
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting data: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
