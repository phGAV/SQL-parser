package sqlparser.util;

import sqlparser.parser.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

public class TokenNavigationHelper {
    public List<Tokenizer.Token> tokens;
    public int currentTokenIndex;

    // SQL function keywords
    private static final Set<String> FUNCTION_KEYWORDS = new HashSet<>(Arrays.asList(
      "COUNT", "SUM", "AVG", "MIN", "MAX"
    ));

    // Keywords that indicate the end of an expression
    private static final Set<String> END_OF_EXPRESSION_KEYWORDS = new HashSet<>(Arrays.asList(
      "AS", "FROM", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET",
      "THEN", "ELSE", "END", "WHEN"
    ));

    // Keywords that indicate the end of a condition
    private static final Set<String> END_OF_CONDITION_KEYWORDS = new HashSet<>(Arrays.asList(
      "AND", "OR", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET"
    ));

    // Join-related keywords
    private static final Set<String> JOIN_KEYWORDS = new HashSet<>(Arrays.asList(
      "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS"
    ));

    public TokenNavigationHelper(List<Tokenizer.Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
    }

    public boolean isKeyword(Set<String> keywords) {
        if (!isType(Tokenizer.TokenType.KEYWORD)) {
            return false;
        }
        String value = currentToken().getValue().toUpperCase();
        return keywords.contains(value);
    }

    public boolean isType(Tokenizer.TokenType type) {
        return hasMoreTokens() && currentToken().getType() == type;
    }

    public boolean isFunctionKeyword() {
        return isKeyword(FUNCTION_KEYWORDS);
    }

    public boolean isEndOfExpressionKeyword() {
        return isKeyword(END_OF_EXPRESSION_KEYWORDS);
    }

    public boolean isEndOfConditionKeyword() {
        return isKeyword(END_OF_CONDITION_KEYWORDS);
    }

    public boolean isOperator() {
        return isType(Tokenizer.TokenType.OPERATOR) || isType(Tokenizer.TokenType.STAR);
    }

    public boolean isIdentifierOrLiteral() {
        return isType(Tokenizer.TokenType.IDENTIFIER) ||
               isType(Tokenizer.TokenType.NUMERIC_LITERAL) ||
               isType(Tokenizer.TokenType.STRING_LITERAL);
    }

    public boolean isIdentifierOrKeyword() {
        return isType(Tokenizer.TokenType.IDENTIFIER) ||
               isType(Tokenizer.TokenType.KEYWORD);
    }

    public boolean isJoinKeyword() {
            return isKeyword(JOIN_KEYWORDS);
    }
    /**
     * Gets the current token without advancing.
     *
     * @return The current token
     * @throws SQLParserException if there are no more tokens
     */
    public Tokenizer.Token currentToken() {
        if (!hasMoreTokens()) {
            throw new SQLParserException("Unexpected end of query");
        }
        return tokens.get(currentTokenIndex);
    }

    /**
     * Gets the current token and advances to the next token.
     *
     * @return The current token
     * @throws SQLParserException if there are no more tokens
     */
    public Tokenizer.Token consumeToken() {
        Tokenizer.Token token = currentToken();
        currentTokenIndex++;
        return token;
    }

    /**
     * Checks if the current token matches the given type and value.
     *
     * @param type The token type to match
     * @param value The token value to match (case-insensitive)
     * @return true if the current token matches, false otherwise
     */
    public boolean matchToken(Tokenizer.TokenType type, String value) {
        // Check if we're at the end of tokens first
        if (!hasMoreTokens()) {
            return false;
        }

        Tokenizer.Token token = tokens.get(currentTokenIndex);
        return token.getType() == type && token.getValue().equalsIgnoreCase(value);
    }

    /**
     * Consumes the current token if it matches the given type and value.
     *
     * @param type The token type to match
     * @param value The token value to match (case-insensitive)
     * @return true if the token was consumed, false otherwise
     */
    public boolean consumeIfMatch(Tokenizer.TokenType type, String value) {
        if (matchToken(type, value)) {
            currentTokenIndex++;
            return true;
        }
        return false;
    }

    /**
     * Expects the current token to match the given type and value, and consumes it.
     *
     * @param type The token type to match
     * @param value The token value to match (case-insensitive)
     * @throws SQLParserException if the current token does not match
     */
    public void expectToken(Tokenizer.TokenType type, String value) {
        if (!hasMoreTokens()) {
            throw new SQLParserException("Expected '" + value + "', but reached end of query");
        }

        Tokenizer.Token token = tokens.get(currentTokenIndex);
        if (token.getType() == type && token.getValue().equalsIgnoreCase(value)) {
            currentTokenIndex++;
        } else {
            throw new SQLParserException("Expected '" + value + "', but got '" +
                                         token.getValue() + "'", token.getPosition());
        }
    }

    public int getCurrentIndex() {
        return currentTokenIndex;
    }

    public void setCurrentIndex(int index) {
        this.currentTokenIndex = index;
    }

    public boolean hasMoreTokens() {
        return currentTokenIndex < tokens.size();
    }
}
