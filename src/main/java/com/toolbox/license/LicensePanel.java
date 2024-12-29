package com.toolbox.license;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

public class LicensePanel extends JPanel {
    private final JTextField licenseKeyField;
    private final JButton activateButton;
    private final JLabel statusLabel;
    private final JPanel statusPanel;

    public LicensePanel() {
        setLayout(new MigLayout("fillx, insets 20", "[grow]", "[]20[]20[]20[]20[]"));

        // Title
        JLabel titleLabel = new JLabel("License Activation");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16));
        add(titleLabel, "wrap");

        // Status panel
        statusPanel = new JPanel(new MigLayout("fillx", "[right][grow]", "[]5[]5[]5[]"));
        statusPanel.setBorder(BorderFactory.createTitledBorder("License Status"));
        add(statusPanel, "span, growx, wrap");

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

        statusPanel.removeAll();
        statusPanel.add(new JLabel("Status:"), "");
        JLabel statusLabel = new JLabel("Activated");
        statusLabel.setForeground(new Color(0, 128, 0));
        statusPanel.add(statusLabel, "wrap");

        String email = LicenseManager.getEmail();
        if (email != null && !email.isEmpty()) {
            statusPanel.add(new JLabel("Email:"), "");
            statusPanel.add(new JLabel(email), "wrap");
        }

        String expirationDate = LicenseManager.getExpirationDate();
        if (expirationDate != null && !expirationDate.isEmpty()) {
            statusPanel.add(new JLabel("Expires:"), "");
            statusPanel.add(new JLabel(expirationDate), "wrap");
        }

        String licenseType = LicenseManager.getLicenseType();
        if (licenseType != null && !licenseType.isEmpty()) {
            statusPanel.add(new JLabel("License:"), "");
            statusPanel.add(new JLabel(licenseType), "wrap");
        }

        statusPanel.revalidate();
        statusPanel.repaint();
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
