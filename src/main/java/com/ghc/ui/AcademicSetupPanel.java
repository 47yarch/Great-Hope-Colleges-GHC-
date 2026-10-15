
package com.ghc.ui;

import com.ghc.database.DatabaseConnection;
import com.ghc.security.AccessControl;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.Collections;
import java.util.regex.Pattern;

final class AcademicSetupPanel extends JPanel {

    private final SetupTable departments = new SetupTable(
        "DEPARTMENTS",
        new String[]{
            "ID", "DEPARTMENT_CODE", "DEPARTMENT_NAME", "DESCRIPTION"
        },
        new String[]{
            "Department code", "Department name", "Description"
        },
        new String[]{
            "DEPARTMENT_CODE", "DEPARTMENT_NAME", "DESCRIPTION"
        }
    );

    private final SetupTable programs = new SetupTable(
        "PROGRAMS",
        new String[]{
            "ID", "PROGRAM_CODE", "PROGRAM_NAME",
            "DEPARTMENT_ID", "DESCRIPTION"
        },
        new String[]{
            "Program code", "Program name",
            "Department ID (optional)", "Description"
        },
        new String[]{
            "PROGRAM_CODE", "PROGRAM_NAME",
            "DEPARTMENT_ID", "DESCRIPTION"
        }
    );

    AcademicSetupPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BEIGE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel header = Theme.pageHeader(
            "ACADEMIC SETUP",
            "Manage college departments and academic programs."
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 15));
        tabs.setBackground(Theme.BEIGE);
        tabs.addTab("  Departments  ", departments);
        tabs.addTab("  Programs  ", programs);

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private static final class SetupTable extends JPanel {

        private final String table;
        private final String[] columns;
        private final String[] labels;
        private final String[] fields;

        private final DefaultTableModel model;
        private final JTable grid;
        private final JTextField searchField = new JTextField(20);
        private final JLabel countLabel = new JLabel("0 records");

        SetupTable(
                String table,
                String[] columns,
                String[] labels,
                String[] fields) {

            this.table = table;
            this.columns = columns;
            this.labels = labels;
            this.fields = fields;

            setLayout(new BorderLayout(10, 12));
            setBackground(Theme.BEIGE);
            setBorder(BorderFactory.createEmptyBorder(14, 10, 10, 10));

            model = new DefaultTableModel(columns, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            grid = new JTable(model);
            Theme.styleTable(grid);

            TableRowSorter<DefaultTableModel> sorter =
                new TableRowSorter<>(model);
            grid.setRowSorter(sorter);

            JPanel toolbar = new JPanel(new BorderLayout(10, 10));
            toolbar.setOpaque(false);

            JPanel searchPanel =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            searchPanel.setOpaque(false);

            JLabel searchLabel = new JLabel("Search records");
            searchLabel.setFont(Theme.LABEL_FONT);

            searchField.setFont(Theme.BODY_FONT);
            searchField.setToolTipText(
                "Search by code, name, department ID, or description");
            searchField.setPreferredSize(new Dimension(240, 36));

            JButton searchButton = Theme.button("Search");
            JButton clearButton = Theme.button("Clear");
            JButton refreshButton = Theme.button("Refresh");

            searchPanel.add(searchLabel);
            searchPanel.add(searchField);
            searchPanel.add(searchButton);
            searchPanel.add(clearButton);
            searchPanel.add(refreshButton);

            JPanel actions =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            actions.setOpaque(false);

            JButton addButton = Theme.button("Add New");
            JButton editButton = Theme.button("Edit Selected");
            JButton deleteButton = Theme.button("Delete Selected");

            actions.add(addButton);
            actions.add(editButton);
            actions.add(deleteButton);

            toolbar.add(searchPanel, BorderLayout.NORTH);
            toolbar.add(actions, BorderLayout.CENTER);

            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);
            countLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            countLabel.setForeground(Theme.DARK_GREEN);
            footer.add(countLabel, BorderLayout.WEST);

            JScrollPane scroll = new JScrollPane(grid);
            scroll.getViewport().setBackground(Theme.WHITE);
            scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

            add(toolbar, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);

            searchButton.addActionListener(e ->
                applySearch(sorter));

            searchField.addActionListener(e ->
                applySearch(sorter));

            clearButton.addActionListener(e -> {
                searchField.setText("");
                applySearch(sorter);
                searchField.requestFocusInWindow();
            });

            refreshButton.addActionListener(e -> load());

            addButton.addActionListener(e -> save(false));
            editButton.addActionListener(e -> save(true));
            deleteButton.addActionListener(e -> delete());

            grid.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    // Selection changes do not modify database records.
                }
            });

            load();
        }

        private void applySearch(
                TableRowSorter<DefaultTableModel> sorter) {

            String query = searchField.getText().trim();

            if (query.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(
                    RowFilter.regexFilter(
                        "(?i)" + Pattern.quote(query)
                    )
                );
            }

            updateCount();
        }

        private void load() {
            try {
                AccessControl.requireAdmin();

                model.setRowCount(0);

                String sql = "SELECT "
                    + String.join(",", columns)
                    + " FROM " + table
                    + " ORDER BY 1";

                try (Connection connection =
                         DatabaseConnection.getConnection();
                     Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(sql)) {

                    while (result.next()) {
                        Object[] row = new Object[columns.length];

                        for (int i = 0; i < columns.length; i++) {
                            row[i] = result.getObject(i + 1);
                        }

                        model.addRow(row);
                    }
                }

                updateCount();

            } catch (Exception ex) {
                showError("Unable to load " + table + ".", ex);
            }
        }

        private void updateCount() {
            int visible = grid.getRowCount();
            int total = model.getRowCount();

            countLabel.setText(
                visible + " visible record(s) • "
                    + total + " total record(s)"
            );
        }

        private void save(boolean update) {
            int id = -1;
            int modelRow = -1;

            if (update) {
                int viewRow = grid.getSelectedRow();

                if (viewRow < 0) {
                    showMessage("Select a record to edit.");
                    return;
                }

                modelRow = grid.convertRowIndexToModel(viewRow);
                id = ((Number) model.getValueAt(modelRow, 0)).intValue();
            }

            JTextField[] inputs = new JTextField[labels.length];

            JPanel form = new JPanel(new GridBagLayout());
            form.setBackground(Theme.BEIGE);
            form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(7, 7, 7, 7);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            for (int i = 0; i < labels.length; i++) {
                JLabel label = new JLabel(labels[i]);
                label.setFont(Theme.LABEL_FONT);

                inputs[i] = new JTextField(24);
                inputs[i].setFont(Theme.BODY_FONT);
                inputs[i].setPreferredSize(new Dimension(300, 36));

                if (update) {
                    int modelColumn = i + 1;

                    if (modelColumn < columns.length) {
                        Object value =
                            model.getValueAt(modelRow, modelColumn);

                        inputs[i].setText(
                            value == null ? "" : String.valueOf(value)
                        );
                    }
                }

                gbc.gridx = 0;
                gbc.gridy = i;
                gbc.weightx = 0;
                form.add(label, gbc);

                gbc.gridx = 1;
                gbc.weightx = 1;
                form.add(inputs[i], gbc);
            }

            if (table.equals("PROGRAMS")) {
                JLabel hint = new JLabel(
                    "Enter an existing department ID, or leave blank."
                );
                hint.setForeground(Theme.MUTED);
                hint.setFont(new Font("SansSerif", Font.ITALIC, 12));

                gbc.gridx = 1;
                gbc.gridy = labels.length;
                form.add(hint, gbc);
            }

            JScrollPane formScroll = new JScrollPane(form);
            formScroll.setPreferredSize(new Dimension(520, 240));
            formScroll.setBorder(BorderFactory.createEmptyBorder());

            int result = JOptionPane.showConfirmDialog(
                this,
                formScroll,
                update ? "Edit " + singularName()
                       : "Add " + singularName(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String[] values = new String[inputs.length];

            for (int i = 0; i < inputs.length; i++) {
                values[i] = inputs[i].getText().trim();

                if (i < 2 && values[i].isEmpty()) {
                    showMessage(labels[i] + " is required.");
                    inputs[i].requestFocusInWindow();
                    return;
                }
            }

            if (table.equals("PROGRAMS")
                    && !values[2].isEmpty()) {
                try {
                    int departmentId = Integer.parseInt(values[2]);

                    if (departmentId <= 0) {
                        showMessage(
                            "Department ID must be a positive number.");
                        return;
                    }

                    if (!departmentExists(departmentId)) {
                        showMessage(
                            "That department ID does not exist. "
                            + "Check the Departments tab first.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showMessage(
                        "Department ID must be a whole number, "
                        + "or leave it blank.");
                    return;
                } catch (SQLException ex) {
                    showError("Unable to validate department.", ex);
                    return;
                }
            }

            try {
                AccessControl.requireAdmin();

                String sql;

                if (update) {
                    StringBuilder builder =
                        new StringBuilder("UPDATE " + table + " SET ");

                    for (int i = 0; i < fields.length; i++) {
                        if (i > 0) {
                            builder.append(", ");
                        }
                        builder.append(fields[i]).append(" = ?");
                    }

                    builder.append(" WHERE ID = ?");
                    sql = builder.toString();

                } else {
                    sql = "INSERT INTO " + table
                        + " (" + String.join(",", fields) + ") VALUES ("
                        + String.join(",",
                            Collections.nCopies(fields.length, "?"))
                        + ")";
                }

                try (Connection connection =
                         DatabaseConnection.getConnection();
                     PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];

                        if (value.isEmpty()) {
                            int sqlType =
                                fields[i].equals("DEPARTMENT_ID")
                                    ? Types.INTEGER : Types.VARCHAR;

                            statement.setNull(i + 1, sqlType);

                        } else if (fields[i].equals("DEPARTMENT_ID")) {
                            statement.setInt(i + 1,
                                Integer.parseInt(value));

                        } else {
                            statement.setString(i + 1, value);
                        }
                    }

                    if (update) {
                        statement.setInt(values.length + 1, id);
                    }

                    statement.executeUpdate();
                }

                load();

                JOptionPane.showMessageDialog(
                    this,
                    singularName() + (update
                        ? " updated successfully."
                        : " added successfully."),
                    "GHC Academic Setup",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (SQLIntegrityConstraintViolationException ex) {
                showError(
                    "This record may already exist or be referenced "
                    + "by another record. Check the code and related "
                    + "academic records before trying again.", ex
                );
            } catch (Exception ex) {
                showError(
                    "Unable to " + (update ? "update" : "add")
                        + " " + singularName() + ".", ex
                );
            }
        }

        private boolean departmentExists(int departmentId)
                throws SQLException {

            try (Connection connection =
                     DatabaseConnection.getConnection();
                 PreparedStatement statement =
                     connection.prepareStatement(
                         "SELECT ID FROM DEPARTMENTS WHERE ID = ?")) {

                statement.setInt(1, departmentId);

                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        }

        private void delete() {
            int viewRow = grid.getSelectedRow();

            if (viewRow < 0) {
                showMessage("Select a record to delete.");
                return;
            }

            int modelRow = grid.convertRowIndexToModel(viewRow);
            int id = ((Number) model.getValueAt(modelRow, 0)).intValue();

            String displayName =
                String.valueOf(model.getValueAt(modelRow, 1));

            int answer = JOptionPane.showConfirmDialog(
                this,
                "Delete " + singularName() + " '" + displayName + "'?\n\n"
                    + "The database may reject deletion if this record "
                    + "is referenced by other academic records.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                AccessControl.requireAdmin();

                try (Connection connection =
                         DatabaseConnection.getConnection();
                     PreparedStatement statement =
                         connection.prepareStatement(
                             "DELETE FROM " + table + " WHERE ID = ?")) {

                    statement.setInt(1, id);

                    int affected = statement.executeUpdate();

                    if (affected == 0) {
                        showMessage(
                            "The record was not found. Refresh and try again.");
                    } else {
                        load();
                        showMessage(
                            singularName() + " deleted successfully.");
                    }
                }

            } catch (SQLIntegrityConstraintViolationException ex) {
                showError(
                    "This record cannot be deleted because another "
                    + "academic record references it. Remove or reassign "
                    + "those references first.", ex
                );
            } catch (Exception ex) {
                showError(
                    "Unable to delete " + singularName() + ".", ex
                );
            }
        }

        private String singularName() {
            return table.equals("DEPARTMENTS")
                ? "Department" : "Program";
        }

        private void showMessage(String message) {
            JOptionPane.showMessageDialog(
                this,
                message,
                "GHC Academic Setup",
                JOptionPane.INFORMATION_MESSAGE
            );
        }

        private void showError(String message, Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                message + "\n\nDetails: " + ex.getMessage(),
                "GHC Academic Setup",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
