package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static network.aika.neuron.activation.Position.Operator.*;
import static network.aika.neuron.activation.Position.Operator;


public class PositionRelation extends Relation {
    public static final int RELATION_TYPE = 0;

    public int fromSlot;
    public int toSlot;

    public Position.Operator relation;

    PositionRelation() {}

    public PositionRelation(int fromSlot, int toSlot, Position.Operator relation) {
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
        return new PositionRelation(toSlot, fromSlot, relation.invert());
    }


    @Override
    public void mapRange(Map<Integer, Position> slots, Activation act) {
        if(relation == Position.Operator.EQUALS) {
            slots.put(toSlot, act.getSlot(fromSlot));
        }
    }


    @Override
    public void linksOutputs(Set<Integer> results) {
        results.add(toSlot);
    }


    @Override
    public int getRelationType() {
        return RELATION_TYPE;
    }


    @Override
    public int compareTo(Relation rel) {
        PositionRelation pr = (PositionRelation) rel;

        int r = Integer.compare(fromSlot, pr.fromSlot);
        if(r != 0) return r;
        r = Integer.compare(toSlot, pr.toSlot);
        if(r != 0) return r;

        return relation.compareTo(pr.relation);
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

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


    public static PositionRelation read(DataInput in, Model m) throws IOException {
        PositionRelation rr = new PositionRelation();
        rr.readFields(in, m);
        return rr;
    }


    @Override
    public boolean isExact() {
        return relation == Operator.EQUALS;
    }


    @Override
    public Stream<Activation> getActivations(INeuron n, Activation linkedAct) {
        INeuron.ThreadState th = n.getThreadState(linkedAct.doc.threadId, false);

        if(th == null || th.isEmpty()) {
            return Stream.empty();
        }

        Position pos = linkedAct.getSlot(toSlot);

        Stream<Activation> results;
        if(relation == Operator.EQUALS) {
            results = th.getActivations(
                    fromSlot, pos, true,
                    fromSlot, pos, true
            );
        } else if(relation == LESS_THAN || relation == LESS_THAN_EQUAL) {
            results = th.getActivations(
                    fromSlot, Position.MIN, true,
                    fromSlot, pos, relation == LESS_THAN_EQUAL
            );
        } else if(relation == GREATER_THAN || relation == GREATER_THAN_EQUAL) {
            results = th.getActivations(
                    fromSlot, pos, relation == GREATER_THAN_EQUAL,
                    fromSlot, Position.MAX, true
            );
        } else {
            results = th.getActivations();
        }

        return results
                .filter(act -> test(act, linkedAct));
    }


    @Override
    public void registerRequiredSlots(Neuron input) {
        input.get().slotRequired.add(fromSlot);
    }
}
