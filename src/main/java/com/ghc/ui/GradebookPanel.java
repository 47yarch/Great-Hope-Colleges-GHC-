
package com.ghc.ui;

import com.ghc.dao.GradeDAO;
import com.ghc.database.DatabaseConnection;
import com.ghc.grading.GradingCalculator;
import com.ghc.reports.ReportCardPdf;
import com.ghc.security.AccessControl;
import com.ghc.security.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.nio.file.Path;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

final class GradebookPanel extends JPanel {

    private final UserSession user;

    private final DefaultTableModel model =
        new DefaultTableModel(
            new String[]{
                "Enrollment ID", "Student No.", "Student", "Course",
                "Section", "School Year", "Semester",
                "Final Raw Grade", "Equivalent"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(18);

    GradebookPanel(UserSession user) {
        this.user = user;

        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BEIGE);
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        top.setBackground(Theme.BEIGE);

        JLabel searchLabel = new JLabel("Find student or course:");
        searchLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        top.add(searchLabel);
        top.add(search);

        JButton find = Theme.button("Search");
        JButton refresh = Theme.button("Refresh");
        JButton enter = Theme.button("Enter / Edit Grades");
        JButton export = Theme.button("Export Report Card PDF");
        JButton print = Theme.button("Print Gradebook");

        top.add(find);
        top.add(refresh);
        top.add(enter);
        top.add(export);
        top.add(print);

        add(top, BorderLayout.NORTH);

        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        find.addActionListener(e -> load());
        search.addActionListener(e -> load());

        refresh.addActionListener(e -> {
            search.setText("");
            load();
        });

        enter.addActionListener(e -> editGrade());
        export.addActionListener(e -> exportReportCard());
        print.addActionListener(e -> printGradebook());

        load();
    }

    private void load() {
        model.setRowCount(0);

        String sql = """
            SELECT E.ID, S.STUDENT_NO,
                   S.FIRST_NAME || ' ' || S.LAST_NAME AS STUDENT_NAME,
                   C.COURSE_CODE, CS.SECTION_CODE,
                   E.SCHOOL_YEAR, E.SEMESTER,
                   E.GRADE, E.GRADE_EQUIVALENT
            FROM ENROLLMENTS E
            JOIN STUDENTS S ON E.STUDENT_ID = S.ID
            JOIN COURSES C ON E.COURSE_ID = C.ID
            LEFT JOIN CLASS_SECTIONS CS ON E.SECTION_ID = CS.ID
            WHERE (
                LOWER(S.STUDENT_NO) LIKE ?
                OR LOWER(S.FIRST_NAME) LIKE ?
                OR LOWER(S.LAST_NAME) LIKE ?
                OR LOWER(C.COURSE_CODE) LIKE ?
                OR LOWER(C.COURSE_NAME) LIKE ?
            )
            """ + (user.isInstructor()
                ? " AND CS.INSTRUCTOR_USER_ID = ? " : "")
                + " ORDER BY E.SCHOOL_YEAR DESC, S.LAST_NAME, S.FIRST_NAME";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            String q = "%" + search.getText().trim().toLowerCase() + "%";

            for (int i = 1; i <= 5; i++) {
                p.setString(i, q);
            }

            if (user.isInstructor()) {
                p.setInt(6, user.getUserId());
            }

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Object grade = r.getObject("GRADE");

                    model.addRow(new Object[]{
                        r.getInt("ID"),
                        r.getString("STUDENT_NO"),
                        r.getString("STUDENT_NAME"),
                        r.getString("COURSE_CODE"),
                        r.getString("SECTION_CODE") == null
                            ? "Not assigned" : r.getString("SECTION_CODE"),
                        r.getString("SCHOOL_YEAR"),
                        r.getString("SEMESTER"),
                        grade == null ? "Pending" : r.getDouble("GRADE"),
                        r.getString("GRADE_EQUIVALENT") == null
                            ? "—" : r.getString("GRADE_EQUIVALENT")
                    });
                }
            }
        } catch (Exception ex) {
            showError("Unable to load the gradebook.", ex);
        }
    }

    
private void editGrade() {
    int viewRow = table.getSelectedRow();

    if (viewRow < 0) {
        message("Select an enrolled student first.");
        return;
    }

    int row = table.convertRowIndexToModel(viewRow);
    int enrollmentId = ((Number) model.getValueAt(row, 0)).intValue();
    int sectionId;

    try (Connection c = DatabaseConnection.getConnection();
         PreparedStatement p = c.prepareStatement(
             "SELECT SECTION_ID FROM ENROLLMENTS WHERE ID = ?")) {

        p.setInt(1, enrollmentId);

        try (ResultSet r = p.executeQuery()) {
            if (!r.next() || r.getObject(1) == null) {
                message("Assign this enrollment to a class section first.");
                return;
            }
            sectionId = r.getInt(1);
        }

        if (user.isInstructor()) {
            try (PreparedStatement p2 = c.prepareStatement("""
                SELECT COUNT(*)
                FROM CLASS_SECTIONS
                WHERE ID = ? AND INSTRUCTOR_USER_ID = ? AND ACTIVE = 1
                """)) {
                p2.setInt(1, sectionId);
                p2.setInt(2, user.getUserId());

                try (ResultSet r = p2.executeQuery()) {
                    r.next();
                    if (r.getInt(1) == 0) {
                        throw new SecurityException(
                            "You may only grade your assigned active classes.");
                    }
                }
            }
        } else {
            AccessControl.requireAdmin();
        }

    } catch (Exception ex) {
        showError("Grade access check failed.", ex);
        return;
    }

    String[] keys = {
        "LEC_WW", "LEC_UNIT_TEST", "LEC_TERM_TEST",
        "LAB_WW", "LAB_UNIT_TEST", "LAB_TERM_TEST",
        "LAB_PERFORMANCE"
    };

    String[] labels = {
        "Lecture — Written works (30%)",
        "Lecture — Unit test (40%)",
        "Lecture — Term test (30%)",
        "Laboratory — Written works (20%)",
        "Laboratory — Unit test (25%)",
        "Laboratory — Term test (20%)",
        "Laboratory — Performance tasks (35%)"
    };

    try {
        Map<String, Double> saved = readScores(enrollmentId);
        JTextField[] fields = new JTextField[keys.length];

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBackground(Theme.BEIGE);

        JCheckBox hasLab =
            new JCheckBox("This course has a laboratory component", true);
        hasLab.setBackground(Theme.BEIGE);

        for (int i = 0; i < keys.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("SansSerif", Font.PLAIN, 14));

            fields[i] = new JTextField(
                saved.containsKey(keys[i])
                    ? String.valueOf(saved.get(keys[i])) : ""
            );

            form.add(label);
            form.add(fields[i]);
        }

        form.add(new JLabel("Final grade formula"));
        form.add(hasLab);

        // Disable and clear laboratory fields when the checkbox is unticked.
        Runnable updateLaboratoryFields = () -> {
            boolean enabled = hasLab.isSelected();

            for (int i = 3; i < fields.length; i++) {
                fields[i].setEnabled(enabled);
                fields[i].setEditable(enabled);

                if (!enabled) {
                    fields[i].setText("");
                }
            }
        };

        hasLab.addActionListener(e -> updateLaboratoryFields.run());
        updateLaboratoryFields.run();

        int result = JOptionPane.showConfirmDialog(
            this,
            new JScrollPane(form),
            "Grade Components — " + model.getValueAt(row, 2),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        double[] scores = new double[7];

        // Lecture components are always required.
        for (int i = 0; i < 3; i++) {
            scores[i] = parseScore(fields[i].getText(), labels[i]);
        }

        // Laboratory components are required only if the course has lab.
        for (int i = 3; i < 7; i++) {
            if (hasLab.isSelected()) {
                scores[i] = parseScore(fields[i].getText(), labels[i]);
            } else {
                scores[i] = 0.0;
            }
        }

        double lecture = GradingCalculator.calculateLecture(
            scores[0], scores[1], scores[2]);

        double laboratory = hasLab.isSelected()
            ? GradingCalculator.calculateLaboratory(
                scores[3], scores[4], scores[5], scores[6])
            : 0.0;

        double raw = GradingCalculator.finalRawGrade(
            lecture, laboratory, hasLab.isSelected());

        Map<String, Double> submitted = new LinkedHashMap<>();

        for (int i = 0; i < 3; i++) {
            submitted.put(keys[i], scores[i]);
        }

        if (hasLab.isSelected()) {
            for (int i = 3; i < 7; i++) {
                submitted.put(keys[i], scores[i]);
            }
        }

        new GradeDAO().saveCalculatedGrade(
            enrollmentId, submitted, hasLab.isSelected());

        message("Grade saved successfully.\n"
            + "Lecture grade: " + lecture + "\n"
            + "Laboratory grade: "
            + (hasLab.isSelected()
                ? String.valueOf(laboratory) : "Not applicable") + "\n"
            + "Final raw grade: " + raw + "\n"
            + "Equivalent: " + GradingCalculator.equivalentGrade(raw));

        load();

    } catch (Exception ex) {
        showError("Unable to save the grade.", ex);
    }
}

private double parseScore(String input, String label) {
    String value = input.trim();

    if (value.isEmpty()) {
        throw new IllegalArgumentException(
            "Enter a score for " + label + " (0–100).");
    }

    double score;

    try {
        score = Double.parseDouble(value);
    } catch (NumberFormatException ex) {
        throw new IllegalArgumentException(
            "Enter a valid number for " + label + ".");
    }

    if (!Double.isFinite(score) || score < 0 || score > 100) {
        throw new IllegalArgumentException(
            label + " must be between 0 and 100.");
    }

    return score;
}


    private Map<String, Double> readScores(int enrollmentId)
            throws SQLException {

        Map<String, Double> scores = new LinkedHashMap<>();

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT COMPONENT_TYPE, SCORE
                 FROM GRADE_COMPONENTS
                 WHERE ENROLLMENT_ID = ?
                 """)) {

            p.setInt(1, enrollmentId);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    scores.put(r.getString(1), r.getDouble(2));
                }
            }
        }

        return scores;
    }

    /**
     * Exports a report card for the selected student and academic term.
     * All displayed subjects and grades are read from Derby.
     */
    private void exportReportCard() {
        int viewRow = table.getSelectedRow();

        if (viewRow < 0) {
            message("Select a student's enrollment row first.");
            return;
        }

        int row = table.convertRowIndexToModel(viewRow);
        int enrollmentId =
            ((Number) model.getValueAt(row, 0)).intValue();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save GHC Report Card PDF");
        chooser.setSelectedFile(
            new java.io.File("GHC_Report_Card.pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File selectedFile = chooser.getSelectedFile();
        String path = selectedFile.getAbsolutePath();

        if (!path.toLowerCase().endsWith(".pdf")) {
            path += ".pdf";
        }

        try {
            exportSelectedEnrollment(enrollmentId, Path.of(path));
            message("Report card PDF created:\n" + path);

            int open = JOptionPane.showConfirmDialog(
                this,
                "Open the generated PDF now?",
                "Report Card Ready",
                JOptionPane.YES_NO_OPTION
            );

            if (open == JOptionPane.YES_OPTION
                    && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new java.io.File(path));
            }

        } catch (Exception ex) {
            showError("Unable to export the report card PDF.", ex);
        }
    }

    private void exportSelectedEnrollment(int enrollmentId, Path output)
            throws Exception {

        String studentName;
        String studentNumber;
        String program;
        String yearLevel;
        String schoolYear;
        String semester;
        int studentId;

        String selectedSql = """
            SELECT S.ID, S.STUDENT_NO,
                   S.FIRST_NAME || ' ' || S.LAST_NAME AS STUDENT_NAME,
                   S.PROGRAM, S.YEAR_LEVEL,
                   E.SCHOOL_YEAR, E.SEMESTER
            FROM ENROLLMENTS E
            JOIN STUDENTS S ON E.STUDENT_ID = S.ID
            LEFT JOIN CLASS_SECTIONS CS ON E.SECTION_ID = CS.ID
            WHERE E.ID = ?
            """ + (user.isInstructor()
                ? " AND CS.INSTRUCTOR_USER_ID = ? " : "");

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(selectedSql)) {

            p.setInt(1, enrollmentId);
            if (user.isInstructor()) {
                p.setInt(2, user.getUserId());
            }

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new SecurityException(
                        "The selected enrollment was not found or you do not have access.");
                }

                studentId = r.getInt("ID");
                studentNumber = r.getString("STUDENT_NO");
                studentName = r.getString("STUDENT_NAME");
                program = r.getString("PROGRAM");
                yearLevel = r.getString("YEAR_LEVEL");
                schoolYear = r.getString("SCHOOL_YEAR");
                semester = r.getString("SEMESTER");
            }

            java.util.List<ReportCardPdf.CourseGrade> courses =
                new java.util.ArrayList<>();

            String courseSql = """
                SELECT C.COURSE_CODE, C.COURSE_NAME, C.UNITS,
                       E.GRADE, E.GRADE_EQUIVALENT
                FROM ENROLLMENTS E
                JOIN COURSES C ON E.COURSE_ID = C.ID
                LEFT JOIN CLASS_SECTIONS CS ON E.SECTION_ID = CS.ID
                WHERE E.STUDENT_ID = ?
                  AND E.SCHOOL_YEAR = ?
                  AND E.SEMESTER = ?
                """ + (user.isInstructor()
                    ? " AND CS.INSTRUCTOR_USER_ID = ? " : "")
                    + " ORDER BY C.COURSE_CODE";

            try (PreparedStatement courseStatement =
         c.prepareStatement(courseSql)) {

    courseStatement.setInt(1, studentId);
    courseStatement.setString(2, schoolYear);
    courseStatement.setString(3, semester);

    if (user.isInstructor()) {
        courseStatement.setInt(4, user.getUserId());
    }

    try (ResultSet r = courseStatement.executeQuery()) {
        while (r.next()) {
            Object raw = r.getObject("GRADE");

            String grade = raw == null
                ? "Pending"
                : (r.getString("GRADE_EQUIVALENT") == null
                    ? String.valueOf(r.getDouble("GRADE"))
                    : r.getString("GRADE_EQUIVALENT"));

            courses.add(new ReportCardPdf.CourseGrade(
                r.getString("COURSE_CODE"),
                r.getString("COURSE_NAME"),
                grade,
                r.getInt("UNITS")
            ));
        }
    }
}

            ReportCardPdf.generate(
                output,
                "Great Hope Colleges",
                studentName,
                studentNumber,
                program,
                yearLevel,
                schoolYear,
                semester,
                courses
            );
        }
    }

    private void printGradebook() {
        try {
            table.print(
                JTable.PrintMode.FIT_WIDTH,
                new java.text.MessageFormat("GHC Gradebook"),
                new java.text.MessageFormat("Page {0}")
            );
        } catch (PrinterException ex) {
            showError("Unable to print the gradebook.", ex);
        }
    }

    private void message(String text) {
        JOptionPane.showMessageDialog(
            this, text, "GHC Gradebook", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String title, Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            title + "\n" + ex.getMessage(),
            "GHC Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
