package com.ghc.ui;

import com.ghc.dao.StudentPortalDAO;
import com.ghc.security.AccessControl;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Allows Admin and Registrar users to review enrollment requests.
 */
public final class EnrollmentRequestsPanel extends JPanel {

    private final StudentPortalDAO dao = new StudentPortalDAO();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{
                    "ID",
                    "Student No.",
                    "Student Name",
                    "Course",
                    "Section",
                    "School Year",
                    "Semester",
                    "Status"
            },
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable requestsTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);

    public EnrollmentRequestsPanel() {
        // Reject unauthorized roles before displaying the review queue.
        AccessControl.requireAdminOrRegistrar();

        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.BEIGE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(createToolbar(), BorderLayout.NORTH);

        Theme.styleTable(requestsTable);
        add(new JScrollPane(requestsTable), BorderLayout.CENTER);

        loadRequests();
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);

        toolbar.add(new JLabel("Find request:"));
        toolbar.add(searchField);

        JButton searchButton = Theme.button("Search");
        JButton refreshButton = Theme.button("Refresh");
        JButton approveButton = Theme.button("Approve");
        JButton rejectButton = Theme.button("Reject");

        toolbar.add(searchButton);
        toolbar.add(refreshButton);
        toolbar.add(approveButton);
        toolbar.add(rejectButton);

        searchButton.addActionListener(e -> loadRequests());
        searchField.addActionListener(e -> loadRequests());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadRequests();
        });

        approveButton.addActionListener(e -> reviewRequest(true));
        rejectButton.addActionListener(e -> reviewRequest(false));

        return toolbar;
    }

    
private void loadRequests() {
    try {
        java.util.List<Object[]> requests =
                dao.pendingRequests(searchField.getText().trim());

        tableModel.setRowCount(0);

        for (Object[] request : requests) {
            tableModel.addRow(request);
        }

    } catch (Exception ex) {
        showMessage(
                "Could not load enrollment requests:\n" + ex.getMessage(),
                true
        );
    }
}


    private void reviewRequest(boolean approve) {
        int selectedRow = requestsTable.getSelectedRow();

        if (selectedRow < 0) {
            showMessage("Please select an enrollment request first.", false);
            return;
        }

        int modelRow = requestsTable.convertRowIndexToModel(selectedRow);
        int requestId = ((Number) tableModel.getValueAt(modelRow, 0)).intValue();

        String action = approve ? "approve" : "reject";

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action + " this request?",
                "Confirm Review",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dao.review(requestId, approve);
            loadRequests();

            showMessage(
                    "Enrollment request " + (approve ? "approved" : "rejected") + ".",
                    false
            );

        } catch (Exception ex) {
            showMessage(
                    "Could not review the enrollment request:\n" + ex.getMessage(),
                    true
            );
        }
    }

    private void showMessage(String message, boolean error) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "GHC Enrollment Requests",
                error
                        ? JOptionPane.ERROR_MESSAGE
                        : JOptionPane.INFORMATION_MESSAGE
        );
    }
}