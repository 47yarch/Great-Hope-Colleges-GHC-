
package com.ghc.ui;

import com.ghc.dao.UserDAO;
import com.ghc.security.SessionContext;
import com.ghc.security.UserSession;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class LoginFrame extends JFrame {

    private final JTextField user = new JTextField(18);
    private final JPasswordField pass = new JPasswordField(18);
    private final JButton loginButton = Theme.button("Sign In");

    public LoginFrame() {
        Theme.apply();

        setTitle("GHC | Login");
        setSize(480, 540);
        setMinimumSize(new Dimension(460, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Set the small GHC icon in the window title bar.
        URL logoUrl = LoginFrame.class.getResource("/ghc-logo.png");

        if (logoUrl != null) {
            Image originalImage = new ImageIcon(logoUrl).getImage();

            Image windowIcon = originalImage.getScaledInstance(
                    32, 32, Image.SCALE_SMOOTH);

            setIconImage(windowIcon);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BEIGE);

        // Header with logo, college name, and subtitle.
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBackground(Theme.GREEN);
        head.setBorder(BorderFactory.createEmptyBorder(18, 25, 20, 25));

        // Display the actual logo image inside the login screen.
        JLabel logoLabel;

        if (logoUrl != null) {
            Image originalImage = new ImageIcon(logoUrl).getImage();

            Image displayImage = originalImage.getScaledInstance(
                    100, 100, Image.SCALE_SMOOTH);

            logoLabel = new JLabel(new ImageIcon(displayImage));
        } else {
            logoLabel = new JLabel("GHC", SwingConstants.CENTER);
            logoLabel.setForeground(Theme.YELLOW);
            logoLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        }

        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel title = new JLabel(
                "GREAT HOPE COLLEGES", SwingConstants.CENTER);
        title.setForeground(Theme.YELLOW);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(
                "Student Enrollment, Grading & Records",
                SwingConstants.CENTER);
        sub.setForeground(Color.WHITE);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        head.add(logoLabel);
        head.add(Box.createVerticalStrut(10));
        head.add(title);
        head.add(Box.createVerticalStrut(5));
        head.add(sub);

        root.add(head, BorderLayout.NORTH);

          
        // Login form.
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BEIGE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(9, 8, 9, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Username / Student Number label.
        JLabel usernameLabel = new JLabel("Username / Student No.");
        usernameLabel.setFont(Theme.LABEL_FONT);

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        g.insets = new Insets(9, 8, 2, 8);
        form.add(usernameLabel, g);

        // Username field.
        user.setFont(Theme.BODY_FONT);
        user.setToolTipText("For students, enter your student number");

        g.gridx = 1;
        g.gridy = 0;
        g.weightx = 1;
        g.insets = new Insets(9, 8, 2, 8);
        form.add(user, g);

        // Student helper text.
        JLabel studentHint = new JLabel(
                "For students, enter your student number."
        );
        studentHint.setForeground(Theme.MUTED);
        studentHint.setFont(new Font("SansSerif", Font.ITALIC, 10));

        g.gridx = 1;
        g.gridy = 1;
        g.insets = new Insets(0, 8, 9, 8);
        form.add(studentHint, g);

        // Password label.
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(Theme.LABEL_FONT);

        g.gridx = 0;
        g.gridy = 2;
        g.weightx = 0;
        g.insets = new Insets(9, 8, 9, 8);
        form.add(passwordLabel, g);

        // Password field.
        pass.setFont(Theme.BODY_FONT);
        pass.setToolTipText("Enter your password");

        g.gridx = 1;
        g.gridy = 2;
        g.weightx = 1;
        form.add(pass, g);

        // Sign In button.
        loginButton.setFont(Theme.BUTTON_FONT);
        loginButton.setPreferredSize(new Dimension(150, 40));
        loginButton.setBackground(Theme.GREEN);
        loginButton.setForeground(Color.WHITE);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        g.weightx = 1;
        g.insets = new Insets(18, 8, 8, 8);
        form.add(loginButton, g);

        root.add(form, BorderLayout.CENTER);


        // Login hint.
        JLabel hint = new JLabel(
                "Created by: Agustin & Sabulao | BSIT - 2B",
                SwingConstants.CENTER);
        hint.setForeground(Theme.MUTED);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setBorder(BorderFactory.createEmptyBorder(8, 10, 16, 10));

        root.add(hint, BorderLayout.SOUTH);

        setContentPane(root);

        loginButton.addActionListener(e -> authenticate());
        pass.addActionListener(e -> authenticate());

        getRootPane().setDefaultButton(loginButton);
    }

    private void authenticate() {
        loginButton.setEnabled(false);

        try {
            UserSession session = new UserDAO().login(
                    user.getText().trim(),
                    new String(pass.getPassword())
            );

            if (session == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid username/password, or this account is inactive.",
                        "Sign in failed",
                        JOptionPane.WARNING_MESSAGE
                );

                pass.setText("");
                pass.requestFocusInWindow();
                return;
            }

            SessionContext.signIn(session);
            dispose();
            new DashboardFrame().setVisible(true);

        } catch (Exception e) {
    e.printStackTrace();

    JOptionPane.showMessageDialog(
            this,
            "Login failed: " + e.getMessage()
                    + "\nCheck the Run console for the exact source line.",
            "Error",
            JOptionPane.ERROR_MESSAGE
    );
} finally {
    loginButton.setEnabled(true);
}
    }
}
