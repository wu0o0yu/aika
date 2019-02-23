package network.aika.neuron.activation;


import network.aika.Document;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class Position {

    public static final Position MIN = new Position(null, Integer.MIN_VALUE);
    public static final Position MAX = new Position(null, Integer.MAX_VALUE);


    public SortedMap<ActKey, Activation> activations = new TreeMap<>();


    private Document doc;
    public final int id;
    private Integer finalPosition;


    public Position(Document doc) {
        this(doc, null);
    }


    public Position(Document doc, Integer pos) {
        this.doc = doc;
        this.id = doc != null ? doc.positionIdCounter++ : -1;
        finalPosition = pos;
    }


    public Document getDocument() {
        return doc;
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
        return finalPosition != null ? "" + finalPosition : "<" + id + ">";
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


    public void setFinalPosition(int finalPos) {
        finalPosition = finalPos;
    }


    public void addActivation(Integer slot, Activation act) {
        activations.put(new ActKey(slot, act.id), act);
    }


    public Stream<Activation> getActivations(int slot) {
        return activations.subMap(new ActKey(slot, Integer.MIN_VALUE), new ActKey(slot, Integer.MAX_VALUE))
                .values()
                .stream();
    }


    public static class ActKey implements Comparable<ActKey> {
        int slot;
        int actId;

        public ActKey(int slot, int actId) {
            this.slot = slot;
            this.actId = actId;
        }

        @Override
        public int compareTo(ActKey ak) {
            int r = Integer.compare(slot, ak.slot);
            if(r != 0) return r;
            return Integer.compare(actId, ak.actId);
        }
    }

}
