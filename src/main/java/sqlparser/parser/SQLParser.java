package main.java.sqlparser.parser;

import main.java.sqlparser.model.*;
import sqlparser.util.LevenshteinDistance;

import java.util.*;

/**
 * Parser for SQL SELECT queries.
 * Parses a SQL query string into a structured Query object.
 */
public class SQLParser {
    private List<Tokenizer.Token> tokens;
    private int currentTokenIndex;

    private boolean isKeyword(String[] keywords) {
        if (!isType(Tokenizer.TokenType.KEYWORD)) {
            return false;
        }

        String value = currentToken().getValue().toUpperCase();
        for (String keyword : keywords) {
            if (value.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isType(Tokenizer.TokenType type) {
        return currentTokenIndex < tokens.size() &&
               currentToken().getType() == type;
    }

    private boolean isFunctionKeyword() {
        String[] funcKeywords = {"COUNT", "SUM", "AVG", "MIN", "MAX"};
        return isKeyword(funcKeywords);
    }

    private boolean isEndOfExpressionKeyword() {
        String[] endOfExpressionKeywords = {"AS", "FROM", "WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "THEN", "ELSE", "END", "WHEN"};
        return isKeyword(endOfExpressionKeywords);
    }

    private boolean isEndOfConditionKeyword() {
        String[] endOfConditionKeywords = {"AND", "OR", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET"};
        return isKeyword(endOfConditionKeywords);
    }

    private boolean isOperator() {
        return isType(Tokenizer.TokenType.OPERATOR) || isType(Tokenizer.TokenType.STAR);
    }

    private boolean isIdentifierOrLiteral() {
        return isType(Tokenizer.TokenType.IDENTIFIER) ||
               isType(Tokenizer.TokenType.NUMERIC_LITERAL) ||
               isType(Tokenizer.TokenType.STRING_LITERAL);
    }

    private boolean isIdentifierOrKeyword() {
        return isType(Tokenizer.TokenType.IDENTIFIER) ||
               isType(Tokenizer.TokenType.KEYWORD);
    }

    /**
     * Parses a SQL query string.
     *
     * @param sql The SQL query string to parse
     * @return The parsed Query object
     * @throws SQLParserException if the query cannot be parsed
     */
    public Query parse(String sql) {
        // Tokenize the SQL query
        tokens = Tokenizer.tokenize(sql);
        currentTokenIndex = 0;

        // Parse the SELECT statement
        return parseSelectStatement();
    }

    /**
     * Gets the current token without advancing.
     *
     * @return The current token
     * @throws SQLParserException if there are no more tokens
     */
    private Tokenizer.Token currentToken() {
        if (currentTokenIndex >= tokens.size()) {
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
    private Tokenizer.Token consumeToken() {
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
    private boolean matchToken(Tokenizer.TokenType type, String value) {
        // Check if we're at the end of tokens first
        if (currentTokenIndex >= tokens.size()) {
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
    private boolean consumeIfMatch(Tokenizer.TokenType type, String value) {
        if (currentTokenIndex >= tokens.size()) {
            return false;
        }

        Tokenizer.Token token = tokens.get(currentTokenIndex);
        if (token.getType() == type && token.getValue().equalsIgnoreCase(value)) {
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
    private void expectToken(Tokenizer.TokenType type, String value) {
        if (currentTokenIndex >= tokens.size()) {
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

    /**
     * Parses a SELECT statement.
     *
     * @return The parsed Query object
     */
    private Query parseSelectStatement() {
        Query query = new Query();

        try {
            parseSelectClause(query);
            parseFromClause(query);
            parseWhereClause(query);
            parseGroupByClause(query);
            parseHavingClause(query);
            parseOrderByClause(query);
            parseLimitClause(query);
            parseOffsetClause(query);

            return query;
        } catch (SQLParserException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLParserException("Unexpected error parsing SQL: " + e.getMessage(), e);
        }
    }

    private void parseOffsetClause(Query query) {
        // Check for typos in OFFSET keyword if present
        checkIfTypoOf("OFFSET");

        // Parse OFFSET clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OFFSET")) {
            Tokenizer.Token offsetToken = consumeToken();
            if (offsetToken.getType() != Tokenizer.TokenType.NUMERIC_LITERAL) {
                throw new SQLParserException("Expected numeric literal for OFFSET", offsetToken.getPosition());
            }
            query.setOffset(Integer.parseInt(offsetToken.getValue()));
        }
    }

    private void parseLimitClause(Query query) {
        // Check for typos in LIMIT keyword if present
        checkIfTypoOf("LIMIT");

        // Parse LIMIT clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LIMIT")) {
            Tokenizer.Token limitToken = consumeToken();
            if (limitToken.getType() != Tokenizer.TokenType.NUMERIC_LITERAL) {
                throw new SQLParserException("Expected numeric literal for LIMIT", limitToken.getPosition());
            }
            query.setLimit(Integer.parseInt(limitToken.getValue()));
        }
    }

    private void parseHavingClause(Query query) {
        // Check for typos in HAVING keyword if present
        checkIfTypoOf("HAVING");

        // Parse HAVING clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "HAVING")) {
            query.setHavingCondition(parseCondition());
        }
    }

    private void parseWhereClause(Query query) {
        // Check for typos in WHERE keyword if present
        checkIfTypoOf("WHERE");

        // Parse WHERE clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "WHERE")) {
            query.setWhereCondition(parseCondition());
        }
    }

    private void parseSelectClause(Query query) {
        // Parse SELECT
        expectToken(Tokenizer.TokenType.KEYWORD, "SELECT");

        // Parse DISTINCT if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "DISTINCT")) {
            query.setDistinct(true);
        }

        // Parse columns
        parseSelectColumns(query);
    }

    /**
     * Checks if the token is a typo of given keyword
     *
     * @throws SQLParserException if the token is a typo
     * */
    private void checkIfTypoOf(String keyword) {
        if (currentTokenIndex < tokens.size() &&
            isLikelyTypoOf(currentToken(), keyword) &&
            !currentToken().getValue().equalsIgnoreCase(keyword)) {
            throw new SQLParserException("Expected '"+ keyword +"', but got '" +
                                         currentToken().getValue() + "'",
                                         currentToken().getPosition());
        }
    }

    /**
     * Checks if string is likely a typo of the given keyword.
     *
     * @param token The token to check
     * @param keyword The expected keyword
     * @return true if the token is likely a typo of the keyword
     */
    private boolean isLikelyTypoOf(Tokenizer.Token token, String keyword) {
        // If the token is already a recognized SQL keyword, it's not a typo
        if (token.getType() == Tokenizer.TokenType.KEYWORD) {
            return false;
        }
        String tokenValue = token.getValue().toUpperCase();
        String expectedKeyword = keyword.toUpperCase();

        // Method 1: Check if token starts with the first letter of the keyword
        // and has at least 60% of characters in common
        if (tokenValue.startsWith(expectedKeyword.substring(0, 1))) {
            // Calculate Levenshtein distance
            int distance = LevenshteinDistance.compute(tokenValue, expectedKeyword);
            int maxLength = Math.max(tokenValue.length(), expectedKeyword.length());

            // If the token is within 40% edit distance of the expected keyword
            return distance <= maxLength * 0.4;
        }

        return false;
    }

    /**
     * Parses the column list in a SELECT clause.
     *
     * @param query The Query object to populate
     */
    private void parseSelectColumns(Query query) {
        // Check for SELECT *
        if (consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
            query.addColumn(new Column("*"));
            return;
        }

        // Parse column list
        do {
            Column column = parseColumn();
            query.addColumn(column);
        } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
    }

    /**
     * Parses a single column in a SELECT or GROUP BY clause.
     *
     * @return The parsed Column object
     */
    private Column parseColumn() {
        // Check if we have tokens before proceeding
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected column, but reached end of query");
        }

        StringBuilder expressionBuilder = new StringBuilder();
        String alias = null;

        // Handle special cases: COUNT(*), SUM(field), etc.
        if (isType(Tokenizer.TokenType.KEYWORD) && isFunctionKeyword()) {

            String functionName = consumeToken().getValue();
            expressionBuilder.append(functionName);

            expectToken(Tokenizer.TokenType.PARENTHESIS_OPEN, "(");
            expressionBuilder.append("(");

            // Parse function argument
            if (consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
                expressionBuilder.append("*");
            } else {
                // Parse expression inside function
                while (currentTokenIndex < tokens.size() && !matchToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                    expressionBuilder.append(consumeToken().getValue());
                }

                // If we've reached the end without finding a closing parenthesis
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Unclosed function parenthesis");
                }
            }

            expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
            expressionBuilder.append(")");
        } else {
            // Parse column expression (could be a simple column name or a complex expression)
            expressionBuilder.append(parseSimpleExpression());
        }

        // Check for column alias only if we have more tokens
        if (currentTokenIndex < tokens.size()) {
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                // AS keyword is present - make sure we have another token
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Expected identifier for column alias after AS, but reached end of query");
                }

                if (isIdentifierOrKeyword()) {
                    alias = consumeToken().getValue();
                } else {
                    throw new SQLParserException("Expected identifier for column alias", currentToken().getPosition());
                }
            } else if (isType(Tokenizer.TokenType.IDENTIFIER) &&
                       !matchToken(Tokenizer.TokenType.COMMA, ",") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "FROM") && !isLikelyTypoOf(currentToken(), "FROM") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "WHERE") && !isLikelyTypoOf(currentToken(), "WHERE") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "GROUP") && !isLikelyTypoOf(currentToken(), "GROUP") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "HAVING") && !isLikelyTypoOf(currentToken(), "HAVING") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "ORDER") && !isLikelyTypoOf(currentToken(), "ORDER") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "LIMIT") && !isLikelyTypoOf(currentToken(), "LIMIT") &&
                       !matchToken(Tokenizer.TokenType.KEYWORD, "OFFSET") && !isLikelyTypoOf(currentToken(), "OFFSET")) {
                // Implicit alias (no AS keyword)
                alias = consumeToken().getValue();
            }
        }

        return new Column(expressionBuilder.toString(), alias);
    }

    /**
     * Parses a simple expression (column name, literal, or simple calculation).
     *
     * @return The parsed expression as a string
     */
    private String parseSimpleExpression() {
        StringBuilder expressionBuilder = new StringBuilder();

        // Check if the expression starts with a parenthesis
        if (isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
            // Add the opening parenthesis
            expressionBuilder.append(consumeToken().getValue());

            // Parse the expression inside the parentheses
            int parenCount = 1;
            while (currentTokenIndex < tokens.size() && parenCount > 0) {
                if (isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
                    parenCount++;
                }
                else if (isType(Tokenizer.TokenType.PARENTHESIS_CLOSE)) {
                    parenCount--;
                }

                expressionBuilder.append(consumeToken().getValue());
            }

            // If we exited the loop without balancing parentheses
            if (parenCount > 0) {
                throw new SQLParserException("Unbalanced parentheses in expression");
            }

            // Continue parsing the rest of the expression (if any)
            if (isOperator()) {
                // Continue parsing operators and operands
                while (parseExpressionPart(expressionBuilder)) {
                    // Continue as long as we can parse expression parts
                }
            }

        return expressionBuilder.toString();
        // Check if the expression starts with CASE
        } else if (isType(Tokenizer.TokenType.KEYWORD) &&
                 currentToken().getValue().equalsIgnoreCase("CASE")) {
            // Parse CASE expression
            return parseCaseExpression();
        }
        // Parse the first token (could be an identifier, literal, etc.)
        Tokenizer.Token token = consumeToken();
        expressionBuilder.append(token.getValue());

        // Handle table.column notation
        if (token.getType() == Tokenizer.TokenType.IDENTIFIER &&
            currentTokenIndex < tokens.size() &&
            isType(Tokenizer.TokenType.DOT)) {
            expressionBuilder.append(consumeToken().getValue()); // Add the dot

            if (currentTokenIndex < tokens.size() &&
                isType(Tokenizer.TokenType.IDENTIFIER)) {
                expressionBuilder.append(consumeToken().getValue()); // Add the column name
            } else {
                throw new SQLParserException("Expected identifier after '.' in expression");
            }
        }

        // Parse the rest of the expression (operators, etc.)
        while (parseExpressionPart(expressionBuilder)) {
            // Continue as long as we can parse expression parts
        }

        return expressionBuilder.toString();
    }

    /**
     * Parses a CASE expression.
     *
     * @return The parsed CASE expression as a string
     */
    private String parseCaseExpression() {
        StringBuilder caseBuilder = new StringBuilder();

        // Append "CASE"
        caseBuilder.append(consumeToken().getValue()).append(" ");

        // Check for optional CASE value (simple case)
        if (!matchToken(Tokenizer.TokenType.KEYWORD, "WHEN")) {
            caseBuilder.append(parseSimpleExpression()).append(" ");
        }

        // Parse WHEN...THEN clauses
        while (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "WHEN")) {
            caseBuilder.append("WHEN ");

            // Parse the WHEN condition
            caseBuilder.append(parseSimpleExpression()).append(" ");

            expectToken(Tokenizer.TokenType.KEYWORD, "THEN");
            caseBuilder.append("THEN ");

            // Parse the THEN result
            caseBuilder.append(parseSimpleExpression()).append(" ");
        }

        // Parse optional ELSE clause
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ELSE")) {
            caseBuilder.append("ELSE ");
            caseBuilder.append(parseSimpleExpression()).append(" ");
        }

        // Parse END
        expectToken(Tokenizer.TokenType.KEYWORD, "END");
        caseBuilder.append("END");

        return caseBuilder.toString();
    }

    /**
     * Parses a part of an expression (operator and operand) and adds it to the expression builder.
     *
     * @param expressionBuilder The StringBuilder to append to
     * @return true if a part was parsed, false if the expression is complete
     */
    private boolean parseExpressionPart(StringBuilder expressionBuilder) {
        // Check if we've reached the end of tokens
        if (currentTokenIndex >= tokens.size()) {
            return false;
        }

        // Stop if we reach certain SQL keywords that indicate the end of the expression
        if (isType(Tokenizer.TokenType.KEYWORD) && isEndOfExpressionKeyword()) {
            return false;
        }

        // Stop if we reach a comma or other special characters that indicate the end of the expression
        if (isType(Tokenizer.TokenType.COMMA) ||
            isType(Tokenizer.TokenType.PARENTHESIS_CLOSE)) {
            return false;
        }

        // Check if we have an operator
        if (isOperator()) {
            // Add the operator
            expressionBuilder.append(consumeToken().getValue());

            // Check for opening parenthesis after the operator
            if (currentTokenIndex < tokens.size() &&
                isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
                // Add the opening parenthesis
                expressionBuilder.append(consumeToken().getValue());

                // Parse the expression inside the parentheses
                int parenCount = 1;
                while (currentTokenIndex < tokens.size() && parenCount > 0) {
                    if (isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
                        parenCount++;
                    } else if (isType(Tokenizer.TokenType.PARENTHESIS_CLOSE)) {
                        parenCount--;
                    }

                    expressionBuilder.append(consumeToken().getValue());
                }

                // If we exited the loop without balancing parentheses
                if (parenCount > 0) {
                    throw new SQLParserException("Unbalanced parentheses in expression");
                }

                return true;
            }

            // Check for the next value
            if (currentTokenIndex < tokens.size() && isIdentifierOrLiteral()) {
                Tokenizer.Token valueToken = consumeToken();
                expressionBuilder.append(valueToken.getValue());

                // Handle table.column notation after an operator
                if (valueToken.getType() == Tokenizer.TokenType.IDENTIFIER &&
                    currentTokenIndex < tokens.size() &&
                    isType(Tokenizer.TokenType.DOT)) {
                    expressionBuilder.append(consumeToken().getValue()); // Add the dot

                    if (currentTokenIndex < tokens.size() &&
                        isType(Tokenizer.TokenType.IDENTIFIER)) {
                        expressionBuilder.append(consumeToken().getValue()); // Add the column name
                    } else {
                        throw new SQLParserException("Expected identifier after '.' in expression");
                    }
                }

                return true;
            } else {
                throw new SQLParserException("Expected value after operator",
                                             currentTokenIndex < tokens.size() ?
                                             currentToken().getPosition() :
                                             tokens.get(tokens.size() - 1).getPosition());
            }
        }

        return false; // No more parts to parse
    }

    /**
     * Parses the FROM clause of a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseFromClause(Query query) {
        // Check for typos in FROM keyword if present
        checkIfTypoOf("FROM");

        // Parse FROM clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "FROM")) {
            // Parse table sources
            do {
                TableSource tableSource = parseTableSource();
                query.addFromSource(tableSource);

                // Add implicit join for comma-separated tables
                if (consumeIfMatch(Tokenizer.TokenType.COMMA, ",")) {
                    // There's another table after the comma, so we need to parse it
                    // and create an implicit join
                    TableSource rightTable = parseTableSource();
                    Join implicitJoin = new Join(rightTable, JoinType.IMPLICIT);
                    query.addJoin(implicitJoin);
                }
            }
            while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
        }

        // Parse JOIN clauses if present
        parseJoinClauses(query);
    }

    /**
     * Parses a table source in the FROM clause (table name or subquery).
     *
     * @return The parsed TableSource object
     */
    private TableSource parseTableSource() {
        // Check if we have tokens before proceeding
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected table source, but reached end of query");
        }

        if (consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
            // Subquery
            Query subquery = parseSelectStatement();
            expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");

            // Check if we have tokens for the alias
            if (currentTokenIndex >= tokens.size()) {
                throw new SQLParserException("Subquery in FROM clause must have an alias, but reached end of query");
            }

            // Alias is required for subqueries
            String alias;
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                // Explicit alias with AS
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Expected identifier for subquery alias, but reached end of query");
                }

                if (isIdentifierOrKeyword()) {
                    alias = consumeToken().getValue();
                } else {
                    throw new SQLParserException("Expected identifier for subquery alias", currentToken().getPosition());
                }
            } else if (isIdentifierOrKeyword()) {
                // Implicit alias without AS
                alias = consumeToken().getValue();
            } else {
                throw new SQLParserException("Subquery in FROM clause must have an alias", currentToken().getPosition());
            }

            return new TableSource(subquery, alias);
        } else {
            // Table name
            if (!isType(Tokenizer.TokenType.IDENTIFIER) &&
                !isType(Tokenizer.TokenType.KEYWORD)) {
                throw new SQLParserException("Expected table name", currentToken().getPosition());
            }

            String tableName = consumeToken().getValue();
            String alias = null;

            // Check for alias - only if we have more tokens
            if (currentTokenIndex < tokens.size()) {
                if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                    // Explicit alias with AS
                    if (currentTokenIndex >= tokens.size()) {
                        throw new SQLParserException("Expected identifier for table alias, but reached end of query");
                    }

                    if (isIdentifierOrKeyword()) {
                        alias = consumeToken().getValue();
                    } else {
                        throw new SQLParserException("Expected identifier for table alias", currentToken().getPosition());
                    }
                } else if (isIdentifierOrKeyword()) {
                    // Check if the next token is not a reserved keyword for clauses
                    if (!matchToken(Tokenizer.TokenType.KEYWORD, "ON") && !isLikelyTypoOf(currentToken(), "ON") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "JOIN") && !isLikelyTypoOf(currentToken(), "JOIN") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "INNER") && !isLikelyTypoOf(currentToken(), "INNER") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "LEFT") && !isLikelyTypoOf(currentToken(), "LEFT") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "RIGHT") && !isLikelyTypoOf(currentToken(), "RIGHT") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "FULL") && !isLikelyTypoOf(currentToken(), "FULL") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "CROSS") && !isLikelyTypoOf(currentToken(), "CROSS") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "WHERE") && !isLikelyTypoOf(currentToken(), "WHERE") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "GROUP") && !isLikelyTypoOf(currentToken(), "GROUP") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "HAVING") && !isLikelyTypoOf(currentToken(), "HAVING") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "ORDER") && !isLikelyTypoOf(currentToken(), "ORDER") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "LIMIT") && !isLikelyTypoOf(currentToken(), "LIMIT") &&
                        !matchToken(Tokenizer.TokenType.KEYWORD, "OFFSET") && !isLikelyTypoOf(currentToken(), "OFFSET") &&
                        !matchToken(Tokenizer.TokenType.COMMA, ",")) {
                        // Implicit alias without AS
                        alias = consumeToken().getValue();
                    }
                }
            }

            return new TableSource(tableName, alias);
        }
    }

    /**
     * Parses JOIN clauses in a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseJoinClauses(Query query) {
        while (isJoinKeyword()) {
            JoinType joinType = parseJoinType();
            expectToken(Tokenizer.TokenType.KEYWORD, "JOIN");

            TableSource tableSource = parseTableSource();
            Condition onCondition = null;
            String usingColumns = null;

            // Parse ON or USING clause - only if we have more tokens
            if (currentTokenIndex < tokens.size()) {
                if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ON")) {
                    onCondition = parseCondition();
                } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "USING")) {
                    expectToken(Tokenizer.TokenType.PARENTHESIS_OPEN, "(");
                    usingColumns = consumeToken().getValue();
                    expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
                } else if (joinType != JoinType.CROSS) {
                    // ON or USING is required for all join types except CROSS JOIN
                    throw new SQLParserException("Expected ON or USING clause after JOIN",
                                                 currentTokenIndex < tokens.size() ? currentToken().getPosition() : tokens.get(tokens.size() - 1).getPosition());
                }
            } else if (joinType != JoinType.CROSS) {
                // We're at the end of tokens but need an ON or USING clause
                throw new SQLParserException("Expected ON or USING clause after JOIN, but reached end of query");
            }

            Join join = new Join(tableSource, joinType, onCondition);
            join.setUsingColumns(usingColumns);
            query.addJoin(join);
        }
    }

    /**
     * Checks if the current token is a JOIN keyword.
     *
     * @return true if the current token is a JOIN keyword, false otherwise
     */
    private boolean isJoinKeyword() {
        if (currentTokenIndex >= tokens.size()) {
            return false;
        }
        return matchToken(Tokenizer.TokenType.KEYWORD, "JOIN") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "INNER") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "LEFT") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "RIGHT") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "FULL") ||
               matchToken(Tokenizer.TokenType.KEYWORD, "CROSS");
    }

    /**
     * Parses a JOIN type.
     *
     * @return The parsed JoinType
     */
    private JoinType parseJoinType() {
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "INNER")) {
            return JoinType.INNER;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LEFT")) {
            consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.LEFT;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "RIGHT")) {
            consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.RIGHT;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "FULL")) {
            consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.FULL;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "CROSS")) {
            return JoinType.CROSS;
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "JOIN")) {
            // If just "JOIN" is specified, it's an INNER JOIN
            currentTokenIndex--; // Put back the "JOIN" token
            return JoinType.INNER;
        } else {
            // Safety check for accessing the token
            if (currentTokenIndex >= tokens.size()) {
                throw new SQLParserException("Expected JOIN type, but reached end of query");
            }
            throw new SQLParserException("Expected JOIN type", currentToken().getPosition());
        }
    }

    /**
     * Parses a condition (for WHERE, HAVING, or ON clauses).
     *
     * @return The parsed Condition object
     */
    private Condition parseCondition() {
        Condition condition = new Condition();

        // Check for NOT
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "NOT")) {
            condition.setNegated(true);
        }

        // Check for parenthesized condition
        if (consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
            // Nested condition
            Condition nestedCondition = parseCondition();
            expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
            condition.addNestedCondition(nestedCondition);
        } else {
            // Parse the left expression
            Object leftExpr = parseExpression();
            // Check if we've reached the end of the condition or a CASE expression is used as a complete boolean expression
            boolean isEndOfCondition = currentTokenIndex >= tokens.size() ||
                                       (isType(Tokenizer.TokenType.KEYWORD) && isEndOfConditionKeyword()) ||
                                       isType(Tokenizer.TokenType.PARENTHESIS_CLOSE) ||
                                       (leftExpr instanceof String && ((String)leftExpr).startsWith("CASE ") &&
                                        ((String)leftExpr).endsWith("END"));
            if (isEndOfCondition) {
                // Expression as a complete condition (like a CASE expression returning a boolean)
                condition.setLeftExpression(leftExpr);
                condition.setOperator("="); // Using a dummy operator for boolean expressions
                condition.setRightExpression("TRUE"); // Using TRUE as dummy right expression
            } else {
                // Regular condition with operator and right expression
                String operator = parseOperator();
                Object rightExpr = parseExpression();

                condition.setLeftExpression(leftExpr);
                condition.setOperator(operator);
                condition.setRightExpression(rightExpr);
            }
        }

        // Check for logical operators (AND, OR)
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AND")) {
            condition.setLogicalOperator("AND");
            condition.setNextCondition(parseCondition());
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OR")) {
            condition.setLogicalOperator("OR");
            condition.setNextCondition(parseCondition());
        }

        return condition;
    }

    /**
     * Parses an expression (column, literal, function, etc.).
     *
     * @return The parsed expression as an Object
     */
    private Object parseExpression() {
        // Check if we have tokens before accessing
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected expression, but reached end of query");
        }

        // Check for CASE expression
        if (isType(Tokenizer.TokenType.KEYWORD) &&
            currentToken().getValue().equalsIgnoreCase("CASE")) {
            return parseCaseExpression();
        }

        // Check for aggregate functions
        if (isType(Tokenizer.TokenType.KEYWORD) && isFunctionKeyword()) {

            StringBuilder expression = new StringBuilder();

            // Add the function name
            expression.append(consumeToken().getValue());

            // Add the opening parenthesis
            if (currentTokenIndex >= tokens.size() || !consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
                throw new SQLParserException("Expected '(' after aggregate function");
            }
            expression.append("(");

            // Handle the function arguments
            if (consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
                // Special case for COUNT(*)
                expression.append("*");
            } else {
                // Handle other arguments
                while (currentTokenIndex < tokens.size() && !matchToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                    expression.append(consumeToken().getValue());
                }
            }

            // Add the closing parenthesis
            if (currentTokenIndex >= tokens.size() || !consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                throw new SQLParserException("Expected ')' to close aggregate function");
            }
            expression.append(")");

            return expression.toString();
        } else if (isType(Tokenizer.TokenType.IDENTIFIER)) {
            StringBuilder sb = new StringBuilder(consumeToken().getValue());

            // Handle table.column notation
            if (consumeIfMatch(Tokenizer.TokenType.DOT, ".")) {
                // Check if we have another token after the dot
                if (currentTokenIndex >= tokens.size()) {
                    throw new SQLParserException("Incomplete column reference after '.'");
                }
                sb.append(".").append(consumeToken().getValue());
            }

            return sb.toString();
        } else if (isType(Tokenizer.TokenType.STRING_LITERAL)) {
            return consumeToken().getValue();
        } else if (isType(Tokenizer.TokenType.NUMERIC_LITERAL)) {
            return consumeToken().getValue();
        } else {
            throw new SQLParserException("Expected expression", currentToken().getPosition());
        }
    }

    /**
     * Parses an operator.
     *
     * @return The parsed operator as a String
     */
    private String parseOperator() {
        // Check if we have tokens before accessing
        if (currentTokenIndex >= tokens.size()) {
            throw new SQLParserException("Expected operator, but reached end of query");
        }

        if (isOperator()) {
            return consumeToken().getValue();
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "IN")) {
            return "IN";
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "BETWEEN")) {
            return "BETWEEN";
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LIKE")) {
            return "LIKE";
        } else if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "IS")) {
            if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "NOT")) {
                return "IS NOT";
            }
            return "IS";
        } else {
            throw new SQLParserException("Expected operator", currentToken().getPosition());
        }
    }

    /**
     * Parses the GROUP BY clause of a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseGroupByClause(Query query) {
        // Check for typos in GROUP keyword if present
        checkIfTypoOf("GROUP");

        // Parse GROUP BY clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "GROUP")) {
            expectToken(Tokenizer.TokenType.KEYWORD, "BY");
            do {
                // Parse column expression
                String columnExpr = parseSimpleExpression();
                query.addGroupByColumn(new Column(columnExpr));
            } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
        }
    }

    /**
     * Parses the ORDER BY clause of a SELECT statement.
     *
     * @param query The Query object to populate
     */
    private void parseOrderByClause(Query query) {
        // Check for typos in ORDER keyword if present
        checkIfTypoOf("ORDER");

        // Parse ORDER BY clause if present
        if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ORDER")) {
            expectToken(Tokenizer.TokenType.KEYWORD, "BY");
            do {
                // Parse column expression
                String columnExpr = parseSimpleExpression();
                boolean isAscending = true;

                // Parse optional ASC/DESC
                if (consumeIfMatch(Tokenizer.TokenType.KEYWORD, "DESC")) {
                    isAscending = false;
                } else {
                    consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ASC"); // ASC is optional
                }

                query.addOrderByColumn(new OrderByColumn(columnExpr, isAscending));
            } while (consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
        }
    }
}