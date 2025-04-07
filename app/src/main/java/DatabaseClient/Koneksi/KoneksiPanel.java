package DatabaseClient.Koneksi;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.sql.*;

public class KoneksiPanel {
    public static class ConnectAction implements ActionListener {
        private JTextField hostField, portField, userField;
        private JPasswordField passField;
        private JFrame frame;
        private DefaultMutableTreeNode rootNode;
        private DefaultTreeModel treeModel;
        private Connection[] connectionHolder;

        public ConnectAction(JTextField hostField, JTextField portField, JTextField userField,
                             JPasswordField passField, JFrame frame,
                             DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel,
                             Connection[] connectionHolder) {
            this.hostField = hostField;
            this.portField = portField;
            this.userField = userField;
            this.passField = passField;
            this.frame = frame;
            this.rootNode = rootNode;
            this.treeModel = treeModel;
            this.connectionHolder = connectionHolder;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String host = hostField.getText();
            String port = portField.getText();
            String user = userField.getText();
            String password = new String(passField.getPassword());
            String url = "jdbc:mysql://" + host + ":" + port + "/?serverTimezone=UTC";

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connectionHolder[0] = DriverManager.getConnection(url, user, password);
                JOptionPane.showMessageDialog(frame, "Connection Successful");
                loadDatabases();
            } catch (ClassNotFoundException | SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Connection Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void loadDatabases() {
            try {
                Statement stmt = connectionHolder[0].createStatement();
                ResultSet rs = stmt.executeQuery("SHOW DATABASES");
                rootNode.removeAllChildren();
                while (rs.next()) {
                    rootNode.add(new DefaultMutableTreeNode(rs.getString(1)));
                }
                treeModel.reload();
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error loading databases: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
