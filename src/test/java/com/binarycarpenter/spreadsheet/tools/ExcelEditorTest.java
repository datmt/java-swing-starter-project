package com.binarycarpenter.spreadsheet.tools;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelEditorTest {
    private ExcelEditorPanel editor;
    private File testFile;
    
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        editor = new ExcelEditorPanel();
        
        // Create a test Excel file
        testFile = tempDir.resolve("test.xlsx").toFile();
        createTestFile(testFile);
    }
    
    private void createTestFile(File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create first sheet with test data
            Sheet sheet1 = workbook.createSheet("Sheet1");
            Row headerRow = sheet1.createRow(0);
            headerRow.createCell(0).setCellValue("Name");
            headerRow.createCell(1).setCellValue("Age");
            headerRow.createCell(2).setCellValue("Date");
            
            Row dataRow1 = sheet1.createRow(1);
            dataRow1.createCell(0).setCellValue("John");
            dataRow1.createCell(1).setCellValue(30);
            dataRow1.createCell(2).setCellValue(new Date());
            
            Row dataRow2 = sheet1.createRow(2);
            dataRow2.createCell(0).setCellValue("Jane");
            dataRow2.createCell(1).setCellValue(25);
            dataRow2.createCell(2).setCellValue(new Date());
            
            // Create second sheet
            Sheet sheet2 = workbook.createSheet("Sheet2");
            Row sheet2Row = sheet2.createRow(0);
            sheet2Row.createCell(0).setCellValue("Test");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }
    
    @Test
    void testLoadExcelFile() throws Exception {
        // Load the test file
        editor.loadFile(testFile);
        
        // Get the table model
        DefaultTableModel model = (DefaultTableModel) editor.getTable().getModel();
        
        // Verify column count
        assertThat(model.getColumnCount()).isEqualTo(3);
        
        // Verify column names (A, B, C)
        assertThat(model.getColumnName(0)).isEqualTo("A");
        assertThat(model.getColumnName(1)).isEqualTo("B");
        assertThat(model.getColumnName(2)).isEqualTo("C");
        
        // Verify row count (2 data rows + 1 header row)
        assertThat(model.getRowCount()).isEqualTo(3);
        
        // Verify cell values
        assertThat(model.getValueAt(0, 0)).isEqualTo("Name");
        assertThat(model.getValueAt(0, 1)).isEqualTo("Age");
        assertThat(model.getValueAt(0, 2)).isEqualTo("Date");
        assertThat(model.getValueAt(1, 0)).isEqualTo("John");
        assertThat(model.getValueAt(1, 1)).isEqualTo(30.0);
        assertThat(model.getValueAt(2, 0)).isEqualTo("Jane");
        assertThat(model.getValueAt(2, 1)).isEqualTo(25.0);
    }
    
    @Test
    void testSaveExcelFile(@TempDir Path tempDir) throws Exception {
        // Load the test file
        editor.loadFile(testFile);
        
        // Modify some values
        DefaultTableModel model = (DefaultTableModel) editor.getTable().getModel();
        model.setValueAt("Modified", 1, 0);
        model.setValueAt(35.0, 1, 1);
        
        // Save to a new file
        File savedFile = tempDir.resolve("saved.xlsx").toFile();
        editor.setCurrentFile(savedFile);
        editor.saveFile();
        
        // Verify the saved file
        try (Workbook workbook = new XSSFWorkbook(savedFile)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Modified");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(35.0);
        }
    }
    
    @Test
    void testMultipleSheets() throws Exception {
        // Load the test file
        editor.loadFile(testFile);
        
        // Verify sheet count
        assertThat(editor.getSheetSelector().getItemCount()).isEqualTo(2);
        assertThat(editor.getSheetSelector().getItemAt(0)).isEqualTo("Sheet1");
        assertThat(editor.getSheetSelector().getItemAt(1)).isEqualTo("Sheet2");
        
        // Switch to second sheet
        editor.getSheetSelector().setSelectedIndex(1);
        
        // Verify second sheet content
        DefaultTableModel model = (DefaultTableModel) editor.getTable().getModel();
        assertThat(model.getValueAt(0, 0)).isEqualTo("Test");
    }
    
    @Test
    void testExportFile(@TempDir Path tempDir) throws Exception {
        // Load the test file
        editor.loadFile(testFile);
        
        // Export to a new file
        File exportedFile = tempDir.resolve("exported.xlsx").toFile();
        editor.exportFile(exportedFile);
        
        // Verify the exported file
        try (Workbook workbook = new XSSFWorkbook(exportedFile)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("John");
            assertThat(sheet.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(30.0);
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Jane");
            assertThat(sheet.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(25.0);
        }
    }
}
