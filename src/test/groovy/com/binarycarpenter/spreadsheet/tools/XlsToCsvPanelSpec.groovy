package com.binarycarpenter.spreadsheet.tools

import com.binarycarpenter.spreadsheet.tools.spreadsheet.ConversionResult
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.IgnoreIf
import java.awt.GraphicsEnvironment
import javax.swing.*

class XlsToCsvPanelSpec extends Specification {
    @TempDir
    File tempDir

    XlsToCsvPanel panel
    File testFile

    def setup() {
        panel = new XlsToCsvPanel()
        testFile = new File(tempDir, "test.xlsx")
        testFile.text = "dummy content"

        // Need to realize the panel to make it valid
        SwingUtilities.invokeAndWait {
            def frame = new JFrame()
            frame.add(panel)
            frame.pack()
        }
    }
    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def "should initialize with correct default state"() {
        expect: "initial state is correct"
        !panel.convertButton.enabled
        panel.sameLocationRadio.selected
        !panel.customLocationRadio.selected
        !panel.selectOutputButton.enabled
    }

    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def "should handle file list operations correctly"() {
        when: "adding a file"
        SwingUtilities.invokeAndWait {
            panel.inputFilesModel.addElement(testFile)
        }

        then: "file is in the list"
        panel.inputFilesModel.size == 1
        panel.inputFilesModel.getElementAt(0) == testFile

        when: "removing the file"
        SwingUtilities.invokeAndWait {
            panel.inputFilesList.setSelectedIndex(0)
            panel.removeSelectedButton.doClick()
        }

        then: "list is empty"
        panel.inputFilesModel.size == 0
    }

    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def "should detect duplicate files"() {
        when: "checking non-existent file"
        def result1 = panel.isFileAlreadyAdded(testFile)

        then: "should return false"
        !result1

        when: "adding file and checking again"
        SwingUtilities.invokeAndWait {
            panel.inputFilesModel.addElement(testFile)
        }
        def result2 = panel.isFileAlreadyAdded(testFile)

        then: "should return true"
        result2
    }

    @IgnoreIf({ GraphicsEnvironment.isHeadless() })
    def "should handle results table model correctly"() {
        given: "success and failure results"
        def successResult = new ConversionResult(testFile, [new File("output1.csv"), new File("output2.csv")])
        def failureResult = new ConversionResult(testFile, "Test error")

        when: "clearing model"
        SwingUtilities.invokeAndWait {
            panel.resultsModel.clear()
        }

        then: "table is empty"
        panel.resultsModel.rowCount == 0

        when: "adding success result"
        SwingUtilities.invokeAndWait {
            panel.resultsModel.setResults([successResult])
        }

        then: "table shows success"
        panel.resultsModel.rowCount == 1
        panel.resultsModel.getValueAt(0, 0) == "test.xlsx"
        panel.resultsModel.getValueAt(0, 1) == "Success"
        panel.resultsModel.getValueAt(0, 2) == "2 CSV file(s) created"

        when: "adding failure result"
        SwingUtilities.invokeAndWait {
            panel.resultsModel.setResults([failureResult])
        }

        then: "table shows failure"
        panel.resultsModel.rowCount == 1
        panel.resultsModel.getValueAt(0, 0) == "test.xlsx"
        panel.resultsModel.getValueAt(0, 1) == "Failed"
        panel.resultsModel.getValueAt(0, 2) == "Test error"
    }
}
