package com.datmt.tools.helper;


import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class CommonUI {

    public static Dimension inputDimension = new Dimension(300, 34);
    public static Dimension buttonDimension = new Dimension(50, 34);
    public static Dimension comboboxDimension = new Dimension(50, 30);
    public static Dimension comboboxMaxDimension = new Dimension(120, 30);
    public static Dimension labelDimension = new Dimension(Integer.MAX_VALUE, 40);

    public static JComboBox<String> createStringCombobox(String[] options) {
        return new JComboBox<>(options) {
            @Override
            public Dimension getPreferredSize() {
                // Set the maximum height to the preferred height to avoid vertical stretching
                return comboboxDimension;
            }

            @Override
            public Dimension getMaximumSize() {
                return comboboxMaxDimension;
            }
        };
    }

    public static JButton createButton(String text) {
        return new JButton(text) {
            @Override
            public Dimension getPreferredSize() {
                // Set the maximum height to the preferred height to avoid vertical stretching
                return buttonDimension;
            }
        };
    }

    public static JLabel createLabel(String text) {
        return new JLabel(text) {
            @Override
            public Dimension getMaximumSize() {
                return labelDimension;
            }
        };
    }

    public static JLabel createLabel(String text, int size) {
        var label = new JLabel(text) {
            @Override
            public Dimension getMaximumSize() {
                return labelDimension;
            }
        };

        label.setFont(label.getFont().deriveFont((float) size));

        return label;
    }


    public static JTextField createInput() {
        return new JTextField() {
            @Override
            public Dimension getPreferredSize() {
                return inputDimension;
            }
        };
    }

    public static JTextArea createTextArea(boolean editable) {
        var textarea = new JTextArea() {

//            @Override
//            public Dimension getPreferredSize() {
//                return new Dimension(640, 560);
//            }
        };

        textarea.setEditable(editable);
        return textarea;
    }

    public static JTextArea createTextArea(boolean editable, int prefWidth, int prefHeight) {
        var textarea = new JTextArea() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(prefWidth, prefHeight);
            }
        };

        textarea.setEditable(editable);
        return textarea;
    }

    //language:SyntaxConstants
    public static RSyntaxTextArea createCodeEditor(String language) {
        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(language);
        textArea.setCodeFoldingEnabled(true);

        return textArea;
    }

    public static void showDialog(String text, String title, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, text, title, messageType);
        });
    }

    public static void showErrorDialog(String text, String title) {
        showDialog(text, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfoDialog(String text, String title) {
        showDialog(text, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static JScrollPane wrapInScrollPane(JComponent component) {
        var scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(component);
        return scrollPane;
    }

    public static Box wrapInRow(java.util.List<JComponent> componentList) {
        var box = Box.createHorizontalBox();
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setBackground(Color.WHITE);
        for (JComponent component : componentList) {
            box.add(component);
        }
        return box;
    }

    public static JScrollPane wrapInScrollPane(JComponent component, int prefWidth, int prefHeight) {
        var scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(prefWidth, prefHeight);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(prefWidth, Integer.MAX_VALUE);
            }
        };
        scrollPane.setViewportView(component);
        return scrollPane;
    }


    public static JTabbedPane createTabbedPane() {
        return new JTabbedPane() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        };
    }

    public static void addTabsToPanel(Map<String, JPanel> tabs, JTabbedPane tabbedPane) {
        for (Map.Entry<String, JPanel> entry : tabs.entrySet()) {
            tabbedPane.add(entry.getKey(), entry.getValue());
        }
    }

    public static Optional<File> selectDir(String title) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            return Optional.of(chooser.getSelectedFile());
        } else {
            log.error("No file selected");
        }
        return Optional.empty();
    }

    public static void selectDir(String title, Consumer<Optional<File>> consumer) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnValue = chooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            consumer.accept(Optional.of(chooser.getSelectedFile()));
            return;
        } else {
            log.error("No file selected");
        }

        consumer.accept(Optional.empty());
    }
}
