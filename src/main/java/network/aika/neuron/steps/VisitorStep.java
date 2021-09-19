package network.aika.neuron.steps;

import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.visitor.tasks.VisitorTask;

import java.util.List;
import java.util.Set;

public abstract class VisitorStep<E extends Element, T extends VisitorTask> extends Step<E> {

    protected final T task;
    protected final List<Direction> directions;

    public VisitorStep(E element, T task, List<Direction> dirs) {
        super(element);
        this.task = task;
        this.directions = dirs;
    }

    @Override
    public String getStepName() {
        return super.getStepName() + ":" + directions;
    }
}
