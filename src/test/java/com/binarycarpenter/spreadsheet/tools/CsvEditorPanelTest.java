package com.binarycarpenter.spreadsheet.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class CsvEditorPanelTest {
    private CsvEditorPanel panel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JCheckBox exactMatchCheckBox;
    
    @BeforeEach
    void setUp() {
        panel = new CsvEditorPanel();
        try {
            var tableField = CsvEditorPanel.class.getDeclaredField("table");
            var sorterField = CsvEditorPanel.class.getDeclaredField("sorter");
            var searchField = CsvEditorPanel.class.getDeclaredField("searchField");
            var exactMatchCheckBox = CsvEditorPanel.class.getDeclaredField("exactMatchCheckBox");
            
            tableField.setAccessible(true);
            sorterField.setAccessible(true);
            searchField.setAccessible(true);
            exactMatchCheckBox.setAccessible(true);
            
            table = (JTable) tableField.get(panel);
            sorter = (TableRowSorter<DefaultTableModel>) sorterField.get(panel);
            this.searchField = (JTextField) searchField.get(panel);
            this.exactMatchCheckBox = (JCheckBox) exactMatchCheckBox.get(panel);
        } catch (Exception e) {
            fail("Could not access fields: " + e.getMessage());
        }
    }

    private void triggerSearch() {
        try {
            Method updateSearch = CsvEditorPanel.class.getDeclaredMethod("updateSearch");
            updateSearch.setAccessible(true);
            updateSearch.invoke(panel);
        } catch (Exception e) {
            fail("Could not trigger search: " + e.getMessage());
        }
    }

    private void clearSearchState() {
        try {
            Method clearSearchState = CsvEditorPanel.class.getDeclaredMethod("clearSearchState");
            clearSearchState.setAccessible(true);
            clearSearchState.invoke(panel);
        } catch (Exception e) {
            fail("Could not clear search state: " + e.getMessage());
        }
    }

    @Test
    void testNumericSorting() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Index", "Name"}, 0
                );
                model.addRow(new Object[]{"1", "Alice"});
                model.addRow(new Object[]{"10", "Bob"});
                model.addRow(new Object[]{"2", "Charlie"});
                model.addRow(new Object[]{"100", "David"});
                
                table.setModel(model);
                sorter = new TableRowSorter<>(model);
                table.setRowSorter(sorter);
                
                Comparator<String> numericComparator = new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        try {
                            long n1 = Long.parseLong(s1);
                            long n2 = Long.parseLong(s2);
                            return Long.compare(n1, n2);
                        } catch (NumberFormatException e) {
                            return s1.compareTo(s2);
                        }
                    }
                };
                
                sorter.setComparator(0, numericComparator);
                
                sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
                sorter.sort();
                
                latch.countDown();
            } catch (Exception e) {
                fail("Could not set up test data: " + e.getMessage());
            }
        });
        
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Table update timed out");
        
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("1", table.getValueAt(0, 0), "First row should be 1");
            assertEquals("2", table.getValueAt(1, 0), "Second row should be 2");
            assertEquals("10", table.getValueAt(2, 0), "Third row should be 10");
            assertEquals("100", table.getValueAt(3, 0), "Fourth row should be 100");
        });
    }

    @Test
    void testSearch(@TempDir Path tempDir) throws Exception {
        File testFile = tempDir.resolve("test.csv").toFile();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("Index,Name,Value\n");
            writer.write("1,Test Data,100\n");
            writer.write("2,Another Test,200\n");
            writer.write("3,Final Test,300\n");
        }

        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Index", "Name", "Value"}, 0
                );
                model.addRow(new Object[]{"1", "Test Data", "100"});
                model.addRow(new Object[]{"2", "Another Test", "200"});
                model.addRow(new Object[]{"3", "Final Test", "300"});
                
                table.setModel(model);
                clearSearchState();
                searchField.setText("Test");
                triggerSearch();
                
                var matchingCellsField = CsvEditorPanel.class.getDeclaredField("matchingCells");
                matchingCellsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Point> matchingCells = (List<Point>) matchingCellsField.get(panel);

                assertEquals(3, matchingCells.size(), "Should find 3 matches");
                latch.countDown();
            } catch (Exception e) {
                fail("Search test failed: " + e.getMessage());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Search operation timed out");
    }

    @Test
    void testColumnResizing() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Index", "Name"}, 0
                );
                model.addRow(new Object[]{"1", "Test"});
                table.setModel(model);
                
                TableColumn column = table.getColumnModel().getColumn(0);
                int originalWidth = column.getPreferredWidth();
                column.setPreferredWidth(200);
                
                assertEquals(200, column.getPreferredWidth());
                assertNotEquals(originalWidth, column.getPreferredWidth());
                latch.countDown();
            } catch (Exception e) {
                fail("Column resize test failed: " + e.getMessage());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Column resize timed out");
    }

    @Test
    void testExactMatch() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        SwingUtilities.invokeAndWait(() -> {
            try {
                DefaultTableModel model = new DefaultTableModel(
                    new Object[]{"Index", "Name"}, 0
                );
                model.addRow(new Object[]{"1", "Test"});
                model.addRow(new Object[]{"2", "Testing"});
                model.addRow(new Object[]{"3", "Test Data"});
                table.setModel(model);

                clearSearchState();
                searchField.setText("Test");
                exactMatchCheckBox.setSelected(true);
                triggerSearch();

                var matchingCellsField = CsvEditorPanel.class.getDeclaredField("matchingCells");
                matchingCellsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Point> matchingCells = (List<Point>) matchingCellsField.get(panel);

                assertEquals(1, matchingCells.size(), "Should only find exact matches");
                Point match = matchingCells.get(0);
                assertEquals("Test", table.getValueAt(match.x, match.y), 
                    "Should only match exact 'Test' cell");
                latch.countDown();
            } catch (Exception e) {
                fail("Exact match test failed: " + e.getMessage());
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Exact match test timed out");
    }
}
