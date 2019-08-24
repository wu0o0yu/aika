package network.aika.training;

import network.aika.neuron.activation.Activation;

import java.util.Comparator;

import static network.aika.neuron.Synapse.State.NEXT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;


public class StatUtil {


    public static int getPosWithinDoc(Activation act, int slot) {
        switch (act.getType()) {
            case EXCITATORY:
            case INHIBITORY:
                if(slot == BEGIN) {
                    return act
                            .getInputLinks()
                            .filter(l -> !l.isInactive() && !l.getSynapse().isWeak(NEXT))
                            .map(l -> l.getInput().lookupSlot(slot).getFinalPosition())
                            .min(Comparator.comparingInt(p -> p))
                            .orElse(act.lookupSlot(slot).getFinalPosition());
                } else if(slot == END) {
                    return act
                            .getInputLinks()
                            .filter(l -> !l.isInactive() && !l.getSynapse().isWeak(NEXT))
                            .map(l -> l.getInput().lookupSlot(slot).getFinalPosition())
                            .max(Comparator.comparingInt(p -> p))
                            .orElse(act.lookupSlot(slot).getFinalPosition());
                }
                break;
            case INPUT:
                return act.lookupSlot(slot).getFinalPosition();
        }
        return 0;
    }


    public static int getCurrentPos(Activation act, int slot) {
        MetaModel metaModel = (MetaModel) act.getNeuron().getModel();
        return metaModel.charCounter + StatUtil.getPosWithinDoc(act, slot);
    }
}
