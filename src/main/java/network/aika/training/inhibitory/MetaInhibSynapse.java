package network.aika.training.inhibitory;

import network.aika.Document;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.TDocument;
import network.aika.training.TNeuron;
import network.aika.training.TSynapse;


public class MetaInhibSynapse extends TSynapse {


    public MetaInhibSynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }


    public InhibitorySynapse transferTemplateSynapse(Document doc, TNeuron inputNeuron) {
        InhibitoryNeuron inhibNeuron = (InhibitoryNeuron) getOutput().get(doc);
        InhibitorySynapse targetSynapse = create(doc, inputNeuron.getProvider(), inhibNeuron);

        targetSynapse.setRecurrent(isRecurrent());
        targetSynapse.setIdentity(isIdentity());

        if(targetSynapse.applied) {
            return targetSynapse;
        }

        targetSynapse.updateDelta(
                doc,
                getWeight(),
                getLimit()
        );

        System.out.println("  Transfer Template Synapse: IN:" +
                inputNeuron.getLabel() +
                " OUT:" + inhibNeuron.getLabel() +
                " M-SynId:" + getId() +
                " T-SynId:" + targetSynapse.getId() +
                " W:" + targetSynapse.getNewWeight() +
                " Rec:" + targetSynapse.isRecurrent() +
                " Ident:"  + targetSynapse.isIdentity()
        );

        targetSynapse.applied = true;

        TDocument.transferOutputMetaRelations(this, targetSynapse, null, null);

        return targetSynapse;
    }



    public static InhibitorySynapse create(Document doc, Neuron inputNeuron, InhibitoryNeuron outputNeuron) {
        inputNeuron.get(doc);
        InhibitorySynapse synapse = new InhibitorySynapse(inputNeuron, outputNeuron.getProvider(), outputNeuron.getNewSynapseId());
        synapse.link();

        return synapse;
    }


    public static class Builder extends Synapse.Builder {
        protected SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new MetaInhibSynapse(input, output, id);
        }
    }
}
