package network.aika.neuron.activation.scopes;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.direction.Direction;

public class Transition implements Comparable<Transition> {

    private Class<? extends Synapse> type;
    private Direction dir;
    private Scope input;
    private Scope output;


    private Transition(Class<? extends Synapse> type, Direction dir, Scope input, Scope output) {
        this.type = type;
        this.dir = dir;
        this.input = input;
        this.output = output;
    }

    public static void add(Class<? extends Synapse> type, Direction dir, Scope input, Scope output) {
        Transition t = new Transition(type, dir, input, output);

        input.outputs.add(t);
        output.inputs.add(t);
    }


    public boolean check(Direction dir, Direction startDir, boolean checkFinalRequirement) {
        return false;
    }

    public Class<? extends Synapse> getType() {
        return type;
    }

    public Scope getInput() {
        return input;
    }

    public Scope getOutput() {
        return output;
    }

    public Direction getDir() {
        return dir;
    }

    @Override
    public int compareTo(Transition t) {
        return 0;
    }
}
