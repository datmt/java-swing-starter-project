package com.toolbox.tools;

import com.opencsv.exceptions.CsvException;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class CsvMappingPanel extends JPanel {
    private File sourceFile;
    private File targetFile;
    private JComboBox<String> sourceLookupColumnCombo;
    private JComboBox<String> sourceValueColumnCombo;
    private JComboBox<String> targetLookupColumnCombo;
    private JComboBox<String> targetOutputColumnCombo;
    private JTextField defaultValueField;
    private JTextArea logArea;
    private JButton performMappingButton;
    
    public CsvMappingPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Description panel
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        JLabel descriptionLabel = new JLabel("<html>CSV Mapping Tool<br/>Map values from a source CSV file to a target CSV file using lookup values, similar to Excel's VLOOKUP function.</html>");
        descriptionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        descriptionLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        descriptionPanel.add(descriptionLabel, BorderLayout.CENTER);
        add(descriptionPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new MigLayout("fillx, wrap 2", "[right][grow,fill]"));
        mainPanel.setBorder(BorderFactory.createTitledBorder("CSV Mapping Configuration"));

        // File selection
        JButton selectSourceButton = new JButton("Select Source CSV");
        selectSourceButton.addActionListener(e -> selectSourceFile());
        mainPanel.add(new JLabel("Source File:"), "right");
        mainPanel.add(selectSourceButton, "growx");

        JButton selectTargetButton = new JButton("Select Target CSV");
        selectTargetButton.addActionListener(e -> selectTargetFile());
        mainPanel.add(new JLabel("Target File:"), "right");
        mainPanel.add(selectTargetButton, "growx");

        // Column selection
        sourceLookupColumnCombo = new JComboBox<>();
        sourceValueColumnCombo = new JComboBox<>();
        targetLookupColumnCombo = new JComboBox<>();
        targetOutputColumnCombo = new JComboBox<>();

        mainPanel.add(new JLabel("Source Lookup Column:"), "right");
        mainPanel.add(sourceLookupColumnCombo, "growx");

        mainPanel.add(new JLabel("Source Value Column:"), "right");
        mainPanel.add(sourceValueColumnCombo, "growx");

        mainPanel.add(new JLabel("Target Lookup Column:"), "right");
        mainPanel.add(targetLookupColumnCombo, "growx");

        mainPanel.add(new JLabel("Target Output Column:"), "right");
        mainPanel.add(targetOutputColumnCombo, "growx");

        // Default value
        defaultValueField = new JTextField();
        mainPanel.add(new JLabel("Default Value:"), "right");
        mainPanel.add(defaultValueField, "growx");

        // Perform mapping button
        performMappingButton = new JButton("Perform Mapping");
        performMappingButton.setEnabled(false);
        performMappingButton.addActionListener(e -> performMapping());
        mainPanel.add(performMappingButton, "span 2, center");

        add(mainPanel, BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(6, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(245, 245, 245));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        add(logPanel, BorderLayout.SOUTH);
    }

    private void selectSourceFile() {
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
            sourceFile = fileChooser.getSelectedFile();
            updateSourceColumns();
            checkEnableMapping();
        }
    }

    private void selectTargetFile() {
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
            targetFile = fileChooser.getSelectedFile();
            updateTargetColumns();
            checkEnableMapping();
        }
    }

    private void updateSourceColumns() {
        try {
            String[] headers = CsvMapper.getHeaders(sourceFile);
            sourceLookupColumnCombo.setModel(new DefaultComboBoxModel<>(headers));
            sourceValueColumnCombo.setModel(new DefaultComboBoxModel<>(headers));
            logArea.append("Loaded source file: " + sourceFile.getName() + "\n");
        } catch (IOException | CsvException e) {
            logArea.append("Error loading source file: " + e.getMessage() + "\n");
        }
    }

    private void updateTargetColumns() {
        try {
            String[] headers = CsvMapper.getHeaders(targetFile);
            targetLookupColumnCombo.setModel(new DefaultComboBoxModel<>(headers));
            targetOutputColumnCombo.setModel(new DefaultComboBoxModel<>(headers));
            logArea.append("Loaded target file: " + targetFile.getName() + "\n");
        } catch (IOException | CsvException e) {
            logArea.append("Error loading target file: " + e.getMessage() + "\n");
        }
    }

    private void checkEnableMapping() {
        boolean filesSelected = sourceFile != null && targetFile != null;
        boolean columnsSelected = sourceLookupColumnCombo.getSelectedItem() != null &&
                                sourceValueColumnCombo.getSelectedItem() != null &&
                                targetLookupColumnCombo.getSelectedItem() != null &&
                                targetOutputColumnCombo.getSelectedItem() != null;
        
        performMappingButton.setEnabled(filesSelected && columnsSelected);
    }

    private void performMapping() {
        try {
            CsvMapper mapper = new CsvMapper(
                sourceFile,
                targetFile,
                sourceLookupColumnCombo.getSelectedIndex(),
                sourceValueColumnCombo.getSelectedIndex(),
                targetLookupColumnCombo.getSelectedIndex(),
                targetOutputColumnCombo.getSelectedIndex(),
                defaultValueField.getText()
            );

            logArea.append("Starting mapping process...\n");
            File outputFile = mapper.performMapping();
            logArea.append("Mapping completed successfully!\n");
            logArea.append("Output file created: " + outputFile.getName() + "\n");

            int option = JOptionPane.showConfirmDialog(
                this,
                "Mapping completed successfully! Would you like to open the output file?",
                "Mapping Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                Desktop.getDesktop().open(outputFile);
            }

        } catch (Exception e) {
            logArea.append("Error during mapping: " + e.getMessage() + "\n");
            JOptionPane.showMessageDialog(
                this,
                "Error during mapping: " + e.getMessage(),
                "Mapping Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
