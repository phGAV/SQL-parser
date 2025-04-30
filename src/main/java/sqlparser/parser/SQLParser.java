package sqlparser.parser;

import sqlparser.model.*;
import sqlparser.util.*;

import java.util.*;

/**
 * Parser for SQL SELECT queries.
 * Parses a SQL query string into a structured Query object.
 */
public class SQLParser {

    private TokenNavigationHelper tokenHelper;

    /**
     * Parses a SQL query string.
     *
     * @param sql The SQL query string to parse
     * @return The parsed Query object
     * @throws SQLParserException if the query cannot be parsed
     */
    public Query parse(String sql) {
        // Tokenize the SQL query
        List<Tokenizer.Token> tokens = Tokenizer.tokenize(sql);

        // Initialize helpers
        tokenHelper = new TokenNavigationHelper(tokens);

        // Parse the SELECT statement
        return parseSelectStatement();
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OFFSET")) {
            Tokenizer.Token offsetToken = tokenHelper.consumeToken();
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LIMIT")) {
            Tokenizer.Token limitToken = tokenHelper.consumeToken();
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "HAVING")) {
            query.setHavingCondition(parseCondition());
        }
    }

    private void parseWhereClause(Query query) {
        // Check for typos in WHERE keyword if present
        checkIfTypoOf("WHERE");

        // Parse WHERE clause if present
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "WHERE")) {
            query.setWhereCondition(parseCondition());
        }
    }

    private void parseSelectClause(Query query) {
        // Parse SELECT
        tokenHelper.expectToken(Tokenizer.TokenType.KEYWORD, "SELECT");

        // Parse DISTINCT if present
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "DISTINCT")) {
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
        if (tokenHelper.hasMoreTokens() &&
            isLikelyTypoOf(tokenHelper.currentToken(), keyword) &&
            !tokenHelper.currentToken().getValue().equalsIgnoreCase(keyword)) {
            throw new SQLParserException("Expected '"+ keyword +"', but got '" +
                                         tokenHelper.currentToken().getValue() + "'",
                                         tokenHelper.currentToken().getPosition());
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
            query.addColumn(new Column("*"));
            return;
        }

        // Parse column list
        do {
            Column column = parseColumn();
            query.addColumn(column);
        } while (tokenHelper.consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
    }

    /**
     * Parses a single column in a SELECT or GROUP BY clause.
     *
     * @return The parsed Column object
     */
    private Column parseColumn() {
        // Check if we have tokens before proceeding
        if (!tokenHelper.hasMoreTokens()) {
            throw new SQLParserException("Expected column, but reached end of query");
        }

        StringBuilder expressionBuilder = new StringBuilder();
        String alias = null;

        // Handle special cases: COUNT(*), SUM(field), etc.
        if (tokenHelper.isType(Tokenizer.TokenType.KEYWORD) && tokenHelper.isFunctionKeyword()) {

            String functionName = tokenHelper.consumeToken().getValue();
            expressionBuilder.append(functionName);

            tokenHelper.expectToken(Tokenizer.TokenType.PARENTHESIS_OPEN, "(");
            expressionBuilder.append("(");

            // Parse function argument
            if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
                expressionBuilder.append("*");
            } else {
                // Parse expression inside function
                while (tokenHelper.hasMoreTokens() && !tokenHelper.matchToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                    expressionBuilder.append(tokenHelper.consumeToken().getValue());
                }

                // If we've reached the end without finding a closing parenthesis
                if (!tokenHelper.hasMoreTokens()) {
                    throw new SQLParserException("Unclosed function parenthesis");
                }
            }

            tokenHelper.expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
            expressionBuilder.append(")");
        } else {
            // Parse column expression (could be a simple column name or a complex expression)
            expressionBuilder.append(parseSimpleExpression());
        }

        // Check for column alias only if we have more tokens
        if (tokenHelper.hasMoreTokens()) {
            if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                // AS keyword is present - make sure we have another token
                if (!tokenHelper.hasMoreTokens()) {
                    throw new SQLParserException("Expected identifier for column alias after AS, but reached end of query");
                }

                if (tokenHelper.isIdentifierOrKeyword()) {
                    alias = tokenHelper.consumeToken().getValue();
                } else {
                    throw new SQLParserException("Expected identifier for column alias", tokenHelper.currentToken().getPosition());
                }
            } else if (tokenHelper.isType(Tokenizer.TokenType.IDENTIFIER) &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.COMMA, ",") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "FROM") && !isLikelyTypoOf(tokenHelper.currentToken(), "FROM") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "WHERE") && !isLikelyTypoOf(tokenHelper.currentToken(), "WHERE") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "GROUP") && !isLikelyTypoOf(tokenHelper.currentToken(), "GROUP") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "HAVING") && !isLikelyTypoOf(tokenHelper.currentToken(), "HAVING") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "ORDER") && !isLikelyTypoOf(tokenHelper.currentToken(), "ORDER") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "LIMIT") && !isLikelyTypoOf(tokenHelper.currentToken(), "LIMIT") &&
                       !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "OFFSET") && !isLikelyTypoOf(tokenHelper.currentToken(), "OFFSET")) {
                // Implicit alias (no AS keyword)
                alias = tokenHelper.consumeToken().getValue();
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
        // Check for parenthesized expression
        if (tokenHelper.currentToken().getType() == Tokenizer.TokenType.PARENTHESIS_OPEN) {
            return parseParenthesizedExpression();
        }

        // Check for CASE expression
        if (tokenHelper.currentToken().getType() == Tokenizer.TokenType.KEYWORD &&
            tokenHelper.currentToken().getValue().equalsIgnoreCase("CASE")) {
            return parseCaseExpression();
        }

        // Parse identifier or literal
        return parseIdentifierOrLiteral();
    }

    private String parseParenthesizedExpression() {
        StringBuilder expressionBuilder = new StringBuilder();
        // Add the opening parenthesis
        expressionBuilder.append(tokenHelper.consumeToken().getValue());

        // Parse the expression inside the parentheses
        int parenCount = 1;
        while (tokenHelper.hasMoreTokens() && parenCount > 0) {
            if (tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
                parenCount++;
            }
            else if (tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_CLOSE)) {
                parenCount--;
            }

            expressionBuilder.append(tokenHelper.consumeToken().getValue());
        }

        // If we exited the loop without balancing parentheses
        if (parenCount > 0) {
            throw new SQLParserException("Unbalanced parentheses in expression");
        }

        // Continue parsing the rest of the expression (if any)
        if (tokenHelper.isOperator()) {
            // Continue parsing operators and operands
            while (parseExpressionPart(expressionBuilder)) {
                // Continue as long as we can parse expression parts
            }
        }

        return expressionBuilder.toString();
    }

    private String parseIdentifierOrLiteral() {
        StringBuilder expressionBuilder = new StringBuilder();
        // Parse the first token (could be an identifier, literal, etc.)
        Tokenizer.Token token = tokenHelper.consumeToken();
        expressionBuilder.append(token.getValue());

        // Handle table.column notation
        if (token.getType() == Tokenizer.TokenType.IDENTIFIER &&
            tokenHelper.hasMoreTokens() &&
            tokenHelper.isType(Tokenizer.TokenType.DOT)) {
            expressionBuilder.append(tokenHelper.consumeToken().getValue()); // Add the dot

            if (tokenHelper.hasMoreTokens() &&
                tokenHelper.isType(Tokenizer.TokenType.IDENTIFIER)) {
                expressionBuilder.append(tokenHelper.consumeToken().getValue()); // Add the column name
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
        caseBuilder.append(tokenHelper.consumeToken().getValue()).append(" ");

        // Check for optional CASE value (simple case)
        if (!tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "WHEN")) {
            caseBuilder.append(parseSimpleExpression()).append(" ");
        }

        // Parse WHEN...THEN clauses
        while (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "WHEN")) {
            caseBuilder.append("WHEN ");

            // Parse the WHEN condition
            caseBuilder.append(parseSimpleExpression()).append(" ");

            tokenHelper.expectToken(Tokenizer.TokenType.KEYWORD, "THEN");
            caseBuilder.append("THEN ");

            // Parse the THEN result
            caseBuilder.append(parseSimpleExpression()).append(" ");
        }

        // Parse optional ELSE clause
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ELSE")) {
            caseBuilder.append("ELSE ");
            caseBuilder.append(parseSimpleExpression()).append(" ");
        }

        // Parse END
        tokenHelper.expectToken(Tokenizer.TokenType.KEYWORD, "END");
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
        if (!tokenHelper.hasMoreTokens()) {
            return false;
        }

        // Stop if we reach certain SQL keywords that indicate the end of the expression
        if (tokenHelper.isType(Tokenizer.TokenType.KEYWORD) && tokenHelper.isEndOfExpressionKeyword()) {
            return false;
        }

        // Stop if we reach a comma or other special characters that indicate the end of the expression
        if (tokenHelper.isType(Tokenizer.TokenType.COMMA) ||
            tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_CLOSE)) {
            return false;
        }

        // Check if we have an operator
        if (tokenHelper.isOperator()) {
            // Add the operator
            expressionBuilder.append(tokenHelper.consumeToken().getValue());

            // Check for opening parenthesis after the operator
            if (tokenHelper.hasMoreTokens() &&
                tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
                // Add the opening parenthesis
                expressionBuilder.append(tokenHelper.consumeToken().getValue());

                // Parse the expression inside the parentheses
                int parenCount = 1;
                while (tokenHelper.hasMoreTokens() && parenCount > 0) {
                    if (tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_OPEN)) {
                        parenCount++;
                    } else if (tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_CLOSE)) {
                        parenCount--;
                    }

                    expressionBuilder.append(tokenHelper.consumeToken().getValue());
                }

                // If we exited the loop without balancing parentheses
                if (parenCount > 0) {
                    throw new SQLParserException("Unbalanced parentheses in expression");
                }

                return true;
            }

            // Check for the next value
            if (tokenHelper.hasMoreTokens() && tokenHelper.isIdentifierOrLiteral()) {
                Tokenizer.Token valueToken = tokenHelper.consumeToken();
                expressionBuilder.append(valueToken.getValue());

                // Handle table.column notation after an operator
                if (valueToken.getType() == Tokenizer.TokenType.IDENTIFIER &&
                    tokenHelper.hasMoreTokens() &&
                    tokenHelper.isType(Tokenizer.TokenType.DOT)) {
                    expressionBuilder.append(tokenHelper.consumeToken().getValue()); // Add the dot

                    if (tokenHelper.hasMoreTokens() &&
                        tokenHelper.isType(Tokenizer.TokenType.IDENTIFIER)) {
                        expressionBuilder.append(tokenHelper.consumeToken().getValue()); // Add the column name
                    } else {
                        throw new SQLParserException("Expected identifier after '.' in expression");
                    }
                }

                return true;
            } else {
                throw new SQLParserException("Expected value after operator",
                                             tokenHelper.hasMoreTokens() ?
                                             tokenHelper.getCurrentIndex() :
                                             tokenHelper.tokens.getLast().getPosition());
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "FROM")) {
            // Parse table sources
            do {
                TableSource tableSource = parseTableSource();
                query.addFromSource(tableSource);

                // Add implicit join for comma-separated tables
                if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.COMMA, ",")) {
                    // There's another table after the comma, so we need to parse it
                    // and create an implicit join
                    TableSource rightTable = parseTableSource();
                    Join implicitJoin = new Join(rightTable, JoinType.IMPLICIT);
                    query.addJoin(implicitJoin);
                }
            }
            while (tokenHelper.consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
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
        if (!tokenHelper.hasMoreTokens()) {
            throw new SQLParserException("Expected table source, but reached end of query");
        }

        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
            // Subquery
            Query subquery = parseSelectStatement();
            tokenHelper.expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");

            // Check if we have tokens for the alias
            if (!tokenHelper.hasMoreTokens()) {
                throw new SQLParserException("Subquery in FROM clause must have an alias, but reached end of query");
            }

            // Alias is required for subqueries
            String alias;
            if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                // Explicit alias with AS
                if (!tokenHelper.hasMoreTokens()) {
                    throw new SQLParserException("Expected identifier for subquery alias, but reached end of query");
                }

                if (tokenHelper.isIdentifierOrKeyword()) {
                    alias = tokenHelper.consumeToken().getValue();
                } else {
                    throw new SQLParserException("Expected identifier for subquery alias", tokenHelper.currentToken().getPosition());
                }
            } else if (tokenHelper.isIdentifierOrKeyword()) {
                // Implicit alias without AS
                alias = tokenHelper.consumeToken().getValue();
            } else {
                throw new SQLParserException("Subquery in FROM clause must have an alias", tokenHelper.currentToken().getPosition());
            }

            return new TableSource(subquery, alias);
        } else {
            // Table name
            if (!tokenHelper.isType(Tokenizer.TokenType.IDENTIFIER) &&
                !tokenHelper.isType(Tokenizer.TokenType.KEYWORD)) {
                throw new SQLParserException("Expected table name", tokenHelper.currentToken().getPosition());
            }

            String tableName = tokenHelper.consumeToken().getValue();
            String alias = null;

            // Check for alias - only if we have more tokens
            if (tokenHelper.hasMoreTokens()) {
                if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AS")) {
                    // Explicit alias with AS
                    if (!tokenHelper.hasMoreTokens()) {
                        throw new SQLParserException("Expected identifier for table alias, but reached end of query");
                    }

                    if (tokenHelper.isIdentifierOrKeyword()) {
                        alias = tokenHelper.consumeToken().getValue();
                    } else {
                        throw new SQLParserException("Expected identifier for table alias", tokenHelper.currentToken().getPosition());
                    }
                } else if (tokenHelper.isIdentifierOrKeyword()) {
                    // Check if the next token is not a reserved keyword for clauses
                    if (!tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "ON") && !isLikelyTypoOf(tokenHelper.currentToken(), "ON") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "JOIN") && !isLikelyTypoOf(tokenHelper.currentToken(), "JOIN") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "INNER") && !isLikelyTypoOf(tokenHelper.currentToken(), "INNER") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "LEFT") && !isLikelyTypoOf(tokenHelper.currentToken(), "LEFT") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "RIGHT") && !isLikelyTypoOf(tokenHelper.currentToken(), "RIGHT") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "FULL") && !isLikelyTypoOf(tokenHelper.currentToken(), "FULL") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "CROSS") && !isLikelyTypoOf(tokenHelper.currentToken(), "CROSS") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "WHERE") && !isLikelyTypoOf(tokenHelper.currentToken(), "WHERE") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "GROUP") && !isLikelyTypoOf(tokenHelper.currentToken(), "GROUP") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "HAVING") && !isLikelyTypoOf(tokenHelper.currentToken(), "HAVING") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "ORDER") && !isLikelyTypoOf(tokenHelper.currentToken(), "ORDER") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "LIMIT") && !isLikelyTypoOf(tokenHelper.currentToken(), "LIMIT") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.KEYWORD, "OFFSET") && !isLikelyTypoOf(tokenHelper.currentToken(), "OFFSET") &&
                        !tokenHelper.matchToken(Tokenizer.TokenType.COMMA, ",")) {
                        // Implicit alias without AS
                        alias = tokenHelper.consumeToken().getValue();
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
        while (tokenHelper.isJoinKeyword()) {
            JoinType joinType = parseJoinType();
            tokenHelper.expectToken(Tokenizer.TokenType.KEYWORD, "JOIN");

            TableSource tableSource = parseTableSource();
            Condition onCondition = null;
            String usingColumns = null;

            // Parse ON or USING clause - only if we have more tokens
            if (tokenHelper.hasMoreTokens()) {
                if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ON")) {
                    onCondition = parseCondition();
                } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "USING")) {
                    tokenHelper.expectToken(Tokenizer.TokenType.PARENTHESIS_OPEN, "(");
                    usingColumns = tokenHelper.consumeToken().getValue();
                    tokenHelper.expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
                } else if (joinType != JoinType.CROSS) {
                    // ON or USING is required for all join types except CROSS JOIN
                    throw new SQLParserException("Expected ON or USING clause after JOIN",
                                                 tokenHelper.hasMoreTokens() ?
                                                 tokenHelper.currentToken().getPosition() :
                                                 tokenHelper.tokens.getLast().getPosition());
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
     * Parses a JOIN type.
     *
     * @return The parsed JoinType
     */
    private JoinType parseJoinType() {
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "INNER")) {
            return JoinType.INNER;
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LEFT")) {
            tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.LEFT;
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "RIGHT")) {
            tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.RIGHT;
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "FULL")) {
            tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OUTER"); // OUTER is optional
            return JoinType.FULL;
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "CROSS")) {
            return JoinType.CROSS;
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "JOIN")) {
            // If just "JOIN" is specified, it's an INNER JOIN
            tokenHelper.currentTokenIndex--; // Put back the "JOIN" token
            return JoinType.INNER;
        } else {
            // Safety check for accessing the token
            if (!tokenHelper.hasMoreTokens()) {
                throw new SQLParserException("Expected JOIN type, but reached end of query");
            }
            throw new SQLParserException("Expected JOIN type", tokenHelper.currentToken().getPosition());
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "NOT")) {
            condition.setNegated(true);
        }

        // Check for parenthesized condition
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
            // Nested condition
            Condition nestedCondition = parseCondition();
            tokenHelper.expectToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")");
            condition.addNestedCondition(nestedCondition);
        } else {
            // Parse the left expression
            Object leftExpr = parseExpression();
            // Check if we've reached the end of the condition or a CASE expression is used as a complete boolean expression
            boolean isEndOfCondition = !tokenHelper.hasMoreTokens() ||
                                       (tokenHelper.isType(Tokenizer.TokenType.KEYWORD) && tokenHelper.isEndOfConditionKeyword()) ||
                                       tokenHelper.isType(Tokenizer.TokenType.PARENTHESIS_CLOSE) ||
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "AND")) {
            condition.setLogicalOperator("AND");
            condition.setNextCondition(parseCondition());
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "OR")) {
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
        if (!tokenHelper.hasMoreTokens()) {
            throw new SQLParserException("Expected expression, but reached end of query");
        }

        // Check for CASE expression
        if (tokenHelper.isType(Tokenizer.TokenType.KEYWORD) &&
            tokenHelper.currentToken().getValue().equalsIgnoreCase("CASE")) {
            return parseCaseExpression();
        }

        // Check for aggregate functions
        if (tokenHelper.isType(Tokenizer.TokenType.KEYWORD) && tokenHelper.isFunctionKeyword()) {

            StringBuilder expression = new StringBuilder();

            // Add the function name
            expression.append(tokenHelper.consumeToken().getValue());

            // Add the opening parenthesis
            if (!tokenHelper.hasMoreTokens() || !tokenHelper.consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_OPEN, "(")) {
                throw new SQLParserException("Expected '(' after aggregate function");
            }
            expression.append("(");

            // Handle the function arguments
            if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.STAR, "*")) {
                // Special case for COUNT(*)
                expression.append("*");
            } else {
                // Handle other arguments
                while (tokenHelper.hasMoreTokens() && !tokenHelper.matchToken(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                    expression.append(tokenHelper.consumeToken().getValue());
                }
            }

            // Add the closing parenthesis
            if (!tokenHelper.hasMoreTokens() || !tokenHelper.consumeIfMatch(Tokenizer.TokenType.PARENTHESIS_CLOSE, ")")) {
                throw new SQLParserException("Expected ')' to close aggregate function");
            }
            expression.append(")");

            return expression.toString();
        } else if (tokenHelper.isType(Tokenizer.TokenType.IDENTIFIER)) {
            StringBuilder sb = new StringBuilder(tokenHelper.consumeToken().getValue());

            // Handle table.column notation
            if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.DOT, ".")) {
                // Check if we have another token after the dot
                if (!tokenHelper.hasMoreTokens()) {
                    throw new SQLParserException("Incomplete column reference after '.'");
                }
                sb.append(".").append(tokenHelper.consumeToken().getValue());
            }

            return sb.toString();
        } else if (tokenHelper.isType(Tokenizer.TokenType.STRING_LITERAL)) {
            return tokenHelper.consumeToken().getValue();
        } else if (tokenHelper.isType(Tokenizer.TokenType.NUMERIC_LITERAL)) {
            return tokenHelper.consumeToken().getValue();
        } else {
            throw new SQLParserException("Expected expression", tokenHelper.currentToken().getPosition());
        }
    }

    /**
     * Parses an operator.
     *
     * @return The parsed operator as a String
     */
    private String parseOperator() {
        // Check if we have tokens before accessing
        if (!tokenHelper.hasMoreTokens()) {
            throw new SQLParserException("Expected operator, but reached end of query");
        }

        if (tokenHelper.isOperator()) {
            return tokenHelper.consumeToken().getValue();
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "IN")) {
            return "IN";
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "BETWEEN")) {
            return "BETWEEN";
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "LIKE")) {
            return "LIKE";
        } else if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "IS")) {
            if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "NOT")) {
                return "IS NOT";
            }
            return "IS";
        } else {
            throw new SQLParserException("Expected operator", tokenHelper.currentToken().getPosition());
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "GROUP")) {
            tokenHelper.expectToken(Tokenizer.TokenType.KEYWORD, "BY");
            do {
                // Parse column expression
                String columnExpr = parseSimpleExpression();
                query.addGroupByColumn(new Column(columnExpr));
            } while (tokenHelper.consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
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
        if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ORDER")) {
            tokenHelper.expectToken(Tokenizer.TokenType.KEYWORD, "BY");
            do {
                // Parse column expression
                String columnExpr = parseSimpleExpression();
                boolean isAscending = true;

                // Parse optional ASC/DESC
                if (tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "DESC")) {
                    isAscending = false;
                } else {
                    tokenHelper.consumeIfMatch(Tokenizer.TokenType.KEYWORD, "ASC"); // ASC is optional
                }

                query.addOrderByColumn(new OrderByColumn(columnExpr, isAscending));
            } while (tokenHelper.consumeIfMatch(Tokenizer.TokenType.COMMA, ","));
        }
    }
}