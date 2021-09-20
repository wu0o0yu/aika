package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.linker.AbstractLinker;

import java.util.List;
import java.util.Map;

public abstract class LinkerStep<E extends Element, L extends AbstractLinker> extends Step<E> {

    protected final L task;
    protected final List<Direction> directions;
    protected final Map<PatternActivation, Byte> bindingSignals;

    public LinkerStep(E element, Map<PatternActivation, Byte> bindingSignals, L task, List<Direction> dirs) {
        super(element);
        this.bindingSignals = bindingSignals;
        this.task = task;
        this.directions = dirs;
    }

    @Override
    public String getStepName() {
        return super.getStepName() + ":" + directions;
    }
}
