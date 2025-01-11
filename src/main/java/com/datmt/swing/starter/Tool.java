package com.datmt.swing.starter;

import lombok.Getter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Tool {
    @Getter
    private final String name;
    private final Supplier<JPanel> contentSupplier;
    @Getter
    private final List<Tool> children;
    private final boolean isCategory;
    private JPanel content;

    public Tool(String name, JPanel content) {
        this(name, content, false);
    }

    public Tool(String name, JPanel content, boolean isCategory) {
        this.name = name;
        this.content = content;
        this.contentSupplier = null;
        this.children = new ArrayList<>();
        this.isCategory = isCategory;
    }

    public Tool(String name, Supplier<JPanel> contentSupplier, boolean isCategory) {
        this.name = name;
        this.content = null;
        this.contentSupplier = contentSupplier;
        this.children = new ArrayList<>();
        this.isCategory = isCategory;
    }

    public JPanel getContent() {
        if (content == null && contentSupplier != null) {
            content = contentSupplier.get();
        }
        return content;
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
