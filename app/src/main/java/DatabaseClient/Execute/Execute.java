// package DatabaseClient.Execute;

// import DatabaseClient.Tabel.TableResult;

// import javax.swing.*;
// import javax.swing.text.BadLocationException;
// import javax.swing.text.StyledDocument;
// import java.awt.event.*;
// import java.sql.*;

// public class Execute {
//     private final JTextPane queryPane;
//     private final JTextArea resultArea;
//     private final StyledDocument queryDoc;
//     private final Connection connection;

//     public void executeAndClear() {
//         String query = queryPane.getText().trim();
//         if (!query.isEmpty()) {
//             queryPane.setText(""); // Hapus query setelah eksekusi
//             executeQuery(query);
//         }
//     }

//     public void executeQuery(String query) {
//         resultArea.append(">> " + query + "\n");

//         try (Statement statement = connection.createStatement()) {
//             if (query.toLowerCase().startsWith("select")) {
//                 try (ResultSet rs = statement.executeQuery(query)) {
//                     String resultTable = TableResult.printTable(rs);
//                     resultArea.append(resultTable + "\n");
//                 }
//             } else {
//                 int rowsAffected = statement.executeUpdate(query);
//                 resultArea.append("Query executed successfully. Rows affected: " + rowsAffected + "\n\n");
//             }
//         } catch (SQLException ex) {
//             resultArea.append("Query Error: " + ex.getMessage() + "\n\n");
//         }
//     }

//     // (Opsional) Jika butuh ActionListener terpisah, bisa expose seperti ini:
//     public ActionListener getQueryActionListener() {
//         return new QueryAction();
//     }

//     private class QueryAction implements ActionListener {
//         @Override
//         public void actionPerformed(ActionEvent e) {
//             executeAndClear();
//         }
//     }
// }
