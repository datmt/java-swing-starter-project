package com.toolbox;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Tool {
    private final String name;
    private final JPanel content;
    private final List<Tool> children;
    private final boolean isCategory;

    public Tool(String name, JPanel content) {
        this(name, content, false);
    }

    public Tool(String name, JPanel content, boolean isCategory) {
        this.name = name;
        this.content = content;
        this.children = new ArrayList<>();
        this.isCategory = isCategory;
    }

    public String getName() {
        return name;
    }

    public JPanel getContent() {
        return content;
    }

    public List<Tool> getChildren() {
        return children;
    }

    public void addChild(Tool child) {
        children.add(child);
    }

    public boolean isCategory() {
        return isCategory;
    }

    @Override
    public String toString() {
        return name;
    }
}
