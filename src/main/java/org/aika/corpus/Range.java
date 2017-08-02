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


import org.aika.Activation;
import org.aika.Utils;
import org.aika.lattice.Node;
import org.aika.lattice.Node.ThreadState;

import java.util.stream.Stream;
import org.aika.neuron.Synapse.RangeMatch;

import static org.aika.neuron.Synapse.RangeMatch.EQUALS;
import static org.aika.neuron.Synapse.RangeMatch.FIRST;
import static org.aika.neuron.Synapse.RangeMatch.GREATER_THAN;

/**
 *
 * @author Lukas Molzberger
 */
public class Range {

    public static final Range MIN = new Range(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public static final Range MAX = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public final Integer begin;
    public final Integer end;


    public static Stream getActivations(Document doc, Node n, Integer rid, Range r, RangeMatch begin, RangeMatch end, Option o, Option.Relation or) {
        ThreadState th = n.getThreadState(doc, false);
        if(th == null) return Stream.empty();

        Stream s;
        if((begin == GREATER_THAN || begin == EQUALS) && r.begin != null) {
            int er = (end == RangeMatch.LESS_THAN || end == RangeMatch.EQUALS) && r.end != null ? r.end : Integer.MAX_VALUE;
            s = th.activations.subMap(
                    new Activation.Key(n, new Range(r.begin, null), null, Option.MIN),
                    true,
                    new Activation.Key(n, new Range(er, Integer.MAX_VALUE), Integer.MAX_VALUE, Option.MAX),
                    true
            )
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
            if(end == FIRST) {
                s = s.limit(1);
            }
        } else if((begin == RangeMatch.LESS_THAN || begin == RangeMatch.EQUALS) && r.begin != null) {
            s = th.activations.descendingMap().subMap(
                    new Activation.Key(n, new Range(r.begin, Integer.MAX_VALUE), null, Option.MAX),
                    true,
                    new Activation.Key(n, new Range(0, null), null, Option.MIN),
                    true
            )
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
            if(end == FIRST) {
                s = s.limit(1);
            }
        } else {
            s = th.activations.values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, begin, end, o, or));
        }

        return s;
    }



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


    public boolean contains(int p) {
        return begin <= p && p < end;
    }



    public static boolean contains(Range ra, Range rb) {
        return Utils.compareInteger(ra.begin, rb.begin) <= 0 && Utils.compareInteger(ra.end, rb.end) >= 0;
    }


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
}
