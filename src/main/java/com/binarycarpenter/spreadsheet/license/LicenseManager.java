package com.binarycarpenter.spreadsheet.license;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.prefs.Preferences;

@Slf4j
public class LicenseManager {
    private static final String VERSION_NUMBER = "73"; // Version 1.0.0.0
    private static final String ACTIVATION_URL = "https://api.gotkey.io/public/activate/30837999853190265244496741031/14346c08-0020-4bba-bbdb-f1f1fae14f8f/30838361981223176236704514728";
    private static final String PREF_LICENSE_KEY = "licenseKey";
    private static final String PREF_ACTIVATION_STATUS = "activationStatus";
    private static final String PREF_EMAIL = "email";
    private static final String PREF_EXPIRATION_DATE = "expirationDate";
    private static final String PREF_LICENSE_TYPE = "licenseType";
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

    public static String getEmail() {
        return prefs.get(PREF_EMAIL, "");
    }

    public static String getExpirationDate() {
        return prefs.get(PREF_EXPIRATION_DATE, "");
    }

    public static String getLicenseType() {
        return prefs.get(PREF_LICENSE_TYPE, "");
    }

    public static void deactivate() {
        prefs.putBoolean(PREF_ACTIVATION_STATUS, false);
        prefs.remove(PREF_LICENSE_KEY);
        prefs.remove(PREF_EMAIL);
        prefs.remove(PREF_EXPIRATION_DATE);
        prefs.remove(PREF_LICENSE_TYPE);
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

        public String getLicenseKey() {
            return licenseKey;
        }

        public void setLicenseKey(String licenseKey) {
            this.licenseKey = licenseKey;
        }

        public String getMachineID() {
            return machineID;
        }

        public void setMachineID(String machineID) {
            this.machineID = machineID;
        }

        public String getVersionNumber() {
            return versionNumber;
        }

        public void setVersionNumber(String versionNumber) {
            this.versionNumber = versionNumber;
        }
    }

    public static class ServerResponse {
        private boolean result;
        private String message;
        private String extraMessage;
        private String email;
        private String expirationDate;
        private String licenseType;

        public boolean isResult() {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getExtraMessage() {
            return extraMessage;
        }

        public void setExtraMessage(String extraMessage) {
            this.extraMessage = extraMessage;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(String expirationDate) {
            this.expirationDate = expirationDate;
        }

        public String getLicenseType() {
            return licenseType;
        }

        public void setLicenseType(String licenseType) {
            this.licenseType = licenseType;
        }
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

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static ActivationResult validateLicense() {
        String savedKey = getSavedLicenseKey();
        if (savedKey.isEmpty()) {
            return new ActivationResult(false, null, "No license key found");
        }
        return activate(savedKey);
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
                    ServerResponse serverResponse = objectMapper.readValue(responseBody, ServerResponse.class);
                    if (serverResponse.isResult()) {
                        // Save all license information
                        prefs.put(PREF_LICENSE_KEY, licenseKey);
                        prefs.putBoolean(PREF_ACTIVATION_STATUS, true);
                        prefs.put(PREF_EMAIL, serverResponse.getEmail() != null ? serverResponse.getEmail() : "");
                        prefs.put(PREF_EXPIRATION_DATE, serverResponse.getExpirationDate());
                        prefs.put(PREF_LICENSE_TYPE,
                                serverResponse.getLicenseType() != null ? serverResponse.getLicenseType() : "");

                        return new ActivationResult(true,
                                serverResponse.getMessage(),
                                null);
                    } else {
                        deactivate();
                        return new ActivationResult(false,
                                null,
                                serverResponse.getMessage() +
                                        (serverResponse.getExtraMessage() != null
                                                ? "\n" + serverResponse.getExtraMessage()
                                                : ""));
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse response: " + e.getMessage());
                    deactivate();
                    return new ActivationResult(false, null, "Invalid server response");
                }
            } else {
                System.err.println("Server returned error: " + response.statusCode() + " - " + responseBody);
                deactivate();
                return new ActivationResult(false, null, "Activation failed: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Activation error: " + e.getMessage());
            deactivate();
            return new ActivationResult(false, null, "Activation error: " + e.getMessage());
        }
    }

    private static String getMachineId() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
                    continue;
                }
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    var machineId = sb.toString();
                    log.info("Machine ID: " + machineId);
                    return machineId;
                }
            }
        } catch (SocketException e) {
            log.error("Error getting machine ID", e);
        }
        return "UNKNOWN";
    }
}
