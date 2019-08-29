package network.aika.training;

import network.aika.Document;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Linker;
import network.aika.training.meta.MetaSynapse;

import static network.aika.neuron.INeuron.Type.INHIBITORY;


public class MetaLinker extends Linker {

    public MetaLinker(Document doc) {
        super(doc);
    }


    @Override
    public Activation computeInputActivation(Synapse s, Activation iAct) {
        if(iAct.getType() == INHIBITORY && s instanceof MetaSynapse) {
            MetaSynapse ms = (MetaSynapse) s;
            if(ms.isMetaVariable) {
                Activation act = iAct.getInputLinks()
                        .map(l -> l.getInput())
                        .findAny()
                        .orElse(null);

                return act != null ? computeInputActivation(s, act) : null;
            }
        }
        return iAct;
    }
}
