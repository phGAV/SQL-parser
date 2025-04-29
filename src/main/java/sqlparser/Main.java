package main.java.sqlparser;

import main.java.sqlparser.model.*;
import main.java.sqlparser.parser.*;
import main.java.sqlparser.util.*;

/**
 * Main class to demonstrate the SQL parser functionality.
 */
public class Main {

    public static void main(String[] args) {
        // Create a new SQL parser
        SQLParser parser = new SQLParser();

        // Example queries to parse
        String[] exampleQueries = {
          // Simple SELECT query
          "SELECT * FROM book",

          // SELECT with explicit columns
          "SELECT id, title, author FROM book",

          // SELECT with aliases
          "SELECT b.id AS book_id, b.title AS book_title FROM book b",

          // SELECT with WHERE clause
          "SELECT * FROM book WHERE price > 10.99 AND category = 'Fiction'",

          // SELECT with JOIN
          "SELECT author.name, book.title FROM author INNER JOIN book ON author.id = book.author_id",

          // SELECT with LEFT JOIN
          "SELECT author.name, book.title FROM author LEFT JOIN book ON author.id = book.author_id",

          // SELECT with implicit join
          "SELECT author.name, book.title FROM author, book WHERE author.id = book.author_id",

          // SELECT with GROUP BY and aggregation
          "SELECT category, COUNT(*) as book_count FROM book GROUP BY category",

          // SELECT with GROUP BY, aggregation, and HAVING
          "SELECT author.name, COUNT(book.id) as book_count, SUM(book.price) as total_price " +
          "FROM author LEFT JOIN book ON author.id = book.author_id " +
          "GROUP BY author.name " +
          "HAVING COUNT(*) > 1 AND SUM(book.price) > 50",

          // SELECT with ORDER BY
          "SELECT * FROM book ORDER BY price DESC, title ASC",

          // SELECT with LIMIT and OFFSET
          "SELECT * FROM book ORDER BY publication_date DESC LIMIT 10 OFFSET 20",

          // SELECT with subquery
          "SELECT * FROM (SELECT id, title FROM book WHERE price > 20) as expensive_books",

          // Complex SELECT with multiple features
          "SELECT author.name, count(book.id) as book_count, sum(book.price) as total_price " +
          "FROM author " +
          "LEFT JOIN book ON (author.id = book.author_id) " +
          "WHERE author.country = 'USA' " +
          "GROUP BY author.name " +
          "HAVING COUNT(*) > 1 AND SUM(book.price) > 500 " +
          "ORDER BY book_count DESC, author.name ASC " +
          "LIMIT 10"
        };

        // Parse each example query and print the result
        for (int i = 0; i < exampleQueries.length; i++) {
            System.out.println("Example " + (i + 1) + ":");
            System.out.println("Input SQL:");
            System.out.println(exampleQueries[i]);
            System.out.println();

            try {
                // Parse the query
                Query query = parser.parse(exampleQueries[i]);

                // Print the parsed query structure
                System.out.println("Parsed Query Structure:");
                System.out.println("- Columns: " + query.getColumns());
                System.out.println("- From Sources: " + query.getFromSources());
                System.out.println("- Joins: " + query.getJoins());
                System.out.println("- Where Condition: " + query.getWhereCondition());
                System.out.println("- Group By Columns: " + query.getGroupByColumns());
                System.out.println("- Having Condition: " + query.getHavingCondition());
                System.out.println("- Order By Columns: " + query.getOrderByColumns());
                System.out.println("- Limit: " + query.getLimit());
                System.out.println("- Offset: " + query.getOffset());
                System.out.println();

                // Format the query and print it
                System.out.println("Formatted SQL:");
                System.out.println(SQLFormatter.format(query));

            } catch (SQLParserException e) {
                System.out.println("Parsing Error: " + e.getMessage());
            }

            System.out.println("\n" + "=".repeat(80) + "\n");
        }

        // Additional demonstration of parsing a query and modifying it programmatically
        try {
            System.out.println("Modifying a query programmatically:");

            // Parse a query
            String originalQuery = "SELECT * FROM product WHERE category = 'Electronics'";
            System.out.println("Original Query: " + originalQuery);

            Query query = parser.parse(originalQuery);

            // Modify the query
            // Add a LIMIT clause
            query.setLimit(5);

            // Add an ORDER BY clause
            query.addOrderByColumn(new OrderByColumn("price", false)); // DESC

            // Add a JOIN clause
            TableSource manufacturerTable = new TableSource("manufacturer");
            Join join = new Join(manufacturerTable, JoinType.INNER);
            Condition onCondition = new Condition("product.manufacturer_id", "=", "manufacturer.id");
            join.setOnCondition(onCondition);
            query.addJoin(join);

            // Add a column
            query.addColumn(new Column("manufacturer.name", "manufacturer_name"));

            // Print the modified query
            System.out.println("\nModified Query:");
            System.out.println(SQLFormatter.format(query));

        } catch (SQLParserException e) {
            System.out.println("Parsing Error: " + e.getMessage());
        }
    }
}
