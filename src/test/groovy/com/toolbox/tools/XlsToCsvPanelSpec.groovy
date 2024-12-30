package com.toolbox.tools

import com.toolbox.tools.spreadsheet.ConversionResult
import spock.lang.Specification
import spock.lang.TempDir
import javax.swing.*
import java.awt.event.ActionEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class XlsToCsvPanelSpec extends Specification {
    @TempDir
    File tempDir
    
    XlsToCsvPanel panel
    File testFile
    
    def setup() {
        panel = new XlsToCsvPanel()
        testFile = new File(tempDir, "test.xlsx")
        testFile.text = "dummy content"
    }
    
    def "should initialize with correct default state"() {
        expect: "initial state is correct"
        !panel.convertButton.enabled
        panel.sameLocationRadio.selected
        !panel.customLocationRadio.selected
        !panel.selectOutputButton.enabled
    }
    
    def "should handle file list operations correctly"() {
        when: "adding a file"
        panel.inputFilesModel.addElement(testFile)
        
        then: "file is in the list"
        panel.inputFilesModel.size == 1
        panel.inputFilesModel.getElementAt(0) == testFile
        
        when: "removing the file"
        panel.inputFilesList.setSelectedIndex(0)
        panel.removeSelectedButton.doClick()
        
        then: "list is empty"
        panel.inputFilesModel.size == 0
    }
    
    def "should detect duplicate files"() {
        when: "checking non-existent file"
        def result1 = panel.isFileAlreadyAdded(testFile)
        
        then: "should return false"
        !result1
        
        when: "adding file and checking again"
        panel.inputFilesModel.addElement(testFile)
        def result2 = panel.isFileAlreadyAdded(testFile)
        
        then: "should return true"
        result2
    }
    
    def "should handle output location selection correctly"() {
        expect: "initial state"
        panel.sameLocationRadio.selected
        !panel.selectOutputButton.enabled
        
        when: "switching to custom location"
        panel.customLocationRadio.selected = true
        
        then: "output button is enabled"
        panel.selectOutputButton.enabled
        
        when: "switching back to same location"
        panel.sameLocationRadio.selected = true
        
        then: "output button is disabled"
        !panel.selectOutputButton.enabled
    }
    
    def "should update convert button state correctly"() {
        expect: "initially disabled"
        !panel.convertButton.enabled
        
        when: "adding file with same location"
        panel.inputFilesModel.addElement(testFile)
        
        then: "button is enabled"
        panel.convertButton.enabled
        
        when: "switching to custom location"
        panel.customLocationRadio.selected = true
        
        then: "button is disabled"
        !panel.convertButton.enabled
        
        when: "setting output directory"
        panel.outputDirectory = tempDir
        panel.updateConvertButton()
        
        then: "button is enabled"
        panel.convertButton.enabled
        
        when: "removing file"
        panel.inputFilesModel.clear()
        
        then: "button is disabled"
        !panel.convertButton.enabled
    }
    
    def "should disable controls during conversion"() {
        given: "setup for conversion"
        panel.inputFilesModel.addElement(testFile)
        panel.sameLocationRadio.selected = true
        def latch = new CountDownLatch(1)
        
        when: "starting conversion"
        SwingUtilities.invokeLater {
            panel.convertButton.doClick()
            latch.countDown()
        }
        latch.await(1, TimeUnit.SECONDS)
        
        then: "controls are disabled"
        !panel.selectFilesButton.enabled
        !panel.selectFolderButton.enabled
        !panel.includeSubfoldersCheckbox.enabled
        !panel.sameLocationRadio.enabled
        !panel.customLocationRadio.enabled
        !panel.convertButton.enabled
        !panel.removeSelectedButton.enabled
        !panel.inputFilesList.enabled
    }
    
    def "should handle results table model correctly"() {
        given: "success and failure results"
        def successResult = new ConversionResult(testFile, [new File("output1.csv"), new File("output2.csv")])
        def failureResult = new ConversionResult(testFile, "Test error")
        
        when: "clearing model"
        panel.resultsModel.clear()
        
        then: "table is empty"
        panel.resultsModel.rowCount == 0
        
        when: "adding success result"
        panel.resultsModel.setResults([successResult])
        
        then: "table shows success"
        panel.resultsModel.rowCount == 1
        panel.resultsModel.getValueAt(0, 0) == "test.xlsx"
        panel.resultsModel.getValueAt(0, 1) == "Success"
        panel.resultsModel.getValueAt(0, 2) == "2 CSV file(s) created"
        
        when: "adding failure result"
        panel.resultsModel.setResults([failureResult])
        
        then: "table shows failure"
        panel.resultsModel.rowCount == 1
        panel.resultsModel.getValueAt(0, 0) == "test.xlsx"
        panel.resultsModel.getValueAt(0, 1) == "Failed"
        panel.resultsModel.getValueAt(0, 2) == "Test error"
    }
}
