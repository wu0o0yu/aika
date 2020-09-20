package network.aika.neuron.activation;

public class Context {
    public Activation origin;

    public Direction downUpDir;
    public Direction startDir;

    public boolean related;
    public boolean input;
    public boolean selfRef;

    public int sameDirSteps;

    public Context(Context c, boolean incr) {
        this.origin = c.origin;
        this.downUpDir = c.downUpDir;
        this.startDir = c.startDir;
        this.related = c.related;
        this.input = c.input;
        this.selfRef = c.selfRef;
        this.sameDirSteps = incr ? c.sameDirSteps++ : c.sameDirSteps;
    }

    public Context(Activation origin, Direction startDir) {
        this.origin = origin;
        this.startDir = startDir;
        this.selfRef = true;
        this.sameDirSteps = 0;
    }
}
