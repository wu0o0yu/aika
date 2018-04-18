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

import network.aika.Document;
import network.aika.lattice.Node;
import network.aika.neuron.Relation;
import network.aika.neuron.INeuron;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Range.Operator.EQUALS;


/**
 * The {@code Selector} class retrieves activations either from the document or the associated neuron.
 *
 * @author Lukas Molzberger
 */
public class Selector {


    public static Stream<Activation> select(Document doc, INeuron n, Relation rel, Activation linkedAct) {
        INeuron.ThreadState th = n.getThreadState(doc.threadId, false);
        if (th == null) return Stream.empty();
        return select(th, n, rel, linkedAct);
    }


    public static Stream<Activation> select(INeuron.ThreadState th, INeuron n, Relation rel, Activation linkedAct) {
        Stream<Activation> results;
        int s = th.activations.size();

        if (s == 0) return Stream.empty();
        else if (s == 1) {
            results = th.activations.values().stream();
        } else {
            results = th.activations.values().stream();
        }

        return results.filter(act -> act.filter(n.node.get(), rel, linkedAct));
    }


    public static Collection<Activation> getActivationsByRange(INeuron.ThreadState th, INeuron n, Range r, Range.Relation rr) {
        Node node = n.node.get();
        if(rr.beginToBegin == EQUALS || rr.beginToEnd == EQUALS || rr.endToBegin == EQUALS || rr.endToEnd == EQUALS) {
            return getActivationsByRangeEquals(th, n, r, rr);
        } else if (((rr.beginToBegin.isGreaterThanOrGreaterThanEqual() || rr.beginToEnd.isGreaterThanOrGreaterThanEqual())) && r.begin <= r.end) {
            return getActivationsByRangeBeginGreaterThan(th, r, rr, node);
        } else if (((rr.endToEnd.isGreaterThanOrGreaterThanEqual() || rr.endToBegin.isGreaterThanOrGreaterThanEqual())) && r.begin >= r.end) {
            return getActivationsByRangeEndGreaterThan(th, r, rr, node);
        } else if ((rr.beginToBegin.isLessThanOrLessThanEqual() || rr.beginToEnd.isLessThanOrLessThanEqual()) && r.begin <= r.end) {
            return getActivationsByRangeBeginLessThanEqual(th, r, rr, node);
        } else if ((rr.endToEnd.isLessThanOrLessThanEqual() || rr.endToBegin.isLessThanOrLessThanEqual()) && r.begin >= r.end) {
            return getActivationsByRangeEndLessThanEqual(th, r, rr, node);
        } else {
            return th.activations.values();
        }
    }


    private static Collection<Activation> getActivationsByRangeBeginGreaterThan(INeuron.ThreadState th, Range r, Range.Relation rr, Node node) {
        int fromKey;
        boolean fromInclusive;

        if(rr.beginToBegin.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.begin;
            fromInclusive = rr.beginToBegin.includesEqual();
        } else {
            fromKey = r.end;
            fromInclusive = rr.beginToEnd.includesEqual();
        }

        int toKey;
        boolean toInclusive;
        if(rr.endToEnd.isLessThanOrLessThanEqual()) {
            toKey = r.end;
            toInclusive = rr.endToEnd.includesEqual();
        } else if(rr.endToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.endToBegin.includesEqual();
        } else {
            toKey = Integer.MAX_VALUE;
            toInclusive = true;
        }

        return th.activations.subMap(
                new Range(fromKey, Integer.MIN_VALUE),
                fromInclusive,
                new Range(toKey, Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    private static Collection<Activation> getActivationsByRangeEndGreaterThan(INeuron.ThreadState th, Range r, Range.Relation rr, Node node) {
        int fromKey;
        boolean fromInclusive;

        if(rr.endToEnd.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.end;
            fromInclusive = rr.endToEnd.includesEqual();
        } else {
            fromKey = r.begin;
            fromInclusive = rr.endToBegin.includesEqual();
        }

        int toKey;
        boolean toInclusive;
        if(rr.beginToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.beginToBegin.includesEqual();
        } else if(rr.beginToEnd.isLessThanOrLessThanEqual()) {
            toKey = r.end;
            toInclusive = rr.beginToEnd.includesEqual();
        } else {
            toKey = Integer.MAX_VALUE;
            toInclusive = true;
        }

        return th.activationsEnd.subMap(
                new Range(Integer.MIN_VALUE, fromKey),
                fromInclusive,
                new Range(Integer.MAX_VALUE, toKey),
                toInclusive
        ).values();
    }


    private static Collection<Activation> getActivationsByRangeBeginLessThanEqual(INeuron.ThreadState th, Range r, Range.Relation rr, Node node) {
        int fromKey;
        boolean fromInclusive;
        if(rr.endToEnd.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.end - th.maxLength;
            fromInclusive = rr.endToEnd.includesEqual();
        } else if(rr.endToBegin.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.begin - th.maxLength;
            fromInclusive = rr.endToBegin.includesEqual();
        } else {
            fromKey = Integer.MIN_VALUE;
            fromInclusive = true;
        }

        int toKey;
        boolean toInclusive;

        if(rr.beginToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.beginToBegin.includesEqual();
        } else {
            toKey = r.end;
            toInclusive = rr.beginToEnd.includesEqual();
        }

        if(fromKey > toKey) return Collections.EMPTY_LIST;

        return th.activations.subMap(
                new Range(fromKey, Integer.MIN_VALUE),
                fromInclusive,
                new Range(toKey, Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    private static Collection<Activation> getActivationsByRangeEndLessThanEqual(INeuron.ThreadState th, Range r, Range.Relation rr, Node node) {
        int fromKey;
        boolean fromInclusive;
        if(rr.beginToEnd.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.end - th.maxLength;
            fromInclusive = rr.beginToEnd.includesEqual();
        } else if(rr.beginToBegin.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.begin - th.maxLength;
            fromInclusive = rr.beginToBegin.includesEqual();
        } else {
            fromKey = Integer.MIN_VALUE;
            fromInclusive = true;
        }

        int toKey;
        boolean toInclusive;

        if(rr.endToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.endToBegin.includesEqual();
        } else {
            toKey = r.end;
            toInclusive = rr.endToEnd.includesEqual();
        }

        if(fromKey > toKey) return Collections.EMPTY_LIST;

        return th.activationsEnd.subMap(
                new Range(Integer.MIN_VALUE, fromKey),
                fromInclusive,
                new Range(Integer.MAX_VALUE, toKey),
                toInclusive
        ).values();
    }



    public static Collection<Activation> getActivationsByRangeEquals(INeuron.ThreadState th, INeuron n, Range r, Range.Relation rr) {
        Node node = n.node.get();
        if(rr.beginToBegin == EQUALS || rr.beginToEnd == EQUALS) {
            int key = rr.beginToBegin == EQUALS ? r.begin : r.end;
            return th.activations.subMap(
                    new Range(key, Integer.MIN_VALUE),
                    true,
                    new Range(key, Integer.MAX_VALUE),
                    true
            ).values();
        } else if(rr.endToEnd == EQUALS || rr.endToBegin == EQUALS) {
            int key = rr.endToEnd == EQUALS ? r.end : r.begin;
            return th.activationsEnd.subMap(
                    new Range(Integer.MIN_VALUE, key),
                    true,
                    new Range(Integer.MAX_VALUE, key),
                    true
            ).values();
        }
        throw new RuntimeException("Invalid Range Relation");
    }


    public static Collection<Activation> select(Linker.SortGroup sortGroup, Activation act) {
        switch(sortGroup) {
            case RANGE_BEGIN: {
                Document.ActKey bk = new Document.ActKey(new Range(act.range.begin, Integer.MIN_VALUE), Node.MIN_NODE);
                Document.ActKey ek = new Document.ActKey(new Range(act.range.begin, Integer.MAX_VALUE), Node.MAX_NODE);

                return act.doc.activationsByRangeBegin.subMap(bk, true, ek, true).values();
            }
            case RANGE_END: {
                Document.ActKey bk = new Document.ActKey(new Range(Integer.MIN_VALUE, act.range.end), Node.MIN_NODE);
                Document.ActKey ek = new Document.ActKey(new Range(Integer.MAX_VALUE, act.range.end), Node.MAX_NODE);

                return act.doc.activationsByRangeEnd.subMap(bk, true, ek, true).values();
            }
            default:
                throw new RuntimeException("Unsupported Sort Group.");
        }
    }
}
