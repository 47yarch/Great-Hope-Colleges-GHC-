package com.ghc.ui;

import com.ghc.dao.CourseDAO;
import com.ghc.dao.EnrollmentDAO;
import com.ghc.dao.StudentDAO;
import com.ghc.security.Role;
import com.ghc.security.SessionContext;
import com.ghc.security.UserSession;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Role-aware application shell. All protected operations are also checked by the DAO layer. */
public class DashboardFrame extends JFrame {
    private final UserSession user = SessionContext.currentUser();
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel studentCount = new JLabel("—");
    private final JLabel courseCount = new JLabel("—");
    private final JLabel enrollmentCount = new JLabel("—");
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String activePage = "HOME";

    public DashboardFrame() {
        setTitle("Great Hope Colleges | Academic Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 790);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BEIGE);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        content.setBackground(Theme.BEIGE);
        content.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(content, BorderLayout.CENTER);
        JLabel footer = new JLabel("  GHC  •  Academic Records Office  •  Green, gold and beige edition");
        footer.setOpaque(true); footer.setBackground(Theme.BEIGE); footer.setForeground(Color.BLACK);
        footer.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        add(footer, BorderLayout.SOUTH);
        showPage("HOME");
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Theme.GREEN);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 20));
        JPanel brand = new JPanel(new GridLayout(0, 1, 2, 2)); brand.setOpaque(false);
        JLabel name = new JLabel("GREAT HOPE COLLEGES (GHC)");
        name.setForeground(Theme.YELLOW); name.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitle = new JLabel("Student Enrollment, Grading and Record Management System");
        subtitle.setForeground(Color.WHITE);
        brand.add(name); brand.add(subtitle); header.add(brand, BorderLayout.WEST);
        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4)); account.setOpaque(false);
        JLabel who = new JLabel(user.getFullName() + "  •  " + roleLabel(user.getRole()));
        who.setForeground(Color.WHITE); who.setFont(new Font("SansSerif", Font.BOLD, 12));
        JButton logout = Theme.button("Log Out"); logout.setBackground(Theme.YELLOW);
        logout.addActionListener(e -> { SessionContext.signOut(); dispose(); new LoginFrame().setVisible(true); });
        account.add(who); account.add(logout); header.add(account, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel(); side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.DARK_GREEN); side.setPreferredSize(new Dimension(235, 10));
        side.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        addSection(side, "WORKSPACE", new String[][]{{"HOME", "Home"}});
        if (user.isAdmin()) {
            addSection(side, "ACADEMIC SETUP", new String[][]{{"ACADEMIC_SETUP", "Departments & Programs"}, {"COURSES", "Courses / Subjects"}});
            addSection(side, "PEOPLE", new String[][]{{"PEOPLE", "User Accounts"}, {"STUDENTS", "Student Directory"}});
            addSection(side, "CLASSES & ENROLLMENT", new String[][]{{"CLASSES", "Class Sections"}, {"ENROLLMENTS", "Enrollment"}});
            addSection(side, "GRADES & REPORTS", new String[][]{{"GRADES", "Gradebook & Reports"}, {"RECORDS", "Academic Records"}});
            addSection(side, "SYSTEM", new String[][]{{"SETTINGS", "Appearance & Settings"}});
        } else if (user.isRegistrar()) {
            addSection(side, "PEOPLE", new String[][]{{"STUDENTS", "Student Directory"}});
            addSection(side, "CLASSES & ENROLLMENT", new String[][]{{"CLASSES", "Class Sections / Assignment"}, {"ENROLLMENTS", "Enrollment"}, {"RECORDS", "Academic Records"}});
            addSection(side, "SYSTEM", new String[][]{{"SETTINGS", "Appearance"}});
        } else {
            addSection(side, "TEACHING", new String[][]{{"CLASSES", "My Assigned Classes"}, {"GRADES", "Gradebook & Reports"}});
            addSection(side, "SYSTEM", new String[][]{{"SETTINGS", "Appearance"}});
        }
        side.add(Box.createVerticalGlue());
        JLabel version = new JLabel("GHC • LOCAL DATABASE"); version.setForeground(new Color(235, 230, 205));
        version.setBorder(BorderFactory.createEmptyBorder(8, 5, 4, 5)); side.add(version);
        return side;
    }

    private void addSection(JPanel side, String title, String[][] entries) {
        JLabel heading = new JLabel(title); heading.setForeground(Theme.YELLOW);
        heading.setFont(new Font("SansSerif", Font.BOLD, 10));
        heading.setBorder(BorderFactory.createEmptyBorder(13, 7, 5, 4));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT); side.add(heading);
        for (String[] entry : entries) {
            JButton b = new JButton(entry[1]); b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
            b.setBackground(Theme.DARK_GREEN); b.setForeground(Color.WHITE); b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8)); b.setFont(new Font("SansSerif", Font.PLAIN, 12));
            b.addActionListener(e -> showPage(entry[0])); navButtons.put(entry[0], b); side.add(b);
        }
    }

    private void showPage(String page) {
        activePage = page;
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean selected = e.getKey().equals(page);
            e.getValue().setBackground(selected ? Theme.YELLOW : Theme.DARK_GREEN);
            e.getValue().setForeground(selected ? Color.BLACK : Color.WHITE);
        }
        content.removeAll();
        switch (page) {
            case "HOME" -> content.add(homePage(), BorderLayout.CENTER);
            case "STUDENTS" -> content.add(new RecordPanel(RecordPanel.Type.STUDENT), BorderLayout.CENTER);
            case "COURSES" -> content.add(new RecordPanel(RecordPanel.Type.COURSE), BorderLayout.CENTER);
            case "ENROLLMENTS" -> content.add(new RecordPanel(RecordPanel.Type.ENROLLMENT), BorderLayout.CENTER);
            case "RECORDS" -> content.add(new RecordPanel(RecordPanel.Type.RECORDS), BorderLayout.CENTER);
            case "PEOPLE" -> content.add(new UserManagementPanel(), BorderLayout.CENTER);
            case "ACADEMIC_SETUP" -> content.add(new AcademicSetupPanel(), BorderLayout.CENTER);
            case "CLASSES" -> content.add(new ClassSectionsPanel(user), BorderLayout.CENTER);
            case "GRADES" -> content.add(new GradebookPanel(user), BorderLayout.CENTER);
            case "SETTINGS" -> content.add(new SettingsPanel(), BorderLayout.CENTER);
            default -> content.add(homePage(), BorderLayout.CENTER);
        }
        content.revalidate(); content.repaint();
    }

    private JPanel homePage() {
        JPanel root = new JPanel(new BorderLayout(15, 15)); root.setBackground(Theme.BEIGE);
        root.setBorder(BorderFactory.createEmptyBorder(26, 28, 24, 28));
        JPanel intro = new JPanel(new GridLayout(0, 1, 5, 5)); intro.setOpaque(false);
        JLabel welcome = new JLabel("Welcome, " + user.getFullName()); welcome.setFont(new Font("SansSerif", Font.BOLD, 25)); welcome.setForeground(Color.BLACK);
        JLabel desc = new JLabel("Your " + roleLabel(user.getRole()).toLowerCase() + " workspace for student information, classes and academic performance."); desc.setForeground(Color.BLACK);
        intro.add(welcome); intro.add(desc); root.add(intro, BorderLayout.NORTH);
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 14)); cards.setOpaque(false);
        cards.add(card("REGISTERED STUDENTS", studentCount)); cards.add(card("COURSES / SUBJECTS", courseCount)); cards.add(card("ENROLLMENTS", enrollmentCount));
        root.add(cards, BorderLayout.CENTER);
        JTextArea guidance = new JTextArea(guidanceText()); guidance.setEditable(false); guidance.setLineWrap(true); guidance.setWrapStyleWord(true);
        guidance.setBackground(Theme.WHITE); guidance.setForeground(Color.BLACK); guidance.setFont(new Font("SansSerif", Font.PLAIN, 14));
        guidance.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(215, 207, 180)), BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        guidance.setRows(5); root.add(guidance, BorderLayout.SOUTH);
        refreshCounts(); return root;
    }

    private JPanel card(String title, JLabel value) {
        JPanel p = new JPanel(new GridLayout(0, 1, 7, 7)); p.setBackground(Theme.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.GREEN, 2), BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        JLabel l = new JLabel(title); l.setForeground(Color.BLACK); l.setFont(new Font("SansSerif", Font.BOLD, 12));
        value.setForeground(Theme.GREEN); value.setFont(new Font("SansSerif", Font.BOLD, 38)); p.add(l); p.add(value); return p;
    }

    private String guidanceText() {
        if (user.isAdmin()) return "ADMINISTRATOR WORKFLOW\nManage departments, programs, courses, accounts, students, class sections, enrollment and grading. Use Gradebook & Reports to review academic performance and Settings to adjust appearance.";
        if (user.isRegistrar()) return "REGISTRAR WORKFLOW\nMaintain student profiles, process course enrollment, and search or print academic records. Course definitions, user accounts and grade changes are reserved for authorized roles.";
        return "INSTRUCTOR WORKFLOW\nReview your assigned class sections, enter and calculate grade components, and inspect class grade reports. You can adjust appearance settings or log out at any time.";
    }

    private void refreshCounts() {
        try { studentCount.setText(String.valueOf(new StudentDAO().count())); courseCount.setText(String.valueOf(new CourseDAO().count())); enrollmentCount.setText(String.valueOf(new EnrollmentDAO().count())); }
        catch (Exception ex) { studentCount.setText("—"); courseCount.setText("—"); enrollmentCount.setText("—"); }
    }
    private static String roleLabel(Role role) { return switch (role) { case ADMIN -> "Administrator"; case REGISTRAR -> "Registrar"; case INSTRUCTOR -> "Instructor"; }; }
}
