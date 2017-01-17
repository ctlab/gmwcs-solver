package ru.ifmo.ctddev.gmwcs.graph;

import java.util.ArrayList;
import java.util.List;

public abstract class Unit implements Comparable<Unit> {
    protected int num;
    protected double weight;
    protected List<Unit> absorbed;
    protected boolean required;

    public Unit(int num, double weight, boolean required) {
        this.num = num;
        this.weight = weight;
        this.required = required;
        absorbed = new ArrayList<>();
    }

    public void absorb(Unit unit) {
        for (Unit u : unit.getAbsorbed()) {
            absorbed.add(u);
            weight += u.weight;
        }
        unit.clear();
        absorbed.add(unit);
        weight += unit.weight;
    }

    public void clear() {
        for (Unit unit : absorbed) {
            weight -= unit.getWeight();
        }
        absorbed.clear();
    }

    public List<Unit> getAbsorbed() {
        return new ArrayList<>(absorbed);
    }

    @Override
    public int hashCode() {
        return num;
    }

    public int getNum() {
        return num;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isRequired(){
        return required;
    }

    public void setRequired(boolean required){
        this.required = required;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        return (o.getClass() == getClass() && num == ((Unit) o).num);
    }

    @Override
    public int compareTo(Unit u) {
        if (u.weight != weight) {
            return Double.compare(u.weight, weight);
        }
        return Integer.compare(u.getNum(), num);
    }
}