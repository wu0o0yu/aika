package network.aika.neuron.inhibitory;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.InhibitoryActivation;
import network.aika.neuron.activation.search.Option;
import network.aika.Config;
import network.aika.neuron.TNeuron;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.meta.MetaNeuron;

import java.util.List;
import java.util.function.Function;

public class InhibitoryNeuron extends TNeuron<InhibitoryActivation> {


    public InhibitoryNeuron(Model model, String label) {
        super(model, label);
    }


    public double getTotalBias(Synapse.State state) {
        return getBias(state);
    }


    protected Activation createActivation(Document doc) {
        return new InhibitoryActivation(doc, this);
    }


    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }


    public static InhibitoryNeuron induceIncoming(Model m, int threadId, List<ExcitatorySynapse> targetSyns) {
        // TODO: Prüfen, ob schon ein passendes inhibitorisches Neuron existiert.

        InhibitoryNeuron n = new InhibitoryNeuron(m, "");
        n.setBias(0.0);

        for(ExcitatorySynapse es: targetSyns) {
            int isSynId = n.getNewSynapseId();
            InhibitorySynapse is = new InhibitorySynapse(es.getInput(), n.getProvider(), isSynId);

            is.link();

            is.update(null, 1.0, 1.0);
        }

        n.commit(n.getProvider().getActiveInputSynapses());
        return n;
    }


    public static InhibitoryNeuron induceOutgoing(int threadId, MetaNeuron mn) {
        // TODO: Prüfen, ob schon ein passendes inhibitorisches Neuron existiert.

        InhibitoryNeuron n = new InhibitoryNeuron(mn.getModel(), "");
        n.setBias(0.0);

        int misSynId = n.getNewSynapseId();
        MetaInhibSynapse mis = new MetaInhibSynapse(mn.getProvider(), n.getProvider(), misSynId);
        mis.link();

        mis.update(null, 1.0, 1.0);

        for(MetaNeuron.MappingLink ml: mn.targetNeurons.values()) {
            ExcitatoryNeuron targetNeuron = ml.targetNeuron;

            int isSynId = n.getNewSynapseId();
            InhibitorySynapse is = new InhibitorySynapse(targetNeuron.getProvider(), n.getProvider(), isSynId);

            is.link();

            is.update(null, ml.nij, 1.0);
        }

        n.commit(n.getProvider().getActiveInputSynapses());
        return n;
    }



    public void prepareMetaTraining(Config c, Option o, Function<Activation, ExcitatoryNeuron> callback) {
        // Nothing to do.
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
    }


    public boolean isMature(Config c) {
        return true;
    }


    public String typeToString() {
        return "INHIBITORY";
    }
}
