package com.toolbox;

import com.formdev.flatlaf.FlatLightLaf;
import com.toolbox.license.LicenseManager;
import com.toolbox.license.LicensePanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

public class MainApp extends JFrame {
    private final JPanel contentPanel;
    private final JTextField searchField;
    private final JTree toolTree;
    private final ToolTreeModel treeModel;
    private JPanel rightPanel;

    public MainApp() {
        super("BC18 Spreadsheet Tools");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu settingsMenu = new JMenu("Settings");
        
        JMenuItem activateMenuItem = new JMenuItem("License Activation");
        activateMenuItem.addActionListener(e -> showLicenseDialog());
        settingsMenu.add(activateMenuItem);
        
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
        
        // Validate license on startup
        SwingWorker<LicenseManager.ActivationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LicenseManager.ActivationResult doInBackground() {
                return LicenseManager.validateLicense();
            }

            @Override
            protected void done() {
                try {
                    LicenseManager.ActivationResult result = get();
                    if (!result.isSuccess()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MainApp.this,
                                "License validation failed: " + result.getError() + "\nPlease reactivate your license.",
                                "License Invalid",
                                JOptionPane.WARNING_MESSAGE);
                            showLicenseDialog();
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
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
                if (selectedTool != null) {
                    handleToolSelection(selectedTool);
                }
            }
        });
    }

    private void handleToolSelection(Tool selectedTool) {
        if (!selectedTool.isCategory()) {
            // Check license activation before allowing access to tools
            if (!LicenseManager.isActivated()) {
                JOptionPane.showMessageDialog(this,
                    "Please activate your license to use this feature.",
                    "License Required",
                    JOptionPane.WARNING_MESSAGE);
                
                showLicenseDialog();
                return;
            }
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

    private void showLicenseDialog() {
        JDialog dialog = new JDialog(this, "License Activation", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(new LicensePanel(), BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
