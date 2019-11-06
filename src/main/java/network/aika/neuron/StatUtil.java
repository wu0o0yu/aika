package network.aika.neuron;

import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Position;

import java.util.Comparator;

import static network.aika.neuron.Synapse.State.NEXT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;


public class StatUtil {


    public static Integer getPosWithinDoc(Activation act, int slot) {
        switch (act.getType()) {
            case EXCITATORY:
            case INHIBITORY:
                if(slot == BEGIN) {
                    return act
                            .getInputLinks()
                            .filter(l -> !l.isInactive() && !l.getSynapse().isWeak(NEXT) && l.getInput().getSlot(slot) != null)
                            .map(l -> l.getInput().getSlot(slot).getFinalPosition())
                            .min(Comparator.comparingInt(p -> p))
                            .orElse(getDefaultSlot(act, slot));
                } else if(slot == END) {
                    return act
                            .getInputLinks()
                            .filter(l -> !l.isInactive() && !l.getSynapse().isWeak(NEXT) && l.getInput().getSlot(slot) != null)
                            .map(l -> l.getInput().getSlot(slot).getFinalPosition())
                            .max(Comparator.comparingInt(p -> p))
                            .orElse(getDefaultSlot(act, slot));
                }
                break;
            case INPUT:
                return act.getSlot(slot).getFinalPosition();
        }
        return 0;
    }


    private static Integer getDefaultSlot(Activation act, int slot) {
        Position pos = act.getSlot(slot);
        return pos != null ? pos.getFinalPosition() : null;
    }


    public static int getCurrentPos(Activation act, int slot) {
        Model metaModel = act.getNeuron().getModel();
        return metaModel.charCounter + StatUtil.getPosWithinDoc(act, slot);
    }
}
