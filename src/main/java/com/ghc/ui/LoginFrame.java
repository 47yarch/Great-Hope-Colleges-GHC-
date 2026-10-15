package com.ghc.ui;

import com.ghc.dao.UserDAO;
import com.ghc.security.SessionContext;
import com.ghc.security.UserSession;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField user = new JTextField(18);
    private final JPasswordField pass = new JPasswordField(18);
    private final JButton loginButton = Theme.button("Sign In");

    public LoginFrame() {
        Theme.apply();

        setTitle("GHC | Secure Login");
        setSize(460, 370);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BEIGE);

        JPanel head = new JPanel(new GridLayout(0, 1, 4, 4));
        head.setBackground(Theme.GREEN);
        head.setBorder(BorderFactory.createEmptyBorder(24, 25, 24, 25));

        JLabel title = new JLabel(
                "GREAT HOPE COLLEGES", SwingConstants.CENTER);
        title.setForeground(Theme.YELLOW);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel sub = new JLabel(
                "Student Enrollment, Grading & Records",
                SwingConstants.CENTER);
        sub.setForeground(Color.WHITE);

        head.add(title);
        head.add(sub);
        root.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BEIGE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(7, 8, 7, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0;
        g.gridy = 0;
        form.add(new JLabel("Username"), g);

        g.gridx = 1;
        form.add(user, g);

        g.gridx = 0;
        g.gridy = 1;
        form.add(new JLabel("Password"), g);

        g.gridx = 1;
        form.add(pass, g);

        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        form.add(loginButton, g);

        root.add(form, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "Default administrator: admin / admin123",
                SwingConstants.CENTER);
        hint.setForeground(Theme.MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        root.add(hint, BorderLayout.SOUTH);

        setContentPane(root);

        loginButton.addActionListener(e -> authenticate());
        pass.addActionListener(e -> authenticate());
    }

    private void authenticate() {
        loginButton.setEnabled(false);

        try {
            UserSession session = new UserDAO().login(
                    user.getText(),
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
            JOptionPane.showMessageDialog(
                    this,
                    "Login failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            loginButton.setEnabled(true);
        }
    }
}
