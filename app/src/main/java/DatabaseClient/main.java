package DatabaseClient;

import DatabaseClient.Execute.Execute;
import DatabaseClient.Koneksi.KoneksiPanel.ConnectAction;
import DatabaseClient.SyntaxHighlight.Highlight;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class main {
    private JFrame frame;
    private JTextPane queryPane;
    private StyledDocument queryDoc;
    private JTextField hostField, userField, portField;
    private JPasswordField passField;
    private JButton connectButton;
    private JTextArea resultArea;
    private JTree databaseTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private Connection[] connectionHolder = new Connection[1]; // Holder connection
    private Execute executor;

    public main() {
        frame = new JFrame("Java Database Client");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        setupConnectionPanel();
        setupTreePanel();
        setupQueryPanel();
        setupResultPanel();

        frame.setVisible(true);
    }

    private void setupConnectionPanel() {
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        hostField = new JTextField("localhost");
        portField = new JTextField("3306");
        userField = new JTextField("root");
        passField = new JPasswordField();

        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        connectionPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        connectionPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        connectionPanel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        connectButton = new JButton("Connect");

        rootNode = new DefaultMutableTreeNode("Databases");
        treeModel = new DefaultTreeModel(rootNode);

        connectButton.addActionListener(
            new ConnectAction(
                hostField, portField, userField, passField,
                frame, rootNode, treeModel, connectionHolder
            )
        );

        connectionPanel.add(connectButton, gbc);
        frame.add(connectionPanel, BorderLayout.NORTH);
    }

    private void setupTreePanel() {
        databaseTree = new JTree(treeModel);

        databaseTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (selectedNode != null && selectedNode.isLeaf()) {
                            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
                            if (parentNode != null) {
                                String databaseName = parentNode.toString();
                                String tableName = selectedNode.toString();
                                String query1 = "USE " + databaseName + ";";
                                String query2 = "SELECT * FROM " + tableName + " LIMIT 100;";
                                queryPane.setText(query1);
                                executor.executeQuery(query1);
                                queryPane.setText(query2);
                                executor.executeQuery(query2);
                                queryPane.setText("");
                            }
                        }
                    }
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(databaseTree);
        treeScroll.setMinimumSize(new Dimension(150, 0));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(treeScroll, BorderLayout.CENTER);

        frame.add(leftPanel, BorderLayout.WEST);
    }

    private void setupQueryPanel() {
        queryPane = new JTextPane();
        queryDoc = queryPane.getStyledDocument();
        queryPane.setFont(new Font("Consolas", Font.PLAIN, 14));

        ((AbstractDocument) queryDoc).setDocumentFilter(new Highlight(queryPane));

        executor = new Execute(queryPane, resultArea, connectionHolder[0]);

        JScrollPane queryScroll = new JScrollPane(queryPane);
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.add(queryScroll, BorderLayout.CENTER);

        JButton executeButton = new JButton("Execute Query");
        executeButton.addActionListener(e -> executor.executeAndClear());

        queryPanel.add(executeButton, BorderLayout.SOUTH);

        frame.add(queryPanel, BorderLayout.CENTER);
    }

    private void setupResultPanel() {
        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        JScrollPane resultScroll = new JScrollPane(resultArea);
        frame.add(resultScroll, BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(main::new);
    }
}
