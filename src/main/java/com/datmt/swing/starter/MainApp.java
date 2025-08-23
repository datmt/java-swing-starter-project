package com.datmt.swing.starter;

import com.formdev.flatlaf.FlatLightLaf;

import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import java.awt.Color;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

@Slf4j
public class MainApp extends JFrame {
    private final JPanel contentPanel;
    private final JTextField searchField;
    private final JTree toolTree;
    private final ToolTreeModel treeModel;
    private JPanel rightPanel;

    public MainApp() {
        super("Java Swing Starter App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu settingsMenu = new JMenu("Settings");

        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        // Initialize components
        contentPanel = new JPanel(new MigLayout("fill"));
        searchField = new JTextField(20);
        treeModel = new ToolTreeModel();
        toolTree = new JTree(treeModel);

        // Setup UI
        setupUI();

        // Add listeners
        setupListeners();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("Component.arc", 0);
                UIManager.put("ProgressBar.arc", 0);
                UIManager.put("TabbedPane.selectedBackground", Color.white);
            } catch (Exception ex) {
                log.error("Failed to start flatlaf", ex);
            }
            new MainApp().setVisible(true);
        });
    }

    private void setupUI() {
        setLayout(new MigLayout("fill"));

        // Left panel with search and tree
        JPanel leftPanel = new JPanel(new MigLayout("fillx, wrap"));
        leftPanel.add(new JLabel("Search:"), "split 2");
        leftPanel.add(searchField, "growx, wrap");

        // Style the tree
        toolTree.setRootVisible(false);
        toolTree.setShowsRootHandles(true);

        JScrollPane treeScrollPane = new JScrollPane(toolTree);
        leftPanel.add(treeScrollPane, "grow");

        // Right content panel
        rightPanel = new JPanel(new MigLayout("insets 0, fill"));
        rightPanel.add(contentPanel, "grow");

        // Add components to frame
        add(leftPanel, "w 250!, growy");
        add(rightPanel, "grow, push");
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

        // Tool tree selection listener
        toolTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Tool selectedTool = treeModel.getToolAtNode(node);
                if (selectedTool != null) {
                    handleToolSelection(selectedTool);
                }
            }
        });
    }

    private void handleToolSelection(Tool selectedTool) {
        if (!selectedTool.isCategory()) {
            showTool(selectedTool);
        }
    }

    private void filterTools() {
        String searchText = searchField.getText();
        treeModel.filterTools(searchText);

        // Expand all nodes after filtering
        for (int i = 0; i < toolTree.getRowCount(); i++) {
            toolTree.expandRow(i);
        }
    }

    private void showTool(Tool tool) {
        contentPanel.removeAll();
        contentPanel.add(tool.getContent(), "grow");

        // Revalidate and repaint both panels
        contentPanel.revalidate();
        contentPanel.repaint();
        rightPanel.revalidate();
        rightPanel.repaint();
    }

}
