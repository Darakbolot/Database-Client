package DatabaseClient;

import DatabaseClient.Koneksi.KoneksiPanel.ConnectAction;
// import DatabaseClient.Navigator.NavigatorPanel.DatabaseTreeListener;
import DatabaseClient.SyntaxHighlight.Highlight;
import DatabaseClient.Tabel.TableResult;

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
    private JButton connectButton, executeButton;
    private JTextArea resultArea;
    private JTree databaseTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private Connection[] connectionHolder = new Connection[1]; // Holder connection

    public main() {
        frame = new JFrame("Java Database Client");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

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
        gbc.weightx = 5;
        connectionPanel.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 5;
        connectionPanel.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        connectionPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 5;
        connectionPanel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        connectionPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 5;
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

        databaseTree = new JTree(treeModel);
        databaseTree.addMouseListener(new MouseAdapter() {  // Terapkan ke databaseTree, bukan treeModel
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Cek double click
                    TreePath path = databaseTree.getPathForLocation(e.getX(), e.getY()); // Gunakan databaseTree, bukan treeModel
                    if (path != null) {
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (selectedNode != null && selectedNode.isLeaf()) { // Pastikan yang diklik adalah tabel, bukan database
                            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent(); // Dapatkan parent (database)
                            
                            if (parentNode != null) { 
                                String databaseName = parentNode.toString(); // Nama database
                                String tableName = selectedNode.toString(); // Nama tabel
                        
                                // Query untuk memilih database terlebih dahulu sebelum SELECT
                                String query = "USE " + databaseName + ";";
                        
                                queryPane.setText(query); // Isi query editor
                                executeQuery(query); // Eksekusi query langsung

                                query = "SELECT * FROM " + tableName + " LIMIT 100;";

                                queryPane.setText(query); // Isi query editor
                                executeQuery(query); // Eksekusi query langsung
                                queryPane.setText(""); // Kosongkan query editor setelah eksekusi
                            }
                        }                        
                    }
                }
            }
        });
        

        databaseTree.addMouseListener(new DatabaseTreeListener());

        JScrollPane treeScroll = new JScrollPane(databaseTree);
        treeScroll.setMinimumSize(new Dimension(150, 0));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(treeScroll, BorderLayout.CENTER);

        queryPane = new JTextPane();
        queryDoc = queryPane.getStyledDocument();
        queryPane.setFont(new Font("Old English Text MT", Font.PLAIN, 14)); // Bisa ganti jadi font SMP

        // Tambahkan DocumentFilter untuk Syntax Highlighting
        ((AbstractDocument) queryDoc).setDocumentFilter(new Highlight(queryPane));

        JScrollPane queryScroll = new JScrollPane(queryPane);
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.add(queryScroll, BorderLayout.CENTER);
        queryPane.addKeyListener(new QueryKeyListener()); // Tambahkan KeyListener untuk Enter

        executeButton = new JButton("Execute Query");
        executeButton.addActionListener(e -> executeAndClear());
        
        queryPanel.add(executeButton, BorderLayout.SOUTH);
        queryPanel.add(new JScrollPane(queryPane), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, queryPanel);
        splitPane.setDividerLocation(150);
        splitPane.setResizeWeight(0.2);

        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            int loc = splitPane.getDividerLocation();
            if (loc < 150) splitPane.setDividerLocation(150);
            else if (loc > 200) splitPane.setDividerLocation(200);
        });

        executeButton = new JButton("Execute Query");
        executeButton.addActionListener(new QueryAction());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(executeButton, BorderLayout.SOUTH);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        frame.add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private class QueryKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                String query = queryPane.getText().trim();
                if (query.endsWith(";")) {
                    queryPane.setText(""); // Kosongkan query setelah eksekusi
                    executeQuery(query);
                    e.consume(); // Hentikan enter agar tidak menambah baris baru
                } else {
                    // Tambahkan newline secara manual
                    try {
                        queryDoc.insertString(queryDoc.getLength(), "\n", null);
                        e.consume(); // Mencegah enter default menambah baris ekstra
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                    
                }
            }
        }
    }

    private void executeQuery(String query) {
        Connection connection = connectionHolder[0];
        if (connection == null) {
            JOptionPane.showMessageDialog(frame, "Not connected to any database.");
            return;
        }
        try (Statement statement = connection.createStatement()) {

        String queryTrimmed = query.trim().toLowerCase();

        if (queryTrimmed.startsWith("select")) {
            try (ResultSet rs = statement.executeQuery(query)) {
                String resultTable = TableResult.printTable(rs);
                resultArea.append(resultTable + "\n");
            }
        } else {
            int rowsAffected = statement.executeUpdate(query);
            resultArea.append("Query executed successfully. Rows affected: " + rowsAffected + "\n\n");
        }
    } catch (SQLException ex) {
            JLabel errorLabel = new JLabel("Query Error: " + ex.getMessage() + '\n');
            errorLabel.setForeground(Color.RED);
    
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createLineBorder(Color.RED));
            panel.add(errorLabel, BorderLayout.CENTER);
            resultArea.add(panel);
            resultArea.revalidate();
            resultArea.repaint();
        }
    }

    public ActionListener getQueryActionListener() {
        return new QueryAction();
    }

    private class QueryAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            executeAndClear();
        }
    }

    public void executeAndClear() {
        String query = queryPane.getText().trim();
        if (!query.isEmpty()) {
            queryPane.setText(""); // Hapus query setelah eksekusi
            executeQuery(query);
        }
    }
    public class DatabaseTreeListener extends MouseAdapter {
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
        Connection connection = connectionHolder[0];
        if (connection == null) {
            JOptionPane.showMessageDialog(frame, "Not connected to any database.");
            return;
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(main::new);
    }
}
