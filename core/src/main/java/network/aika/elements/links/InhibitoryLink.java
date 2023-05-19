package network.aika.elements.links;

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.InhibitoryActivation;
import network.aika.elements.synapses.InhibitorySynapse;

import static network.aika.elements.activations.Activation.isSelfRef;


public class InhibitoryLink extends AbstractInhibitoryLink<InhibitorySynapse, BindingActivation> {

    public InhibitoryLink(InhibitorySynapse inhibitorySynapse, BindingActivation input, InhibitoryActivation output) {
        super(inhibitorySynapse, input, output);
    }

    @Override
    public void connectFields(NegativeFeedbackLink out) {
        if(isSelfRef(getInput(), out.getOutput()))
            return;

        super.connectFields(out);
    }
}
