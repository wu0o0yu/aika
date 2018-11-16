package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;



public abstract class PositionRelation extends Relation {

    public int fromSlot;
    public int toSlot;


    public PositionRelation() {

    }

    public PositionRelation(int fromSlot, int toSlot) {
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct) {
        return test(act.getSlot(fromSlot), linkedAct.getSlot(toSlot));
    }


    public abstract boolean test(Position a, Position b);


    @Override
    public void mapRange(Map<Integer, Position> slots, Activation act) {
    }


    @Override
    public void linksOutputs(Set<Integer> results) {
        results.add(toSlot);
    }


    @Override
    public int compareTo(Relation rel) {
        int r = super.compareTo(rel);
        if(r != 0) return r;

        PositionRelation pr = (PositionRelation) rel;

        r = Integer.compare(fromSlot, pr.fromSlot);
        if(r != 0) return r;
        r = Integer.compare(toSlot, pr.toSlot);
        return r;
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeInt(fromSlot);
        out.writeInt(toSlot);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        fromSlot = in.readInt();
        toSlot = in.readInt();
    }


    @Override
    public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
        INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);

        if(th == null || th.isEmpty()) {
            return Stream.empty();
        }

        Position pos = linkedAct.getSlot(toSlot);

        return getActivations(th, pos)
                .filter(act -> test(act, linkedAct));
    }

    public abstract Stream<Activation> getActivations(INeuron.ThreadState th, Position pos);


    @Override
    public void registerRequiredSlots(Neuron input) {
        input.get().slotRequired.add(fromSlot);
    }


    public static class Equals extends PositionRelation {
        public static int TYPE = 0;

        static {
            registerRelation(TYPE, () -> new Equals());
        }


        public Equals() {}


        public Equals(int fromSlot, int toSlot) {
            super(fromSlot, toSlot);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return new Equals(toSlot, fromSlot);
        }

        @Override
        public boolean test(Position a, Position b) {
            return a == b;
        }

        @Override
        public void mapRange(Map<Integer, Position> slots, Activation act) {
            slots.put(toSlot, act.getSlot(fromSlot));
        }

        @Override
        public boolean isExact() {
            return true;
        }

        @Override
        public Stream<Activation> getActivations(INeuron.ThreadState th, Position pos) {
            return th.getActivations(
                    fromSlot, pos, true,
                    fromSlot, pos, true
            );
        }

        public String toString() {
            return "EQUALS(" + fromSlot + "," + toSlot + ")";
        }
    }


    public static class LessThan extends PositionRelation {
        public static int TYPE = 10;

        private boolean orEquals;

        static {
            registerRelation(TYPE, () -> new LessThan());
        }


        public LessThan() {

        }

        public LessThan(int fromSlot, int toSlot, boolean orEquals) {
            super(fromSlot, toSlot);
            this.orEquals = orEquals;
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return new GreaterThan(toSlot, fromSlot, orEquals);
        }

        @Override
        public boolean isExact() {
            return false;
        }

        @Override
        public boolean test(Position a, Position b) {
            if(a == b) {
                return orEquals;
            }

            return a.getFinalPosition() != null && b.getFinalPosition() != null && a.getFinalPosition() < b.getFinalPosition();
        }

        @Override
        public Stream<Activation> getActivations(INeuron.ThreadState th, Position pos) {
            return th.getActivations(
                    fromSlot, Position.MIN, true,
                    fromSlot, pos, orEquals
            );
        }

        @Override
        public void write(DataOutput out) throws IOException {
            super.write(out);

            out.writeBoolean(orEquals);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            super.readFields(in, m);

            orEquals = in.readBoolean();
        }

        public String toString() {
            return "LT" + (orEquals ? "E" : "") + "(" + fromSlot + "," + toSlot + ")";
        }
    }


    public static class GreaterThan extends PositionRelation {
        public static int TYPE = 11;

        private boolean orEquals;

        static {
            registerRelation(TYPE, () -> new GreaterThan());
        }


        public GreaterThan() {

        }

        public GreaterThan(int fromSlot, int toSlot, boolean orEquals) {
            super(fromSlot, toSlot);
            this.orEquals = orEquals;
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return new LessThan(toSlot, fromSlot, orEquals);
        }

        @Override
        public boolean isExact() {
            return false;
        }

        @Override
        public boolean test(Position a, Position b) {
            if(a == b) {
                return orEquals;
            }

            return a.getFinalPosition() != null && b.getFinalPosition() != null && a.getFinalPosition() > b.getFinalPosition();
        }

        @Override
        public Stream<Activation> getActivations(INeuron.ThreadState th, Position pos) {
            return th.getActivations(
                    fromSlot, pos, orEquals,
                    fromSlot, Position.MAX, true
            );
        }


        @Override
        public void write(DataOutput out) throws IOException {
            super.write(out);

            out.writeBoolean(orEquals);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            super.readFields(in, m);

            orEquals = in.readBoolean();
        }

        public String toString() {
            return "GT" + (orEquals ? "E" : "") + "(" + fromSlot + "," + toSlot + ")";
        }
    }
}
