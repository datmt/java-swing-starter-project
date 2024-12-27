package com.toolbox;

import javax.swing.*;

public class Tool {
    private final String name;
    private final JPanel content;

    public Tool(String name, JPanel content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public JPanel getContent() {
        return content;
    }

    @Override
    public String toString() {
        return name;
    }
}
