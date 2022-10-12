package network.aika.neuron.linking;

import network.aika.Thought;
import network.aika.direction.Direction;

public class Visitor {

    long v;
    Direction dir;


    public Visitor(Thought t, Direction dir) {
        this.v = t.getNewVisitorId();
        this.dir = dir;
    }


    private Visitor(long v, Direction dir) {
        this.v = v;
        this.dir = dir;
    }

    public Visitor next(Direction dir) {
        return new Visitor(v, dir);
    }

    public long getV() {
        return v;
    }

    public Direction getDir() {
        return dir;
    }

    public void setDir(Direction dir) {
        this.dir = dir;
    }
}
