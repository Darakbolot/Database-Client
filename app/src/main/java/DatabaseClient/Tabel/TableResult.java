package DatabaseClient.Tabel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableResult {
    public static String printTable(ResultSet rs) throws SQLException {
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

        // Build ASCII table
        StringBuilder sb = new StringBuilder();
        String horizontal = "+";
        for (int width : colWidths) {
            horizontal += "-".repeat(width + 2) + "+";
        }

        // Header
        sb.append(horizontal).append("\n").append("|");
        for (int i = 0; i < colCount; i++) {
            sb.append(" ").append(centerText(header[i], colWidths[i])).append(" |");
        }
        sb.append("\n").append(horizontal).append("\n");

        // Rows
        for (int r = 1; r < table.size(); r++) {
            String[] row = table.get(r);
            sb.append("|");
            for (int i = 0; i < colCount; i++) {
                sb.append(" ").append(centerText(row[i], colWidths[i])).append(" |");
            }
            sb.append("\n");
        }

        sb.append(horizontal).append("\n(").append(rowCount).append(" rows)");
        return sb.toString();
    }

    private static String centerText(String text, int width) {
        int padSize = width - text.length();
        int padStart = padSize / 2;
        int padEnd = padSize - padStart;
        return " ".repeat(padStart) + text + " ".repeat(padEnd);
    }
}
