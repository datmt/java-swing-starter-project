package com.toolbox.tools.csv;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.SwingWorker;

public class CsvDiffPanel extends JPanel {
    private final Logger log = LoggerFactory.getLogger(CsvDiffPanel.class);
    
    private File file1;
    private File file2;
    private final JTextField file1Field;
    private final JTextField file2Field;
    private final JList<String> columnList;
    private final DefaultListModel<String> columnListModel;
    private final JTable diffTable;
    private final DefaultTableModel diffTableModel;
    private final JCheckBox ignoreCaseCheckbox;
    private final JCheckBox ignoreWhitespaceCheckbox;
    private final JCheckBox strictRowOrderCheckbox;
    private final JCheckBox ignoreDuplicatesCheckbox;
    private final JButton compareButton;
    private final JLabel summaryLabel;
    private final JProgressBar progressBar;

    public CsvDiffPanel() {
        setLayout(new MigLayout("fillx, insets 20", "[grow]", "[]10[]10[]10[]"));

        // File selection panel
        JPanel filePanel = new JPanel(new MigLayout("fillx", "[grow][]", "[]5[]"));
        filePanel.setBorder(BorderFactory.createTitledBorder("Select Files"));

        file1Field = new JTextField();
        file1Field.setEditable(false);
        JButton selectFile1Button = new JButton("Select File 1");
        selectFile1Button.addActionListener(e -> selectFile(1));

        file2Field = new JTextField();
        file2Field.setEditable(false);
        JButton selectFile2Button = new JButton("Select File 2");
        selectFile2Button.addActionListener(e -> selectFile(2));

        filePanel.add(new JLabel("File 1:"), "split 3");
        filePanel.add(file1Field, "growx");
        filePanel.add(selectFile1Button, "wrap");
        filePanel.add(new JLabel("File 2:"), "split 3");
        filePanel.add(file2Field, "growx");
        filePanel.add(selectFile2Button, "wrap");

        add(filePanel, "growx, wrap");

        // Key columns panel
        JPanel keyColumnsPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        keyColumnsPanel.setBorder(BorderFactory.createTitledBorder("Key Columns"));

        columnListModel = new DefaultListModel<>();
        columnList = new JList<>(columnListModel);
        columnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        columnList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCompareButton();
            }
        });
        JScrollPane columnScroll = new JScrollPane(columnList);
        columnScroll.setPreferredSize(new Dimension(0, 100));
        keyColumnsPanel.add(columnScroll, "growx, wrap");

        add(keyColumnsPanel, "growx, wrap");

        // Options panel
        JPanel optionsPanel = new JPanel(new MigLayout("", "[]", "[]5[]5[]5[]"));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

        ignoreCaseCheckbox = new JCheckBox("Ignore case");
        ignoreWhitespaceCheckbox = new JCheckBox("Ignore whitespace");
        strictRowOrderCheckbox = new JCheckBox("Strict row order");
        ignoreDuplicatesCheckbox = new JCheckBox("Ignore duplicates");
        ignoreDuplicatesCheckbox.setToolTipText("When checked, files with the same unique rows will be considered identical, even if one contains duplicates");

        optionsPanel.add(ignoreCaseCheckbox, "wrap");
        optionsPanel.add(ignoreWhitespaceCheckbox, "wrap");
        optionsPanel.add(strictRowOrderCheckbox, "wrap");
        optionsPanel.add(ignoreDuplicatesCheckbox, "wrap");

        compareButton = new JButton("Compare Files");
        compareButton.setEnabled(false);
        compareButton.addActionListener(e -> compareFiles());
        optionsPanel.add(compareButton);

        add(optionsPanel, "growx, wrap");

        // Create progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(0, 4));
        progressBar.setStringPainted(false);
        add(progressBar, "growx, wrap");

        // Results panel
        JPanel resultsPanel = new JPanel(new MigLayout("fillx", "[grow]", "[]5[]"));
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Differences"));

        summaryLabel = new JLabel(" ");
        resultsPanel.add(summaryLabel, "wrap");

        diffTableModel = new DefaultTableModel(
            new String[]{"Type", "Key Values", "Column", "Old Value", "New Value"}, 0);
        diffTable = new JTable(diffTableModel);
        JScrollPane tableScroll = new JScrollPane(diffTable);
        tableScroll.setPreferredSize(new Dimension(0, 300));
        resultsPanel.add(tableScroll, "growx");

        add(resultsPanel, "growx");
    }

    private void selectFile(int fileNum) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (fileNum == 1) {
                file1 = selectedFile;
                file1Field.setText(selectedFile.getPath());
            } else {
                file2 = selectedFile;
                file2Field.setText(selectedFile.getPath());
            }

            updateColumnList();
            updateCompareButton();
        }
    }

    private void updateColumnList() {
        if (file1 != null && file2 != null) {
            try {
                // Read headers from first file
                List<String> headers = CsvUtils.getHeaders(file1);
                columnListModel.clear();
                headers.forEach(columnListModel::addElement);
            } catch (Exception e) {
                log.error("Error reading headers", e);
                JOptionPane.showMessageDialog(this,
                    "Error reading file headers: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateCompareButton() {
        compareButton.setEnabled(file1 != null && file2 != null && 
                               columnList.getSelectedIndices().length > 0);
    }

    private void compareFiles() {
        try {
            // Show progress bar and disable compare button
            progressBar.setVisible(true);
            compareButton.setEnabled(false);

            // Run comparison in background
            SwingWorker<CsvDiffEngine.DiffResult, Void> worker = new SwingWorker<>() {
                @Override
                protected CsvDiffEngine.DiffResult doInBackground() throws Exception {
                    List<String> selectedColumns = columnList.getSelectedValuesList();
                    CsvDiffEngine engine = new CsvDiffEngine();
                    return engine.compareFiles(
                        file1, file2,
                        columnList.getSelectedValuesList(),
                        ignoreCaseCheckbox.isSelected(),
                        ignoreWhitespaceCheckbox.isSelected(),
                        strictRowOrderCheckbox.isSelected(),
                        ignoreDuplicatesCheckbox.isSelected()
                    );
                }

                @Override
                protected void done() {
                    try {
                        CsvDiffEngine.DiffResult result = get();
                        
                        // Update summary
                        updateSummary(result);

                        // Clear and update table
                        diffTableModel.setRowCount(0);

                        // Add modified rows
                        for (CsvDiffEngine.RowDiff diff : result.getModifiedRows()) {
                            for (String column : diff.getModifiedColumns()) {
                                diffTableModel.addRow(new Object[]{
                                    "Modified",
                                    String.join(", ", diff.getKeyValues()),
                                    column,
                                    diff.getOldValues().get(column),
                                    diff.getNewValues().get(column)
                                });
                            }
                        }

                        // Add removed rows
                        for (CsvDiffEngine.RowDiff diff : result.getRemovedRows()) {
                            diffTableModel.addRow(new Object[]{
                                "Removed",
                                String.join(", ", diff.getKeyValues()),
                                "",
                                "",
                                ""
                            });
                        }

                        // Add added rows
                        for (CsvDiffEngine.RowDiff diff : result.getAddedRows()) {
                            diffTableModel.addRow(new Object[]{
                                "Added",
                                String.join(", ", diff.getKeyValues()),
                                "",
                                "",
                                ""
                            });
                        }
                    } catch (Exception e) {
                        log.error("Error comparing files", e);
                        JOptionPane.showMessageDialog(CsvDiffPanel.this,
                            "Error comparing files: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        // Hide progress bar and re-enable compare button
                        progressBar.setVisible(false);
                        updateCompareButton();
                    }
                }
            };
            worker.execute();

        } catch (Exception e) {
            log.error("Error comparing files", e);
            JOptionPane.showMessageDialog(this,
                "Error comparing files: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            progressBar.setVisible(false);
            updateCompareButton();
        }
    }

    private void updateSummary(CsvDiffEngine.DiffResult result) {
        StringBuilder summary = new StringBuilder("<html>");
        summary.append("Summary: ");
        if (result.getAddedColumns().isEmpty() && result.getRemovedColumns().isEmpty() &&
            result.getAddedRows().isEmpty() && result.getRemovedRows().isEmpty() &&
            result.getModifiedRows().isEmpty()) {
            summary.append("<b>Files are identical</b>");
        } else {
            if (!result.getAddedColumns().isEmpty() || !result.getRemovedColumns().isEmpty()) {
                summary.append("<br>Column differences: ");
                summary.append(result.getAddedColumns().size()).append(" added, ");
                summary.append(result.getRemovedColumns().size()).append(" removed");
            }
            summary.append("<br>Row differences: ");
            summary.append(result.getAddedRows().size()).append(" added, ");
            summary.append(result.getRemovedRows().size()).append(" removed, ");
            summary.append(result.getModifiedRows().size()).append(" modified");
        }
        summary.append("</html>");
        summaryLabel.setText(summary.toString());
    }
}
