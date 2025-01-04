package com.binarycarpenter.spreadsheet.tools.base;

import com.binarycarpenter.spreadsheet.utils.Icons;
import lombok.Getter;
import lombok.Setter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class SpreadsheetEditorPanel extends JPanel {
    @Getter
    protected JTable table;
    protected JTextArea textArea;
    protected CardLayout cardLayout;
    protected JPanel contentPanel;
    @Getter
    @Setter
    protected File currentFile;
    protected boolean isSpreadsheetMode = true;
    protected JTextField searchField;
    protected JTextField replaceField;
    protected JButton findNextButton;
    protected JButton replaceButton;
    protected JButton replaceAllButton;
    protected JCheckBox matchCaseCheckBox;
    protected JCheckBox exactMatchCheckBox;
    protected JCheckBox regexCheckBox;
    protected JLabel matchCountLabel;
    protected List<Point> matchingCells = new ArrayList<>();
    protected int currentMatchIndex = -1;
    protected JProgressBar progressBar;
    protected JPanel overlayPanel;
    protected JLayeredPane layeredPane;
    protected JPanel mainPanel;
    protected boolean isProcessing = false;
    protected TableRowSorter<TableModel> sorter;
    @Getter
    protected List<FilterState> currentFilters = new ArrayList<>();

    public SpreadsheetEditorPanel() {
        setupUI();
    }

    protected void setupUI() {
        setLayout(new BorderLayout());

        // Create the main content panel with card layout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Create spreadsheet view
        table = createTable();
        JScrollPane tableScrollPane = new JScrollPane(table);

        // Create text view
        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane textScrollPane = new JScrollPane(textArea);

        // Add views to card layout
        contentPanel.add(tableScrollPane, "spreadsheet");
        contentPanel.add(textScrollPane, "text");

        // Create toolbar
        JToolBar toolbar = createToolbar();

        // Create search panel
        JPanel searchPanel = createSearchPanel();

        // Create main panel to hold toolbar and content
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(toolbar, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(searchPanel, BorderLayout.SOUTH);

        // Create overlay panel for progress bar
        setupOverlayPanel();

        // Add components to layered pane
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(overlayPanel, JLayeredPane.POPUP_LAYER);

        add(layeredPane, BorderLayout.CENTER);
    }

    protected abstract JTable createTable();
    
    protected abstract void saveFile();
    
    protected abstract void loadFile(File file);
    
    protected abstract void exportFile(File file);

    protected JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Create buttons
        JButton openButton = new JButton(Icons.OPEN);
        openButton.setToolTipText("Open File");
        openButton.addActionListener(e -> openFile());

        JButton saveButton = new JButton(Icons.SAVE);
        saveButton.setToolTipText("Save");
        saveButton.addActionListener(e -> saveFile());

        JButton exportButton = new JButton(Icons.EXPORT);
        exportButton.setToolTipText("Export");
        exportButton.addActionListener(e -> exportFile());

        JButton filterButton = new JButton(Icons.FILTER);
        filterButton.setToolTipText("Filter Data");
        filterButton.addActionListener(e -> showFilterDialog());

        JToggleButton viewModeButton = new JToggleButton(Icons.EXPORT);
        viewModeButton.setToolTipText("Toggle View Mode");
        viewModeButton.addActionListener(e -> toggleViewMode());

        // Add buttons to toolbar
        toolbar.add(openButton);
        toolbar.add(saveButton);
        toolbar.add(exportButton);
        toolbar.add(filterButton);
        toolbar.addSeparator();
        toolbar.add(viewModeButton);

        return toolbar;
    }

    protected JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new MigLayout("fillx, insets 5", "[][][grow][][]", "[][]"));
        searchPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create components
        searchField = new JTextField(20);
        replaceField = new JTextField(20);
        findNextButton = new JButton("Find Next");
        replaceButton = new JButton("Replace");
        replaceAllButton = new JButton("Replace All");
        matchCaseCheckBox = new JCheckBox("Match Case");
        exactMatchCheckBox = new JCheckBox("Exact Match");
        regexCheckBox = new JCheckBox("Regex");
        matchCountLabel = new JLabel("No matches");
        matchCountLabel.setForeground(Color.GRAY);

        // Add action listeners
        findNextButton.addActionListener(e -> {
            updateSearch();
            highlightNextMatch();
        });

        replaceButton.addActionListener(e -> {
            if (currentMatchIndex >= 0 && currentMatchIndex < matchingCells.size()) {
                Point match = matchingCells.get(currentMatchIndex);
                int viewRow = match.x;
                if (sorter != null) {
                    viewRow = table.convertRowIndexToView(match.x);
                }
                table.setValueAt(replaceField.getText(), viewRow, match.y);
                updateSearch();
                highlightNextMatch();
            }
        });

        replaceAllButton.addActionListener(e -> {
            for (Point match : matchingCells) {
                int viewRow = match.x;
                if (sorter != null) {
                    viewRow = table.convertRowIndexToView(match.x);
                }
                table.setValueAt(replaceField.getText(), viewRow, match.y);
            }
            updateSearch();
        });

        // Add components to panel
        searchPanel.add(new JLabel("Find:"), "");
        searchPanel.add(searchField, "growx");
        searchPanel.add(findNextButton, "");
        searchPanel.add(matchCaseCheckBox, "");
        searchPanel.add(exactMatchCheckBox, "wrap");
        
        searchPanel.add(new JLabel("Replace:"), "");
        searchPanel.add(replaceField, "growx");
        searchPanel.add(replaceButton, "");
        searchPanel.add(replaceAllButton, "");
        searchPanel.add(regexCheckBox, "wrap");
        
        searchPanel.add(matchCountLabel, "span, gapleft 10");

        return searchPanel;
    }

    protected void setupOverlayPanel() {
        overlayPanel = new JPanel(new GridBagLayout());
        overlayPanel.setOpaque(false);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        
        overlayPanel.add(progressBar);
    }

    protected void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentFile != null ? currentFile.getParentFile() : null);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadFile(file);
        }
    }

    protected void exportFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(currentFile != null ? currentFile.getParentFile() : null);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            exportFile(file);
        }
    }

    protected void toggleViewMode() {
        isSpreadsheetMode = !isSpreadsheetMode;
        cardLayout.show(contentPanel, isSpreadsheetMode ? "spreadsheet" : "text");
    }

    protected void updateSearch() {
        // Reset search results
        matchingCells.clear();
        currentMatchIndex = -1;

        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            matchCountLabel.setText("No matches");
            return;
        }

        TableModel model = table.getModel();
        boolean matchCase = matchCaseCheckBox.isSelected();
        boolean exactMatch = exactMatchCheckBox.isSelected();
        boolean useRegex = regexCheckBox.isSelected();

        Pattern pattern = null;
        if (useRegex) {
            try {
                pattern = Pattern.compile(searchText, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                matchCountLabel.setText("Invalid regex pattern");
                return;
            }
        }

        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    boolean matches;

                    if (useRegex) {
                        matches = pattern.matcher(cellText).find();
                    } else {
                        if (!matchCase) {
                            cellText = cellText.toLowerCase();
                            searchText = searchText.toLowerCase();
                        }
                        matches = exactMatch ? cellText.equals(searchText) : cellText.contains(searchText);
                    }

                    if (matches) {
                        matchingCells.add(new Point(row, col));
                    }
                }
            }
        }

        int matchCount = matchingCells.size();
        if (matchCount > 0) {
            matchCountLabel.setText(String.format("Found %d match%s", matchCount, matchCount == 1 ? "" : "es"));
            highlightNextMatch();
        } else {
            matchCountLabel.setText("No matches found");
        }
    }

    protected void highlightNextMatch() {
        if (matchingCells.isEmpty()) {
            return;
        }

        currentMatchIndex = (currentMatchIndex + 1) % matchingCells.size();
        Point match = matchingCells.get(currentMatchIndex);

        // Convert model indices to view indices if using a sorter
        int viewRow = match.x;
        int viewCol = match.y;
        if (sorter != null) {
            viewRow = table.convertRowIndexToView(match.x);
        }

        // Scroll to the cell and select it
        table.scrollRectToVisible(table.getCellRect(viewRow, viewCol, true));
        table.setRowSelectionInterval(viewRow, viewRow);
        table.setColumnSelectionInterval(viewCol, viewCol);
    }

    protected void showFilterDialog() {
        // Get column names
        List<String> columnNames = new ArrayList<>();
        TableModel model = table.getModel();
        for (int i = 0; i < model.getColumnCount(); i++) {
            columnNames.add(model.getColumnName(i));
        }

        // Create dialog with proper parent window handling
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog;
        if (window instanceof Frame) {
            dialog = new JDialog((Frame) window, "Filter Data", true);
        } else if (window instanceof Dialog) {
            dialog = new JDialog((Dialog) window, "Filter Data", true);
        } else {
            dialog = new JDialog((Frame) null, "Filter Data", true);
        }
        dialog.setLayout(new BorderLayout());

        // Create filter panel
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        // Create "Add Filter" button
        JButton addFilterButton = new JButton("Add Filter");
        addFilterButton.addActionListener(e -> {
            FilterCondition condition = new FilterCondition(columnNames.toArray(new String[0]));
            filterPanel.add(condition);
            filterPanel.revalidate();
            filterPanel.repaint();
        });

        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear All");
        buttonsPanel.add(clearButton);
        buttonsPanel.add(applyButton);

        // Add action listeners
        clearButton.addActionListener(e -> {
            filterPanel.removeAll();
            filterPanel.revalidate();
            filterPanel.repaint();
            if (sorter != null) {
                sorter.setRowFilter(null);
            }
            currentFilters.clear();
            dialog.dispose();
        });

        applyButton.addActionListener(e -> {
            List<FilterCondition> conditions = new ArrayList<>();
            for (Component comp : filterPanel.getComponents()) {
                if (comp instanceof FilterCondition) {
                    conditions.add((FilterCondition) comp);
                }
            }
            applyFilters(conditions);
            dialog.dispose();
        });

        // Add components to dialog
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(addFilterButton);
        
        JScrollPane scrollPane = new JScrollPane(filterPanel);
        scrollPane.setPreferredSize(new Dimension(500, 300));

        dialog.add(topPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Show dialog
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    protected void applyFilters(List<FilterCondition> conditions) {
        currentFilters.clear();
        if (conditions.isEmpty()) {
            if (sorter != null) {
                sorter.setRowFilter(null);
            }
            return;
        }

        RowFilter<TableModel, Integer> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                for (FilterCondition condition : conditions) {
                    String columnName = condition.getColumnName();
                    String operator = condition.getOperator();
                    String value = condition.getValue();

                    // Find column index
                    int columnIndex = -1;
                    for (int i = 0; i < entry.getModel().getColumnCount(); i++) {
                        if (entry.getModel().getColumnName(i).equals(columnName)) {
                            columnIndex = i;
                            break;
                        }
                    }
                    if (columnIndex == -1) continue;

                    // Get cell value
                    Object cellValue = entry.getValue(columnIndex);
                    if (cellValue == null) return false;
                    String cellText = cellValue.toString();

                    // Apply filter based on operator
                    switch (operator) {
                        case "Equals":
                            if (!cellText.equals(value)) return false;
                            break;
                        case "Contains":
                            if (!cellText.contains(value)) return false;
                            break;
                        case "In List":
                            boolean found = false;
                            for (String item : value.split(",")) {
                                if (cellText.equals(item.trim())) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) return false;
                            break;
                        case "Greater Than":
                            try {
                                double cellNum = Double.parseDouble(cellText);
                                double valueNum = Double.parseDouble(value);
                                if (cellNum <= valueNum) return false;
                            } catch (NumberFormatException ex) {
                                return false;
                            }
                            break;
                        case "Less Than":
                            try {
                                double cellNum = Double.parseDouble(cellText);
                                double valueNum = Double.parseDouble(value);
                                if (cellNum >= valueNum) return false;
                            } catch (NumberFormatException ex) {
                                return false;
                            }
                            break;
                        case "Regex Match":
                            try {
                                if (!Pattern.compile(value).matcher(cellText).find()) return false;
                            } catch (Exception ex) {
                                return false;
                            }
                            break;
                    }
                }
                return true;
            }
        };

        if (sorter == null) {
            sorter = new TableRowSorter<>(table.getModel());
            table.setRowSorter(sorter);
        }
        sorter.setRowFilter(filter);
    }

    protected static class FilterCondition extends JPanel {
        private final JComboBox<String> columnCombo;
        private final JComboBox<String> operatorCombo;
        private final JTextField valueField;

        public FilterCondition(String[] columns) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

            columnCombo = new JComboBox<>(columns);
            operatorCombo = new JComboBox<>(new String[]{
                    "Equals", "Contains", "In List",
                    "Greater Than", "Less Than", "Regex Match"
            });
            valueField = new JTextField(20);
            JButton removeButton = new JButton("×");

            // Make all components the same height
            Dimension buttonSize = new Dimension(28, 28);
            Dimension comboSize = new Dimension(120, 28);

            columnCombo.setPreferredSize(comboSize);
            operatorCombo.setPreferredSize(comboSize);
            valueField.setPreferredSize(new Dimension(200, 28));
            removeButton.setPreferredSize(buttonSize);

            // Style the remove button
            removeButton.setFont(new Font("Arial", Font.PLAIN, 18));
            removeButton.setForeground(Color.RED);
            removeButton.setFocusPainted(false);
            removeButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            // Add components
            add(columnCombo);
            add(operatorCombo);
            add(valueField);
            add(removeButton);

            removeButton.addActionListener(e -> {
                Container parent = getParent();
                if (parent != null) {
                    parent.remove(this);
                    parent.revalidate();
                    parent.repaint();
                }
            });
        }

        public String getColumnName() {
            return (String) columnCombo.getSelectedItem();
        }

        public String getOperator() {
            return (String) operatorCombo.getSelectedItem();
        }

        public String getValue() {
            return valueField.getText();
        }
    }

    protected static class FilterState {
        protected String column;
        protected String filterText;
        protected boolean exactMatch;
        protected boolean caseSensitive;

        public FilterState(String column, String filterText, boolean exactMatch, boolean caseSensitive) {
            this.column = column;
            this.filterText = filterText;
            this.exactMatch = exactMatch;
            this.caseSensitive = caseSensitive;
        }

        public String getColumn() {
            return column;
        }

        public String getFilterText() {
            return filterText;
        }

        public boolean isExactMatch() {
            return exactMatch;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }
    }
}
