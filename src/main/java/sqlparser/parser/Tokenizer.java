package main.java.sqlparser.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes SQL queries into individual tokens for parsing.
 */
public class Tokenizer {

    // SQL keywords for case-insensitive matching
    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
      "SELECT", "FROM", "WHERE", "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "OFFSET",
      "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON", "USING",
      "AND", "OR", "NOT", "AS", "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX",
      "UNION", "ALL", "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL", "ASC", "DESC",
      "CASE", "WHEN", "THEN", "ELSE", "END"
    ));

    // Token types
    public enum TokenType {
        KEYWORD,       // SQL keywords (SELECT, FROM, etc.)
        IDENTIFIER,    // Table or column names
        OPERATOR,      // Operators (+, -, *, /, <, >, =, etc.)
        STRING_LITERAL, // String literals ('value')
        NUMERIC_LITERAL, // Numeric literals (123, 3.14)
        PARENTHESIS_OPEN,  // (
        PARENTHESIS_CLOSE, // )
        COMMA,         // ,
        SEMICOLON,     // ;
        DOT,           // .
        STAR,          // *
        WHITESPACE,    // Space, tab, newline
        COMMENT,       // SQL comments
        FUNCTION,      // SQL functions (COUNT, SUM, etc.)
        UNKNOWN        // Unknown token
    }

    public static class Token {
        private final String value;
        private final TokenType type;
        private final int position;

        public Token(String value, TokenType type, int position) {
            this.value = value;
            this.type = type;
            this.position = position;
        }

        public String getValue() {
            return value;
        }

        public TokenType getType() {
            return type;
        }

        public int getPosition() {
            return position;
        }

        @Override
        public String toString() {
            return String.format("%s[%s] @ %d", type, value, position);
        }
    }

    // Regular expressions for token identification
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern QUOTED_IDENTIFIER_PATTERN = Pattern.compile("^\"([^\"]|\"\")*\"");
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("^'([^']|'')*'");
    private static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?([eE][+-]?\\d+)?");
    private static final Pattern OPERATOR_PATTERN = Pattern.compile("^(=|<>|!=|<=|>=|<|>|\\+|-|\\*|/|%|\\|\\||\\&\\&)");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("^\\s+");
    private static final Pattern SINGLE_LINE_COMMENT_PATTERN = Pattern.compile("^--[^\\n]*\\n?");
    private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile("^/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * Tokenizes a SQL query string into a list of tokens.
     *
     * @param sql The SQL query string to tokenize
     * @return List of tokens
     */
    public static List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;

        while (position < sql.length()) {
            // Try to match the next token at the current position
            Token token = matchNextToken(sql, position);

            if (token != null) {
                // Skip whitespace and comments
                if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.COMMENT) {
                    tokens.add(token);
                }
                position += token.getValue().length();
            } else {
                throw new SQLParserException("Invalid token", position);
            }
        }

        return tokens;
    }

    /**
     * Tries to match the next token in the SQL string starting at the given position.
     *
     * @param sql The SQL query string
     * @param position The current position in the string
     * @return The next token, or null if no token could be matched
     */
    private static Token matchNextToken(String sql, int position) {
        String remaining = sql.substring(position);

        // Try to match whitespace
        Matcher whitespaceMatcher = WHITESPACE_PATTERN.matcher(remaining);
        if (whitespaceMatcher.find()) {
            return new Token(whitespaceMatcher.group(), TokenType.WHITESPACE, position);
        }

        // Try to match single-line comment
        Matcher singleLineCommentMatcher = SINGLE_LINE_COMMENT_PATTERN.matcher(remaining);
        if (singleLineCommentMatcher.find()) {
            return new Token(singleLineCommentMatcher.group(), TokenType.COMMENT, position);
        }

        // Try to match multi-line comment
        Matcher multiLineCommentMatcher = MULTI_LINE_COMMENT_PATTERN.matcher(remaining);
        if (multiLineCommentMatcher.find()) {
            return new Token(multiLineCommentMatcher.group(), TokenType.COMMENT, position);
        }

        // Try to match special characters
        if (remaining.startsWith("(")) {
            return new Token("(", TokenType.PARENTHESIS_OPEN, position);
        }

        if (remaining.startsWith(")")) {
            return new Token(")", TokenType.PARENTHESIS_CLOSE, position);
        }

        if (remaining.startsWith(",")) {
            return new Token(",", TokenType.COMMA, position);
        }

        if (remaining.startsWith(";")) {
            return new Token(";", TokenType.SEMICOLON, position);
        }

        if (remaining.startsWith(".")) {
            return new Token(".", TokenType.DOT, position);
        }

        if (remaining.startsWith("*")) {
            return new Token("*", TokenType.STAR, position);
        }

        // Try to match string literal
        Matcher stringLiteralMatcher = STRING_LITERAL_PATTERN.matcher(remaining);
        if (stringLiteralMatcher.find()) {
            return new Token(stringLiteralMatcher.group(), TokenType.STRING_LITERAL, position);
        }

        // Try to match quoted identifier
        Matcher quotedIdentifierMatcher = QUOTED_IDENTIFIER_PATTERN.matcher(remaining);
        if (quotedIdentifierMatcher.find()) {
            return new Token(quotedIdentifierMatcher.group(), TokenType.IDENTIFIER, position);
        }

        // Try to match numeric literal
        Matcher numericLiteralMatcher = NUMERIC_LITERAL_PATTERN.matcher(remaining);
        if (numericLiteralMatcher.find()) {
            return new Token(numericLiteralMatcher.group(), TokenType.NUMERIC_LITERAL, position);
        }

        // Try to match operator
        Matcher operatorMatcher = OPERATOR_PATTERN.matcher(remaining);
        if (operatorMatcher.find()) {
            return new Token(operatorMatcher.group(), TokenType.OPERATOR, position);
        }

        // Try to match identifier
        Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher(remaining);
        if (identifierMatcher.find()) {
            String identifier = identifierMatcher.group();

            // Check if the identifier is a SQL keyword
            if (SQL_KEYWORDS.contains(identifier.toUpperCase())) {
                return new Token(identifier, TokenType.KEYWORD, position);
            }

            return new Token(identifier, TokenType.IDENTIFIER, position);
        }

        // If we get here, we couldn't match a token
        return null;
    }

    /**
     * Removes whitespace and comments from a list of tokens.
     *
     * @param tokens The list of tokens to filter
     * @return A new list with whitespace and comments removed
     */
    public static List<Token> removeWhitespaceAndComments(List<Token> tokens) {
        List<Token> filtered = new ArrayList<>();

        for (Token token : tokens) {
            if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.COMMENT) {
                filtered.add(token);
            }
        }

        return filtered;
    }
}