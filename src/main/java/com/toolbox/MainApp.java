package com.toolbox;

import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends JFrame {
    private final JPanel contentPanel;
    private final JTextField searchField;
    private final JList<Tool> toolList;
    private final DefaultListModel<Tool> listModel;
    private final List<Tool> allTools;

    public MainApp() {
        super("Desktop Tools");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

        // Initialize components
        contentPanel = new JPanel(new MigLayout("fill"));
        searchField = new JTextField(20);
        listModel = new DefaultListModel<>();
        toolList = new JList<>(listModel);
        allTools = new ArrayList<>();

        // Setup tools
        initializeTools();
        
        // Setup UI
        setupUI();
        
        // Add listeners
        setupListeners();
    }

    private void initializeTools() {
        // Add your tools here
        allTools.add(new Tool("Calculator", new JPanel()));
        allTools.add(new Tool("Text Editor", new JPanel()));
        allTools.add(new Tool("Image Viewer", new JPanel()));
        // Add more tools as needed
        
        // Initialize the list model
        updateListModel("");
    }

    private void setupUI() {
        setLayout(new MigLayout("fill"));
        
        // Left panel with search and list
        JPanel leftPanel = new JPanel(new MigLayout("fillx, wrap"));
        leftPanel.add(new JLabel("Search:"), "split 2");
        leftPanel.add(searchField, "growx, wrap");
        
        // Style the list
        toolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        toolList.setCellRenderer(new ToolListCellRenderer());
        
        JScrollPane listScrollPane = new JScrollPane(toolList);
        leftPanel.add(listScrollPane, "grow");
        
        // Right content panel
        JPanel rightPanel = new JPanel(new MigLayout("fill"));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.add(contentPanel, "grow");
        
        // Add components to frame
        add(leftPanel, "w 250!, grow y");
        add(rightPanel, "grow");
    }

    private void setupListeners() {
        // Search field listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTools();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTools();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTools();
            }
        });

        // Tool list selection listener
        toolList.addListSelectionListener(this::handleToolSelection);
    }

    private void filterTools() {
        String searchText = searchField.getText().toLowerCase();
        updateListModel(searchText);
    }

    private void updateListModel(String searchText) {
        listModel.clear();
        for (Tool tool : allTools) {
            if (tool.getName().toLowerCase().contains(searchText)) {
                listModel.addElement(tool);
            }
        }
    }

    private void handleToolSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            Tool selectedTool = toolList.getSelectedValue();
            if (selectedTool != null) {
                showTool(selectedTool);
            }
        }
    }

    private void showTool(Tool tool) {
        contentPanel.removeAll();
        contentPanel.add(tool.getContent(), "grow");
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception ex) {
                System.err.println("Failed to initialize FlatLaf");
            }
            new MainApp().setVisible(true);
        });
    }
}
