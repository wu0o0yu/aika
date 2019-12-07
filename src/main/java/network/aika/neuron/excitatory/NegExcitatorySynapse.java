package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.search.Option;
import network.aika.neuron.Sign;
import network.aika.neuron.TNeuron;


public class NegExcitatorySynapse extends ExcitatorySynapse {

    public static final String TYPE_STR = "NE";


    public NegExcitatorySynapse() {
        super();
    }

    public NegExcitatorySynapse(Neuron input, Neuron output, Integer id) {
        super(input, output, id);
    }

    public NegExcitatorySynapse(Neuron input, Neuron output, Integer id, int lastCount) {
        super(input, output, id, lastCount);
    }


    @Override
    public String getType() {
        return TYPE_STR;
    }


    public void updateCountValue(Option io, Option oo) {
        double inputValue = io != null ? io.getBounds().lb.value : 0.0;
        double outputValue = oo != null ? oo.getBounds().lb.value : 0.0;

        if(!needsCountUpdate) {
            return;
        }
        needsCountUpdate = false;

        double optionProp = (io != null ? io.getP() : 1.0) * (oo != null ? oo.getP() : 1.0);

        if(TNeuron.checkSelfReferencing(oo, io)) {
            countValueIPosOPos += (Sign.POS.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        } else {
            countValueINegOPos += (Sign.NEG.getX(inputValue) * Sign.POS.getX(outputValue) * optionProp);
        }
        countValueIPosONeg += (Sign.POS.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);
        countValueINegONeg += (Sign.NEG.getX(inputValue) * Sign.NEG.getX(outputValue) * optionProp);

        needsFrequencyUpdate = true;
    }


    public static class Builder extends Synapse.Builder {

        public Synapse getSynapse(Neuron outputNeuron) {
            NegExcitatorySynapse s = (NegExcitatorySynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected Synapse.SynapseFactory getSynapseFactory() {
            return (input, output, id) -> new NegExcitatorySynapse(input, output, id, output.getModel().charCounter);
        }
    }
}
