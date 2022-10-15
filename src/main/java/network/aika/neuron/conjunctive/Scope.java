package network.aika.neuron.conjunctive;

import network.aika.direction.Direction;


public enum Scope {
    INPUT(Direction.OUTPUT),
    SAME(Direction.INPUT);

    Direction relationDir;

    Scope(Direction relationDir) {
        this.relationDir = relationDir;
    }

    public boolean compatible(Scope relScope) {
        return this != relScope || this == INPUT;
    }

    public Direction getRelationDir() {
        return relationDir;
    }
}
