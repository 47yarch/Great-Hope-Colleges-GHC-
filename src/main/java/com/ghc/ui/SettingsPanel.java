package com.ghc.ui;

import com.ghc.database.DatabaseConnection;
import com.ghc.security.AccessControl;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** User-local appearance preferences plus administrator-managed general settings. */
final class SettingsPanel extends JPanel {
    SettingsPanel(){setLayout(new BorderLayout());setBackground(Theme.BEIGE);setBorder(BorderFactory.createEmptyBorder(24,28,24,28));JPanel form=new JPanel(new GridLayout(0,2,12,12));form.setBackground(Theme.BEIGE);JLabel title=new JLabel("Appearance & System Settings");title.setFont(new Font("SansSerif",Font.BOLD,22));title.setForeground(Color.BLACK);add(title,BorderLayout.NORTH);
        JComboBox<String> density=new JComboBox<>(new String[]{"Comfortable tables","Compact tables"});JComboBox<String> theme=new JComboBox<>(new String[]{"GHC Green / Gold / Beige"});JTextField schoolYear=new JTextField(read("CURRENT_SCHOOL_YEAR","2026-2027"));JComboBox<String> semester=new JComboBox<>(new String[]{"1st Semester","2nd Semester","Summer"});semester.setSelectedItem(read("CURRENT_SEMESTER","1st Semester"));boolean admin=com.ghc.security.SessionContext.currentUser().isAdmin();schoolYear.setEnabled(admin);semester.setEnabled(admin);
        form.add(new JLabel("Interface theme"));form.add(theme);form.add(new JLabel("Table density"));form.add(density);form.add(new JLabel("Current school year"));form.add(schoolYear);form.add(new JLabel("Current semester"));form.add(semester);JButton save=Theme.button("Save Settings");JPanel south=new JPanel(new FlowLayout(FlowLayout.LEFT));south.setOpaque(false);south.add(save);add(form,BorderLayout.CENTER);add(south,BorderLayout.SOUTH);
        save.addActionListener(e->{try{AccessControl.requireSignedIn();UIManager.put("Table.rowHeight", "Compact tables".equals(density.getSelectedItem()) ? 20 : 26);for(Window w:Window.getWindows())SwingUtilities.updateComponentTreeUI(w);if(AccessControl.requireSignedIn().isAdmin()){if(schoolYear.getText().isBlank())throw new IllegalArgumentException("School year is required.");write("CURRENT_SCHOOL_YEAR",schoolYear.getText().trim());write("CURRENT_SEMESTER",String.valueOf(semester.getSelectedItem()));write("TABLE_DENSITY",String.valueOf(density.getSelectedItem()));JOptionPane.showMessageDialog(this,"Appearance and school settings saved.");}else{JOptionPane.showMessageDialog(this,"Appearance preference applied for this session. Only administrators can change school-wide settings.");}}catch(Exception ex){JOptionPane.showMessageDialog(this,"Could not save settings: "+ex.getMessage());}});
    }
    private String read(String key,String fallback){try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT SETTING_VALUE FROM APP_SETTINGS WHERE SETTING_KEY=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):fallback;}}catch(Exception e){return fallback;}}
    private void write(String key,String value)throws Exception{try(Connection c=DatabaseConnection.getConnection();PreparedStatement q=c.prepareStatement("UPDATE APP_SETTINGS SET SETTING_VALUE=? WHERE SETTING_KEY=?")){q.setString(1,value);q.setString(2,key);if(q.executeUpdate()>0)return;}try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO APP_SETTINGS (SETTING_KEY,SETTING_VALUE) VALUES (?,?)")){p.setString(1,key);p.setString(2,value);p.executeUpdate();}}
}
