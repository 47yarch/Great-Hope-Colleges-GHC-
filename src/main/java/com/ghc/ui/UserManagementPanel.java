package com.ghc.ui;

import com.ghc.dao.UserDAO;
import com.ghc.security.Role;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Administrator-only account management screen. */
final class UserManagementPanel extends JPanel {
    private final UserDAO dao = new UserDAO();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Username", "Full Name", "Role", "Status"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(20);
    UserManagementPanel() {
        setLayout(new BorderLayout(8,8)); setBackground(Theme.BEIGE); setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.setOpaque(false);
        top.add(new JLabel("Search accounts:")); top.add(search);
        JButton find=Theme.button("Search"), refresh=Theme.button("Refresh"), add=Theme.button("Create Account"), edit=Theme.button("Edit Name / Role"), toggle=Theme.button("Activate / Deactivate"), reset=Theme.button("Reset Password");
        top.add(find); top.add(refresh); top.add(add); top.add(edit); top.add(toggle); top.add(reset); add(top,BorderLayout.NORTH);
        Theme.styleTable(table); add(new JScrollPane(table),BorderLayout.CENTER);
        find.addActionListener(e->load()); search.addActionListener(e->load()); refresh.addActionListener(e->{search.setText("");load();});
        add.addActionListener(e->create()); edit.addActionListener(e->edit()); toggle.addActionListener(e->toggle()); reset.addActionListener(e->resetPassword()); load();
    }
    private int selectedId(){int row=table.getSelectedRow(); if(row<0){msg("Select an account first.");return -1;} return ((Number)model.getValueAt(table.convertRowIndexToModel(row),0)).intValue();}
    private void load(){try{model.setRowCount(0);for(Object[] r:dao.all(search.getText()))model.addRow(r);}catch(Exception e){fail(e);}}
    private void create(){JTextField username=new JTextField(), full=new JTextField(); JPasswordField password=new JPasswordField(); JComboBox<Role> role=new JComboBox<>(Role.values());
        JPanel p=form(new String[]{"Username","Full name","Temporary password (8+ characters)","Role"},new JComponent[]{username,full,password,role});
        if(JOptionPane.showConfirmDialog(this,p,"Create User Account",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{dao.createUser(username.getText(),new String(password.getPassword()),full.getText(),(Role)role.getSelectedItem());load();msg("Account created.");}catch(Exception e){fail(e);}}
    private void edit(){int id=selectedId();if(id<0)return;int row=table.convertRowIndexToModel(table.getSelectedRow());String oldName=String.valueOf(model.getValueAt(row,2));String oldRole=String.valueOf(model.getValueAt(row,3));JTextField name=new JTextField(oldName);JComboBox<Role> role=new JComboBox<>(Role.values());role.setSelectedItem(Role.fromDatabase(oldRole));
        if(JOptionPane.showConfirmDialog(this,form(new String[]{"Full name","Role"},new JComponent[]{name,role}),"Edit Account",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{dao.updateUser(id,name.getText(),(Role)role.getSelectedItem());load();}catch(Exception e){fail(e);}}
    private void toggle(){int id=selectedId();if(id<0)return;int row=table.convertRowIndexToModel(table.getSelectedRow());boolean active="Active".equalsIgnoreCase(String.valueOf(model.getValueAt(row,4)));try{dao.setActive(id,!active);load();}catch(Exception e){fail(e);}}
    private void resetPassword(){int id=selectedId();if(id<0)return;JPasswordField password=new JPasswordField();if(JOptionPane.showConfirmDialog(this,password,"Enter new password (8+ characters)",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{dao.resetPassword(id,new String(password.getPassword()));msg("Password reset.");}catch(Exception e){fail(e);}}
    static JPanel form(String[] labels,JComponent[] fields){JPanel p=new JPanel(new GridLayout(0,2,8,8));p.setBackground(Theme.BEIGE);for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));p.add(fields[i]);}return p;}
    private void fail(Exception e){msg("Action failed: "+e.getMessage());} private void msg(String s){JOptionPane.showMessageDialog(this,s,"GHC",JOptionPane.INFORMATION_MESSAGE);}
}
