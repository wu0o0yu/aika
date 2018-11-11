package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class MultiRelation extends Relation {

    @Override
    public int getRelationType() {
        return 0;
    }

    @Override
    public boolean test(Activation act, Activation linkedAct) {
        return false;
    }

    @Override
    public Relation invert() {
        return null;
    }

    @Override
    public void mapRange(Map<Integer, Position> slots, Activation act) {

    }

    @Override
    public void linksOutputs(Set<Integer> results) {
    }


    @Override
    public boolean isExact() {
        return false;
    }

    @Override
    public Collection<Activation> getActivations(INeuron n, Activation linkedAct) {
        return null;
    }

    @Override
    public void registerRequiredSlots(Neuron input) {

    }

    @Override
    public int compareTo(Relation rel) {
        return 0;
    }

    @Override
    public void write(DataOutput out) throws IOException {

    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {

    }
}
