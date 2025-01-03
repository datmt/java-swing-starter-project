package com.binarycarpenter.spreadsheet.tools;

import com.binarycarpenter.spreadsheet.tools.base.SpreadsheetEditorPanel;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

@Slf4j
public class ExcelEditorPanel extends SpreadsheetEditorPanel {
    private Workbook currentWorkbook;
    private List<Sheet> sheets;
    private int currentSheetIndex = 0;
    private JComboBox<String> sheetSelector;

    public ExcelEditorPanel() {
        super();
        addSheetSelector();
    }

    private void addSheetSelector() {
        sheetSelector = new JComboBox<>();
        sheetSelector.addActionListener(e -> {
            if (currentWorkbook != null) {
                currentSheetIndex = sheetSelector.getSelectedIndex();
                loadSheet(currentSheetIndex);
            }
        });
        
        // Add sheet selector to the toolbar
        JToolBar toolbar = (JToolBar) mainPanel.getComponent(0);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Sheet: "));
        toolbar.add(sheetSelector);
    }

    @Override
    protected JTable createTable() {
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        
        // Set up sorting
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        
        // Add right-click menu for column filtering
        JPopupMenu headerPopup = new JPopupMenu();
        JMenuItem filterMenuItem = new JMenuItem("Filter");
        filterMenuItem.addActionListener(e -> showFilterDialog(table.getSelectedColumn()));
        headerPopup.add(filterMenuItem);

        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handleHeaderPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handleHeaderPopup(e);
                }
            }

            private void handleHeaderPopup(MouseEvent e) {
                int column = table.getColumnModel().getColumnIndexAtX(e.getX());
                if (column != -1) {
                    headerPopup.show(e.getComponent(), e.getX(), e.getY());
                    table.setColumnSelectionInterval(column, column);
                }
            }
        });

        return table;
    }

    @Override
    protected void loadFile(File file) {
        try {
            // Close previous workbook if any
            if (currentWorkbook != null) {
                currentWorkbook.close();
            }

            // Load the Excel file
            FileInputStream fis = new FileInputStream(file);
            currentWorkbook = file.getName().toLowerCase().endsWith(".xlsx") ?
                    new XSSFWorkbook(fis) :
                    new HSSFWorkbook(fis);
            fis.close();

            // Update sheet selector
            sheetSelector.removeAllItems();
            for (int i = 0; i < currentWorkbook.getNumberOfSheets(); i++) {
                sheetSelector.addItem(currentWorkbook.getSheetName(i));
            }

            // Load the first sheet by default
            currentSheetIndex = 0;
            sheetSelector.setSelectedIndex(0);
            loadSheet(currentSheetIndex);

            currentFile = file;
        } catch (IOException e) {
            log.error("Error loading Excel file", e);
            JOptionPane.showMessageDialog(this,
                    "Error loading file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSheet(int sheetIndex) {
        Sheet sheet = currentWorkbook.getSheetAt(sheetIndex);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        model.setColumnCount(0);

        // Get the maximum column count
        int maxColumns = 0;
        for (Row row : sheet) {
            maxColumns = Math.max(maxColumns, row.getLastCellNum());
        }

        // Set up column headers (A, B, C, etc.)
        Vector<String> columnNames = new Vector<>();
        for (int i = 0; i < maxColumns; i++) {
            columnNames.add(convertToColumnName(i));
        }
        model.setColumnIdentifiers(columnNames);

        // Load data
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Vector<Object> rowData = new Vector<>();
            
            for (int i = 0; i < maxColumns; i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                rowData.add(getCellValue(cell));
            }
            
            model.addRow(rowData);
        }

        // Update text area
        updateTextArea();
    }

    private String convertToColumnName(int columnNumber) {
        StringBuilder columnName = new StringBuilder();
        while (columnNumber >= 0) {
            int remainder = columnNumber % 26;
            columnName.insert(0, (char) (65 + remainder));
            columnNumber = (columnNumber / 26) - 1;
        }
        return columnName.toString();
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (IllegalStateException e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (IllegalStateException e2) {
                        return cell.getCellFormula();
                    }
                }
            default:
                return "";
        }
    }

    private void updateTextArea() {
        StringBuilder sb = new StringBuilder();
        TableModel model = table.getModel();
        
        // Add headers
        for (int col = 0; col < model.getColumnCount(); col++) {
            if (col > 0) sb.append("\t");
            sb.append(model.getColumnName(col));
        }
        sb.append("\n");
        
        // Add data
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                if (col > 0) sb.append("\t");
                Object value = model.getValueAt(row, col);
                sb.append(value != null ? value.toString() : "");
            }
            sb.append("\n");
        }
        
        textArea.setText(sb.toString());
    }

    @Override
    protected void saveFile() {
        if (currentWorkbook == null || currentFile == null) {
            return;
        }

        try {
            // Update the current sheet with table data
            Sheet sheet = currentWorkbook.getSheetAt(currentSheetIndex);
            TableModel model = table.getModel();

            for (int i = 0; i < model.getRowCount(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    row = sheet.createRow(i);
                }

                for (int j = 0; j < model.getColumnCount(); j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    Object value = model.getValueAt(i, j);
                    setCellValue(cell, value);
                }
            }

            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(currentFile)) {
                currentWorkbook.write(fos);
            }
        } catch (IOException e) {
            log.error("Error saving Excel file", e);
            JOptionPane.showMessageDialog(this,
                    "Error saving file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    @Override
    protected void exportFile(File file) {
        try {
            Workbook newWorkbook = new XSSFWorkbook();
            Sheet newSheet = newWorkbook.createSheet("Sheet1");
            TableModel model = table.getModel();

            // Create header row
            Row headerRow = newSheet.createRow(0);
            for (int i = 0; i < model.getColumnCount(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(model.getColumnName(i));
            }

            // Create data rows
            for (int i = 0; i < model.getRowCount(); i++) {
                Row row = newSheet.createRow(i + 1);
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = model.getValueAt(i, j);
                    setCellValue(cell, value);
                }
            }

            // Save the new workbook
            try (FileOutputStream fos = new FileOutputStream(file)) {
                newWorkbook.write(fos);
            }
            newWorkbook.close();
        } catch (IOException e) {
            log.error("Error exporting Excel file", e);
            JOptionPane.showMessageDialog(this,
                    "Error exporting file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showFilterDialog(int column) {
        if (column < 0 || column >= table.getColumnCount()) {
            return;
        }

        String columnName = table.getColumnName(column);
        
        // Create filter dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Filter " + columnName, true);
        dialog.setLayout(new MigLayout("fillx, wrap 2", "[][grow]", "[]10[]10[]"));
        
        JTextField filterField = new JTextField(20);
        JCheckBox exactMatchBox = new JCheckBox("Exact Match");
        JCheckBox caseSensitiveBox = new JCheckBox("Case Sensitive");
        
        dialog.add(new JLabel("Filter:"));
        dialog.add(filterField, "growx");
        dialog.add(exactMatchBox, "span 2");
        dialog.add(caseSensitiveBox, "span 2");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton clearButton = new JButton("Clear");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> {
            applyFilter(column, filterField.getText(), exactMatchBox.isSelected(), caseSensitiveBox.isSelected());
            dialog.dispose();
        });
        
        clearButton.addActionListener(e -> {
            clearFilter(column);
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(buttonPanel, "span 2, growx");
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void applyFilter(int column, String filterText, boolean exactMatch, boolean caseSensitive) {
        final String finalFilterText = caseSensitive ? filterText : filterText.toLowerCase();
        RowFilter<TableModel, Object> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                Object value = entry.getValue(column);
                if (value == null) {
                    return false;
                }
                
                String cellText = value.toString();
                if (!caseSensitive) {
                    cellText = cellText.toLowerCase();
                }
                
                if (exactMatch) {
                    return cellText.equals(finalFilterText);
                } else {
                    return cellText.contains(finalFilterText);
                }
            }
        };
        
        sorter.setRowFilter(filter);
        
        // Store filter state
        currentFilters.add(new FilterState(
            table.getColumnName(column),
            filterText,
            exactMatch,
            caseSensitive
        ));
    }

    private void clearFilter(int column) {
        sorter.setRowFilter(null);
        currentFilters.removeIf(f -> f.getColumn().equals(table.getColumnName(column)));
    }
}
