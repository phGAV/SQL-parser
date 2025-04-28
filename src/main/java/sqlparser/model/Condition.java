package main.java.sqlparser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a condition in a SQL query, used in WHERE and HAVING clauses.
 * Can represent both simple conditions and complex nested conditions.
 */
public class Condition {
    private Object leftExpression;         // Left side of the condition (can be column, value, or subquery)
    private String operator;               // Comparison operator (=, >, <, etc.)
    private Object rightExpression;        // Right side of the condition (can be column, value, or subquery)
    private String logicalOperator;        // AND, OR for combining with next condition
    private Condition nextCondition;       // Next condition in a chain of conditions
    private List<Condition> nestedConditions; // For grouped conditions in parentheses
    private boolean isNegated = false;     // Whether the condition is negated (e.g., NOT)

    // For simple conditions (e.g., column = value)
    public Condition(Object leftExpression, String operator, Object rightExpression) {
        this.leftExpression = leftExpression;
        this.operator = operator;
        this.rightExpression = rightExpression;
    }

    // For nested conditions (grouped in parentheses)
    public Condition(List<Condition> nestedConditions) {
        this.nestedConditions = nestedConditions;
    }

    // Default constructor
    public Condition() {
        this.nestedConditions = new ArrayList<>();
    }

    public Object getLeftExpression() {
        return leftExpression;
    }

    public void setLeftExpression(Object leftExpression) {
        this.leftExpression = leftExpression;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getRightExpression() {
        return rightExpression;
    }

    public void setRightExpression(Object rightExpression) {
        this.rightExpression = rightExpression;
    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(String logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public Condition getNextCondition() {
        return nextCondition;
    }

    public void setNextCondition(Condition nextCondition) {
        this.nextCondition = nextCondition;
    }

    public boolean isNegated() {
        return isNegated;
    }

    public void setNegated(boolean negated) {
        isNegated = negated;
    }

    public List<Condition> getNestedConditions() {
        return nestedConditions;
    }

    public void setNestedConditions(List<Condition> nestedConditions) {
        this.nestedConditions = nestedConditions;
    }

    public void addNestedCondition(Condition condition) {
        if (this.nestedConditions == null) {
            this.nestedConditions = new ArrayList<>();
        }
        this.nestedConditions.add(condition);
    }

    public boolean hasNestedConditions() {
        return nestedConditions != null && !nestedConditions.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isNegated) {
            sb.append("NOT ");
        }

        if (hasNestedConditions()) {
            sb.append("(");
            for (int i = 0; i < nestedConditions.size(); i++) {
                sb.append(nestedConditions.get(i).toString());
                if (i < nestedConditions.size() - 1 && nestedConditions.get(i).getLogicalOperator() != null) {
                    sb.append(" ").append(nestedConditions.get(i).getLogicalOperator()).append(" ");
                }
            }
            sb.append(")");
        } else {
            sb.append(leftExpression).append(" ").append(operator).append(" ").append(rightExpression);
        }

        if (nextCondition != null && logicalOperator != null) {
            sb.append(" ").append(logicalOperator).append(" ").append(nextCondition.toString());
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Condition condition = (Condition) o;
        return isNegated == condition.isNegated &&
               Objects.equals(leftExpression, condition.leftExpression) &&
               Objects.equals(operator, condition.operator) &&
               Objects.equals(rightExpression, condition.rightExpression) &&
               Objects.equals(logicalOperator, condition.logicalOperator) &&
               Objects.equals(nextCondition, condition.nextCondition) &&
               Objects.equals(nestedConditions, condition.nestedConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftExpression, operator, rightExpression, logicalOperator, nextCondition, nestedConditions, isNegated);
    }
}
