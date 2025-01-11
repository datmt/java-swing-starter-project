package com.datmt.swing.starter;

import com.datmt.swing.starter.tools.JsonToCsvPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolTreeModel extends DefaultTreeModel {
    private final List<Tool> allTools;
    private final Map<String, Class<?>> toolPanels;
    private final DefaultMutableTreeNode rootNode;

    public ToolTreeModel() {
        super(new DefaultMutableTreeNode("Tools"));
        this.rootNode = (DefaultMutableTreeNode) getRoot();
        this.allTools = new ArrayList<>();
        this.toolPanels = new HashMap<>();
        initializeTools();
    }

    private void initializeTools() {
        // Create JSON category
        Tool jsonCategory = new Tool("JSON Tools", new JPanel(), true);

        // Create JSON to CSV tool with its panel
        Tool jsonToCsvTool = new Tool("JSON to CSV", JsonToCsvPanel::new, false);
        jsonCategory.addChild(jsonToCsvTool);


        // Add to all tools list
        allTools.add(jsonCategory);
        allTools.add(jsonToCsvTool);

        // Add to tool panels map
        toolPanels.put("JSON to CSV", JsonToCsvPanel.class);
        // Build tree structure
        buildTreeNodes();
    }

    private void buildTreeNodes() {
        rootNode.removeAllChildren();

        for (Tool tool : allTools) {
            if (tool.isCategory()) {
                DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(tool);
                rootNode.add(categoryNode);

                for (Tool child : tool.getChildren()) {
                    categoryNode.add(new DefaultMutableTreeNode(child));
                }
            }
        }

        reload();
    }

    public void filterTools(String searchText) {
        rootNode.removeAllChildren();
        searchText = searchText.toLowerCase();

        for (Tool tool : allTools) {
            if (tool.isCategory()) {
                DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(tool);
                boolean addCategory = tool.getName().toLowerCase().contains(searchText);
                boolean hasMatchingChildren = false;

                for (Tool child : tool.getChildren()) {
                    if (child.getName().toLowerCase().contains(searchText)) {
                        categoryNode.add(new DefaultMutableTreeNode(child));
                        hasMatchingChildren = true;
                    }
                }

                if (addCategory || hasMatchingChildren) {
                    rootNode.add(categoryNode);
                }
            }
        }

        reload();
    }

    public Tool getToolAtNode(DefaultMutableTreeNode node) {
        if (node == null) return null;
        Object userObject = node.getUserObject();
        if (userObject instanceof Tool) {
            return (Tool) userObject;
        }
        return null;
    }

}
