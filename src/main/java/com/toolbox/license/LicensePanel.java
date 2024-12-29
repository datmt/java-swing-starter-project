package com.toolbox.license;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

public class LicensePanel extends JPanel {
    private final JTextField licenseKeyField;
    private final JButton activateButton;
    private final JLabel statusLabel;

    public LicensePanel() {
        setLayout(new MigLayout("fillx, insets 20", "[grow]", "[]20[]20[]"));

        // Title
        JLabel titleLabel = new JLabel("License Activation");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        add(titleLabel, "wrap");

        // License key input
        JPanel inputPanel = new JPanel(new MigLayout("fillx", "[100]10[grow]", "[]"));
        JLabel keyLabel = new JLabel("License Key:");
        licenseKeyField = new JTextField(20);
        inputPanel.add(keyLabel);
        inputPanel.add(licenseKeyField, "growx");
        add(inputPanel, "growx, wrap");

        // Activate button
        activateButton = new JButton("Activate");
        add(activateButton, "width 150!, center, wrap");

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, "growx, wrap");

        // Load saved license key if any
        String savedKey = LicenseManager.getSavedLicenseKey();
        if (!savedKey.isEmpty()) {
            licenseKeyField.setText(savedKey);
            if (LicenseManager.isActivated()) {
                updateUIForActivatedState();
            }
        }

        setupListeners();
    }

    private void setupListeners() {
        activateButton.addActionListener(e -> {
            String licenseKey = licenseKeyField.getText().trim();
            if (licenseKey.isEmpty()) {
                showError("Please enter a license key");
                return;
            }

            activateButton.setEnabled(false);
            statusLabel.setText("Activating...");
            statusLabel.setForeground(Color.BLACK);

            // Run activation in background
            SwingWorker<LicenseManager.ActivationResult, Void> worker = new SwingWorker<>() {
                @Override
                protected LicenseManager.ActivationResult doInBackground() {
                    return LicenseManager.activate(licenseKey);
                }

                @Override
                protected void done() {
                    try {
                        LicenseManager.ActivationResult result = get();
                        if (result.isSuccess()) {
                            updateUIForActivatedState();
                            showSuccess(result.getMessage());
                        } else {
                            showError(result.getError());
                            activateButton.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        showError("Activation failed: " + ex.getMessage());
                        activateButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });
    }

    private void updateUIForActivatedState() {
        licenseKeyField.setEnabled(false);
        activateButton.setEnabled(false);
        activateButton.setText("Activated");
        showSuccess("License activated");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.RED);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(0, 128, 0));
    }
}
