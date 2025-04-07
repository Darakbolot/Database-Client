package DatabaseClient.SyntaxHighlight;

import javax.swing.*;
import java.awt.*;
import java.util.regex.*;
import javax.swing.text.*;

public class Highlight extends DocumentFilter {
    private JTextPane textPane;
    private StyledDocument doc;

    private Style keywordStyle;
    private Style defaultStyle;
    private Style redStyle;
    private Style greenStyle;

    public Highlight(JTextPane textPane) {
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
                doc.setCharacterAttributes(0, text.length(), defaultStyle, true);

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
