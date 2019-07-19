package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
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

    public PositionRelation(int fromSlot, int toSlot, boolean optional, boolean follow) {
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.optional = optional;
        this.follow = follow;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct, boolean allowUndefined) {
        Position toPos = linkedAct.lookupSlot(toSlot);
        if(allowUndefined && toPos == null) {
            return true;
        }
        return optional || test(act.lookupSlot(fromSlot), toPos);
    }


    public abstract boolean test(Position a, Position b);


    @Override
    public void mapSlots(Map<Integer, Position> slots, Activation act) {
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
        Position pos = linkedAct.lookupSlot(toSlot);
        if(pos == null) {
            return Stream.empty();
        }

        return getActivations(n, pos)
                .filter(act -> test(act, linkedAct, false));
    }

    public abstract Stream<Activation> getActivations(INeuron n, Position pos);


    public static class Equals extends PositionRelation {
        public static int TYPE = 0;

        static {
            registerRelation(TYPE, () -> new Equals());
        }


        public Equals() {}


        public Equals(int fromSlot, int toSlot) {
            super(fromSlot, toSlot);
        }

        public Equals(int fromSlot, int toSlot, boolean optional, boolean follow) {
            super(fromSlot, toSlot, optional, follow);
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return new Equals(toSlot, fromSlot, optional, follow);
        }

        @Override
        public Relation setOptionalAndFollow(boolean optional, boolean follow) {
            return new Equals(fromSlot, toSlot, optional, follow);
        }

        @Override
        public boolean test(Position a, Position b) {
            return a == b;
        }

        @Override
        public void mapSlots(Map<Integer, Position> slots, Activation act) {
            slots.put(toSlot, act.lookupSlot(fromSlot));
        }

        @Override
        public boolean isExact() {
            return true;
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Position pos) {
            if(!follow) return Stream.empty();
            return n.getActivations(pos.getDocument(),
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
        private int maxLength = Integer.MAX_VALUE;

        static {
            registerRelation(TYPE, () -> new LessThan());
        }


        public LessThan() {

        }

        public LessThan(int fromSlot, int toSlot, boolean orEquals) {
            super(fromSlot, toSlot);
            this.orEquals = orEquals;
        }

        public LessThan(int fromSlot, int toSlot, boolean orEquals, boolean optional, boolean follow, int maxLength) {
            super(fromSlot, toSlot, optional, follow);
            this.orEquals = orEquals;
            this.maxLength = maxLength;
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return new GreaterThan(toSlot, fromSlot, orEquals, optional, follow, maxLength);
        }

        @Override
        public Relation setOptionalAndFollow(boolean optional, boolean follow) {
            return new LessThan(fromSlot, toSlot, orEquals, optional, follow, maxLength);
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

            return a.getFinalPosition() != null && b.getFinalPosition() != null && a.getFinalPosition() < b.getFinalPosition() && (b.getFinalPosition() - a.getFinalPosition() < maxLength);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Position pos) {
            if(!follow) return Stream.empty();
            return n.getActivations(
                    pos.getDocument(),
                    fromSlot, new Position(pos.getDocument(), maxLength != Integer.MAX_VALUE ? pos.getFinalPosition() - maxLength : Integer.MIN_VALUE), true,
                    fromSlot, pos, orEquals
            );
        }

        @Override
        public void write(DataOutput out) throws IOException {
            super.write(out);

            out.writeBoolean(orEquals);
            out.writeInt(maxLength);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            super.readFields(in, m);

            orEquals = in.readBoolean();
            maxLength = in.readInt();
        }

        public String toString() {
            return "LT" + (orEquals ? "E" : "") + "(" + fromSlot + "," + toSlot + ")";
        }
    }


    public static class GreaterThan extends PositionRelation {
        public static int TYPE = 11;

        private boolean orEquals;
        private int maxLength = Integer.MAX_VALUE;

        static {
            registerRelation(TYPE, () -> new GreaterThan());
        }


        public GreaterThan() {

        }

        public GreaterThan(int fromSlot, int toSlot, boolean orEquals) {
            super(fromSlot, toSlot);
            this.orEquals = orEquals;
        }

        public GreaterThan(int fromSlot, int toSlot, boolean orEquals, boolean optional, boolean follow, int maxLength) {
            super(fromSlot, toSlot, optional, follow);
            this.orEquals = orEquals;
            this.maxLength = maxLength;
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public Relation invert() {
            return new LessThan(toSlot, fromSlot, orEquals, optional, follow, maxLength);
        }

        @Override
        public Relation setOptionalAndFollow(boolean optional, boolean follow) {
            return new GreaterThan(fromSlot, toSlot, orEquals, optional, follow, maxLength);
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

            return a.getFinalPosition() != null && b.getFinalPosition() != null && a.getFinalPosition() > b.getFinalPosition() && (a.getFinalPosition() - b.getFinalPosition() < maxLength);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Position pos) {
            if(!follow) return Stream.empty();
            return n.getActivations(
                    pos.getDocument(),
                    fromSlot, pos, orEquals,
                    fromSlot, new Position(pos.getDocument(), maxLength != Integer.MAX_VALUE ? pos.getFinalPosition() + maxLength : Integer.MAX_VALUE), true
            );
        }


        @Override
        public void write(DataOutput out) throws IOException {
            super.write(out);

            out.writeBoolean(orEquals);
            out.writeInt(maxLength);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            super.readFields(in, m);

            orEquals = in.readBoolean();
            maxLength = in.readInt();
        }

        public String toString() {
            return "GT" + (orEquals ? "E" : "") + "(" + fromSlot + "," + toSlot + ")";
        }
    }
}
