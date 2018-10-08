package network.aika.neuron.range;


import network.aika.neuron.activation.Activation;

import java.util.SortedSet;
import java.util.TreeSet;

public class Position {

    public static final Position MIN = new Position(Integer.MIN_VALUE);
    public static final Position MAX = new Position(Integer.MAX_VALUE);


    public SortedSet<Activation> beginActivations = new TreeSet<>();
    public SortedSet<Activation> endActivations = new TreeSet<>();


    private Integer finalPosition;


    public Position() {
    }


    public Position(int pos) {
        finalPosition = pos;
    }


    public static int compare(Position a, Position b) {
        return a.compare(b);
    }

    public int compare(Position pos) {
        if(finalPosition != null && pos.finalPosition != null) {
            return Integer.compare(finalPosition, pos.finalPosition);
        }

        return 0; // TODO:
    }


    public String toString() {
        return finalPosition != null ? "" + finalPosition : "X";
    }


    public boolean compare(Operator o, Position pos) {

        if(finalPosition != null && pos.finalPosition != null) {
            switch(o) {
                case EQUALS:
                    return finalPosition == pos.finalPosition;
                case LESS_THAN_EQUAL:
                    return finalPosition <= pos.finalPosition;
                case GREATER_THAN_EQUAL:
                    return finalPosition >= pos.finalPosition;
                case LESS_THAN:
                    return finalPosition < pos.finalPosition;
                case GREATER_THAN:
                    return finalPosition > pos.finalPosition;
                default:
                    return true;
            }
        }

        return false; // TODO:
    }

    public Integer getDistance(Position pos) {
        if(finalPosition != null && pos.finalPosition != null) {
            return pos.finalPosition - finalPosition;
        }
        return null;
    }


    public Integer getFinalPosition() {
        return finalPosition;
    }

    public void addBeginActivation(Activation act) {
        if(finalPosition == 0) {
            beginActivations.add(act);
        }
    }

    public void addEndActivations(Activation act) {
        if(finalPosition == 0) {
            endActivations.add(act);
        }
    }


    public enum Operator {
        EQUALS(0),
        LESS_THAN_EQUAL(1),
        GREATER_THAN_EQUAL(2),
        LESS_THAN(3),
        GREATER_THAN(4),
        NONE(5);

        Operator(int id) {
            this.id = (short) id;
        }

        int id;

        public static Operator getById(int id) {
            for(Operator o: Operator.values()) {
                if(o.id == id) return o;
            }
            return null;
        }


        public boolean isGreaterThanOrGreaterThanEqual() {
            return this == GREATER_THAN || this == GREATER_THAN_EQUAL;
        }


        public boolean isLessThanOrLessThanEqual() {
            return this == LESS_THAN || this == LESS_THAN_EQUAL;
        }


        public boolean includesEqual() {
            return this == EQUALS || this == GREATER_THAN_EQUAL || this == LESS_THAN_EQUAL;
        }


        public int getId() {
            return id;
        }


        public Operator invert() {
            switch(this) {
                case EQUALS:
                    return EQUALS;
                case LESS_THAN_EQUAL:
                    return GREATER_THAN_EQUAL;
                case GREATER_THAN_EQUAL:
                    return LESS_THAN_EQUAL;
                case LESS_THAN:
                    return GREATER_THAN;
                case GREATER_THAN:
                    return LESS_THAN;
                default:
                    return NONE;
            }
        }
    }

}
