package network.aika.elements.synapses;

import network.aika.direction.Direction;


public enum Scope {
    INPUT(Direction.OUTPUT),
    SAME(Direction.INPUT);

    Direction relationDir;

    Scope(Direction relationDir) {
        this.relationDir = relationDir;
    }

    public Direction getRelationDir() {
        return relationDir;
    }
}
