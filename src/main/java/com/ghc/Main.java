
package com.ghc;

import com.ghc.database.DatabaseInitializer;
import com.ghc.ui.LoginFrame;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.Image;
import java.net.URL;

public class Main {

    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Could not initialize the GHC database. "
                    + "Check Java/database permissions.\n"
                    + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();

            // Set the GHC logo on the login window.
            URL logoUrl = Main.class.getResource("/ghc-logo.png");

            if (logoUrl != null) {
                Image logo = new ImageIcon(logoUrl).getImage()
                        .getScaledInstance(32, 32, Image.SCALE_SMOOTH);

                login.setIconImage(logo);
            }

            login.setVisible(true);
        });
    }
}
