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
package org.aika.corpus;

import org.aika.Utils;

/**
 * The class {@code Range} specifies a text range (begin char pos, end char pos) within a given document.
 *
 * @author Lukas Molzberger
 */
public class Range {

    public static final Range MIN = new Range(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public static final Range MAX = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public final Integer begin;
    public final Integer end;


    public Range(Integer begin, Integer end) {
        this.begin = begin;
        this.end = end;
    }


    public static Range mergeRange(Range ra, Range rb) {
        return new Range(
                ra.begin != null ? ra.begin : rb.begin,
                ra.end != null ? ra.end : rb.end
        );
    }


    public static Range getOutputRange(Range r, boolean[] ro) {
        return new Range(
                ro[0] ? r.begin : null,
                ro[1] ? r.end : null
        );
    }


    @Deprecated
    public static boolean overlaps(Range ra, Range rb) {
        return !(Utils.compareInteger(ra.end, rb.begin) <= 0 || Utils.compareInteger(rb.end, ra.begin) <= 0);
    }


    public Integer getBegin(boolean invert) {
        return invert ? end : begin;
    }


    public Integer getEnd(boolean invert) {
        return invert ? begin : end;
    }


    public Range invert(boolean inv) {
        return inv ? new Range(end, begin) : this;
    }


    public boolean isEmpty() {
        return end - begin == 0;
    }


    public int length() {
        return end - begin;
    }


    public boolean contains(Range r) {
        return begin <= r.begin && r.end <= end ;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("(");
        sb.append(begin);
        sb.append(",");
        sb.append(end);
        sb.append(")");

        return sb.toString();
    }


    public static int compare(Range ra, Range rb, boolean inv) {
        if(ra == null && rb == null) return 0;
        if(ra == null && rb != null) return -1;
        if(ra != null && rb == null) return 1;

        int a = Utils.compareInteger(ra.getBegin(inv), rb.getBegin(inv));
        if(a != 0) return a;
        int b = Utils.compareInteger(ra.getEnd(inv), rb.getEnd(inv));
        return b;
    }


    public static int compare(Range ra, Range rb) {
        int a = Utils.compareInteger(ra.begin, rb.begin);
        if(a != 0) return a;
        int b = Utils.compareInteger(ra.end, rb.end);
        return b;
    }


    public enum Operator {
        EQUALS,
        LESS_THAN,
        GREATER_THAN,
        FIRST,
        LAST,
        NONE;

        public boolean compare(Integer a, Integer b, Integer c, Integer d) {
            if(a == null || c == null) return true;
            switch(this) {
                case EQUALS:
                    return a.intValue() == c.intValue();
                case LESS_THAN:
                    return a <= c;
                case GREATER_THAN:
                    return a >= c;
                case FIRST:
                    return d <= b && b < c;
                case LAST:
                    return b <= d && a > d;
                default:
                    return true;
            }
        }

        public static Operator invert(Operator rm) {
            switch(rm) {
                case EQUALS:
                    return EQUALS;
                case LESS_THAN:
                    return GREATER_THAN;
                case GREATER_THAN:
                    return LESS_THAN;
                case FIRST:
                    return LAST;
                case LAST:
                    return FIRST;
                default:
                    return NONE;
            }
        }
    }


    public enum Mapping {
        START,
        END,
        NONE;

        public Integer getSignalPos(Range r) {
            switch(this) {
                case START:
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
                case START:
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
