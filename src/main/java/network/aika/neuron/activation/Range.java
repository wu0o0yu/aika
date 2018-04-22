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
package network.aika.neuron.activation;

import network.aika.Model;
import network.aika.Utils;
import network.aika.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import static network.aika.neuron.activation.Range.Mapping.BEGIN;


/**
 * The class {@code Range} specifies a text range (begin char pos, end char pos) within a given document.
 *
 * @author Lukas Molzberger
 */
public class Range {

    public static Comparator<Range> BEGIN_COMP = (r1, r2) -> {
        int r = Integer.compare(r1.begin, r2.begin);
        if(r != 0) return r;
        return Integer.compare(r1.end, r2.end);
    };

    public static Comparator<Range> END_COMP = (r1, r2) -> {
        int r = Integer.compare(r1.end, r2.end);
        if(r != 0) return r;
        return Integer.compare(r1.begin, r2.begin);
    };

    public static final Range MIN = new Range(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public static final Range MAX = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public final int begin;
    public final int end;


    public Range(Integer begin, Integer end) {
        this.begin = begin != null ? begin : Integer.MIN_VALUE;
        this.end = end != null ? end : Integer.MAX_VALUE;
    }


    @Deprecated
    public static boolean overlaps(Range ra, Range rb) {
        return !(Utils.compareInteger(ra.end, rb.begin) <= 0 || Utils.compareInteger(rb.end, ra.begin) <= 0);
    }


    public int getBegin(boolean invert) {
        return invert ? end : begin;
    }


    public int getEnd(boolean invert) {
        return invert ? begin : end;
    }


    public Range invert(boolean inv) {
        return inv ? new Range(end, begin) : this;
    }


    public boolean isEmpty() {
        return length() == 0;
    }


    public int length() {
        if(begin == Integer.MIN_VALUE || end == Integer.MAX_VALUE) return Integer.MAX_VALUE;

        return end - begin;
    }


    public boolean contains(Range r) {
        return begin <= r.begin && r.end <= end ;
    }


    public boolean equals(Range r) {
        return begin == r.begin && end == r.end;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(");
        sb.append(begin != Integer.MIN_VALUE ? begin : "MIN");
        sb.append(",");
        sb.append(end != Integer.MAX_VALUE ? end : "MAX");
        sb.append(")");

        return sb.toString();
    }


    public static int compare(Range ra, Range rb, boolean inv) {
        int a = Integer.compare(ra.getBegin(inv), rb.getBegin(inv));
        if(a != 0) return a;
        int b = Integer.compare(ra.getEnd(inv), rb.getEnd(inv));
        return b;
    }


    public static int compare(Range ra, Range rb) {
        int a = Integer.compare(ra.begin, rb.begin);
        if(a != 0) return a;
        int b = Integer.compare(ra.end, rb.end);
        return b;
    }


    public static class Relation implements Writable, Comparable<Relation> {
        private static SortedMap<Relation, Relation> map = new TreeMap();

        public static Relation EQUALS = create(Operator.EQUALS, Operator.EQUALS);
        public static Relation BEGIN_EQUALS = create(Operator.EQUALS, Operator.NONE);
        public static Relation END_EQUALS = create(Operator.NONE, Operator.EQUALS);
        public static Relation BEGIN_TO_END_EQUALS = create(Operator.NONE, Operator.EQUALS, Operator.NONE , Operator.NONE);
        public static Relation END_TO_BEGIN_EQUALS = create(Operator.NONE, Operator.NONE, Operator.NONE , Operator.EQUALS);
        public static Relation CONTAINS = create(Operator.LESS_THAN_EQUAL, Operator.GREATER_THAN_EQUAL);
        public static Relation CONTAINED_IN = create(Operator.GREATER_THAN_EQUAL, Operator.LESS_THAN_EQUAL);
        public static Relation OVERLAPS = create(Operator.NONE, Operator.LESS_THAN, Operator.NONE, Operator.GREATER_THAN);
        public static Relation NONE = create(Operator.NONE, Operator.NONE);


        public Operator beginToBegin = Operator.NONE;
        public Operator beginToEnd = Operator.NONE;
        public Operator endToEnd = Operator.NONE;
        public Operator endToBegin = Operator.NONE;

        public Relation() {}

        private Relation(Operator beginToBegin, Operator beginToEnd, Operator endToEnd, Operator endToBegin) {
            this.beginToBegin = beginToBegin;
            this.beginToEnd = beginToEnd;
            this.endToEnd = endToEnd;
            this.endToBegin = endToBegin;
        }


        public static Relation create(Operator beginToBegin, Operator beginToEnd, Operator endToEnd, Operator endToBegin) {
            return lookup(new Relation(beginToBegin, beginToEnd, endToEnd, endToBegin));
        }


        public static Relation create(Operator beginToBegin, Operator endToEnd) {
            return create(beginToBegin, Operator.NONE, endToEnd, Operator.NONE);
        }


        public boolean compare(Range ra, Range rb) {
            return beginToBegin.compare(ra.begin, rb.begin) &&
                    beginToEnd.compare(ra.begin, rb.end) &&
                    endToEnd.compare(ra.end, rb.end) &&
                    endToBegin.compare(ra.end, rb.begin);
        }


        public Relation invert() {
            return new Relation(beginToBegin.invert(), endToBegin.invert(), endToEnd.invert(), beginToEnd.invert());
        }


        @Override
        public int compareTo(Relation rr) {
            int r = beginToBegin.compareTo(rr.beginToBegin);
            if(r != 0) return r;
            r = beginToEnd.compareTo(rr.beginToEnd);
            if(r != 0) return r;
            r = endToEnd.compareTo(rr.endToEnd);
            if(r != 0) return r;
            r = endToBegin.compareTo(rr.endToBegin);
            return r;
        }


        public static Relation lookup(Relation r) {
            Relation rr = map.get(r);
            if(rr == null) {
                rr = r;
                map.put(r, r);
            }
            return rr;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeByte(beginToBegin.getId());
            out.writeByte(beginToEnd.getId());
            out.writeByte(endToEnd.getId());
            out.writeByte(endToBegin.getId());
        }

        public static Relation read(DataInput in, Model m) throws IOException {
            Relation r = new Relation();
            r.readFields(in, m);
            return lookup(r);
        }

        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            beginToBegin = Operator.getById(in.readByte());
            beginToEnd = Operator.getById(in.readByte());
            endToEnd = Operator.getById(in.readByte());
            endToBegin = Operator.getById(in.readByte());
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


        public boolean compare(int a, int b) {
            switch(this) {
                case EQUALS:
                    return a == b;
                case LESS_THAN_EQUAL:
                    return a <= b;
                case GREATER_THAN_EQUAL:
                    return a >= b;
                case LESS_THAN:
                    return a < b;
                case GREATER_THAN:
                    return a > b;
                default:
                    return true;
            }
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


    public static class Output implements Writable, Comparable<Output> {
        private static SortedMap<Output, Output> map = new TreeMap();

        public static Output NONE = create(Mapping.NONE, Mapping.NONE);
        public static Output DIRECT = create(Mapping.BEGIN, Mapping.END);
        public static Output BEGIN = create(Mapping.BEGIN, Mapping.NONE);
        public static Output END = create(Mapping.NONE, Mapping.END);

        public Mapping begin = Mapping.NONE;
        public Mapping end = Mapping.NONE;


        private Output() {}


        private Output(Mapping begin, Mapping end) {
            this.begin = begin;
            this.end = end;
        }


        public Range map(Range r) {
            return new Range(begin.map(r), end.map(r));
        }


        public Output invert() {
            return new Output(
                    begin == Mapping.BEGIN ? Mapping.BEGIN : (end == Mapping.BEGIN ? Mapping.END : Mapping.NONE),
                    begin == Mapping.END ? Mapping.BEGIN : (end == Mapping.END ? Mapping.END : Mapping.NONE)
            );
        }


        public static Output create(Mapping begin, Mapping end) {
            return lookup(new Output(begin, end));
        }


        public static Output lookup(Output r) {
            Output rr = map.get(r);
            if(rr == null) {
                rr = r;
                map.put(r, r);
            }
            return rr;
        }


        @Override
        public int compareTo(Output ro) {
            int r = Utils.compareInteger(begin.ordinal(), ro.begin.ordinal());
            if (r != 0) return r;
            r = Utils.compareInteger(end.ordinal(), ro.end.ordinal());
            return r;
        }


        @Override
        public void write(DataOutput out) throws IOException {
            out.writeByte(begin.getId());
            out.writeByte(end.getId());
        }


        public static Output read(DataInput in, Model m) throws IOException {
            Output r = new Output();
            r.readFields(in, m);
            return lookup(r);
        }


        @Override
        public void readFields(DataInput in, Model m) throws IOException {
            begin = Mapping.getById(in.readByte());
            end = Mapping.getById(in.readByte());
        }

    }


    public enum Mapping {
        BEGIN(0),
        END(1),
        NONE(2);

        Mapping(int id) {
            this.id =  id;
        }

        int id;

        public static Mapping getById(int id) {
            for(Mapping m: Mapping.values()) {
                if(m.id == id) return m;
            }
            return null;
        }


        public int getId() {
            return id;
        }


        public Integer map(Range r) {
            switch(this) {
                case BEGIN:
                    return r.begin;
                case END:
                    return r.end;
                case NONE:
                default:
                    return null;
            }
        }

        public String toString() {
            switch (this) {
                case BEGIN:
                    return "S";
                case END:
                    return "E";
                case NONE:
                    return "N";
            }
            return "";
        }
    }
}
