package network.aika.neuron.steps;

import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.linker.AbstractLinker;

import java.util.List;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public abstract class LinkerStep<E extends Element, T extends AbstractLinker> extends TaskStep<E, T> {

    protected final BindingSignal bindingSignal;

    public LinkerStep(E element, BindingSignal bindingSignal, T task) {
        super(element, task);
        this.bindingSignal = bindingSignal;
    }

    protected List<Direction> getDirections() {
        if(getElement() instanceof InhibitoryActivation)
            return List.of(OUTPUT);

        return List.of(INPUT, OUTPUT);
    }
}
