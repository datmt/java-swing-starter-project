package com.toolbox.tools.spreadsheet

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jopendocument.dom.spreadsheet.SpreadSheet
import spock.lang.Specification
import spock.lang.TempDir

import java.time.LocalDateTime

class SpreadsheetConverterSpec extends Specification {
    @TempDir
    File tempDir

    def "should convert XLSX file to CSV"() {
        given: "an XLSX file with different data types"
        def xlsxFile = createTestExcelFile(true)

        when: "converting to CSV"
        def result = SpreadsheetConverter.convertToCSV(xlsxFile, tempDir)

        then: "conversion should succeed"
        result.success
        result.outputFiles.size() == 2

        and: "CSV content should be correct"
        def basicTypesContent = result.outputFiles[0].text
        basicTypesContent.contains("String,Number,Boolean,Date")
        basicTypesContent.contains("\"Test, with comma\",123.45,true")

        def formulasContent = result.outputFiles[1].text
        formulasContent.contains("10.0,20.0,30.0")
    }

    def "should convert XLS file to CSV"() {
        given: "an XLS file"
        def xlsFile = createTestExcelFile(false)

        when: "converting to CSV"
        def result = SpreadsheetConverter.convertToCSV(xlsFile, tempDir)

        then: "conversion should succeed"
        result.success
        result.outputFiles.size() == 2
    }


    def "should handle unsupported file format"() {
        given: "a text file"
        def textFile = new File(tempDir, "test.txt")
        textFile.text = "test"

        when: "converting to CSV"
        def result = SpreadsheetConverter.convertToCSV(textFile, tempDir)

        then: "conversion should fail"
        !result.success
        result.error.contains("Unsupported file format")
    }

    def "should normalize filenames correctly"() {
        expect:
        SpreadsheetConverter.normalizeFileName(input) == expected

        where:
        input             | expected
        "Test File.csv"   | "test_file.csv"
        "test.csv"        | "test.csv"
    }

    def "should handle invalid input file"() {
        given: "a non-existent file"
        def nonExistentFile = new File(tempDir, "nonexistent.xlsx")

        when: "converting to CSV"
        def result = SpreadsheetConverter.convertToCSV(nonExistentFile, tempDir)

        then: "conversion should fail"
        !result.success
        result.error != null
    }

    def "should handle invalid output directory"() {
        given: "an Excel file and non-existent directory"
        def xlsxFile = createTestExcelFile(true)
        def nonExistentDir = new File(tempDir, "nonexistent")

        when: "converting to CSV"
        def result = SpreadsheetConverter.convertToCSV(xlsxFile, nonExistentDir)

        then: "conversion should fail"
        !result.success
        result.error != null
    }

    private File createTestExcelFile(boolean useXLSX) {
        def workbook = useXLSX ? new XSSFWorkbook() : new HSSFWorkbook()

        // Sheet 1: Basic data types
        def sheet1 = workbook.createSheet("Basic Types")
        def headerRow = sheet1.createRow(0)
        headerRow.createCell(0).setCellValue("String")
        headerRow.createCell(1).setCellValue("Number")
        headerRow.createCell(2).setCellValue("Boolean")
        headerRow.createCell(3).setCellValue("Date")

        def dataRow = sheet1.createRow(1)
        dataRow.createCell(0).setCellValue("Test, with comma")
        dataRow.createCell(1).setCellValue(123.45)
        dataRow.createCell(2).setCellValue(true)

        def dateCell = dataRow.createCell(3)
        dateCell.setCellValue(LocalDateTime.of(2024, 1, 1, 12, 0))
        def dateStyle = workbook.createCellStyle()
        dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"))
        dateCell.setCellStyle(dateStyle)

        // Sheet 2: Empty cells and formulas
        def sheet2 = workbook.createSheet("Formulas")
        def formulaRow = sheet2.createRow(0)
        formulaRow.createCell(0).setCellValue(10.0)
        formulaRow.createCell(1).setCellValue(20.0)
        def formulaCell = formulaRow.createCell(2)
        formulaCell.setCellFormula("A1+B1")

        // Evaluate formulas
        def evaluator = workbook.getCreationHelper().createFormulaEvaluator()
        evaluator.evaluateAll()

        def extension = useXLSX ? ".xlsx" : ".xls"
        def excelFile = new File(tempDir, "test${extension}")
        excelFile.withOutputStream { os ->
            workbook.write(os)
        }
        workbook.close()
        return excelFile
    }

    private File createTestOdsFile() {
        def file = new File(tempDir, "test.ods")
        def sheet = SpreadSheet.create(1, 2, 2)  // rows, columns
        sheet.getSheet(0).setValueAt("Test", 0, 0)
        sheet.getSheet(0).setValueAt(42, 0, 1)
        sheet.saveAs(file)
        return file
    }
}
