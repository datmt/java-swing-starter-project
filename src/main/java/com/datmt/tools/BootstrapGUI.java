package com.datmt.tools;

import com.datmt.tools.helper.CommonUI;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class BootstrapGUI extends JFrame {
    JPanel mainPanel;

    public BootstrapGUI() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout("wrap 1"));
        // Create a button and add it to the frame
        var label = CommonUI.createLabel("Click this button, it does nothing");
        var button = CommonUI.createButton("Hello");

        mainPanel.add(label);
        mainPanel.add(button);

        add(mainPanel);
        // Set the frame size and make it visible
        setSize(300, 200);
        setLocationRelativeTo(null); // Center the window
        setVisible(true);
    }
}
