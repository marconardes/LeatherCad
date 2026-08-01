package com.leathercad.core.parametric;

public class Variable {
    private String name;
    private String expression;
    private double value;
    private String unit;
    private String description;

    public Variable(String name, String expression, double value, String unit, String description) {
        this.name = name;
        this.expression = expression;
        this.value = value;
        this.unit = unit;
        this.description = description;
    }

    public Variable(String name, double value, String unit, String description) {
        this(name, String.valueOf(value), value, unit, description);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
