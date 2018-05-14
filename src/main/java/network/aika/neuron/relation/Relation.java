package network.aika.neuron.relation;


import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.Writable;

import java.io.DataInput;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;



public abstract class Relation implements Comparable<Relation>, Writable {


    public abstract boolean test(Activation act, Activation linkedAct);

    public abstract Relation invert();


    public static Relation read(DataInput in, Model m) throws IOException {
        if(in.readBoolean()) {
            return InstanceRelation.read(in, m);
        } else {
            return RangeRelation.read(in, m);
        }
    }

    public abstract boolean isExact();


    public abstract Collection<Activation> getActivations(INeuron n, Activation linkedAct);

}
