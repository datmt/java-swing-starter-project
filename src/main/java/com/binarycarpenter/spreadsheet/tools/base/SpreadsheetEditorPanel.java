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

        JToggleButton viewModeButton = new JToggleButton(Icons.EXPORT);
        viewModeButton.setToolTipText("Toggle View Mode");
        viewModeButton.addActionListener(e -> toggleViewMode());

        // Add buttons to toolbar
        toolbar.add(openButton);
        toolbar.add(saveButton);
        toolbar.add(exportButton);
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
            return;
        }

        boolean matchCase = matchCaseCheckBox.isSelected();
        boolean exactMatch = exactMatchCheckBox.isSelected();
        boolean useRegex = regexCheckBox.isSelected();

        Pattern pattern = null;
        if (useRegex) {
            try {
                pattern = Pattern.compile(searchText, matchCase ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                return; // Invalid regex
            }
        }

        TableModel model = table.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                Object value = model.getValueAt(row, col);
                if (value != null) {
                    String cellText = value.toString();
                    boolean matches = false;

                    if (useRegex) {
                        matches = pattern.matcher(cellText).find();
                    } else if (exactMatch) {
                        matches = matchCase ? 
                            cellText.equals(searchText) : 
                            cellText.equalsIgnoreCase(searchText);
                    } else {
                        matches = matchCase ? 
                            cellText.contains(searchText) : 
                            cellText.toLowerCase().contains(searchText.toLowerCase());
                    }

                    if (matches) {
                        matchingCells.add(new Point(row, col));
                    }
                }
            }
        }

        if (!matchingCells.isEmpty()) {
            highlightNextMatch();
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
