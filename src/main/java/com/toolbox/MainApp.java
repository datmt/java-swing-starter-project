package com.toolbox;

import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

public class MainApp extends JFrame {
    private final JPanel contentPanel;
    private final JTextField searchField;
    private final JTree toolTree;
    private final ToolTreeModel treeModel;
    private JPanel rightPanel;

    public MainApp() {
        super("Desktop Tools");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

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
        rightPanel = new JPanel(new MigLayout("fill"));
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

        // Tool tree selection listener
        toolTree.addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                Tool selectedTool = treeModel.getToolAtNode(node);
                if (selectedTool != null && !selectedTool.isCategory()) {
                    showTool(selectedTool);
                }
            }
        });
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
