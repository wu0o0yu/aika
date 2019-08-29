package network.aika.training.excitatory;

import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.MetaModel;
import network.aika.training.TSynapse;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.meta.MetaSynapse;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ExcitatorySynapse extends TSynapse {

    public static final Comparator<Synapse> META_SYNAPSE_COMP = Comparator
            .comparing(Synapse::getInput)
            .thenComparing(Synapse::getOutput);


    public Map<MetaSynapse, MetaSynapse.MappingLink> metaSynapses = new TreeMap<>(META_SYNAPSE_COMP);


    public ExcitatorySynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }

    public ExcitatorySynapse(Neuron input, Neuron output, Integer id, int lastCount) {
        super(input, output, id, lastCount);
    }


    public MetaSynapse.MappingLink getMetaSynapse(Neuron in, Neuron out) {
        return metaSynapses.get(new MetaSynapse(in, out, -1, 0));
    }


    public double getUncovered() {
        double max = 0.0;
        for(Map.Entry<MetaSynapse, MetaSynapse.MappingLink> me: metaSynapses.entrySet()) {
            MetaSynapse.MappingLink sml = me.getValue();
            MetaSynapse ms = sml.metaSynapse;
            MetaNeuron mn = (MetaNeuron) ms.getOutput().get();
            ExcitatoryNeuron tn = (ExcitatoryNeuron) getOutput().get();
            MetaNeuron.MappingLink nml = mn.targetNeurons.get(tn);

            max = Math.max(max, nml.nij * ms.getCoverage());
        }

        return 1.0 - max;
    }



    public boolean isMappedToMetaSynapse(MetaSynapse metaSyn) {
        MetaSynapse.MappingLink ml = metaSynapses.get(metaSyn.getOutput().get());
        return ml.metaSynapse.getId() == metaSyn.getId();
    }


    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
    }


    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            ExcitatorySynapse s = (ExcitatorySynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new ExcitatorySynapse(input, output, id, ((MetaModel) output.getModel()).charCounter);
        }
    }

}
