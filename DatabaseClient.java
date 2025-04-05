import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.*;
import javax.swing.tree.*;
import javax.swing.text.*;

public class DatabaseClient {
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
    private Connection connection;

    public DatabaseClient() {
        frame = new JFrame("Java Database Client");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel connectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("Host:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 5;
        hostField = new JTextField("localhost");
        connectionPanel.add(hostField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionPanel.add(new JLabel("Port:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 5;
        portField = new JTextField("3306");
        connectionPanel.add(portField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        connectionPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 5;
        userField = new JTextField("root");
        connectionPanel.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        connectionPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 5;
        passField = new JPasswordField();
        connectionPanel.add(passField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        connectButton = new JButton("Connect");
        connectButton.addActionListener(new ConnectAction());
        connectionPanel.add(connectButton, gbc);

        frame.add(connectionPanel, BorderLayout.NORTH);

        rootNode = new DefaultMutableTreeNode("Databases");
        treeModel = new DefaultTreeModel(rootNode);

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
        ((AbstractDocument) queryDoc).setDocumentFilter(new SyntaxHighlighter(queryPane));

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

    private void executeAndClear() {
        String query = queryPane.getText().trim();
        if (!query.isEmpty()) {
            executeQuery(query);
            queryPane.setText(""); // Hapus query setelah dieksekusi
        }
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
    
    class SyntaxHighlighter extends DocumentFilter {
        private JTextPane textPane;
        private StyledDocument doc;
    
        // Gaya untuk warna sintaks
        private Style keywordStyle;
        private Style defaultStyle;
        private Style redStyle;
        private Style greenStyle;
    
        public SyntaxHighlighter(JTextPane textPane) {
            this.textPane = textPane;
            this.doc = textPane.getStyledDocument();
            
            keywordStyle = textPane.addStyle("Keyword", null);
            StyleConstants.setForeground(keywordStyle, Color.BLUE);
            StyleConstants.setBold(keywordStyle, true);
    
            defaultStyle = textPane.addStyle("Default", null);
            StyleConstants.setForeground(defaultStyle, Color.BLACK);

            redStyle = textPane.addStyle("Red", null);
            StyleConstants.setForeground(redStyle, Color.RED);

            greenStyle = textPane.addStyle("Green", null);
            StyleConstants.setForeground(greenStyle, Color.GREEN);
        }
    
        private void highlightSyntax() {

            SwingUtilities.invokeLater(() -> {
                try {
                    String text = doc.getText(0, doc.getLength());
    
                    // Reset ke default
                    doc.setCharacterAttributes(0, text.length(), defaultStyle, true);
    
                    // Pola regex untuk kata kunci SQL
                    Pattern pattern = Pattern.compile("\\b(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|USE|SHOW|TABLE|DATABASE)\\b", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(text);
    
                    while (matcher.find()) {
                        doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keywordStyle, false);
                    }

                    Pattern conjunctions = Pattern.compile("\\b(FROM|WHERE|ALTER|JOIN|ON|INTO|VALUES)\\b", Pattern.CASE_INSENSITIVE);
                    Matcher conjunctionsMatcher = conjunctions.matcher(text);
    
                    while (conjunctionsMatcher.find()) {
                        doc.setCharacterAttributes(conjunctionsMatcher.start(), conjunctionsMatcher.end() - conjunctionsMatcher.start(), greenStyle, false);
                    }

                    Pattern quote = Pattern.compile("'[^']*'|\"[^\"]*\"");
                    Matcher quoteMatcher = quote.matcher(text);

                    while (quoteMatcher.find()) {
                        doc.setCharacterAttributes(quoteMatcher.start(), quoteMatcher.end() - quoteMatcher.start(), redStyle, false);
                    }

                    Pattern symbols = Pattern.compile("[(){}*+\\-]");
                    Matcher symbolsMatcher = symbols.matcher(text);
                    while (symbolsMatcher.find()) {
                        doc.setCharacterAttributes(symbolsMatcher.start(), symbolsMatcher.end() - symbolsMatcher.start(), redStyle, false);
                    }

                    SwingUtilities.invokeLater(() -> textPane.repaint());

                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        }
    
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            super.insertString(fb, offset, string, attr);
            highlightSyntax();
        }
    
        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
            highlightSyntax();
        }
    
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(fb, offset, length, text, attrs);
            highlightSyntax();
        }
    }

    private void executeQuery(String query) {
        try {
            Statement statement = connection.createStatement();
            StringBuilder resultText = new StringBuilder();
    
            if (query.trim().toLowerCase().startsWith("select")) {
                ResultSet rs = statement.executeQuery(query);
                resultArea.append(printTable(rs) + "\n"); // tampilkan tabel dan hasil ke resultArea
            } else {
                int rowsAffected = statement.executeUpdate(query);
                resultText.append("Query executed successfully. Rows affected: ").append(rowsAffected).append('\n');
            }
            
            resultArea.append(resultText.toString() + "\n");
            statement.close();
    
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

    private String printTable(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
    
        List<String[]> table = new ArrayList<>();
        int[] colWidths = new int[colCount];
    
        // Header
        String[] header = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            header[i] = meta.getColumnLabel(i + 1);
            colWidths[i] = header[i].length();
        }
        table.add(header);
    
        // Rows
        int rowCount = 0;
        while (rs.next()) {
            String[] row = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                String value = rs.getString(i + 1);
                if (value == null) value = "";
                row[i] = value;
                colWidths[i] = Math.max(colWidths[i], value.length());
            }
            table.add(row);
            rowCount++;
        }
    
        // Buat tabel ASCII
        StringBuilder sb = new StringBuilder();
    
        // Horizontal border
        String horizontal = "+";
        for (int width : colWidths) {
            horizontal += "-".repeat(width + 2) + "+";
        }
    
        // Cetak header
        sb.append(horizontal).append("\n");
        String[] headerRow = table.get(0);
        sb.append("|");
        for (int i = 0; i < colCount; i++) {
            sb.append(" ").append(centerText(headerRow[i], colWidths[i])).append(" |");
        }
        sb.append("\n");
        sb.append(horizontal).append("\n");
    
        // Cetak isi tabel
        for (int r = 1; r < table.size(); r++) {
            String[] row = table.get(r);
            sb.append("|");
            for (int i = 0; i < colCount; i++) {
                sb.append(" ").append(centerText(row[i], colWidths[i])).append(" |");
            }
            sb.append("\n");
        }
    
        sb.append(horizontal).append("\n");
        sb.append("(").append(rowCount).append(" rows)");
    
        return sb.toString();
    }
    
    private String centerText(String text, int width) {
        if (text == null) text = "";
        int padSize = width - text.length();
        int padStart = padSize / 2;
        int padEnd = padSize - padStart;
        return " ".repeat(padStart) + text + " ".repeat(padEnd);
    }

    private class ConnectAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String host = hostField.getText();
            String port = portField.getText();
            String user = userField.getText();
            String password = new String(passField.getPassword());
            String url = "jdbc:mysql://" + host + ":" + port + "/?serverTimezone=UTC";
            
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, user, password);
                JOptionPane.showMessageDialog(frame, "Connection Successful");
                loadDatabases();
            } catch (ClassNotFoundException | SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Connection Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadDatabases() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SHOW DATABASES");
            rootNode.removeAllChildren();
            while (resultSet.next()) {
                rootNode.add(new DefaultMutableTreeNode(resultSet.getString(1)));
            }
            treeModel.reload();
            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading databases: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private class QueryAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String query = queryPane.getText();
            try {
                Statement statement = connection.createStatement();
                if (query.trim().toLowerCase().startsWith("select")) {
                    ResultSet resultSet = statement.executeQuery(query);
                    StringBuilder resultText = new StringBuilder();
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        resultText.append(resultSet.getMetaData().getColumnName(i)).append("\t");
                    }
                    resultText.append("\n");
                    while (resultSet.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            resultText.append(resultSet.getString(i)).append("\t");
                        }
                        resultText.append("\n");
                    }
                    resultArea.setText(resultText.toString());
                    resultSet.close();
                } else {
                    int rowsAffected = statement.executeUpdate(query);
                    resultArea.setText("Query executed successfully. Rows affected: " + rowsAffected + '\n');
                    loadDatabases();
                }
                statement.close();
            } catch (SQLException ex) {
                resultArea.setText("Query Error: " + ex.getMessage() + '\n');
            }
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DatabaseClient::new);
    }
}
