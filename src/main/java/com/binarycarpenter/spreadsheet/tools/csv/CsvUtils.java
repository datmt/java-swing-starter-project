package com.binarycarpenter.spreadsheet.tools.csv;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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