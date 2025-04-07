package DatabaseClient.Navigator;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.event.*;
import java.sql.*;

public class NavigatorPanel {
    private final JTree databaseTree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final Connection connection;
    private final JFrame frame;

    public NavigatorPanel(JTree databaseTree, DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel, Connection connection, JFrame frame) {
        this.databaseTree = databaseTree;
        this.rootNode = rootNode;
        this.treeModel = treeModel;
        this.connection = connection;
        this.frame = frame;

        this.databaseTree.addMouseListener(new DatabaseTreeListener());
    }

    private class DatabaseTreeListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());
            if (path != null && path.getPathCount() == 2) {
                String databaseName = path.getLastPathComponent().toString();
                loadTables(databaseName);
            }
        }
    }

    private void loadTables(String databaseName) {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SHOW TABLES FROM " + databaseName);
            DefaultMutableTreeNode dbNode = new DefaultMutableTreeNode(databaseName);
            while (resultSet.next()) {
                dbNode.add(new DefaultMutableTreeNode(resultSet.getString(1)));
            }
            rootNode.add(dbNode);
            treeModel.reload();
            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading tables: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
