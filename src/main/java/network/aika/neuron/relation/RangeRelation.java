package network.aika.neuron.relation;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.range.Position;
import network.aika.neuron.range.Range;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static network.aika.neuron.range.Position.Operator.EQUALS;
import static network.aika.neuron.range.Position.Operator.GREATER_THAN_EQUAL;
import static network.aika.neuron.range.Position.Operator.LESS_THAN_EQUAL;


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
    public Range mapRange(Activation act, Linker.Direction direction) {
        Range.Relation rel = relation;
        if(direction == Linker.Direction.INPUT) {
            rel = rel.invert();
        }

        Range r = act.range;
        Position begin = null;
        Position end = null;
        if(rel.beginToBegin == EQUALS) {
            begin = r.begin;
        } else if(rel.endToBegin == EQUALS) {
            begin = r.end;
        }
        if(rel.endToEnd == EQUALS) {
            end = r.end;
        } else if(rel.beginToEnd == EQUALS) {
            end = r.begin;
        }

        return new Range(begin, end);
    }


    @Override
    public boolean linksOutputBegin() {
        return relation.beginToBegin == EQUALS || relation.endToBegin == EQUALS;
    }


    @Override
    public boolean linksOutputEnd() {
        return relation.endToEnd == EQUALS || relation.beginToEnd == EQUALS;
    }


    @Override
    public int getRelationType() {
        return 0;
    }

    @Override
    public int compareTo(Relation rel) {
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
        return relation.beginToBegin == EQUALS ||
                relation.beginToEnd == EQUALS ||
                relation.endToBegin == EQUALS ||
                relation.endToEnd == EQUALS;
    }


    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);

        if(th == null || th.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Range r = linkedAct.range;

        Collection<Activation> results;
        if(isExact()) {
            results = getActivationsByRangeEquals(th, r, relation);
        } else if (((relation.beginToBegin.isGreaterThanOrGreaterThanEqual() || relation.beginToEnd.isGreaterThanOrGreaterThanEqual())) && r.begin.compare(LESS_THAN_EQUAL, r.end)) {
            results = getActivationsByRangeBeginGreaterThan(th, r, relation);
        } else if (((relation.endToEnd.isGreaterThanOrGreaterThanEqual() || relation.endToBegin.isGreaterThanOrGreaterThanEqual())) && r.begin.compare(GREATER_THAN_EQUAL, r.end)) {
            results = getActivationsByRangeEndGreaterThan(th, r, relation);
        } else if ((relation.beginToBegin.isLessThanOrLessThanEqual() || relation.beginToEnd.isLessThanOrLessThanEqual()) && r.begin.compare(LESS_THAN_EQUAL, r.end)) {
            results = getActivationsByRangeBeginLessThanEqual(th, r, relation);
        } else if ((relation.endToEnd.isLessThanOrLessThanEqual() || relation.endToBegin.isLessThanOrLessThanEqual()) && r.begin.compare(GREATER_THAN_EQUAL, r.end)) {
            results = getActivationsByRangeEndLessThanEqual(th, r, relation);
        } else {
            results = th.getActivations();
        }

        return results.stream().filter(act -> test(act, linkedAct)).collect(Collectors.toList());
    }


    private static Collection<Activation> getActivationsByRangeBeginGreaterThan(INeuron.ThreadState th, Range r, Range.Relation rr) {
        Position fromKey;
        boolean fromInclusive;

        if(rr.beginToBegin.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.begin;
            fromInclusive = rr.beginToBegin.includesEqual();
        } else {
            fromKey = r.end;
            fromInclusive = rr.beginToEnd.includesEqual();
        }

        Position toKey;
        boolean toInclusive;
        if(rr.endToEnd.isLessThanOrLessThanEqual()) {
            toKey = r.end;
            toInclusive = rr.endToEnd.includesEqual();
        } else if(rr.endToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.endToBegin.includesEqual();
        } else {
            toKey = Position.MAX;
            toInclusive = true;
        }

        return th.getActivationsByRangeBegin(
                fromKey, fromInclusive,
                toKey, toInclusive
        );
    }


    private static Collection<Activation> getActivationsByRangeEndGreaterThan(INeuron.ThreadState th, Range r, Range.Relation rr) {
        Position fromKey;
        boolean fromInclusive;

        if(rr.endToEnd.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.end;
            fromInclusive = rr.endToEnd.includesEqual();
        } else {
            fromKey = r.begin;
            fromInclusive = rr.endToBegin.includesEqual();
        }

        Position toKey;
        boolean toInclusive;
        if(rr.beginToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.beginToBegin.includesEqual();
        } else if(rr.beginToEnd.isLessThanOrLessThanEqual()) {
            toKey = r.end;
            toInclusive = rr.beginToEnd.includesEqual();
        } else {
            toKey = Position.MAX;
            toInclusive = true;
        }

        return th.getActivationsByRangeEnd(
                fromKey, fromInclusive,
                toKey, toInclusive
        );
    }


    private static Collection<Activation> getActivationsByRangeBeginLessThanEqual(INeuron.ThreadState th, Range r, Range.Relation rr) {
        Position fromKey;
        boolean fromInclusive;
        if(rr.endToEnd.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.end;
            fromInclusive = rr.endToEnd.includesEqual();
        } else if(rr.endToBegin.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.begin;
            fromInclusive = rr.endToBegin.includesEqual();
        } else {
            fromKey = Position.MIN;
            fromInclusive = true;
        }

        Position toKey;
        boolean toInclusive;

        if(rr.beginToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.beginToBegin.includesEqual();
        } else {
            toKey = r.end;
            toInclusive = rr.beginToEnd.includesEqual();
        }

        return th.getActivationsByRangeBeginLimited(
                fromKey, fromInclusive,
                toKey, toInclusive
        );
    }


    private static Collection<Activation> getActivationsByRangeEndLessThanEqual(INeuron.ThreadState th, Range r, Range.Relation rr) {
        Position fromKey;
        boolean fromInclusive;
        if(rr.beginToEnd.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.end;
            fromInclusive = rr.beginToEnd.includesEqual();
        } else if(rr.beginToBegin.isGreaterThanOrGreaterThanEqual()) {
            fromKey = r.begin;
            fromInclusive = rr.beginToBegin.includesEqual();
        } else {
            fromKey = Position.MIN;
            fromInclusive = true;
        }

        Position toKey;
        boolean toInclusive;

        if(rr.endToBegin.isLessThanOrLessThanEqual()) {
            toKey = r.begin;
            toInclusive = rr.endToBegin.includesEqual();
        } else {
            toKey = r.end;
            toInclusive = rr.endToEnd.includesEqual();
        }

        return th.getActivationsByRangeEndLimited(
                fromKey, fromInclusive,
                toKey, toInclusive
        );
    }



    public static Collection<Activation> getActivationsByRangeEquals(INeuron.ThreadState th, Range r, Range.Relation rr) {
        if(rr.beginToBegin == EQUALS || rr.beginToEnd == EQUALS) {
            Position key = rr.beginToBegin == EQUALS ? r.begin : r.end;
            return th.getActivationsByRangeBegin(
                    key, true,
                    key, true
            );
        } else if(rr.endToEnd == EQUALS || rr.endToBegin == EQUALS) {
            Position key = rr.endToEnd == EQUALS ? r.end : r.begin;

            return th.getActivationsByRangeEnd(
                    key, true,
                    key, true
            );
        }
        throw new RuntimeException("Invalid Range Relation");
    }


    public static Collection<Activation> getActivationsByRangeEquals(Document doc, Range r, Range.Relation rr) {
        if(rr.beginToBegin == EQUALS || rr.beginToEnd == EQUALS) {
            Position key = rr.beginToBegin == EQUALS ? r.begin : r.end;
            return doc.getActivationsByRangeBegin(
                    new Range(key, Position.MIN), true,
                    new Range(key, Position.MAX), true
            );
        } else if(rr.endToEnd == EQUALS || rr.endToBegin == EQUALS) {
            Position key = rr.endToEnd == EQUALS ? r.end : r.begin;
            return doc.getActivationByRangeEnd(
                    new Range(Position.MIN, key), true,
                    new Range(Position.MAX, key), true
            );
        }
        throw new RuntimeException("Invalid Range Relation");
    }
}
