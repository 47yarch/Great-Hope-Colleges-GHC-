package com.ghc.ui;

import com.ghc.dao.StudentPortalDAO;
import com.ghc.reports.ReportCardPdf;
import com.ghc.security.AccessControl;
import com.ghc.security.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Student self-service portal:
 * - View available class sections
 * - Request enrollment
 * - Check registration status
 * - Download a PDF grade report
 */
public final class StudentPortalPanel extends JPanel {

    private final StudentPortalDAO dao = new StudentPortalDAO();
    private final UserSession user = AccessControl.requireSignedIn();

    private final DefaultTableModel sectionModel = createModel(
            "ID", "Section", "Course Code", "Subject", "Units",
            "School Year", "Semester", "Capacity", "Enrolled"
    );

    private final DefaultTableModel enrollmentModel = createModel(
            "ID", "Course Code", "Subject", "Section",
            "School Year", "Semester", "Status", "Grade", "Remarks"
    );

    private final JTable sectionsTable = new JTable(sectionModel);
    private final JTable enrollmentsTable = new JTable(enrollmentModel);

    public StudentPortalPanel() {
        if (!user.isStudent()) {
            throw new SecurityException("Student accounts only.");
        }

        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BEIGE);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(createHeader(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);

        loadSections();
        loadEnrollments();
    }

    
private JPanel createHeader() {
    JPanel header = new JPanel(new BorderLayout(0, 12));
    header.setOpaque(false);
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

    // Welcome banner.
    JPanel banner = new JPanel(new BorderLayout(12, 8));
    banner.setBackground(Theme.GREEN);
    banner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.GREEN, 1, true),
            BorderFactory.createEmptyBorder(18, 22, 18, 22)
    ));

    JPanel welcomeText = new JPanel();
    welcomeText.setOpaque(false);
    welcomeText.setLayout(new BoxLayout(welcomeText, BoxLayout.Y_AXIS));

    JLabel eyebrow = new JLabel("GREAT HOPE COLLEGES  |  STUDENT PORTAL");
    eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
    eyebrow.setForeground(Theme.YELLOW);
    eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel title = new JLabel("Welcome to your Student Portal");
    title.setFont(new Font("SansSerif", Font.BOLD, 23));
    title.setForeground(Color.WHITE);
    title.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel subtitle = new JLabel(
            "Manage your enrollment and keep track of your academic progress."
    );
    subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
    subtitle.setForeground(Color.WHITE);
    subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

    welcomeText.add(eyebrow);
    welcomeText.add(Box.createVerticalStrut(7));
    welcomeText.add(title);
    welcomeText.add(Box.createVerticalStrut(5));
    welcomeText.add(subtitle);

    banner.add(welcomeText, BorderLayout.CENTER);
    header.add(banner, BorderLayout.NORTH);

    // Student information card.
    JPanel profileCard = new JPanel(new GridLayout(0, 2, 12, 8));
    profileCard.setBackground(Color.WHITE);
    profileCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
    ));

    try {
        Object[] info = dao.studentInfo();

        addProfileDetail(profileCard, "STUDENT NUMBER",
                String.valueOf(info[0]));
        addProfileDetail(profileCard, "FULL NAME",
                String.valueOf(info[1]));
        addProfileDetail(profileCard, "PROGRAM",
                String.valueOf(info[2]));
        addProfileDetail(profileCard, "YEAR LEVEL",
                String.valueOf(info[3]));

    } catch (Exception ex) {
        JLabel error = new JLabel(
                "Student profile unavailable: " + ex.getMessage()
        );
        error.setForeground(Color.RED);
        profileCard.add(error);
    }

    header.add(profileCard, BorderLayout.CENTER);

    return header;
}

private void addProfileDetail(
        JPanel panel, String labelText, String valueText) {

    JPanel detail = new JPanel();
    detail.setOpaque(false);
    detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));

    JLabel label = new JLabel(labelText);
    label.setFont(new Font("SansSerif", Font.BOLD, 10));
    label.setForeground(Theme.MUTED);

    JLabel value = new JLabel(valueText);
    value.setFont(new Font("SansSerif", Font.BOLD, 13));
    value.setForeground(Theme.GREEN);

    detail.add(label);
    detail.add(Box.createVerticalStrut(4));
    detail.add(value);

    panel.add(detail);
}


    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Available sections and enrollment requests
        JPanel requestPanel = new JPanel(new BorderLayout(8, 8));
        requestPanel.setBackground(Theme.BEIGE);

        JPanel requestControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        requestControls.setOpaque(false);

        JButton refreshSections = new JButton("Refresh Sections");
        JButton requestEnrollment = Theme.button("Request Enrollment");

        requestControls.add(refreshSections);
        requestControls.add(requestEnrollment);

        Theme.styleTable(sectionsTable);

        requestPanel.add(requestControls, BorderLayout.NORTH);
        requestPanel.add(new JScrollPane(sectionsTable), BorderLayout.CENTER);

        refreshSections.addActionListener(e -> loadSections());
        requestEnrollment.addActionListener(e -> requestEnrollment());

        // Tab 2: Student's own requests and registration status
        JPanel statusPanel = new JPanel(new BorderLayout(8, 8));
        statusPanel.setBackground(Theme.BEIGE);

        JPanel statusControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusControls.setOpaque(false);

        JButton refreshStatus = Theme.button("Refresh Status");
        JButton downloadReport = Theme.button("Download Grade Report (PDF)");

        statusControls.add(refreshStatus);
        statusControls.add(downloadReport);

        Theme.styleTable(enrollmentsTable);

        statusPanel.add(statusControls, BorderLayout.NORTH);
        statusPanel.add(new JScrollPane(enrollmentsTable), BorderLayout.CENTER);

        refreshStatus.addActionListener(e -> loadEnrollments());
        downloadReport.addActionListener(e -> downloadGradeReport());

        tabs.addTab("Available Sections / Request Enrollment", requestPanel);
        tabs.addTab("My Registration Status", statusPanel);

        return tabs;
    }

    private static DefaultTableModel createModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void loadSections() {
        sectionModel.setRowCount(0);

        try {
            for (Object[] row : dao.availableSections()) {
                sectionModel.addRow(row);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void loadEnrollments() {
        enrollmentModel.setRowCount(0);

        try {
            for (Object[] row : dao.myEnrollments()) {
                enrollmentModel.addRow(row);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void requestEnrollment() {
        int selectedRow = sectionsTable.getSelectedRow();

        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a class section first.",
                    "No Section Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = sectionsTable.convertRowIndexToModel(selectedRow);
        int sectionId = ((Number) sectionModel.getValueAt(modelRow, 0)).intValue();

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Submit an enrollment request for the selected section?\n"
                + "Your request will remain pending until reviewed.",
                "Confirm Enrollment Request",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            dao.requestEnrollment(sectionId);

            loadSections();
            loadEnrollments();

            JOptionPane.showMessageDialog(
                    this,
                    "Your enrollment request was submitted.\n"
                    + "Check the My Registration Status tab for updates.",
                    "Request Submitted",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void downloadGradeReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save GHC Grade Report");
        chooser.setSelectedFile(
                new java.io.File("GHC_Grade_Report.pdf")
        );

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Object[] studentInfo = dao.studentInfo();
            List<Object[]> grades = dao.gradeReport();
            List<ReportCardPdf.CourseGrade> courses = new ArrayList<>();

            String schoolYear = "Multiple / Current";
            String semester = "Multiple / Current";

            if (!grades.isEmpty()) {
                schoolYear = String.valueOf(grades.get(0)[5]);
                semester = String.valueOf(grades.get(0)[6]);

                for (Object[] row : grades) {
                    String grade;

                    if (row[3] == null) {
                        grade = "Not released";
                    } else {
                        grade = String.valueOf(
                                row[4] == null ? row[3] : row[4]
                        );
                    }

                    courses.add(
                            new ReportCardPdf.CourseGrade(
                                    String.valueOf(row[0]),
                                    String.valueOf(row[1]),
                                    grade,
                                    ((Number) row[2]).intValue()
                            )
                    );
                }
            }

            ReportCardPdf.generate(
                    Path.of(chooser.getSelectedFile().getAbsolutePath()),
                    "GREAT HOPE COLLEGES",
                    String.valueOf(studentInfo[1]),
                    String.valueOf(studentInfo[0]),
                    String.valueOf(studentInfo[2]),
                    String.valueOf(studentInfo[3]),
                    schoolYear,
                    semester,
                    courses
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Grade report saved successfully.",
                    "Report Downloaded",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "Unable to complete the action:\n" + ex.getMessage(),
                "GHC Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}