package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Linker;
import network.aika.neuron.range.Range;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.Synapse.VARIABLE;
import static network.aika.neuron.activation.Linker.Direction.INPUT;


public class InputRelation extends Relation {
    public static final int RELATION_TYPE = 2;


    Relation relation;

    int fromInput;
    int toInput;


    public InputRelation() {
    }


    public InputRelation(Relation relation, int fromInput, int toInput) {
        this.relation = relation;
        this.fromInput = fromInput;
        this.toInput = toInput;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct) {
        for(Activation iAct: followLinks(fromInput, act, INPUT)) {
            for(Activation linkedIAct: followLinks(toInput, linkedAct, INPUT)) {
                return relation.test(iAct, linkedIAct);
            }
        }
        return false;
    }


    private Collection<Activation> followLinks(int inputSynId, Activation act, Linker.Direction dir) {
        if(inputSynId == OUTPUT) {
            return Collections.singleton(act);
        } else {
            Stream<Activation.Link> s = dir == INPUT ?
                    act.getInputLinks(false, false) :
                    act.getOutputLinks(false);
            if(inputSynId != VARIABLE) {
                s = s.filter(l -> l.synapse.id == inputSynId);
            }

            return s.map(l -> l.input)
                    .collect(Collectors.toList());
        }
    }


    @Override
    public Relation invert() {
        InputRelation inverted = new InputRelation();

        inverted.fromInput = toInput;
        inverted.toInput = fromInput;
        inverted.relation = relation.invert();

        return inverted;
    }


    @Override
    public Range mapRange(Activation act, Linker.Direction direction) {
        return null; //relation.mapRange(act, direction);
    }


    @Override
    public boolean linksOutputBegin() {
        return relation.linksOutputBegin();
    }


    @Override
    public boolean linksOutputEnd() {
        return relation.linksOutputEnd();
    }


    @Override
    public boolean isExact() {
        return relation.isExact();
    }


    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        Collection<Activation> results = new ArrayList<>();
        for(Activation linkedIAct: followLinks(toInput, linkedAct, INPUT)) {
            for(Activation iAct: relation.getActivations(n, linkedIAct)) {
                for(Activation act: followLinks(fromInput, iAct, Linker.Direction.OUTPUT)) {
                    if(act.getINeuron() == n) {
                        results.add(act);
                    }
                }
            }
        }
        return results;
    }


    public String toString() {
        return "IR[" + fromInput + "," + toInput + "](" + relation + ")";
    }


    @Override
    public int getRelationType() {
        return RELATION_TYPE;
    }


    @Override
    public int compareTo(Relation rel) {
        InputRelation ir = (InputRelation) rel;

        int r = Integer.compare(fromInput, ir.fromInput);
        if(r != 0) return r;

        r = Integer.compare(toInput, ir.toInput);
        if(r != 0) return r;

        return relation.compareTo(ir.relation);
    }


    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(getRelationType());

        out.writeInt(fromInput);
        out.writeInt(toInput);
        relation.write(out);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        fromInput = in.readInt();
        toInput = in.readInt();
        relation = Relation.read(in, m);
    }


    public static InputRelation read(DataInput in, Model m) throws IOException {
        InputRelation ir = new InputRelation();
        ir.readFields(in, m);
        return ir;
    }

}
