package network.aika.training.inhibitory;

import network.aika.ActivationFunction;
import network.aika.lattice.Converter;
import network.aika.neuron.INeuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.PositionRelation;
import network.aika.training.MetaModel;
import network.aika.training.TNeuron;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.meta.MetaNeuron;

import java.util.List;

public class InhibitoryNeuron extends TNeuron {


    public InhibitoryNeuron(MetaModel model, String label) {
        this(model, label, null);
    }

    public InhibitoryNeuron(MetaModel model, String label, String outputText) {
        super(model, label, outputText, INeuron.Type.INHIBITORY, ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT);
    }


    public static InhibitoryNeuron induceIncoming(MetaModel m, int threadId, List<ExcitatorySynapse> targetSyns) {
        // TODO: Prüfen, ob schon ein passendes inhibitorisches Neuron existiert.

        InhibitoryNeuron n = new InhibitoryNeuron(m, "");
        n.setBias(0.0);

        for(ExcitatorySynapse es: targetSyns) {
            int isSynId = n.getNewSynapseId();
            InhibitorySynapse is = new InhibitorySynapse(es.getInput(), n.getProvider(), isSynId);

            is.link();

            n.createInhibitoryRelations((TNeuron) es.getInput().get(), isSynId);

            is.update(null, 1.0, 1.0);
        }

        n.commit(n.getProvider().getActiveInputSynapses());
        Converter.convert(threadId, null, n, n.getProvider().getActiveInputSynapses());
        return n;
    }


    public static InhibitoryNeuron induceOutgoing(int threadId, MetaNeuron mn) {
        // TODO: Prüfen, ob schon ein passendes inhibitorisches Neuron existiert.

        InhibitoryNeuron n = new InhibitoryNeuron(mn.getModel(), "");
        n.setBias(0.0);

        int misSynId = n.getNewSynapseId();
        MetaInhibSynapse mis = new MetaInhibSynapse(mn.getProvider(), n.getProvider(), misSynId);
        mis.link();

        n.createInhibitoryRelations(mn, misSynId);

        mis.update(null, 1.0, 1.0);

        for(MetaNeuron.MappingLink ml: mn.targetNeurons.values()) {
            ExcitatoryNeuron targetNeuron = ml.targetNeuron;

            int isSynId = n.getNewSynapseId();
            InhibitorySynapse is = new InhibitorySynapse(targetNeuron.getProvider(), n.getProvider(), isSynId);

            is.link();

            n.createInhibitoryRelations(targetNeuron, isSynId);

            is.update(null, ml.nij, 1.0);
        }

        n.commit(n.getProvider().getActiveInputSynapses());
        Converter.convert(threadId, null, n, n.getProvider().getActiveInputSynapses());
        return n;
    }


    public void createInhibitoryRelations(TNeuron mn, Integer relSynId) {
        for(Integer slot: mn. getOutputSlots()) {
            PositionRelation rel = new PositionRelation.Equals();

            rel.fromSlot = slot;
            rel.toSlot = slot;

            rel.link(getProvider(), relSynId, Synapse.OUTPUT);
        }
    }


    public void train(int threadId, MetaNeuron mn) {
        for(Synapse s: getProvider().getActiveInputSynapses()) {
            if(s instanceof InhibitorySynapse) {
                InhibitorySynapse is = (InhibitorySynapse) s;
                ExcitatoryNeuron targetNeuron = (ExcitatoryNeuron) is.getInput().get();
                MetaNeuron.MappingLink ml = targetNeuron.metaNeurons.get(mn);

                is.update(null, ml.nij, 1.0);
            }
        }
        Converter.convert(threadId, null, this, getInputSynapses());
    }


    public boolean isMature() {
        return true;
    }


    public String typeToString() {
        return "INHIBITORY";
    }
}
