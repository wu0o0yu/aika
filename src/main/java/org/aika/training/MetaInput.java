package org.aika.training;


import org.aika.Input;
import org.aika.Neuron;
import org.aika.neuron.Synapse;

public class MetaInput extends Input {

    public double metaWeight;
    public double metaBias;
    public boolean metaRelativeRid;

    public MetaInput setMetaWeight(double metaWeight) {
        this.metaWeight = metaWeight;
        return this;
    }

    public MetaInput setMetaBias(double metaBias) {
        this.metaBias = metaBias;
        return this;
    }

    public MetaInput setMetaRelativeRid(boolean metaRelativeRid) {
        this.metaRelativeRid = metaRelativeRid;
        return this;
    }


    protected Synapse getSynapse(Neuron outputNeuron) {
        Synapse s = super.getSynapse(outputNeuron);

        MetaSynapse ss = new MetaSynapse();
        ss.metaWeight = metaWeight;
        ss.metaBias = metaBias;
        ss.metaRelativeRid = metaRelativeRid;
        s.meta = ss;
        return s;
    }
}
