package com.datmt.swing.starter.tools;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StarterPanel extends JPanel {
    private final JButton myButton;

    public StarterPanel() {
        myButton = new JButton("Customize your panel here");

        setupAction();
        this.add(myButton);
    }

    void setupAction() {
        myButton.addActionListener(e -> {
            //open a dialog with message
            JOptionPane.showMessageDialog(this, "This is a starter panel. Customize it as needed!");
            log.info("Button clicked in StarterPanel");
        });
    }
}
