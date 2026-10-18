
package com.ghc.ui;

import com.ghc.dao.*;
import com.ghc.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordPanel extends JPanel {

    enum Type {
        STUDENT, COURSE, ENROLLMENT, RECORDS
    }

    private final Type type;
    private final JTextField search = new JTextField(22);
    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = new JTable(model);

    private final StudentDAO studentDAO = new StudentDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    private List<Student> students = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();

    RecordPanel(Type type) {
        this.type = type;

        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.BEIGE);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 8));
        top.setBackground(Theme.BEIGE);

        JLabel searchLabel = new JLabel(
            type == Type.STUDENT ? "Search students:"
                : type == Type.COURSE ? "Search courses:"
                : "Search academic records:"
        );
        searchLabel.setForeground(Color.BLACK);
        top.add(searchLabel);

        search.setForeground(Color.BLACK);
        search.setBackground(Color.WHITE);
        search.setPreferredSize(new Dimension(230, 32));
        top.add(search);

        JButton find = Theme.button("Search");
        JButton reset = Theme.button("Refresh");

        top.add(find);
        top.add(reset);

        boolean admin =
            com.ghc.security.SessionContext.currentUser().isAdmin();
        boolean registrar =
            com.ghc.security.SessionContext.currentUser().isRegistrar();

        boolean canMaintain =
            (type == Type.STUDENT && (admin || registrar))
            || (type == Type.COURSE && admin)
            || (type == Type.ENROLLMENT && (admin || registrar));

        if (canMaintain) {
            JButton add = Theme.button(
                type == Type.STUDENT ? "Add Student"
                    : type == Type.COURSE ? "Add Course"
                    : "Enroll Student"
            );

            JButton edit = Theme.button("Edit Selected");
            JButton del = Theme.button("Delete Selected");

            top.add(add);
            top.add(edit);
            top.add(del);

            add.addActionListener(e -> addRecord());
            edit.addActionListener(e -> editRecord());
            del.addActionListener(e -> deleteRecord());
        }

        JButton exportPdf = Theme.button("Export PDF");
        top.add(exportPdf);

        exportPdf.addActionListener(e -> {
    if (type == Type.STUDENT) {
        exportStudentDirectoryPdf();
    } else if (type == Type.COURSE) {
        exportCourseDirectoryPdf();
    } else if (type == Type.ENROLLMENT || type == Type.RECORDS) {
        exportEnrollmentReportPdf();
    }
});

        String pageTitle = switch (type) {
            case STUDENT -> "STUDENT DIRECTORY";
            case COURSE -> "COURSES / SUBJECTS";
            case ENROLLMENT -> "STUDENT ENROLLMENT";
            case RECORDS -> "ACADEMIC RECORDS";
        };

        String pageSubtitle = switch (type) {
            case STUDENT ->
                "Search, maintain, and review student information.";
            case COURSE ->
                "Browse course codes, subject titles, units, and descriptions.";
            case ENROLLMENT ->
                "Manage student course registrations and academic terms.";
            case RECORDS ->
                "Search, review, and export academic information.";
        };

        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.setBackground(Theme.BEIGE);
        page.add(Theme.pageHeader(pageTitle, pageSubtitle),
                 BorderLayout.NORTH);
        page.add(top, BorderLayout.CENTER);

        add(page, BorderLayout.NORTH);

        Theme.styleTable(table);
        table.setForeground(Color.BLACK);
        table.setBackground(Theme.WHITE);
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionBackground(new Color(214, 231, 195));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(210, 210, 195));

        table.getTableHeader().setForeground(Color.BLACK);
        table.getTableHeader().setBackground(Theme.BEIGE);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        find.addActionListener(e -> load());
        search.addActionListener(e -> load());

        reset.addActionListener(e -> {
            search.setText("");
            load();
        });

        load();
    }

    // ---------------------------------------------------------
    // PDF EXPORTS
    // ---------------------------------------------------------

    private void exportStudentDirectoryPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Student Directory as PDF");
        chooser.setSelectedFile(new File("GHC_Student_Directory.pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        List<com.ghc.reports.StudentDirectoryPdf.StudentRow> rows =
            new ArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            rows.add(new com.ghc.reports.StudentDirectoryPdf.StudentRow(
                valueAt(i, 1),
                valueAt(i, 2),
                valueAt(i, 3),
                valueAt(i, 4),
                valueAt(i, 5),
                valueAt(i, 6),
                valueAt(i, 7),
                valueAt(i, 8),
                valueAt(i, 9),
                valueAt(i, 10),
                valueAt(i, 11)
            ));
        }

        try {
            com.ghc.reports.StudentDirectoryPdf.generate(
                chooser.getSelectedFile().toPath(),
                rows
            );

            showExportSuccess(chooser.getSelectedFile());
        } catch (Exception ex) {
            error("Unable to export Student Directory PDF", ex);
        }
    }

    private void exportCourseDirectoryPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Courses / Subjects as PDF");
        chooser.setSelectedFile(new File("GHC_Course_Directory.pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        List<com.ghc.reports.CourseDirectoryPdf.CourseRow> rows =
            new ArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            rows.add(new com.ghc.reports.CourseDirectoryPdf.CourseRow(
                valueAt(i, 1),
                valueAt(i, 2),
                valueAt(i, 3),
                valueAt(i, 4)
            ));
        }

        try {
            com.ghc.reports.CourseDirectoryPdf.generate(
                chooser.getSelectedFile().toPath(),
                rows
            );

            showExportSuccess(chooser.getSelectedFile());
        } catch (Exception ex) {
            error("Unable to export Courses / Subjects PDF", ex);
        }
    }

    private void exportEnrollmentReportPdf() {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Export Enrollment Report as PDF");
    chooser.setSelectedFile(
            new java.io.File("GHC_Enrollment_Report.pdf"));

    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
        return;
    }

    java.util.List<com.ghc.reports.EnrollmentReportPdf.EnrollmentRow>
            rows = new java.util.ArrayList<>();

    for (int i = 0; i < model.getRowCount(); i++) {
        rows.add(
                new com.ghc.reports.EnrollmentReportPdf.EnrollmentRow(
                        valueAt(i, 0),
                        valueAt(i, 1),
                        valueAt(i, 2),
                        valueAt(i, 3),
                        valueAt(i, 4),
                        valueAt(i, 5),
                        valueAt(i, 6),
                        valueAt(i, 7)
                )
        );
    }

    try {
        com.ghc.reports.EnrollmentReportPdf.generate(
                chooser.getSelectedFile().toPath(),
                rows
        );

        JOptionPane.showMessageDialog(
                this,
                "Enrollment Report PDF exported successfully.",
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE
        );
    } catch (Exception ex) {
        error("Unable to export Enrollment Report PDF", ex);
    }
}

    private void showExportSuccess(File file) {
        int answer = JOptionPane.showConfirmDialog(
            this,
            "PDF exported successfully:\n" + file.getAbsolutePath()
                + "\n\nWould you like to open it now?",
            "Export Complete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );

        if (answer == JOptionPane.YES_OPTION) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    msg("Your system does not support opening files automatically.");
                }
            } catch (Exception ex) {
                error("The PDF was exported, but could not be opened automatically.", ex);
            }
        }
    }


    
    private String valueAt(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    // ---------------------------------------------------------
    // LOAD TABLE DATA
    // ---------------------------------------------------------

    private void load() {
        try {
            model.setRowCount(0);
            model.setColumnCount(0);

            switch (type) {
                case STUDENT -> {
                    String[] cols = {
                        "ID", "Student No.", "First Name", "Last Name",
                        "Gender", "Birth Date", "Email", "Phone",
                        "Address", "Year Level", "Program", "Status"
                    };

                    for (String c : cols) {
                        model.addColumn(c);
                    }

                    students = studentDAO.all(search.getText());

                    for (Student s : students) {
                        model.addRow(new Object[]{
                            s.id, s.studentNo, s.firstName, s.lastName,
                            s.gender, s.birthDate, s.email, s.phone,
                            s.address, s.yearLevel, s.program, s.status
                        });
                    }
                }

                case COURSE -> {
                    for (String c : new String[]{
                        "ID", "Course Code", "Course Name",
                        "Units", "Description"
                    }) {
                        model.addColumn(c);
                    }

                    courses = courseDAO.all(search.getText());

                    for (Course c : courses) {
                        model.addRow(new Object[]{
                            c.id, c.code, c.name, c.units, c.description
                        });
                    }
                }

                case ENROLLMENT, RECORDS -> {
                    for (String c : new String[]{
                        "ID", "Student No.", "Student Name", "Course",
                        "School Year", "Semester", "Grade", "Remarks",
                        "Student ID", "Course ID"
                    }) {
                        model.addColumn(c);
                    }

                    for (Object[] r : enrollmentDAO.all(search.getText())) {
                        model.addRow(r);
                    }
                }
            }

            if (type == Type.ENROLLMENT || type == Type.RECORDS) {
                hideColumn(8);
                hideColumn(8);
            } else {
                hideColumn(0);
            }

            table.setForeground(Color.BLACK);
            table.setBackground(Theme.WHITE);
            table.setSelectionForeground(Color.BLACK);
            table.setSelectionBackground(new Color(214, 231, 195));

            table.getTableHeader().setFont(Theme.TABLE_HEADER_FONT);
            table.getTableHeader().setForeground(Color.BLACK);
            table.getTableHeader().setBackground(Theme.BEIGE);

        } catch (Exception e) {
            error("Unable to load records", e);
        }
    }

    private void hideColumn(int i) {
        if (table.getColumnModel().getColumnCount() > i) {
            table.removeColumn(table.getColumnModel().getColumn(i));
        }
    }

    private int selectedId() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a record first.",
                "No Record Selected",
                JOptionPane.WARNING_MESSAGE
            );
            return -1;
        }

        return ((Number) table.getModel().getValueAt(
            table.convertRowIndexToModel(row), 0
        )).intValue();
    }

    // ---------------------------------------------------------
    // ADD, EDIT, DELETE
    // ---------------------------------------------------------

    private void addRecord() {
        if (type == Type.STUDENT) {
            studentForm(null);
        } else if (type == Type.COURSE) {
            courseForm(null);
        } else {
            enrollmentForm(null);
        }
    }

    private void editRecord() {
        int id = selectedId();
        if (id < 0) {
            return;
        }

        int row = table.convertRowIndexToModel(table.getSelectedRow());

        if (type == Type.STUDENT) {
            for (Student s : students) {
                if (s.id == id) {
                    studentForm(s);
                    return;
                }
            }
        } else if (type == Type.COURSE) {
            for (Course c : courses) {
                if (c.id == id) {
                    courseForm(c);
                    return;
                }
            }
        } else {
            Object[] r = new Object[model.getColumnCount()];

            for (int i = 0; i < r.length; i++) {
                r[i] = model.getValueAt(row, i);
            }

            enrollmentForm(r);
        }
    }

    private void deleteRecord() {
        int id = selectedId();
        if (id < 0) {
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
            this,
            "Delete selected record? This cannot be undone.",
            "Confirm deletion",
            JOptionPane.YES_NO_OPTION
        );

        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (type == Type.STUDENT) {
                studentDAO.delete(id);
            } else if (type == Type.COURSE) {
                courseDAO.delete(id);
            } else {
                enrollmentDAO.delete(id);
            }

            load();

        } catch (Exception e) {
            error(
                "Delete failed. A student/course with enrollments "
                    + "cannot be deleted until related enrollments are removed.",
                e
            );
        }
    }

    private void studentForm(Student old) {
        JTextField[] f = new JTextField[10];
        JPasswordField passwordField = new JPasswordField(20);
        JLabel passwordLabel = new JLabel(
    old == null
        ? "Initial Password *"
        : "New Password (leave blank to keep current)"
);

        String[] labels = {
            "Student Number *", "First Name *", "Last Name *",
            "Birth Date (YYYY-MM-DD)", "Email", "Phone", "Address",
            "Year Level", "Program", "Status"
        };

        JComboBox<String> gender = new JComboBox<>(
            new String[]{"Prefer not to say", "Female", "Male", "Other"}
        );

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 7));
        p.setBackground(Theme.BEIGE);

        for (int i = 0; i < f.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setForeground(Color.BLACK);
            p.add(label);

            f[i] = new JTextField(20);
            f[i].setForeground(Color.BLACK);
            p.add(f[i]);
        }

        JLabel genderLabel = new JLabel("Gender");
        genderLabel.setForeground(Color.BLACK);
        p.add(genderLabel);

        gender.setForeground(Color.BLACK);
        p.add(gender);

       
    passwordLabel.setForeground(Color.BLACK);
    p.add(passwordLabel);

    passwordField.setForeground(Color.BLACK);
    p.add(passwordField);


        f[7].setText("1st Year");
        f[9].setText("Active");

        if (old != null) {
            String[] v = {
                old.studentNo, old.firstName, old.lastName,
                old.birthDate, old.email, old.phone, old.address,
                old.yearLevel, old.program, old.status
            };

            for (int i = 0; i < f.length; i++) {
                f[i].setText(n(v[i]));
            }

            gender.setSelectedItem(
                old.gender == null ? "Prefer not to say" : old.gender
            );
        }

       JScrollPane formScroll = new JScrollPane(p);
formScroll.setPreferredSize(new Dimension(480, 500));
formScroll.setBorder(BorderFactory.createEmptyBorder());
formScroll.getVerticalScrollBar().setUnitIncrement(16);

int dialogResult = JOptionPane.showConfirmDialog(
    this,
    formScroll,
    (old == null ? "Add" : "Edit") + " Student ",
    JOptionPane.OK_CANCEL_OPTION,
    JOptionPane.PLAIN_MESSAGE
);

if (dialogResult != JOptionPane.OK_OPTION) {
    return;
}

        if (blank(f[0]) || blank(f[1]) || blank(f[2])) {
            msg("Student number, first name and last name are required.");
            return;
        }

        int passwordLength = passwordField.getPassword().length;

if ((old == null || passwordLength > 0)
        && passwordLength < 8) {
    msg(
        old == null
            ? "Initial password must contain at least 8 characters."
            : "New password must contain at least 8 characters."
    );
    return;
}

        if (!f[3].getText().trim().isEmpty()
                && !f[3].getText().trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            msg("Birth date must use YYYY-MM-DD.");
            return;
        }

        String[] v = new String[10];

        for (int i = 0; i < 10; i++) {
            v[i] = f[i].getText().trim();
        }

        try {
            Student student = new Student(
    old == null ? 0 : old.id,
    v[0], v[1], v[2],
    (String) gender.getSelectedItem(),
    v[3], v[4], v[5], v[6], v[7], v[8],
    v[9].isEmpty() ? "Active" : v[9]
);

String password = new String(passwordField.getPassword());

if (old == null || !password.isBlank()) {
    studentDAO.save(student, password);
} else {
    studentDAO.save(student);
}

            load();

        } catch (Exception e) {
    e.printStackTrace();

    error(
        "Could not save student: "
            + e.getClass().getSimpleName()
            + ": "
            + e.getMessage(),
        e
    );
}
    }

    private void courseForm(Course old) {
        JTextField code = new JTextField(old == null ? "" : old.code, 18);
        JTextField name = new JTextField(old == null ? "" : old.name, 18);
        JTextField units = new JTextField(
            old == null ? "3" : "" + old.units, 18
        );
        JTextField desc = new JTextField(
            old == null ? "" : n(old.description), 18
        );

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.setBackground(Theme.BEIGE);

        for (Object[] x : new Object[][]{
            {"Course Code *", code},
            {"Course Name *", name},
            {"Units *", units},
            {"Description", desc}
        }) {
            JLabel label = new JLabel((String) x[0]);
            label.setForeground(Color.BLACK);
            p.add(label);

            ((JComponent) x[1]).setForeground(Color.BLACK);
            p.add((JComponent) x[1]);
        }

        if (JOptionPane.showConfirmDialog(
                this, p, (old == null ? "Add" : "Edit") + " Course",
                JOptionPane.OK_CANCEL_OPTION
            ) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            if (code.getText().isBlank() || name.getText().isBlank()) {
                throw new IllegalArgumentException(
                    "Course code and name are required."
                );
            }

            int u = Integer.parseInt(units.getText().trim());

            if (u < 1 || u > 12) {
                throw new IllegalArgumentException(
                    "Units must be between 1 and 12."
                );
            }

            courseDAO.save(new Course(
                old == null ? 0 : old.id,
                code.getText().trim(),
                name.getText().trim(),
                u,
                desc.getText().trim()
            ));

            load();

        } catch (Exception e) {
            error(
                "Could not save course. Verify the fields and unique course code.",
                e
            );
        }
    }

    private void enrollmentForm(Object[] old) {
        try {
            students = studentDAO.all("");
            courses = courseDAO.all("");
        } catch (Exception e) {
            error("Could not load students/courses.", e);
            return;
        }

        if (students.isEmpty() || courses.isEmpty()) {
            msg("Add at least one student and one course before enrolling.");
            return;
        }

        JComboBox<Student> st = new JComboBox<>(
            students.toArray(new Student[0])
        );
        JComboBox<Course> co = new JComboBox<>(
            courses.toArray(new Course[0])
        );

        JTextField year = new JTextField("2026-2027", 14);
        JTextField grade = new JTextField(14);

        JComboBox<String> sem = new JComboBox<>(
            new String[]{"1st Semester", "2nd Semester", "Summer"}
        );

        JComboBox<String> remarks = new JComboBox<>(
            new String[]{"Pending", "Passed", "Failed", "Incomplete", "Dropped"}
        );

        grade.setEditable(false);
        grade.setBackground(new Color(235, 232, 220));
        grade.setToolTipText(
            "Final grades are calculated from components in Grades & Reports."
        );

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.setBackground(Theme.BEIGE);

        for (Object[] x : new Object[][]{
            {"Student *", st},
            {"Course *", co},
            {"School Year *", year},
            {"Semester *", sem},
            {"Grade (0–100; blank if pending)", grade},
            {"Remarks", remarks}
        }) {
            JLabel label = new JLabel((String) x[0]);
            label.setForeground(Color.BLACK);
            p.add(label);

            Component component = (Component) x[1];

            if (component instanceof JComponent jc) {
                jc.setForeground(Color.BLACK);
            }

            p.add(component);
        }

        int id = 0;

        if (old != null) {
            id = ((Number) old[0]).intValue();

            for (Student s : students) {
                if (s.id == ((Number) old[8]).intValue()) {
                    st.setSelectedItem(s);
                }
            }

            for (Course c : courses) {
                if (c.id == ((Number) old[9]).intValue()) {
                    co.setSelectedItem(c);
                }
            }

            year.setText("" + old[4]);
            sem.setSelectedItem(old[5]);
            grade.setText(old[6] == null ? "" : "" + old[6]);
            remarks.setSelectedItem(old[7]);
        }

        if (JOptionPane.showConfirmDialog(
                this, p,
                old == null ? "Enroll Student" : "Edit Enrollment / Grade",
                JOptionPane.OK_CANCEL_OPTION
            ) != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            if (year.getText().isBlank()) {
                throw new IllegalArgumentException(
                    "School year is required."
                );
            }

            Double g = null;

            if (!grade.getText().trim().isEmpty()) {
                g = Double.parseDouble(grade.getText().trim());

                if (g < 0 || g > 100) {
                    throw new IllegalArgumentException(
                        "Grade must be between 0 and 100."
                    );
                }
            }

            enrollmentDAO.save(
                id,
                ((Student) st.getSelectedItem()).id,
                ((Course) co.getSelectedItem()).id,
                year.getText().trim(),
                (String) sem.getSelectedItem(),
                g,
                (String) remarks.getSelectedItem()
            );

            load();

        } catch (Exception e) {
            error(
                "Could not save enrollment. Duplicate enrollment for the "
                    + "same course/year/semester is not allowed.",
                e
            );
        }
    }

    private String n(String s) {
        return s == null ? "" : s;
    }

    private boolean blank(JTextField f) {
        return f.getText().trim().isEmpty();
    }

    private void msg(String s) {
        JOptionPane.showMessageDialog(
            this, s, "Validation", JOptionPane.WARNING_MESSAGE
        );
    }

    private void error(String s, Exception e) {
        JOptionPane.showMessageDialog(
            this,
            s + "\n" + e.getMessage(),
            "GHC Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
