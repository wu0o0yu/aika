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
import org.aika.Activation.Key;
import org.aika.Utils;
import org.aika.lattice.Node;
import org.aika.neuron.Synapse;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.aika.neuron.Synapse.RangeMatch;

/**
 *
 * @author Lukas Molzberger
 */
public class Range {

    public static final Range MIN = new Range(Integer.MIN_VALUE, Integer.MIN_VALUE);
    public static final Range MAX = new Range(Integer.MAX_VALUE, Integer.MAX_VALUE);

    public final Integer begin;
    public final Integer end;


    public static abstract class Relation {
        public int id;

        public static Map<Integer, Relation> RANGE_RELATIONS = new TreeMap<>();

        public Relation(int id) {
            this.id = id;

            RANGE_RELATIONS.put(id, this);
        }

        public static Relation EQUALS = new Relation(0) {
            @Override
            public boolean match(Range ra, Range rb) {
                return Range.compare(ra, rb, false) == 0;
            }

            public Stream getActivations(Document doc, Node n, Integer rid, Range r, Option o, Option.Relation or) {
                return n.getThreadState(doc).activations.subMap(new Activation.Key(n, r, null, Option.MIN), true, new Activation.Key(n, r, Integer.MAX_VALUE, Option.MAX), true)
                        .values()
                        .stream()
                        .filter(act -> act.filter(n, rid, r, this, o, or));
            }

            public Relation invert() {
                return EQUALS;
            }
        };

        public static Relation CONTAINS = new Relation(1) {
            @Override
            public boolean match(Range ra, Range rb) {
                return contains(ra, rb);
            }


            public Relation invert() {
                return CONTAINED_IN;
            }
        };


        public static Relation CONTAINED_IN = new Relation(2) {
            @Override
            public boolean match(Range ra, Range rb) {
                return contains(ra, rb);
            }


            public Relation invert() {
                return CONTAINS;
            }
        };

        public static Relation OVERLAPS = new Relation(3) {
            @Override
            public boolean match(Range ra, Range rb) {
                return overlaps(ra, rb);
            }

            public Relation invert() {
                return OVERLAPS;
            }
        };

        public static Relation BEGINS_WITH = new Relation(4) {
            @Override
            public boolean match(Range ra, Range rb) {
                return ra.begin == rb.begin;
            }

            public Relation invert() {
                return BEGINS_WITH;
            }
        };

        public static Relation ENDS_WITH = new Relation(5) {
            @Override
            public boolean match(Range ra, Range rb) {
                return ra.end == rb.end;
            }

            public Relation invert() {
                return ENDS_WITH;
            }
        };

        public static Relation NONE = new Relation(6) {
            @Override
            public boolean match(Range ra, Range rb) {
                return true;
            }

            public Relation invert() {
                return NONE;
            }
        };

        public abstract Relation invert();

        public abstract boolean match(Range ra, Range rb);

        public Stream<Activation> getActivations(Document doc, Node n, Integer rid, Range r, Option o, Option.Relation or) {
            return n.getThreadState(doc).activations.values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, this, o, or));
        }
    }


    public static class RangeMatcher extends Relation {
        public RangeMatch begin;
        public RangeMatch end;

        public RangeMatcher(RangeMatch begin, RangeMatch end) {
            super(-1);
            this.begin = begin;
            this.end = end;
        }


        @Override
        public Relation invert() {
            return new RangeMatcher(RangeMatch.invert(begin), RangeMatch.invert(end));
        }

        @Override
        public boolean match(Range ra, Range rb) {
            return (ra.begin == null || rb.begin == null || begin.compare(ra.begin, rb.begin)) &&
                   (ra.end == null || rb.end == null || end.compare(ra.end, rb.end));
        }

        public Stream getActivations(Document doc, Node n, Integer rid, Range r, Option o, Option.Relation or) {
            if((begin == RangeMatch.GREATER_THAN || begin == RangeMatch.EQUALS) && r.begin != null) {
                int er = (end == RangeMatch.LESS_THAN || end == RangeMatch.EQUALS) && r.end != null ? r.end : Integer.MAX_VALUE;
                return n.getThreadState(doc).activations.subMap(new Activation.Key(n, new Range(r.begin, null), null, Option.MIN), true, new Activation.Key(n, new Range(er, Integer.MAX_VALUE), Integer.MAX_VALUE, Option.MAX), true)
                        .values()
                        .stream()
                        .filter(act -> act.filter(n, rid, r, this, o, or));
            } else {
                return n.getThreadState(doc).activations.values()
                        .stream()
                        .filter(act -> act.filter(n, rid, r, this, o, or));
            }
        }

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

/*
    public static class SynapseRangeMatcher extends Relation {
        public boolean dir;
        public Synapse s;

        public SynapseRangeMatcher(Synapse s, boolean dir) {
            this.s = s;
            this.dir = dir;
        }


        @Override
        public boolean match(Range ra, Range rb) {
            int x = getDirection() ? ra.end : ra.begin;
            int y = getSignal() ? rb.end : rb.begin;

            return x == y;
        }

        private boolean getDirection() {
            if(!dir && s.key.startSignal == RangeSignal.START && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(!dir && s.key.endSignal == RangeSignal.START && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(dir && s.key.startSignal != RangeSignal.NONE && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(!dir && s.key.startSignal == RangeSignal.END && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            } else if(!dir && s.key.endSignal == RangeSignal.END && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            } else if(dir &&s.key.endSignal != RangeSignal.NONE && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            }

            return false;
        }

        private boolean getSignal() {
            if(!dir && s.key.startSignal == RangeSignal.START && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(!dir && s.key.startSignal == RangeSignal.END && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(dir && s.key.startSignal == RangeSignal.START && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(dir && s.key.endSignal == RangeSignal.START && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return false;
            } else if(!dir && s.key.endSignal == RangeSignal.START && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            } else if(!dir && s.key.endSignal == RangeSignal.END && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            } else if(dir && s.key.startSignal == RangeSignal.END && s.key.startRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            } else if(dir && s.key.endSignal == RangeSignal.END && s.key.endRangeOutput == RangeOutput.MATCH_INPUT) {
                return true;
            }
            return false;
        }

        @Override
        public Stream<Activation> getActivations(Document doc, Node n, Integer rid, Range r, Option o, Option.Relation or) {
            Node.ThreadState th = n.getThreadState(doc);
            boolean x = getDirection();
            int y = getSignal() ? r.end : r.begin;
            NavigableMap<Key, Activation> tmp = (x ? th.activationsEnd : th.activations).subMap(
                    new Activation.Key(n, x ? new Range(Integer.MIN_VALUE, y) : new Range(y, Integer.MIN_VALUE), null, Option.MIN), true,
                    new Activation.Key(n, x ? new Range(Integer.MAX_VALUE, y) : new Range(y, Integer.MAX_VALUE), Integer.MAX_VALUE, Option.MAX), true
            );

            return tmp
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, this, o, or));
        }
    }

    public static class BeginEndMatcher extends Relation {
        boolean[] mra;
        boolean[] mrb;

        public BeginEndMatcher(boolean[] mra, boolean[] mrb) {
            this.mra = mra;
            this.mrb = mrb;
        }

        @Override
        public boolean match(Range ra, Range rb) {
            if(mra[0] && mrb[0]) {
                return ra.begin == rb.begin;
            } else if(mra[1] && mrb[1]) {
                return ra.end == rb.end;
            } else if (mra[1] && mrb[0]) {
                return ((rb.begin == Integer.MIN_VALUE && ra.begin < rb.end) || ra.begin == rb.begin) &&
                        ((ra.end == Integer.MAX_VALUE && rb.end > ra.begin) || ra.end >= rb.end);
            } else if(mra[0] && mrb[1]) {
                return ((rb.end == Integer.MAX_VALUE && ra.end > rb.begin) || ra.end == rb.end) &&
                        ((ra.begin == Integer.MIN_VALUE && rb.begin < ra.end) || ra.begin <= rb.begin);
            }

            return true;
        }

        @Override
        public Stream<Activation> getActivations(Document doc, Node n, Integer rid, Range r, Option o, Option.Relation or) {
            NavigableMap<Key, Activation> tmp;
            boolean flag = false;
            Node.ThreadState th = n.getThreadState(doc);
            if(mra[0] && mrb[0]) {
                tmp = th.activations.subMap(
                        new Key(n, new Range(r.begin, Integer.MIN_VALUE), null, Option.MIN), true,
                        new Key(n, new Range(r.begin, Integer.MAX_VALUE), Integer.MAX_VALUE, Option.MAX), true
                );
            } else if(mra[1] && mrb[1]) {
                tmp = th.activationsEnd.subMap(
                        new Key(n, new Range(Integer.MIN_VALUE, r.end), null, Option.MIN), true,
                        new Key(n, new Range(Integer.MAX_VALUE, r.end), Integer.MAX_VALUE, Option.MAX), true
                );
            } else if (mra[1] && mrb[0]) {
                tmp = th.activations.descendingMap().subMap(
                        new Key(n, new Range(r.end, Integer.MAX_VALUE), Integer.MAX_VALUE, Option.MAX), false,
                        new Key(n, MIN, null, Option.MIN), true
                );
                flag = true;
            } else if(mra[0] && mrb[1]) {
                tmp = th.activationsEnd.subMap(
                        new Key(n, new Range(Integer.MIN_VALUE, r.begin), null, Option.MIN), false,
                        new Key(n, MAX, Integer.MAX_VALUE, Option.MAX), true
                );
                flag = true;
            } else {
                tmp = th.activations;
            }

            Stream<Activation> s = tmp
                    .values()
                    .stream()
                    .filter(act -> act.filter(n, rid, r, this, o, or));
            return !flag ? s : s.limit(1);
        }
    }

    */
}
