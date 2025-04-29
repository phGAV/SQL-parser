package test.java.sqlparser.parser;

import main.java.sqlparser.model.*;
import main.java.sqlparser.parser.*;
import main.java.sqlparser.util.*;

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
        assertEquals("*", query.getColumns().get(0).getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals("book", query.getFromSources().get(0).getTableName());
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
        assertEquals(JoinType.INNER, query.getJoins().get(0).getType());
        assertEquals("book", query.getJoins().get(0).getTableSource().getTableName());
        assertNotNull(query.getJoins().get(0).getOnCondition());
        assertEquals("author.id", query.getJoins().get(0).getOnCondition().getLeftExpression());
        assertEquals("=", query.getJoins().get(0).getOnCondition().getOperator());
        assertEquals("book.author_id", query.getJoins().get(0).getOnCondition().getRightExpression());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with implicit join")
    void testSelectWithImplicitJoin() {
        String sql = "SELECT author.name, book.title FROM author, book WHERE author.id = book.author_id";
        Query query = parser.parse(sql);

        // Verify FROM sources and implicit join
        assertEquals(1, query.getFromSources().size());
        assertEquals("author", query.getFromSources().get(0).getTableName());
        assertEquals(1, query.getJoins().size());
        assertEquals(JoinType.IMPLICIT, query.getJoins().get(0).getType());
        assertEquals("book", query.getJoins().get(0).getTableSource().getTableName());
    }

    @Test
    @DisplayName("Test parsing a SELECT query with GROUP BY and aggregation")
    void testSelectWithGroupByAndAggregation() {
        String sql = "SELECT category, COUNT(*) as book_count FROM book GROUP BY category";
        Query query = parser.parse(sql);

        // Verify GROUP BY clause
        assertEquals(1, query.getGroupByColumns().size());
        assertEquals("category", query.getGroupByColumns().get(0).getExpression());

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
        assertEquals("author.name", query.getGroupByColumns().get(0).getExpression());

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
        assertEquals("price", query.getOrderByColumns().get(0).getColumn());
        assertFalse(query.getOrderByColumns().get(0).isAscending());
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
        assertTrue(query.getFromSources().get(0).isSubquery());
        assertEquals("expensive_books", query.getFromSources().get(0).getAlias());

        // Verify subquery structure
        Query subquery = query.getFromSources().get(0).getSubquery();
        assertEquals(2, subquery.getColumns().size());
        assertEquals("id", subquery.getColumns().get(0).getExpression());
        assertEquals("title", subquery.getColumns().get(1).getExpression());
        assertEquals(1, subquery.getFromSources().size());
        assertEquals("book", subquery.getFromSources().get(0).getTableName());
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
        assertEquals(JoinType.LEFT, query.getJoins().get(0).getType());
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
        assertEquals("category", query.getColumns().get(0).getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals("book", query.getFromSources().get(0).getTableName());
    }

    @Test
    @DisplayName("Test SELECT query without FROM clause")
    void testSelectWithoutFrom() {
        String sql = "SELECT 1 AS constant_value";
        Query query = parser.parse(sql);

        // Verify query structure
        assertEquals(1, query.getColumns().size());
        assertEquals("1", query.getColumns().get(0).getExpression());
        assertEquals("constant_value", query.getColumns().get(0).getAlias());
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
        assertEquals("1", query.getColumns().get(0).getExpression());
        assertEquals("num", query.getColumns().get(0).getAlias());
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
        assertEquals("author", query.getFromSources().get(0).getTableName());
        assertEquals("a", query.getFromSources().get(0).getAlias());

        // Verify joins
        assertEquals(2, query.getJoins().size());

        // First join
        assertEquals(JoinType.INNER, query.getJoins().get(0).getType());
        assertEquals("b", query.getJoins().get(0).getTableSource().getAlias());
        assertEquals("book", query.getJoins().get(0).getTableSource().getTableName());
        assertNotNull(query.getJoins().get(0).getOnCondition());
        assertEquals("a.id", query.getJoins().get(0).getOnCondition().getLeftExpression());
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
        assertEquals("*", query.getColumns().get(0).getExpression());
        assertEquals(1, query.getFromSources().size());
        assertEquals("book", query.getFromSources().get(0).getTableName());

        // Verify complex WHERE condition
        assertNotNull(query.getWhereCondition());
        assertTrue(query.getWhereCondition().hasNestedConditions());
        assertEquals("OR", query.getWhereCondition().getLogicalOperator());
    }

    // Typo detection tests
    @Test
    @DisplayName("Test parsing with FROM typo")
    void testParsingFromTypo() {
        String sql = "SELECT * FROMM book";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'FROM'"));
    }

    @Test
    @DisplayName("Test parsing with WHERE typo")
    void testParsingWhereTypo() {
        String sql = "SELECT * FROM book WEHRE price > 10";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'WHERE'"));
    }

    @Test
    @DisplayName("Test parsing with GROUP typo")
    void testParsingGroupTypo() {
        String sql = "SELECT category, COUNT(*) FROM book GRUOP BY category";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'GROUP'"));
    }

    @Test
    @DisplayName("Test parsing with HAVING typo")
    void testParsingHavingTypo() {
        String sql = "SELECT category, COUNT(*) FROM book GROUP BY category HAVVING COUNT(*) > 5";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'HAVING'"));
    }

    @Test
    @DisplayName("Test parsing with ORDER typo")
    void testParsingOrderTypo() {
        String sql = "SELECT * FROM book ORDERR BY price DESC";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'ORDER'"));
    }

    @Test
    @DisplayName("Test parsing with LIMIT typo")
    void testParsingLimitTypo() {
        String sql = "SELECT * FROM book LIMT 10";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'LIMIT'"));
    }

    @Test
    @DisplayName("Test parsing with OFFSET typo")
    void testParsingOffsetTypo() {
        String sql = "SELECT * FROM book LIMIT 10 OFSET 5";

        Exception exception = assertThrows(SQLParserException.class, () -> {
            parser.parse(sql);
        });

        assertTrue(exception.getMessage().contains("Expected 'OFFSET'"));
    }
}
