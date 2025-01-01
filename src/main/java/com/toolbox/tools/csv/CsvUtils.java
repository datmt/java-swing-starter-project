package com.toolbox.tools.csv;

import com.opencsv.CSVReader;
import java.io.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class CsvUtils {
    private static final Logger log = LoggerFactory.getLogger(CsvUtils.class);
    public static List<String> getHeaders(File csvFile) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();
            if (headers != null) {
                return new ArrayList<>(List.of(headers));
            }
            return new ArrayList<>();
        } catch (Exception e) {
            // TODO: handle exception
            log.error("Error reading headers", e);
        }
        return new ArrayList<>();
    }
}