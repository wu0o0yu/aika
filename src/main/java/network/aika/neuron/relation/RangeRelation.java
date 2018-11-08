package network.aika.neuron.relation;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.range.Position;

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
    public static final int RELATION_TYPE = 0;

    public int fromSlot;
    public int toSlot;

    public Position.Operator relation;

    RangeRelation() {}

    public RangeRelation(int fromSlot, int toSlot, Position.Operator relation) {
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.relation = relation;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct) {
        return relation.compare(act.getSlot(fromSlot), linkedAct.getSlot(toSlot));
    }


    public String toString() {
        return "RR(" + fromSlot + "," + toSlot + "," + relation + ")";
    }


    @Override
    public Relation invert() {
        return new RangeRelation(toSlot, fromSlot, relation.invert());
    }


    @Override
    public Range mapRange(Activation act, Linker.Direction direction) {
        Range.Relation rel = relation;

        Range r = act.range;
        Position begin = null;
        Position end = null;
        if(rel.beginToBegin == EQUALS) {
            begin = r.begin;
        } else if(rel.beginToEnd == EQUALS) {
            begin = r.end;
        }
        if(rel.endToEnd == EQUALS) {
            end = r.end;
        } else if(rel.endToBegin == EQUALS) {
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
        return RELATION_TYPE;
    }

    @Override
    public int compareTo(Relation rel) {
        RangeRelation rr = (RangeRelation) rel;

        return relation.compareTo(rr.relation);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(getRelationType());

        out.writeInt(fromSlot);
        out.writeInt(toSlot);

        out.writeByte(relation.getId());
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {

        fromSlot = in.readInt();
        toSlot = in.readInt();

        relation = Position.Operator.getById(in.readByte());
    }

    public static RangeRelation read(DataInput in, Model m) throws IOException {
        RangeRelation rr = new RangeRelation();
        rr.readFields(in, m);
        return rr;
    }

    @Override
    public boolean isExact() {
        return relation == EQUALS;
    }


    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);

        if(th == null || th.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Position pos = linkedAct.getSlot(toSlot);

        Collection<Activation> results;
        if(relation == EQUALS) {
            results = th.getActivations(
                    fromSlot, pos, true,
                    fromSlot, pos, true
            );
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
