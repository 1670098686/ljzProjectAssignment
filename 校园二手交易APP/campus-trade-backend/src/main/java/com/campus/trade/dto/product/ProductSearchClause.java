package com.campus.trade.dto.product;

import java.util.ArrayList;
import java.util.List;

public class ProductSearchClause {

    private Field field;
    private Operator operator;
    private List<String> values = new ArrayList<>();
    private Relation relation = Relation.AND;

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public Relation getRelation() {
        return relation;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public enum Field {
        CATEGORY,
        STATUS,
        AUDIT_STATUS,
        PRICE,
        LOCATION,
        SELLER_SCHOOL,
        CREATED_TIME
    }

    public enum Operator {
        EQ,
        NE,
        IN,
        NOT_IN,
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }

    public enum Relation {
        AND,
        OR
    }
}
