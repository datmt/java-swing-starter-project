package com.toolbox.tools;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class JsonToCsvPanel extends JPanel {
    private final JTextArea logArea;
    private final JButton convertButton;
    private final JButton openLastFileButton;
    private List<File> selectedFiles = new ArrayList<>();
    private File lastConvertedFile;
    private final JsonToCsvConverter converter;
    private final DefaultTableModel historyTableModel;
    private final List<ConversionRecord> conversionHistory = new ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public JsonToCsvPanel() {
        converter = new JsonToCsvConverter();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top panel with description
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("<html>Convert JSON files to CSV format. Supports both single objects and arrays.<br/>Nested objects will be stored as JSON strings in the CSV.</html>");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descriptionLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        descriptionPanel.add(descriptionLabel, BorderLayout.CENTER);
        add(descriptionPanel, BorderLayout.NORTH);

        // Center panel with buttons and history table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Select files button
        JButton selectButton = new JButton("Select JSON Files");
        selectButton.setIcon(UIManager.getIcon("FileView.fileIcon"));
        selectButton.addActionListener(this::handleFileSelection);
        buttonPanel.add(selectButton);

        // Convert button
        convertButton = new JButton("Convert to CSV");
        convertButton.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        convertButton.setEnabled(false);
        convertButton.addActionListener(this::handleConversion);
        buttonPanel.add(convertButton);

        // Open last file button
        openLastFileButton = new JButton("Open Last Converted File");
        openLastFileButton.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        openLastFileButton.setEnabled(false);
        openLastFileButton.addActionListener(e -> openLastConvertedFile());
        buttonPanel.add(openLastFileButton);

        centerPanel.add(buttonPanel, BorderLayout.NORTH);

        // History table
        String[] columnNames = {"Time", "Source File", "Target File", "Status", "Actions"};
        historyTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable historyTable = new JTable(historyTableModel);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        // Custom renderer for the status column
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof ConversionRecord.ConversionStatus) {
                    setText(value.toString());
                }
                return c;
            }
        });

        // Custom renderer for the actions column
        historyTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JButton button = new JButton("Open");
                button.setEnabled(value instanceof Boolean && (Boolean) value);
                return button;
            }
        });

        // Handle click on the actions column
        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = historyTable.rowAtPoint(evt.getPoint());
                int col = historyTable.columnAtPoint(evt.getPoint());
                if (col == 4 && row >= 0 && row < conversionHistory.size()) {
                    ConversionRecord record = conversionHistory.get(row);
                    if (record.getStatus() == ConversionRecord.ConversionStatus.COMPLETED) {
                        openFile(record.getTargetFile());
                    }
                }
            }
        });

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Conversion History"));
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        centerPanel.add(historyPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel with log area
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Conversion Log"));
        
        logArea = new JTextArea(6, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(245, 245, 245));
        logArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logPanel.add(scrollPane, BorderLayout.CENTER);

        // Clear log button
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logButtonPanel.add(clearLogButton);
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);

        add(logPanel, BorderLayout.SOUTH);
    }

    private void updateHistoryTable() {
        historyTableModel.setRowCount(0);
        for (ConversionRecord record : conversionHistory) {
            Vector<Object> row = new Vector<>();
            row.add(record.getTimestamp().format(timeFormatter));
            row.add(record.getSourceFile().getName());
            row.add(record.getTargetFile() != null ? record.getTargetFile().getName() : "");
            row.add(record.getStatus());
            row.add(record.getStatus() == ConversionRecord.ConversionStatus.COMPLETED);
            historyTableModel.addRow(row);
        }
    }

    private void handleFileSelection(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
            }
            public String getDescription() {
                return "JSON Files (*.json)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFiles = List.of(fileChooser.getSelectedFiles());
            convertButton.setEnabled(!selectedFiles.isEmpty());
            logArea.append("Selected " + selectedFiles.size() + " file(s)\n");
            for (File file : selectedFiles) {
                logArea.append("- " + file.getName() + "\n");
            }
            logArea.append("-------------------\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private void handleConversion(ActionEvent e) {
        SwingWorker<Void, ConversionRecord> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                convertButton.setEnabled(false);
                for (File jsonFile : selectedFiles) {
                    ConversionRecord record = new ConversionRecord(jsonFile);
                    conversionHistory.add(record);
                    publish(record);
                    
                    try {
                        lastConvertedFile = converter.convertJsonToCsv(jsonFile);
                        record.setCompleted(lastConvertedFile);
                        logArea.append("✓ Successfully converted " + jsonFile.getName() + "\n");
                        logArea.append("CSV file created: " + lastConvertedFile.getName() + "\n");
                    } catch (Exception ex) {
                        record.setFailed(ex.getMessage());
                        logArea.append("❌ Error converting " + jsonFile.getName() + ": " + ex.getMessage() + "\n");
                    }
                    publish(record);
                    logArea.append("-------------------\n");
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                }
                return null;
            }

            @Override
            protected void process(List<ConversionRecord> records) {
                updateHistoryTable();
            }

            @Override
            protected void done() {
                convertButton.setEnabled(true);
                openLastFileButton.setEnabled(lastConvertedFile != null);
                updateHistoryTable();
                
                if (lastConvertedFile != null) {
                    int option = JOptionPane.showConfirmDialog(
                        JsonToCsvPanel.this,
                        "Conversion completed. Would you like to open the last converted file?",
                        "Conversion Complete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    if (option == JOptionPane.YES_OPTION) {
                        openLastConvertedFile();
                    }
                }
            }
        };
        worker.execute();
    }

    private void openLastConvertedFile() {
        if (lastConvertedFile != null) {
            openFile(lastConvertedFile);
        }
    }

    private void openFile(File file) {
        if (file != null && file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Error opening file: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
