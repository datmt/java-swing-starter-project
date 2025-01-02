package com.binarycarpenter.spreadsheet.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class Icons {
    public static final Icon OPEN = createIcon("folder-open", 16);
    public static final Icon SAVE = createIcon("save", 16);
    public static final Icon EXPORT = createIcon("export", 16);
    public static final Icon FILTER = createIcon("filter", 16);
    public static final Icon FIND = createIcon("search", 16);
    public static final Icon REPLACE = createIcon("find-replace", 16);
    public static final Icon REPLACE_ALL = createIcon("replace-all", 16);

    private static Icon createIcon(String name, int size) {
        try {
            String iconPath = "/icons/" + name + ".png";
            InputStream is = Icons.class.getResourceAsStream(iconPath);
            if (is == null) {
                System.err.println("Could not find icon: " + iconPath);
                return createEmptyIcon(name, size);
            }

            try {
                Image image = ImageIO.read(is);
                if (image == null) {
                    System.err.println("Could not read icon image: " + iconPath);
                    return createEmptyIcon(name, size);
                }

                if (image.getWidth(null) != size) {
                    image = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                }
                return new ImageIcon(image);
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                    // Ignore close errors
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading icon '" + name + "': " + e.getMessage());
            return createEmptyIcon(name, size);
        }
    }

    private static Icon createEmptyIcon(String name, int size) {
        return new EmptyIcon(name, size);
    }

    private static class EmptyIcon implements Icon {
        private final int size;
        private final String name;

        public EmptyIcon(String name, int size) {
            this.name = name;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw border
            g2.setColor(Color.GRAY);
            g2.drawRect(x, y, size - 1, size - 1);

            // Draw first character of icon name
            if (name != null && !name.isEmpty()) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size - 4));
                FontMetrics fm = g2.getFontMetrics();
                String text = name.substring(0, 1).toUpperCase();
                int textX = x + (size - fm.stringWidth(text)) / 2;
                int textY = y + (size + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
