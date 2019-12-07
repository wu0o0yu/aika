package network.aika.neuron.inhibitory;

import network.aika.Document;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.TSynapse;


public class MetaInhibSynapse extends TSynapse {

    public static final String TYPE_STR = "MI";

    public MetaInhibSynapse() {
        super();
    }

    public MetaInhibSynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }

    @Override
    public String getType() {
        return TYPE_STR;
    }

    @Override
    public boolean storeOnInputSide() {
        return false;
    }

    @Override
    public boolean storeOOutputSide() {
        return true;
    }


    public InhibitorySynapse transferMetaSynapse(Document doc, TNeuron<?, ?> inputNeuron) {
        InhibitoryNeuron inhibNeuron = (InhibitoryNeuron) getOutput().get(doc);
        InhibitorySynapse targetSynapse = create(doc, inputNeuron.getProvider(), inhibNeuron);

        targetSynapse.updateDelta(
                doc,
                getWeight()
        );

        System.out.println("  Transfer Template Synapse: IN:" +
                inputNeuron.getLabel() +
                " OUT:" + inhibNeuron.getLabel() +
                " M-SynId:" + getId() +
                " T-SynId:" + targetSynapse.getId() +
                " W:" + targetSynapse.getNewWeight()
        );

        transferMetaRelations(this, targetSynapse);

        return targetSynapse;
    }


    private void transferMetaRelations(MetaInhibSynapse metaInhibSynapse, InhibitorySynapse targetSynapse) {
        // TODO
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
