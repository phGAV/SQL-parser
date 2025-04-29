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
}
