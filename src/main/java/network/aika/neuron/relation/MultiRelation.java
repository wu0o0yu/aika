package network.aika.neuron.relation;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Position;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;


public class MultiRelation extends Relation {
    public static final int RELATION_TYPE = 2;

    private List<Relation> relations;


    public MultiRelation(List<Relation> rels) {
        relations = rels;
    }


    @Override
    public int getRelationType() {
        return RELATION_TYPE;
    }


    @Override
    public boolean test(Activation act, Activation linkedAct) {
        for(Relation rel: relations) {
            if(!rel.test(act, linkedAct)) {
                return false;
            }
        }

        return true;
    }


    @Override
    public Relation invert() {
        List<Relation> invRels = new ArrayList<>();
        for(Relation rel: relations) {
            invRels.add(rel.invert());
        }
        return new MultiRelation(invRels);
    }


    @Override
    public void mapRange(Map<Integer, Position> slots, Activation act) {
        for(Relation rel: relations) {
            rel.mapRange(slots, act);
        }
    }


    @Override
    public void linksOutputs(Set<Integer> results) {
        for(Relation rel: relations) {
            rel.linksOutputs(results);
        }
    }


    @Override
    public boolean isExact() {
        for(Relation rel: relations) {
            if(rel.isExact()) {
                return true;
            }
        }
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
