package com.ghc.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class Theme {

    private Theme() {
        // Utility class.
    }

    /**
     * Applies the system look and feel.
     * Used by LoginFrame.java through Theme.apply().
     */
    public static void apply() {
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception e) {
            System.err.println(
                "Unable to apply system look and feel: "
                + e.getMessage()
            );
        }
    }

    // GHC brand colors
    public static final Color BEIGE =
        new Color(246, 241, 225);

    public static final Color GREEN =
        new Color(45, 105, 75);

    public static final Color DARK_GREEN =
        new Color(28, 72, 51);

    public static final Color YELLOW =
        new Color(230, 190, 65);

    public static final Color WHITE =
        Color.WHITE;

    public static final Color TEXT =
        new Color(42, 48, 42);

    public static final Color MUTED =
        new Color(105, 111, 103);

    public static final Color BORDER =
        new Color(218, 211, 193);

    public static final Color ROW_ALT =
        new Color(250, 247, 237);

    // Shared fonts
    public static final Font BODY_FONT =
        new Font("SansSerif", Font.PLAIN, 14);

    public static final Font LABEL_FONT =
        new Font("SansSerif", Font.BOLD, 14);

    public static final Font BUTTON_FONT =
        new Font("SansSerif", Font.BOLD, 14);

    public static final Font TABLE_FONT =
        new Font("SansSerif", Font.PLAIN, 14);

    public static final Font TABLE_HEADER_FONT =
        new Font("SansSerif", Font.BOLD, 14);

    public static final Font PAGE_TITLE_FONT =
        new Font("SansSerif", Font.BOLD, 24);

    /**
     * Applies the general GHC appearance to a Swing component tree.
     */
    public static void apply(Component component) {
        if (component instanceof JComponent jc) {
            jc.setFont(BODY_FONT);
            jc.setForeground(TEXT);
        }

        if (component instanceof JPanel panel) {
            panel.setBackground(BEIGE);
        }

        if (component instanceof JLabel label) {
            label.setForeground(TEXT);
        }

        if (component instanceof JTextField field) {
            field.setBackground(WHITE);
            field.setForeground(TEXT);
            field.setCaretColor(DARK_GREEN);
        }

        if (component instanceof JTextArea area) {
            area.setBackground(WHITE);
            area.setForeground(TEXT);
            area.setCaretColor(DARK_GREEN);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                apply(child);
            }
        }
    }

    /**
     * Creates a consistently styled GHC button.
     */
    public static JButton button(String text) {
        JButton button = new JButton(text);

        button.setFont(BUTTON_FONT);
        button.setForeground(WHITE);
        button.setBackground(DARK_GREEN);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DARK_GREEN),
                new EmptyBorder(9, 15, 9, 15)
            )
        );

        return button;
    }

    /**
     * Applies consistent font, spacing, colors, and row heights to tables.
     */
    public static void styleTable(JTable table) {
        table.setFont(TABLE_FONT);
        table.setForeground(TEXT);
        table.setBackground(WHITE);
        table.setSelectionBackground(
            new Color(216, 231, 216)
        );
        table.setSelectionForeground(DARK_GREEN);
        table.setGridColor(BORDER);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        
JTableHeader header = table.getTableHeader();
header.setFont(TABLE_HEADER_FONT);
header.setForeground(Color.BLACK);
header.setBackground(DARK_GREEN);
header.setReorderingAllowed(false);
header.setPreferredSize(new Dimension(100, 36));


        table.setBorder(
            BorderFactory.createLineBorder(BORDER)
        );
    }

    /**
     * Creates a standard GHC page title.
     */
    public static JLabel pageTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(PAGE_TITLE_FONT);
        label.setForeground(DARK_GREEN);
        return label;
    }

    /**
     * Creates a shared page heading with title, subtitle, and gold accent.
     */
    public static JPanel pageHeader(
            String title,
            String subtitle) {

        JPanel header = new JPanel(new BorderLayout(0, 7));
        header.setBackground(BEIGE);
        header.setBorder(
            BorderFactory.createEmptyBorder(0, 0, 10, 0)
        );

        JPanel textPanel = new JPanel();
        textPanel.setLayout(
            new BoxLayout(textPanel, BoxLayout.Y_AXIS)
        );
        textPanel.setBackground(BEIGE);

        JLabel titleLabel = pageTitle(title);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(BODY_FONT);
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel accent = new JPanel();
        accent.setBackground(YELLOW);
        accent.setPreferredSize(new Dimension(100, 4));
        accent.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, 4)
        );
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subtitleLabel);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(accent);

        header.add(textPanel, BorderLayout.CENTER);

        return header;
    }
}
