
package com.ghc.ui;

import com.ghc.database.DatabaseConnection;
import com.ghc.model.Course;
import com.ghc.security.AccessControl;
import com.ghc.security.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * GHC class section management and enrollment assignment.
 *
 * Administrators:
 *   - View and create class sections
 *   - Assign instructors
 *   - Manage enrollment-to-section assignments
 *
 * Registrars:
 *   - View class sections
 *   - Assign enrollments to sections
 *
 * Instructors:
 *   - View their assigned sections only
 */
final class ClassSectionsPanel extends JPanel {

    private final UserSession user;

    private final DefaultTableModel sectionModel = createModel(
        "ID", "Section", "Course", "Instructor",
        "School Year", "Semester", "Capacity",
        "Enrolled", "Status", "Schedule", "Room"
    );

    private final JTable sections = new JTable(sectionModel);

    private final DefaultTableModel enrollmentModel = createModel(
        "ID", "Student No.", "Student", "Course",
        "School Year", "Semester", "Section"
    );

    private final JTable enrollments = new JTable(enrollmentModel);

    private final JComboBox<SectionChoice> sectionChoice =
        new JComboBox<>();

    private final JTextField sectionSearch = new JTextField(18);
    private final JTextField enrollmentSearch = new JTextField(18);

    private final JLabel sectionCount = new JLabel("0 sections");
    private final JLabel enrollmentCount = new JLabel("0 enrollments");

    ClassSectionsPanel(UserSession user) {
        this.user = user;

        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BEIGE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JPanel header = Theme.pageHeader(
            "CLASS SECTIONS",
            "Manage class offerings, instructor assignments, "
                + "schedules, and student section assignments."
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 15));
        tabs.setBackground(Theme.BEIGE);

        tabs.addTab(
            user.isInstructor()
                ? "  My Assigned Classes  "
                : "  Class Sections  ",
            buildSectionsTab()
        );

        if (user.isAdmin() || user.isRegistrar()) {
            tabs.addTab("  Assign Enrollments  ", buildEnrollmentTab());
        }

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadSections();

        if (user.isAdmin() || user.isRegistrar()) {
            loadEnrollments();
        }
    }

    private static DefaultTableModel createModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel buildSectionsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(Theme.BEIGE);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 8, 8, 8));

        Theme.styleTable(sections);
        sections.setAutoCreateRowSorter(true);
        sections.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JPanel searchBar =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.setOpaque(false);

        JLabel searchLabel = new JLabel("Search sections");
        searchLabel.setFont(Theme.LABEL_FONT);

        sectionSearch.setFont(Theme.BODY_FONT);
        sectionSearch.setPreferredSize(new Dimension(220, 36));
        sectionSearch.setToolTipText(
            "Search section code, course, instructor, school year, or room"
        );

        JButton searchButton = Theme.button("Search");
        JButton clearButton = Theme.button("Clear");
        JButton refreshButton = Theme.button("Refresh");

        searchBar.add(searchLabel);
        searchBar.add(sectionSearch);
        searchBar.add(searchButton);
        searchBar.add(clearButton);
        searchBar.add(refreshButton);

        JPanel actions =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setOpaque(false);

        if (user.isAdmin()) {
            JButton createButton = Theme.button("Create Section");
            actions.add(createButton);
            createButton.addActionListener(e -> createSection());
        }

        actions.add(refreshButton);

        toolbar.add(searchBar, BorderLayout.NORTH);
        toolbar.add(actions, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        sectionCount.setFont(new Font("SansSerif", Font.BOLD, 13));
        sectionCount.setForeground(Theme.DARK_GREEN);
        footer.add(sectionCount, BorderLayout.WEST);

        JScrollPane scroll = new JScrollPane(sections);
        scroll.getViewport().setBackground(Theme.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> filterSections());
        sectionSearch.addActionListener(e -> filterSections());

        clearButton.addActionListener(e -> {
            sectionSearch.setText("");
            filterSections();
        });

        refreshButton.addActionListener(e -> loadSections());

        return panel;
    }

    private JPanel buildEnrollmentTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(Theme.BEIGE);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 8, 8, 8));

        Theme.styleTable(enrollments);
        enrollments.setAutoCreateRowSorter(true);
        enrollments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        toolbar.setOpaque(false);

        JPanel searchBar =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.setOpaque(false);

        JLabel searchLabel = new JLabel("Search enrollments");
        searchLabel.setFont(Theme.LABEL_FONT);

        enrollmentSearch.setFont(Theme.BODY_FONT);
        enrollmentSearch.setPreferredSize(new Dimension(210, 36));
        enrollmentSearch.setToolTipText(
            "Search student number, student name, course, or school year"
        );

        JButton searchButton = Theme.button("Search");
        JButton clearButton = Theme.button("Clear");
        JButton refreshButton = Theme.button("Refresh");

        searchBar.add(searchLabel);
        searchBar.add(enrollmentSearch);
        searchBar.add(searchButton);
        searchBar.add(clearButton);
        searchBar.add(refreshButton);

        JPanel assignmentBar =
            new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        assignmentBar.setOpaque(false);

        JLabel targetLabel = new JLabel("Target section:");
        targetLabel.setFont(Theme.LABEL_FONT);

        sectionChoice.setFont(Theme.BODY_FONT);
        sectionChoice.setPreferredSize(new Dimension(280, 36));

        JButton assignButton =
            Theme.button("Assign Selected Enrollment");

        assignmentBar.add(targetLabel);
        assignmentBar.add(sectionChoice);
        assignmentBar.add(assignButton);

        toolbar.add(searchBar, BorderLayout.NORTH);
        toolbar.add(assignmentBar, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        enrollmentCount.setFont(new Font("SansSerif", Font.BOLD, 13));
        enrollmentCount.setForeground(Theme.DARK_GREEN);
        footer.add(enrollmentCount, BorderLayout.WEST);

        JScrollPane scroll = new JScrollPane(enrollments);
        scroll.getViewport().setBackground(Theme.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> filterEnrollments());
        enrollmentSearch.addActionListener(e -> filterEnrollments());

        clearButton.addActionListener(e -> {
            enrollmentSearch.setText("");
            filterEnrollments();
        });

        refreshButton.addActionListener(e -> {
            loadSections();
            loadEnrollments();
        });

        assignButton.addActionListener(e -> assignEnrollment());

        return panel;
    }

    private void filterSections() {
        filterTable(sections, sectionModel, sectionSearch.getText());
        sectionCount.setText(
            sections.getRowCount() + " visible section(s) • "
                + sectionModel.getRowCount() + " total"
        );
    }

    private void filterEnrollments() {
        filterTable(
            enrollments, enrollmentModel, enrollmentSearch.getText()
        );

        enrollmentCount.setText(
            enrollments.getRowCount() + " visible enrollment(s) • "
                + enrollmentModel.getRowCount() + " total"
        );
    }

    private void filterTable(
            JTable table,
            DefaultTableModel model,
            String query) {

        if (!(table.getRowSorter()
                instanceof TableRowSorter<?> sorter)) {
            return;
        }

        String text = query == null ? "" : query.trim();

        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(
                RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(text)
                )
            );
        }
    }

    private void loadSections() {
        sectionModel.setRowCount(0);
        sectionChoice.removeAllItems();

        String sql =
            "SELECT CS.ID, CS.SECTION_CODE, "
                + "C.COURSE_CODE, C.COURSE_NAME, "
                + "U.FULL_NAME, CS.SCHOOL_YEAR, CS.SEMESTER, "
                + "CS.CAPACITY, CS.ACTIVE, "
                + "(SELECT COUNT(*) FROM ENROLLMENTS E "
                + " WHERE E.SECTION_ID = CS.ID) AS ENROLLED_COUNT, "
                + "SS.DAY_OF_WEEK, SS.START_TIME, SS.END_TIME, SS.ROOM "
                + "FROM CLASS_SECTIONS CS "
                + "JOIN COURSES C ON CS.COURSE_ID = C.ID "
                + "LEFT JOIN USERS U ON CS.INSTRUCTOR_USER_ID = U.ID "
                + "LEFT JOIN SECTION_SCHEDULES SS "
                + " ON SS.SECTION_ID = CS.ID "
                + (user.isInstructor()
                    ? "WHERE CS.INSTRUCTOR_USER_ID = ? "
                    : "")
                + "ORDER BY CS.SCHOOL_YEAR DESC, CS.SECTION_CODE";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            if (user.isInstructor()) {
                statement.setInt(1, user.getUserId());
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int sectionId = result.getInt("ID");
                    String code = result.getString("SECTION_CODE");
                    String courseCode = result.getString("COURSE_CODE");
                    String courseName = result.getString("COURSE_NAME");
                    String instructor = result.getString("FULL_NAME");

                    int capacity = result.getInt("CAPACITY");
                    int enrolled = result.getInt("ENROLLED_COUNT");

                    String day = result.getString("DAY_OF_WEEK");
                    String start = result.getString("START_TIME");
                    String end = result.getString("END_TIME");
                    String room = result.getString("ROOM");

                    String schedule = "Not scheduled";

                    if (day != null) {
                        schedule = day;

                        if (start != null && end != null) {
                            schedule += " " + start + "–" + end;
                        }
                    }

                    sectionModel.addRow(new Object[]{
                        sectionId,
                        code,
                        courseCode + " — " + courseName,
                        instructor == null ? "Unassigned" : instructor,
                        result.getString("SCHOOL_YEAR"),
                        result.getString("SEMESTER"),
                        capacity,
                        enrolled,
                        result.getInt("ACTIVE") == 1
                            ? "Active" : "Inactive",
                        schedule,
                        room == null ? "" : room
                    });

                    if (result.getInt("ACTIVE") == 1) {
                        sectionChoice.addItem(
                            new SectionChoice(
                                sectionId,
                                code + " — " + courseCode
                                    + " (" + result.getString("SCHOOL_YEAR")
                                    + ", " + result.getString("SEMESTER")
                                    + "; " + enrolled + "/" + capacity + ")"
                            )
                        );
                    }
                }
            }

            filterSections();

        } catch (Exception ex) {
            showError("Unable to load class sections.", ex);
        }
    }

    private void createSection() {
        try {
            AccessControl.requireAdmin();

            List<Course> courses = new ArrayList<>();

            String courseSql =
                "SELECT ID, COURSE_CODE, COURSE_NAME, UNITS, DESCRIPTION "
                    + "FROM COURSES ORDER BY COURSE_CODE";

            try (Connection connection = DatabaseConnection.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(courseSql)) {

                while (result.next()) {
                    courses.add(new Course(
                        result.getInt("ID"),
                        result.getString("COURSE_CODE"),
                        result.getString("COURSE_NAME"),
                        result.getInt("UNITS"),
                        result.getString("DESCRIPTION")
                    ));
                }
            }

            if (courses.isEmpty()) {
                showMessage("No courses were found. Create a course first.");
                return;
            }

            List<UserChoice> instructors = new ArrayList<>();

            String instructorSql =
                "SELECT ID, USERNAME, FULL_NAME FROM USERS "
                    + "WHERE ROLE = 'INSTRUCTOR' AND ACTIVE = 1 "
                    + "ORDER BY FULL_NAME";

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement =
                     connection.prepareStatement(instructorSql);
                 ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    instructors.add(new UserChoice(
                        result.getInt("ID"),
                        result.getString("USERNAME") + " — "
                            + result.getString("FULL_NAME")
                    ));
                }
            }

            JTextField code = new JTextField();
            JTextField schoolYear = new JTextField("2026-2027");
            JTextField capacity = new JTextField("40");
            JTextField startTime = new JTextField("08:00");
            JTextField endTime = new JTextField("09:30");
            JTextField room = new JTextField();

            JComboBox<Course> courseBox =
                new JComboBox<>(courses.toArray(new Course[0]));

            JComboBox<UserChoice> instructorBox =
                new JComboBox<>(instructors.toArray(new UserChoice[0]));

            JComboBox<String> semesterBox = new JComboBox<>(
                new String[]{
                    "1st Semester", "2nd Semester", "Summer"
                }
            );

            JComboBox<String> dayBox = new JComboBox<>(
                new String[]{
                    "", "Monday", "Tuesday", "Wednesday",
                    "Thursday", "Friday", "Saturday", "Sunday"
                }
            );

            JPanel form = UserManagementPanel.form(
                new String[]{
                    "Section code",
                    "Course",
                    "Instructor (optional)",
                    "School year",
                    "Semester",
                    "Capacity",
                    "Schedule day (optional)",
                    "Start time (HH:mm)",
                    "End time (HH:mm)",
                    "Room (optional)"
                },
                new JComponent[]{
                    code,
                    courseBox,
                    instructorBox,
                    schoolYear,
                    semesterBox,
                    capacity,
                    dayBox,
                    startTime,
                    endTime,
                    room
                }
            );

            int choice = JOptionPane.showConfirmDialog(
                this,
                form,
                "Create Class Section",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String sectionCode = code.getText().trim();
            String year = schoolYear.getText().trim();
            String capacityText = capacity.getText().trim();

            if (sectionCode.isEmpty() || year.isEmpty()) {
                showMessage("Section code and school year are required.");
                return;
            }

            int capacityValue;

            try {
                capacityValue = Integer.parseInt(capacityText);
            } catch (NumberFormatException ex) {
                showMessage("Capacity must be a whole number.");
                return;
            }

            if (capacityValue < 1 || capacityValue > 500) {
                showMessage("Capacity must be between 1 and 500.");
                return;
            }

            Course selectedCourse = (Course) courseBox.getSelectedItem();

            if (selectedCourse == null) {
                showMessage("Select a course.");
                return;
            }

            String selectedDay = (String) dayBox.getSelectedItem();
            String start = startTime.getText().trim();
            String end = endTime.getText().trim();

            if (selectedDay != null && !selectedDay.isBlank()) {
                LocalTime startValue = parseTime(start);
                LocalTime endValue = parseTime(end);

                if (startValue == null || endValue == null) {
                    showMessage(
                        "Enter valid schedule times in HH:mm format, "
                            + "for example 08:00 and 09:30."
                    );
                    return;
                }

                if (!endValue.isAfter(startValue)) {
                    showMessage("End time must be later than start time.");
                    return;
                }
            }

            UserChoice selectedInstructor =
                (UserChoice) instructorBox.getSelectedItem();

            String selectedSemester =
                (String) semesterBox.getSelectedItem();

            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);

                try {
                    int sectionId;

                    String insertSql =
                        "INSERT INTO CLASS_SECTIONS "
                            + "(SECTION_CODE, COURSE_ID, "
                            + "INSTRUCTOR_USER_ID, SCHOOL_YEAR, "
                            + "SEMESTER, CAPACITY, ACTIVE) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 1)";

                    try (PreparedStatement statement =
                             connection.prepareStatement(
                                 insertSql,
                                 Statement.RETURN_GENERATED_KEYS)) {

                        statement.setString(1, sectionCode);
                        statement.setInt(2, selectedCourse.id);

                        if (selectedInstructor == null) {
                            statement.setNull(3, Types.INTEGER);
                        } else {
                            statement.setInt(3, selectedInstructor.id);
                        }

                        statement.setString(4, year);
                        statement.setString(5, selectedSemester);
                        statement.setInt(6, capacityValue);

                        statement.executeUpdate();

                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (keys.next()) {
                                sectionId = keys.getInt(1);
                            } else {
                                try (PreparedStatement find =
                                         connection.prepareStatement(
                                             "SELECT ID FROM CLASS_SECTIONS "
                                                 + "WHERE SECTION_CODE = ? "
                                                 + "AND SCHOOL_YEAR = ? "
                                                 + "AND SEMESTER = ?")) {

                                    find.setString(1, sectionCode);
                                    find.setString(2, year);
                                    find.setString(3, selectedSemester);

                                    try (ResultSet result = find.executeQuery()) {
                                        if (!result.next()) {
                                            throw new SQLException(
                                                "Could not retrieve the new "
                                                    + "section ID."
                                            );
                                        }
                                        sectionId = result.getInt(1);
                                    }
                                }
                            }
                        }
                    }

                    if (selectedDay != null && !selectedDay.isBlank()) {
                        String scheduleSql =
                            "INSERT INTO SECTION_SCHEDULES "
                                + "(SECTION_ID, DAY_OF_WEEK, START_TIME, "
                                + "END_TIME, ROOM) VALUES (?, ?, ?, ?, ?)";

                        try (PreparedStatement statement =
                                 connection.prepareStatement(scheduleSql)) {

                            statement.setInt(1, sectionId);
                            statement.setString(2, selectedDay);
                            statement.setString(3, start);
                            statement.setString(4, end);

                            if (room.getText().trim().isEmpty()) {
                                statement.setNull(5, Types.VARCHAR);
                            } else {
                                statement.setString(
                                    5, room.getText().trim()
                                );
                            }

                            statement.executeUpdate();
                        }
                    }

                    connection.commit();

                } catch (Exception ex) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackError) {
                        ex.addSuppressed(rollbackError);
                    }
                    throw ex;
                } finally {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ignored) {
                        // Connection is closing.
                    }
                }
            }

            loadSections();

            showMessage("Class section created successfully.");

        } catch (Exception ex) {
            showError("Unable to create class section.", ex);
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(
                value,
                DateTimeFormatter.ofPattern("HH:mm")
            );
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void loadEnrollments() {
        enrollmentModel.setRowCount(0);

        String sql =
            "SELECT E.ID, S.STUDENT_NO, "
                + "S.FIRST_NAME || ' ' || S.LAST_NAME AS STUDENT_NAME, "
                + "C.COURSE_CODE, E.SCHOOL_YEAR, E.SEMESTER, "
                + "E.SECTION_ID "
                + "FROM ENROLLMENTS E "
                + "JOIN STUDENTS S ON E.STUDENT_ID = S.ID "
                + "JOIN COURSES C ON E.COURSE_ID = C.ID "
                + "ORDER BY E.SCHOOL_YEAR DESC, S.LAST_NAME";

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                Object sectionId = result.getObject("SECTION_ID");

                enrollmentModel.addRow(new Object[]{
                    result.getInt("ID"),
                    result.getString("STUDENT_NO"),
                    result.getString("STUDENT_NAME"),
                    result.getString("COURSE_CODE"),
                    result.getString("SCHOOL_YEAR"),
                    result.getString("SEMESTER"),
                    sectionId == null
                        ? "Unassigned" : sectionId
                });
            }

            filterEnrollments();

        } catch (Exception ex) {
            showError("Unable to load student enrollments.", ex);
        }
    }

    private void assignEnrollment() {
        int viewRow = enrollments.getSelectedRow();
        SectionChoice target =
            (SectionChoice) sectionChoice.getSelectedItem();

        if (viewRow < 0) {
            showMessage("Select an enrollment first.");
            return;
        }

        if (target == null) {
            showMessage("No active class sections are available.");
            return;
        }

        int modelRow = enrollments.convertRowIndexToModel(viewRow);
        int enrollmentId =
            ((Number) enrollmentModel.getValueAt(modelRow, 0)).intValue();

        int targetSectionId = target.id;

        int confirmation = JOptionPane.showConfirmDialog(
            this,
            "Assign student " + enrollmentModel.getValueAt(modelRow, 1)
                + " to section " + target.label + "?",
            "Confirm Section Assignment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            AccessControl.requireAdminOrRegistrar();

            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);

                try {
                    int enrollmentCourseId;
                    String enrollmentYear;
                    String enrollmentSemester;

                    String enrollmentSql =
                        "SELECT COURSE_ID, SCHOOL_YEAR, SEMESTER "
                            + "FROM ENROLLMENTS WHERE ID = ?";

                    try (PreparedStatement statement =
                             connection.prepareStatement(enrollmentSql)) {

                        statement.setInt(1, enrollmentId);

                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException(
                                    "Enrollment was not found."
                                );
                            }

                            enrollmentCourseId =
                                result.getInt("COURSE_ID");
                            enrollmentYear =
                                result.getString("SCHOOL_YEAR");
                            enrollmentSemester =
                                result.getString("SEMESTER");
                        }
                    }

                    int sectionCourseId;
                    int sectionCapacity;
                    int sectionActive;
                    String sectionYear;
                    String sectionSemester;

                    String sectionSql =
                        "SELECT COURSE_ID, CAPACITY, ACTIVE, "
                            + "SCHOOL_YEAR, SEMESTER "
                            + "FROM CLASS_SECTIONS WHERE ID = ?";

                    try (PreparedStatement statement =
                             connection.prepareStatement(sectionSql)) {

                        statement.setInt(1, targetSectionId);

                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException(
                                    "Target section was not found."
                                );
                            }

                            sectionCourseId =
                                result.getInt("COURSE_ID");
                            sectionCapacity =
                                result.getInt("CAPACITY");
                            sectionActive = result.getInt("ACTIVE");
                            sectionYear =
                                result.getString("SCHOOL_YEAR");
                            sectionSemester =
                                result.getString("SEMESTER");
                        }
                    }

                    if (sectionActive != 1) {
                        throw new SQLException(
                            "The target section is inactive."
                        );
                    }

                    if (sectionCourseId != enrollmentCourseId) {
                        throw new SQLException(
                            "The section's course does not match "
                                + "the student's enrollment course."
                        );
                    }

                    if (!same(enrollmentYear, sectionYear)
                            || !same(enrollmentSemester, sectionSemester)) {
                        throw new SQLException(
                            "The section's school year and semester "
                                + "must match the enrollment."
                        );
                    }

                    int existingSectionId = -1;

                    String existingSql =
                        "SELECT SECTION_ID FROM ENROLLMENTS WHERE ID = ?";

                    try (PreparedStatement statement =
                             connection.prepareStatement(existingSql)) {

                        statement.setInt(1, enrollmentId);

                        try (ResultSet result = statement.executeQuery()) {
                            if (result.next()) {
                                existingSectionId =
                                    result.getInt("SECTION_ID");

                                if (result.wasNull()) {
                                    existingSectionId = -1;
                                }
                            }
                        }
                    }

                    if (existingSectionId != targetSectionId) {
                        int assignedCount;

                        String countSql =
                            "SELECT COUNT(*) FROM ENROLLMENTS "
                                + "WHERE SECTION_ID = ?";

                        try (PreparedStatement statement =
                                 connection.prepareStatement(countSql)) {

                            statement.setInt(1, targetSectionId);

                            try (ResultSet result = statement.executeQuery()) {
                                result.next();
                                assignedCount = result.getInt(1);
                            }
                        }

                        if (assignedCount >= sectionCapacity) {
                            throw new SQLException(
                                "The target section has reached its capacity."
                            );
                        }

                        String updateSql =
                            "UPDATE ENROLLMENTS SET SECTION_ID = ? "
                                + "WHERE ID = ?";

                        try (PreparedStatement statement =
                                 connection.prepareStatement(updateSql)) {

                            statement.setInt(1, targetSectionId);
                            statement.setInt(2, enrollmentId);

                            if (statement.executeUpdate() == 0) {
                                throw new SQLException(
                                    "Enrollment assignment was not saved."
                                );
                            }
                        }
                    }

                    connection.commit();

                } catch (Exception ex) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackError) {
                        ex.addSuppressed(rollbackError);
                    }
                    throw ex;
                } finally {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ignored) {
                        // Connection is closing.
                    }
                }
            }

            loadSections();
            loadEnrollments();

            showMessage("Enrollment assigned successfully.");

        } catch (Exception ex) {
            showError("Unable to assign enrollment.", ex);
        }
    }

    private boolean same(String first, String second) {
        return first == null
            ? second == null
            : first.equals(second);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "GHC Class Sections",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            message + "\n\nDetails: " + ex.getMessage(),
            "GHC Class Sections",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private record UserChoice(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record SectionChoice(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
