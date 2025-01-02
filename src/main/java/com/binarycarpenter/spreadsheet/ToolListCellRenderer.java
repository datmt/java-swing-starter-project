package com.binarycarpenter.spreadsheet;

import javax.swing.*;
import java.awt.*;

public class ToolListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        // Add some padding
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // You can customize the appearance further here
        if (value instanceof Tool) {
            Tool tool = (Tool) value;
            label.setText(tool.getName());
        }
        
        return label;
    }
}
