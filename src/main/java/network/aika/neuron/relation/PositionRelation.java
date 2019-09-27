/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;


/**
 *
 * @author Lukas Molzberger
 */
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
    public boolean test(Activation act, Activation linkedAct, boolean allowUndefined, Direction dir) {
        Position toPos = linkedAct.getSlot(dir == Direction.FORWARD ? toSlot : fromSlot);
        if(allowUndefined && toPos == null) {
            return true;
        }
        return test(act.getSlot(dir == Direction.FORWARD ? fromSlot : toSlot), toPos, dir);
    }


    public abstract boolean test(Position a, Position b, Direction dir);


    protected int getFromSlot(boolean dir) {
        return dir ? fromSlot : toSlot;
    }

    protected int getToSlot(boolean dir) {
        return dir ? toSlot : fromSlot;
    }

    protected int getFromSlot(Direction dir) {
        return dir == Direction.FORWARD ? fromSlot : toSlot;
    }

    protected int getToSlot(Direction dir) {
        return dir == Direction.FORWARD ? toSlot : fromSlot;
    }


    @Override
    public void mapSlots(Map<Integer, Position> slots, Activation act, Direction dir) {
    }


    @Override
    public int compareTo(Relation rel, boolean sameDir) {
        int r = super.compareTo(rel, sameDir);
        if(r != 0) return r;

        PositionRelation pr = (PositionRelation) rel;

        r = Integer.compare(fromSlot, pr.getFromSlot(sameDir));
        if(r != 0) return r;
        r = Integer.compare(toSlot, pr.getToSlot(sameDir));
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
    public Stream<Activation> getActivations(INeuron n, Activation linkedAct, Direction dir) {
        Position pos = linkedAct.getSlot(getToSlot(dir));
        if(pos == null) {
            return Stream.empty();
        }

        return getActivations(n, pos, dir)
                .filter(act -> test(act, linkedAct, false, dir));
    }

    public abstract Stream<Activation> getActivations(INeuron n, Position pos, Direction dir);


    protected Stream<Activation> getActivationsGreaterThan(INeuron n, int slot, Position pos, boolean orEquals, int maxLength) {
        return n.getActivations(
                pos.getDocument(),
                slot, pos, orEquals,
                slot, new Position(pos.getDocument(), maxLength != Integer.MAX_VALUE ? pos.getFinalPosition() + maxLength : Integer.MAX_VALUE), true
        );
    }


    protected Stream<Activation> getActivationsLessThan(INeuron n, int slot, Position pos, boolean orEquals, int maxLength) {
        return n.getActivations(
                pos.getDocument(),
                slot, new Position(pos.getDocument(), maxLength != Integer.MAX_VALUE ? pos.getFinalPosition() - maxLength : Integer.MIN_VALUE), true,
                slot, pos, orEquals
        );
    }


    protected boolean testGreaterThan(Position a, Position b, boolean orEquals, int maxLength) {
        if(a == b) {
            return orEquals;
        }

        return a.getFinalPosition() != null && b.getFinalPosition() != null && a.getFinalPosition() > b.getFinalPosition() && (a.getFinalPosition() - b.getFinalPosition() < maxLength);
    }


    protected boolean testLessThan(Position a, Position b, boolean orEquals, int maxLength) {
        if(a == b) {
            return orEquals;
        }

        return a.getFinalPosition() != null && b.getFinalPosition() != null && a.getFinalPosition() < b.getFinalPosition() && (b.getFinalPosition() - a.getFinalPosition() < maxLength);
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
        public boolean test(Position a, Position b, Direction dir) {
            return a == b;
        }

        @Override
        public void mapSlots(Map<Integer, Position> slots, Activation act, Direction dir) {
            Position pos = act.getSlot(getFromSlot(dir));
            if(pos != null) {
                slots.put(getToSlot(dir), pos);
            }
        }

        @Override
        public boolean isExact() {
            return true;
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Position pos, Direction dir) {
            return n.getActivations(pos.getDocument(),
                    getFromSlot(dir), pos, true,
                    getFromSlot(dir), pos, true
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

        public LessThan(int fromSlot, int toSlot, boolean orEquals, int maxLength) {
            super(fromSlot, toSlot);
            this.orEquals = orEquals;
            this.maxLength = maxLength;
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public boolean isExact() {
            return false;
        }


        @Override
        public boolean test(Position a, Position b, Direction dir) {
            return dir == Direction.FORWARD ?
                    testLessThan(a, b, orEquals, maxLength) :
                    testGreaterThan(a, b, orEquals, maxLength);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Position pos, Direction dir) {
            return dir == Direction.FORWARD ?
                    getActivationsLessThan(n, getFromSlot(dir), pos, orEquals, maxLength) :
                    getActivationsGreaterThan(n, getFromSlot(dir), pos, orEquals, maxLength);
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

        public GreaterThan(int fromSlot, int toSlot, boolean orEquals, int maxLength) {
            super(fromSlot, toSlot);
            this.orEquals = orEquals;
            this.maxLength = maxLength;
        }

        @Override
        public int getType() {
            return TYPE;
        }

        @Override
        public boolean isExact() {
            return false;
        }

        @Override
        public boolean test(Position a, Position b, Direction dir) {
            return dir == Direction.FORWARD ?
                    testGreaterThan(a, b, orEquals, maxLength) :
                    testLessThan(a, b, orEquals, maxLength);
        }

        @Override
        public Stream<Activation> getActivations(INeuron n, Position pos, Direction dir) {
            return dir == Direction.FORWARD ?
                    getActivationsGreaterThan(n, getFromSlot(dir), pos, orEquals, maxLength) :
                    getActivationsLessThan(n, getFromSlot(dir), pos, orEquals, maxLength);
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
