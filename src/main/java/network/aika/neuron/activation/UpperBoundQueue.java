package network.aika.neuron.activation;

import java.util.ArrayDeque;

import network.aika.neuron.activation.Activation.Link;


public class UpperBoundQueue {
    private final ArrayDeque<Activation> queue = new ArrayDeque<>();


    public void add(Link l) {
        if(!l.isRecurrent()) {
            add(l.getOutput());
        }
    }


    public void add(Activation act) {
        if(!act.ubQueued && act.getInputValue() == null) {
            act.ubQueued = true;
            queue.addLast(act);
        }
    }


    public boolean process() {
        boolean flag = false;
        while(!queue.isEmpty()) {
            flag = true;
            Activation act = queue.pollFirst();
            act.ubQueued = false;

            act.processBounds();
        }
        return flag;
    }
}