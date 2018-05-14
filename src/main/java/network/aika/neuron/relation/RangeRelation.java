package network.aika.neuron.relation;

import network.aika.Document;
import network.aika.Model;
import network.aika.lattice.Node;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Range;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static network.aika.neuron.activation.Range.Operator.EQUALS;


public class RangeRelation extends Relation {
    public Range.Relation relation;

    RangeRelation() {}

    public RangeRelation(Range.Relation relation) {
        this.relation = relation;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct) {
        return relation.compare(act.range, linkedAct.range);
    }

    public String toString() {
        return "RR(" + relation + ")";
    }

    @Override
    public Relation invert() {
        return new RangeRelation(relation.invert());
    }


    @Override
    public int compareTo(Relation rel) {
        if(rel instanceof InstanceRelation) return -1;
        RangeRelation rr = (RangeRelation) rel;

        return relation.compareTo(rr.relation);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeBoolean(false);
        relation.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        relation = Range.Relation.read(in, m);
    }

    public static RangeRelation read(DataInput in, Model m) throws IOException {
        RangeRelation rr = new RangeRelation();
        rr.readFields(in, m);
        return rr;
    }

    @Override
    public boolean isExact() {
        return relation.beginToBegin == Range.Operator.EQUALS ||
                relation.beginToEnd == Range.Operator.EQUALS ||
                relation.endToBegin == Range.Operator.EQUALS ||
                relation.endToEnd == Range.Operator.EQUALS;
    }


    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);

        if(th == null || th.activations.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Range r = linkedAct.range;

        Collection<Activation> results;
        if(isExact()) {
            results =getActivationsByRangeEquals(th, r, relation);
        } else if (((relation.beginToBegin.isGreaterThanOrGreaterThanEqual() || relation.beginToEnd.isGreaterThanOrGreaterThanEqual())) && r.begin <= r.end) {
            results = getActivationsByRangeBeginGreaterThan(th, r, relation);
        } else if (((relation.endToEnd.isGreaterThanOrGreaterThanEqual() || relation.endToBegin.isGreaterThanOrGreaterThanEqual())) && r.begin >= r.end) {
            results = getActivationsByRangeEndGreaterThan(th, r, relation);
        } else if ((relation.beginToBegin.isLessThanOrLessThanEqual() || relation.beginToEnd.isLessThanOrLessThanEqual()) && r.begin <= r.end) {
            results = getActivationsByRangeBeginLessThanEqual(th, r, relation);
        } else if ((relation.endToEnd.isLessThanOrLessThanEqual() || relation.endToBegin.isLessThanOrLessThanEqual()) && r.begin >= r.end) {
            results = getActivationsByRangeEndLessThanEqual(th, r, relation);
        } else {
            results = th.activations.values();
        }

        return results.stream().filter(act -> test(act, linkedAct)).collect(Collectors.toList());
    }


    private static Collection<Activation> getActivationsByRangeBeginGreaterThan(INeuron.ThreadState th, Range r, Range.Relation rr) {
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
                new INeuron.ActKey(new Range(fromKey, Integer.MIN_VALUE), Integer.MIN_VALUE),
                fromInclusive,
                new INeuron.ActKey(new Range(toKey, Integer.MAX_VALUE), Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    private static Collection<Activation> getActivationsByRangeEndGreaterThan(INeuron.ThreadState th, Range r, Range.Relation rr) {
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
                new INeuron.ActKey(new Range(Integer.MIN_VALUE, fromKey), Integer.MIN_VALUE),
                fromInclusive,
                new INeuron.ActKey(new Range(Integer.MAX_VALUE, toKey), Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    private static Collection<Activation> getActivationsByRangeBeginLessThanEqual(INeuron.ThreadState th, Range r, Range.Relation rr) {
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
                new INeuron.ActKey(new Range(fromKey, Integer.MIN_VALUE), Integer.MIN_VALUE),
                fromInclusive,
                new INeuron.ActKey(new Range(toKey, Integer.MAX_VALUE), Integer.MAX_VALUE),
                toInclusive
        ).values();
    }


    private static Collection<Activation> getActivationsByRangeEndLessThanEqual(INeuron.ThreadState th, Range r, Range.Relation rr) {
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
                new INeuron.ActKey(new Range(Integer.MIN_VALUE, fromKey), Integer.MIN_VALUE),
                fromInclusive,
                new INeuron.ActKey(new Range(Integer.MAX_VALUE, toKey), Integer.MAX_VALUE),
                toInclusive
        ).values();
    }



    public static Collection<Activation> getActivationsByRangeEquals(INeuron.ThreadState th, Range r, Range.Relation rr) {
        if(rr.beginToBegin == EQUALS || rr.beginToEnd == EQUALS) {
            int key = rr.beginToBegin == EQUALS ? r.begin : r.end;
            return th.activations.subMap(
                    new INeuron.ActKey(new Range(key, Integer.MIN_VALUE), Integer.MIN_VALUE),
                    true,
                    new INeuron.ActKey(new Range(key, Integer.MAX_VALUE), Integer.MAX_VALUE),
                    true
            ).values();
        } else if(rr.endToEnd == EQUALS || rr.endToBegin == EQUALS) {
            int key = rr.endToEnd == EQUALS ? r.end : r.begin;
            return th.activationsEnd.subMap(
                    new INeuron.ActKey(new Range(Integer.MIN_VALUE, key), Integer.MIN_VALUE),
                    true,
                    new INeuron.ActKey(new Range(Integer.MAX_VALUE, key), Integer.MAX_VALUE),
                    true
            ).values();
        }
        throw new RuntimeException("Invalid Range Relation");
    }
}
