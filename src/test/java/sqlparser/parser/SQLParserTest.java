package sqlparser.parser;

import sqlparser.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SQLParser class.
 */
public class SQLParserTest {

    private SQLParser parser;

    @BeforeEach
    void setUp() {
        parser = new SQLParser();
    }

    @Test
    @DisplayName("Test parsing a simple SELECT query")
    void testSimpleSelectQuery() {
        String sql = "SELECT * FROM book";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("*", query.getColumns().getFirst().getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals("book", query.getFromSources().getFirst().getTableName());
        assertTrue(query.getJoins().isEmpty());
        assertNull(query.getWhereCondition());
        assertTrue(query.getGroupByColumns().isEmpty());
        assertNull(query.getHavingCondition());
        assertTrue(query.getOrderByColumns().isEmpty());
        assertNull(query.getLimit());
        assertNull(query.getOffset());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with columns and aliases")
    void testSelectWithColumnsAndAliases() {
        String sql = "SELECT id AS book_id, title AS book_title FROM book";
        Query query = parser.parse(sql);

        // Verify columns and aliases
        assertEquals(2, query.getColumns().size());
        assertEquals("id", query.getColumns().get(0).getExpression());
        assertEquals("book_id", query.getColumns().get(0).getAlias());
        assertEquals("title", query.getColumns().get(1).getExpression());
        assertEquals("book_title", query.getColumns().get(1).getAlias());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with WHERE clause")
    void testSelectWithWhere() {
        String sql = "SELECT * FROM book WHERE price > 10.99 AND category = 'Fiction'";
        Query query = parser.parse(sql);

        // Verify WHERE condition
        assertNotNull(query.getWhereCondition());
        assertEquals("price", query.getWhereCondition().getLeftExpression());
        assertEquals(">", query.getWhereCondition().getOperator());
        assertEquals("10.99", query.getWhereCondition().getRightExpression());
        assertEquals("AND", query.getWhereCondition().getLogicalOperator());
        assertNotNull(query.getWhereCondition().getNextCondition());
        assertEquals("category", query.getWhereCondition().getNextCondition().getLeftExpression());
        assertEquals("=", query.getWhereCondition().getNextCondition().getOperator());
        assertEquals("'Fiction'", query.getWhereCondition().getNextCondition().getRightExpression());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with JOIN")
    void testSelectWithJoin() {
        String sql = "SELECT author.name, book.title FROM author INNER JOIN book ON author.id = book.author_id";
        Query query = parser.parse(sql);

        // Verify JOIN clause
        assertEquals(1, query.getJoins().size());
        assertEquals(JoinType.INNER, query.getJoins().getFirst().getType());
        assertEquals("book", query.getJoins().getFirst().getTableSource().getTableName());
        assertNotNull(query.getJoins().getFirst().getOnCondition());
        assertEquals("author.id", query.getJoins().getFirst().getOnCondition().getLeftExpression());
        assertEquals("=", query.getJoins().getFirst().getOnCondition().getOperator());
        assertEquals("book.author_id", query.getJoins().getFirst().getOnCondition().getRightExpression());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with implicit join")
    void testSelectWithImplicitJoin() {
        String sql = "SELECT author.name, book.title FROM author, book WHERE author.id = book.author_id";
        Query query = parser.parse(sql);

        // Verify FROM sources and implicit join
        assertEquals(1, query.getFromSources().size());
        assertEquals("author", query.getFromSources().getFirst().getTableName());
        assertEquals(1, query.getJoins().size());
        assertEquals(JoinType.IMPLICIT, query.getJoins().getFirst().getType());
        assertEquals("book", query.getJoins().getFirst().getTableSource().getTableName());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with GROUP BY and aggregation")
    void testSelectWithGroupByAndAggregation() {
        String sql = "SELECT category, COUNT(*) as book_count FROM book GROUP BY category";
        Query query = parser.parse(sql);

        // Verify GROUP BY clause
        assertEquals(1, query.getGroupByColumns().size());
        assertEquals("category", query.getGroupByColumns().getFirst().getExpression());

        // Verify aggregation function
        assertEquals(2, query.getColumns().size());
        assertEquals("category", query.getColumns().get(0).getExpression());
        assertEquals("COUNT(*)", query.getColumns().get(1).getExpression());
        assertEquals("book_count", query.getColumns().get(1).getAlias());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with GROUP BY, HAVING, and aggregation")
    void testSelectWithGroupByHavingAndAggregation() {
        String sql = "SELECT author.name, COUNT(book.id) as book_count " +
                     "FROM author LEFT JOIN book ON author.id = book.author_id " +
                     "GROUP BY author.name " +
                     "HAVING COUNT(*) > 1";
        Query query = parser.parse(sql);

        // Verify GROUP BY clause
        assertEquals(1, query.getGroupByColumns().size());
        assertEquals("author.name", query.getGroupByColumns().getFirst().getExpression());

        // Verify HAVING clause
        assertNotNull(query.getHavingCondition());
        assertEquals("COUNT(*)", query.getHavingCondition().getLeftExpression());
        assertEquals(">", query.getHavingCondition().getOperator());
        assertEquals("1", query.getHavingCondition().getRightExpression());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with ORDER BY")
    void testSelectWithOrderBy() {
        String sql = "SELECT * FROM book ORDER BY price DESC, title ASC";
        Query query = parser.parse(sql);

        // Verify ORDER BY clause
        assertEquals(2, query.getOrderByColumns().size());
        assertEquals("price", query.getOrderByColumns().getFirst().getColumn());
        assertFalse(query.getOrderByColumns().getFirst().isAscending());
        assertEquals("title", query.getOrderByColumns().get(1).getColumn());
        assertTrue(query.getOrderByColumns().get(1).isAscending());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with LIMIT and OFFSET")
    void testSelectWithLimitAndOffset() {
        String sql = "SELECT * FROM book LIMIT 10 OFFSET 20";
        Query query = parser.parse(sql);

        // Verify LIMIT and OFFSET clauses
        assertEquals(Integer.valueOf(10), query.getLimit());
        assertEquals(Integer.valueOf(20), query.getOffset());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with subquery")
    void testSelectWithSubquery() {
        String sql = "SELECT * FROM (SELECT id, title FROM book WHERE price > 20) as expensive_books";
        Query query = parser.parse(sql);

        // Verify subquery
        assertEquals(1, query.getFromSources().size());
        assertTrue(query.getFromSources().getFirst().isSubquery());
        assertEquals("expensive_books", query.getFromSources().getFirst().getAlias());

        // Verify subquery structure
        Query subquery = query.getFromSources().getFirst().getSubquery();
        assertEquals(2, subquery.getColumns().size());
        assertEquals("id", subquery.getColumns().getFirst().getExpression());
        assertEquals("title", subquery.getColumns().get(1).getExpression());
        assertEquals(1, subquery.getFromSources().size());
        assertEquals("book", subquery.getFromSources().getFirst().getTableName());
        assertNotNull(subquery.getWhereCondition());
        assertEquals("price", subquery.getWhereCondition().getLeftExpression());
        assertEquals(">", subquery.getWhereCondition().getOperator());
        assertEquals("20", subquery.getWhereCondition().getRightExpression());
    }

    @Test
    @DisplayName("Test parsing a complex SELECT query")
    void testComplexSelectQuery() {
        String sql = "SELECT author.name, count(book.id) as book_count, sum(book.price) as total_price " +
                     "FROM author " +
                     "LEFT JOIN book ON (author.id = book.author_id) " +
                     "WHERE author.country = 'USA' " +
                     "GROUP BY author.name " +
                     "HAVING COUNT(*) > 1 AND SUM(book.price) > 500 " +
                     "ORDER BY book_count DESC, author.name ASC " +
                     "LIMIT 10";
        Query query = parser.parse(sql);

        // Verify complex query structure
        assertEquals(3, query.getColumns().size());
        assertEquals(1, query.getFromSources().size());
        assertEquals(1, query.getJoins().size());
        assertEquals(JoinType.LEFT, query.getJoins().getFirst().getType());
        assertNotNull(query.getWhereCondition());
        assertEquals(1, query.getGroupByColumns().size());
        assertNotNull(query.getHavingCondition());
        assertEquals(2, query.getOrderByColumns().size());
        assertEquals(Integer.valueOf(10), query.getLimit());
    }

    @Test
    @DisplayName("Test SELECT query with DISTINCT")
    void testSelectWithDistinct() {
        String sql = "SELECT DISTINCT category FROM book";
        Query query = parser.parse(sql);

        // Verify DISTINCT flag is set
        assertTrue(query.isDistinct());
        assertEquals(1, query.getColumns().size());
        assertEquals("category", query.getColumns().getFirst().getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals("book", query.getFromSources().getFirst().getTableName());
    }

    @Test
    @DisplayName("Test SELECT query without FROM clause")
    void testSelectWithoutFrom() {
        String sql = "SELECT 1 AS constant_value";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("1", query.getColumns().getFirst().getExpression());
        assertEquals("constant_value", query.getColumns().getFirst().getAlias());
        assertTrue(query.getFromSources().isEmpty());
        assertTrue(query.getJoins().isEmpty());
        assertNull(query.getWhereCondition());
    }

    @Test
    @DisplayName("Test SELECT query with multiple expressions without FROM")
    void testSelectMultipleExpressionsWithoutFrom() {
        String sql = "SELECT 1 AS num, 'Hello' AS greeting, 2+2 AS calculation";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(3, query.getColumns().size());
        assertEquals("1", query.getColumns().getFirst().getExpression());
        assertEquals("num", query.getColumns().getFirst().getAlias());
        assertEquals("'Hello'", query.getColumns().get(1).getExpression());
        assertEquals("greeting", query.getColumns().get(1).getAlias());
        assertEquals("2+2", query.getColumns().get(2).getExpression());
        assertEquals("calculation", query.getColumns().get(2).getAlias());
        assertTrue(query.getFromSources().isEmpty());
    }

    @Test
    @DisplayName("Test SELECT query with multiple JOINs")
    void testSelectWithMultipleJoins() {
        String sql = "SELECT a.name, b.title, c.year FROM author a " +
                     "INNER JOIN book b ON a.id = b.author_id " +
                     "LEFT JOIN publication c ON b.id = c.book_id";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(3, query.getColumns().size());
        assertEquals(1, query.getFromSources().size());
        assertEquals("author", query.getFromSources().getFirst().getTableName());
        assertEquals("a", query.getFromSources().getFirst().getAlias());

        // Verify joins
        assertEquals(2, query.getJoins().size());

        // First join
        assertEquals(JoinType.INNER, query.getJoins().getFirst().getType());
        assertEquals("b", query.getJoins().getFirst().getTableSource().getAlias());
        assertEquals("book", query.getJoins().getFirst().getTableSource().getTableName());
        assertNotNull(query.getJoins().getFirst().getOnCondition());
        assertEquals("a.id", query.getJoins().getFirst().getOnCondition().getLeftExpression());
        assertEquals("=", query.getJoins().get(0).getOnCondition().getOperator());
        assertEquals("b.author_id", query.getJoins().get(0).getOnCondition().getRightExpression());

        // Second join
        assertEquals(JoinType.LEFT, query.getJoins().get(1).getType());
        assertEquals("c", query.getJoins().get(1).getTableSource().getAlias());
        assertEquals("publication", query.getJoins().get(1).getTableSource().getTableName());
        assertNotNull(query.getJoins().get(1).getOnCondition());
        assertEquals("b.id", query.getJoins().get(1).getOnCondition().getLeftExpression());
        assertEquals("=", query.getJoins().get(1).getOnCondition().getOperator());
        assertEquals("c.book_id", query.getJoins().get(1).getOnCondition().getRightExpression());
    }

    @Test
    @DisplayName("Test SELECT query with parentheses in WHERE")
    void testSelectWithParenthesesInWhere() {
        String sql = "SELECT * FROM book WHERE (price > 10 AND category = 'Fiction') OR (price < 5 AND category = 'Non-Fiction')";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("*", query.getColumns().getFirst().getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals("book", query.getFromSources().getFirst().getTableName());

        // Verify complex WHERE condition
        assertNotNull(query.getWhereCondition());
        assertTrue(query.getWhereCondition().hasNestedConditions());
        assertEquals("OR", query.getWhereCondition().getLogicalOperator());
    }

    @Test
    @DisplayName("Test SELECT with arithmetic expression and parentheses")
    void testSelectWithArithmeticExpressionAndParentheses() {
        String sql = "SELECT price * (1 - discount) AS final_price FROM products";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("price*(1-discount)", query.getColumns().getFirst().getExpression());
        assertEquals("final_price", query.getColumns().getFirst().getAlias());
        assertEquals(1, query.getFromSources().size());
        assertEquals("products", query.getFromSources().getFirst().getTableName());
    }

    @Test
    @DisplayName("Test SELECT with parenthesized expression and multiplication")
    void testSelectWithParenthesizedExpressionAndMultiplication() {
        String sql = "SELECT (a + b) * c AS calculation FROM math_table";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("(a+b)*c", query.getColumns().getFirst().getExpression());
        assertEquals("calculation", query.getColumns().getFirst().getAlias());
        assertEquals(1, query.getFromSources().size());
        assertEquals("math_table", query.getFromSources().getFirst().getTableName());
    }

    @Test
    @DisplayName("Test SELECT with comparison expression")
    void testSelectWithComparisonExpression() {
        String sql = "SELECT column1 = column2 AS is_equal FROM comparison_table";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("column1=column2", query.getColumns().getFirst().getExpression());
        assertEquals("is_equal", query.getColumns().getFirst().getAlias());
        assertEquals(1, query.getFromSources().size());
        assertEquals("comparison_table", query.getFromSources().getFirst().getTableName());
    }

    @Test
    @DisplayName("Test SELECT with CASE expression")
    void testSelectWithCaseExpression() {
        String sql = "SELECT CASE WHEN price > 100 THEN 'Expensive' ELSE 'Cheap' END AS price_category FROM products";

        // This test currently verifies that the parser doesn't throw an exception
        // for this complex CASE expression, but doesn't verify the exact parsing structure
        Exception exception = null;
        try {
            Query query = parser.parse(sql);

            // We at least expect the column alias to be captured correctly
            assertEquals(1, query.getColumns().size());
            assertEquals("price_category", query.getColumns().getFirst().getAlias());
            assertEquals(1, query.getFromSources().size());
            assertEquals("products", query.getFromSources().getFirst().getTableName());
        } catch (Exception e) {
            exception = e;
        }

        assertNull(exception, "Parser should not throw an exception for CASE expressions");
    }

    @Test
    @DisplayName("Test SELECT with multiple expressions without FROM")
    void testSelectWithComplexExpressionsWithoutFrom() {
        String sql = "SELECT 5 * (2 + 3) AS result1, (10 / 2) - 1 AS result2";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(2, query.getColumns().size());
        assertEquals("5*(2+3)", query.getColumns().getFirst().getExpression());
        assertEquals("result1", query.getColumns().getFirst().getAlias());
        assertEquals("(10/2)-1", query.getColumns().get(1).getExpression());
        assertEquals("result2", query.getColumns().get(1).getAlias());
        assertTrue(query.getFromSources().isEmpty());
    }

    @Test
    @DisplayName("Test SELECT with mixed table.column and expression")
    void testSelectWithMixedTableColumnAndExpression() {
        String sql = "SELECT a.value + b.value * 2 AS calculated_value FROM table1 a, table2 b";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("a.value+b.value*2", query.getColumns().getFirst().getExpression());
        assertEquals("calculated_value", query.getColumns().getFirst().getAlias());
        assertEquals(1, query.getFromSources().size());
        assertEquals("table1", query.getFromSources().getFirst().getTableName());
        assertEquals("a", query.getFromSources().getFirst().getAlias());
        assertEquals(1, query.getJoins().size());
        assertEquals(JoinType.IMPLICIT, query.getJoins().getFirst().getType());
        assertEquals("table2", query.getJoins().getFirst().getTableSource().getTableName());
        assertEquals("b", query.getJoins().getFirst().getTableSource().getAlias());
    }

    // Typo detection tests
    @Test
    @DisplayName("Test parsing with FROM typo")
    void testParsingFromTypo() {
        String sql = "SELECT * FROMM book";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'FROM'"));
    }

    @Test
    @DisplayName("Test parsing with WHERE typo")
    void testParsingWhereTypo() {
        String sql = "SELECT * FROM book WEHRE price > 10";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'WHERE'"));
    }

    @Test
    @DisplayName("Test parsing with GROUP typo")
    void testParsingGroupTypo() {
        String sql = "SELECT category, COUNT(*) FROM book GRUOP BY category";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'GROUP'"));
    }

    @Test
    @DisplayName("Test parsing with HAVING typo")
    void testParsingHavingTypo() {
        String sql = "SELECT category, COUNT(*) FROM book GROUP BY category HAVVING COUNT(*) > 5";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'HAVING'"));
    }

    @Test
    @DisplayName("Test parsing with ORDER typo")
    void testParsingOrderTypo() {
        String sql = "SELECT * FROM book ORDERR BY price DESC";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'ORDER'"));
    }

    @Test
    @DisplayName("Test parsing with LIMIT typo")
    void testParsingLimitTypo() {
        String sql = "SELECT * FROM book LIMT 10";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'LIMIT'"));
    }

    @Test
    @DisplayName("Test parsing with OFFSET typo")
    void testParsingOffsetTypo() {
        String sql = "SELECT * FROM book LIMIT 10 OFSET 5";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'OFFSET'"));
    }

    // Test CASE expression
    @Test
    @DisplayName("Test SELECT with searched CASE expression")
    void testSelectWithSearchedCaseExpression() {
        String sql = "SELECT CASE WHEN price > 100 THEN 'Expensive' WHEN price > 50 THEN 'Moderate' ELSE 'Cheap' END AS price_category FROM products";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("CASE WHEN price>100 THEN 'Expensive' WHEN price>50 THEN 'Moderate' ELSE 'Cheap' END",
                     query.getColumns().getFirst().getExpression());
        assertEquals("price_category", query.getColumns().getFirst().getAlias());
        assertEquals(1, query.getFromSources().size());
        assertEquals("products", query.getFromSources().getFirst().getTableName());
    }

    @Test
    @DisplayName("Test SELECT with simple CASE expression")
    void testSelectWithSimpleCaseExpression() {
        String sql = "SELECT CASE category WHEN 'Electronics' THEN 1.1 WHEN 'Books' THEN 1.05 ELSE 1.0 END AS tax_multiplier FROM products";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("CASE category WHEN 'Electronics' THEN 1.1 WHEN 'Books' THEN 1.05 ELSE 1.0 END",
                     query.getColumns().getFirst().getExpression());
        assertEquals("tax_multiplier", query.getColumns().getFirst().getAlias());
        assertEquals(1, query.getFromSources().size());
        assertEquals("products", query.getFromSources().getFirst().getTableName());
    }

    @Test
    @DisplayName("Test SELECT with multiple columns including CASE expression")
    void testSelectWithMultipleColumnsIncludingCase() {
        String sql = "SELECT id, name, CASE WHEN price > 100 THEN 'High' ELSE 'Low' END AS price_category FROM products";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(3, query.getColumns().size());
        assertEquals("id", query.getColumns().getFirst().getExpression());
        assertEquals("name", query.getColumns().get(1).getExpression());
        assertEquals("CASE WHEN price>100 THEN 'High' ELSE 'Low' END",
                     query.getColumns().get(2).getExpression());
        assertEquals("price_category", query.getColumns().get(2).getAlias());
    }

    @Test
    @DisplayName("Test CASE expression in WHERE clause")
    void testCaseExpressionInWhereClause() {
        String sql = "SELECT * FROM products WHERE CASE WHEN category = 'Electronics' THEN price < 500 ELSE price < 100 END";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("*", query.getColumns().getFirst().getExpression());
        assertEquals(1, query.getFromSources().size());
        assertNotNull(query.getWhereCondition());
        String whereCondition = query.getWhereCondition().toString();
        assertTrue(whereCondition.contains("CASE WHEN category='Electronics' THEN price<500 ELSE price<100 END"));
    }

    @Test
    @DisplayName("Test CASE expression in ORDER BY clause")
    void testCaseExpressionInOrderByClause() {
        String sql = "SELECT * FROM products ORDER BY CASE WHEN category = 'Priority' THEN 1 ELSE 2 END, name";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("*", query.getColumns().getFirst().getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals(2, query.getOrderByColumns().size());
        assertTrue(query.getOrderByColumns().getFirst().getColumn().contains("CASE WHEN category='Priority' THEN 1 ELSE 2 END"));
        assertEquals("name", query.getOrderByColumns().get(1).getColumn());
    }

    @Test
    @DisplayName("Test missing SELECT keyword")
    void testMissingSelectKeyword() {
        String sql = "* FROM book";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected 'SELECT'"));
    }

    @Test
    @DisplayName("Test unbalanced parentheses in expressions")
    void testUnbalancedParentheses() {
        String sql = "SELECT (a + b * c FROM table1";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Unbalanced parentheses") ||
                   exception.getMessage().contains("Expected expression"));
    }

    @Test
    @DisplayName("Test missing alias in subquery")
    void testMissingSubqueryAlias() {
        String sql = "SELECT * FROM (SELECT id FROM products)";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("alias"));
    }

    @Test
    @DisplayName("Test missing ON clause in JOIN")
    void testMissingOnClauseInJoin() {
        String sql = "SELECT * FROM table1 INNER JOIN table2";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("ON") ||
                   exception.getMessage().contains("USING"));
    }

    @Test
    @DisplayName("Test invalid operator in WHERE clause")
    void testInvalidOperatorInWhereClause() {
        String sql = "SELECT * FROM table1 WHERE a !! b";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Invalid token"));
    }

    @Test
    @DisplayName("Test unexpected end of query")
    void testUnexpectedEndOfQuery() {
        String sql = "SELECT * FROM";

        Exception exception = assertThrows(SQLParserException.class, () -> parser.parse(sql));

        assertTrue(exception.getMessage().contains("Expected") ||
                   exception.getMessage().contains("end of query"));
    }
}
