# SQL Query Parser

A Java implementation of a parser for SQL SELECT queries, capable of parsing and representing complex queries with various clauses and features.

## Overview

This project provides a robust SQL SELECT query parser that converts SQL query strings into structured Java objects. 
It supports a wide range of SQL features including:

- Column selection (explicit fields with aliases or *)
- Table sources in FROM clauses
- Joins (both implicit and explicit: INNER, LEFT, RIGHT, FULL, CROSS)
- WHERE conditions with various operators and logical combinations
- Subqueries
- GROUP BY clauses
- HAVING clauses
- ORDER BY clauses
- LIMIT and OFFSET clauses

## Project Structure

The project is organized into the following packages:

- `model`: Data structures representing SQL query components
- `parser`: The query parsing logic
- `util`: Utility classes for formatting and displaying queries

## Key Classes

### Models

- `Query`: Represents a complete SQL SELECT query
- `Column`: Represents a column in a SELECT or GROUP BY clause
- `TableSource`: Represents a table in the FROM clause, can be a table name or subquery
- `Join`: Represents a JOIN between tables
- `Condition`: Represents conditions in WHERE, HAVING, and ON clauses
- `OrderByColumn`: Represents a column in an ORDER BY clause
- `JoinType`: Enum of supported JOIN types

### Parser

- `SQLParser`: The main parser class that converts SQL strings to Query objects

### Utilities

- `SQLFormatter`: Formats Query objects back into readable SQL strings

## Usage

The parser can be used to:

1. Parse SQL queries into structured objects
2. Analyze and modify query components programmatically
3. Generate formatted SQL strings from Query objects

### Example

```java
// Create a parser
SQLParser parser = new SQLParser();

// Parse a SQL query
String sql = "SELECT author.name, count(book.id) as book_count " +
             "FROM author JOIN book ON author.id = book.author_id " +
             "GROUP BY author.name HAVING COUNT(*) > 1";
Query query = parser.parse(sql);

// Access query components
List<Column> columns = query.getColumns();
List<TableSource> tables = query.getFromSources();
Condition whereCondition = query.getWhereCondition();

// Modify query programmatically
query.setLimit(10);
query.addOrderByColumn(new OrderByColumn("book_count", false)); // DESC

// Generate formatted SQL
String formattedSql = SQLFormatter.format(query);
System.out.println(formattedSql);
```

## Supported Features

### Required Features

- ✓ Explicit column selection with aliases
- ✓ SELECT * syntax
- ✓ Implicit table joins (SELECT * FROM A, B, C)
- ✓ Explicit table joins (INNER, LEFT, RIGHT, FULL JOIN)
- ✓ Filter conditions (WHERE clause)
- ✓ Subqueries
- ✓ GROUP BY clauses
- ✓ ORDER BY clauses
- ✓ LIMIT and OFFSET clauses

### Excluded Features

- ✗ UNION and UNION ALL
- ✗ Common Table Expressions (CTE)
- ✗ Window functions

## Testing

`./gradlew test` - for running unit tests

## Running the Demo

`./gradlew run` - for running the main class with example SQL queries

## Requirements

- Java 21 or higher

## Optimization Plan

### High-Priority Improvements

1. **Parser Structure**
    - Break down large methods in SQLParser (parseCondition, parseExpression)
    - Extract specialized parsers
    - Implement expression parsing with operator precedence

2. **Error Handling**
    - Improve error messages with context and position information

3. **Performance**
    - Optimize tokenization to reduce string operations
    - Reduce object creation during parsing

### Medium-Priority Improvements

4. **Features & Testing**
   - Enhance subquery and complex JOIN support
   - Add more test cases for edge cases and error scenarios

### Low-Priority Improvements

5. **Documentation & Usability**
    - Enhance JavaDoc for all public APIs
    - Create a matrix of supported SQL features
