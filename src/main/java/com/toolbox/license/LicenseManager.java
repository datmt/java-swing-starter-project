package com.toolbox.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.prefs.Preferences;

public class LicenseManager {
    private static final String VERSION_NUMBER = "1000";  // Version 1.0.0.0
    private static final String ACTIVATION_URL = "https://api.gotkey.io/public/activate/30837999853190265244496741031/14346c08-0020-4bba-bbdb-f1f1fae14f8f/30838361981223176236704514728";
    private static final String PREF_LICENSE_KEY = "licenseKey";
    private static final String PREF_ACTIVATION_STATUS = "activationStatus";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Preferences prefs = Preferences.userNodeForPackage(LicenseManager.class);

    public static boolean isActivated() {
        return prefs.getBoolean(PREF_ACTIVATION_STATUS, false);
    }

    public static String getSavedLicenseKey() {
        return prefs.get(PREF_LICENSE_KEY, "");
    }

    public static class ActivationRequest {
        private String licenseKey;
        private String machineID;
        private String versionNumber;

        public ActivationRequest(String licenseKey, String machineID, String versionNumber) {
            this.licenseKey = licenseKey;
            this.machineID = machineID;
            this.versionNumber = versionNumber;
        }

        // Getters and setters for Jackson
        public String getLicenseKey() { return licenseKey; }
        public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
        public String getMachineID() { return machineID; }
        public void setMachineID(String machineID) { this.machineID = machineID; }
        public String getVersionNumber() { return versionNumber; }
        public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }
    }

    public static class ActivationResponse {
        private boolean success;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ActivationResult {
        private boolean success;
        private String message;
        private String error;

        public ActivationResult(boolean success, String message, String error) {
            this.success = success;
            this.message = message;
            this.error = error;
        }

        // Getters and setters for Jackson
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static ActivationResult activate(String licenseKey) {
        try {
            String machineId = getMachineId();
            ActivationRequest request = new ActivationRequest(licenseKey, machineId, VERSION_NUMBER);
            
            String requestBody = objectMapper.writeValueAsString(request);
            System.out.println("Sending activation request: " + requestBody);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(ACTIVATION_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            System.out.println("Received response: " + responseBody);
            
            if (response.statusCode() == 200) {
                try {
                    // Try to parse the response as JSON
                    ActivationResponse serverResponse = objectMapper.readValue(responseBody, ActivationResponse.class);
                    if (serverResponse.isSuccess()) {
                        // Save activation status
                        prefs.put(PREF_LICENSE_KEY, licenseKey);
                        prefs.putBoolean(PREF_ACTIVATION_STATUS, true);
                        return new ActivationResult(true, "License activated successfully!", null);
                    } else {
                        return new ActivationResult(false, null, serverResponse.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse response: " + e.getMessage());
                    return new ActivationResult(false, null, "Invalid server response");
                }
            } else {
                System.err.println("Server returned error: " + response.statusCode() + " - " + responseBody);
                return new ActivationResult(false, null, "Activation failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Activation error: " + e.getMessage());
            return new ActivationResult(false, null, "Activation error: " + e.getMessage());
        }
    }

    private static String getMachineId() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    return sb.toString();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }
}
