package com.ghc;
import com.ghc.database.DatabaseInitializer;
import com.ghc.ui.LoginFrame;
import com.ghc.ui.Theme;
import javax.swing.SwingUtilities;
public class Main {
  public static void main(String[] args) {
    try { DatabaseInitializer.initialize(); }
    catch (Exception e) { e.printStackTrace(); javax.swing.JOptionPane.showMessageDialog(null,"Could not initialize the GHC database. Check Java/database permissions.\\n"+e.getMessage(),"Database Error",javax.swing.JOptionPane.ERROR_MESSAGE); return; }
    SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
  }
}
