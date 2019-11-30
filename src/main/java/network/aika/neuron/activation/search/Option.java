/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron.activation.search;

import network.aika.Utils;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.link.Direction;
import network.aika.neuron.activation.State;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.util.*;
import java.util.function.Consumer;

import static network.aika.Document.MAX_ROUND;
import static network.aika.neuron.activation.link.Direction.INPUT;
import static network.aika.neuron.activation.link.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class Option implements Comparable<Option> {

    private State state;

    public Activation act;
    public SearchNode searchNode;

    private Option parent;
    private Set<Option> children = new TreeSet<>();

    public Decision decision;

    private double weight;
    public double remainingWeight;
    public int cacheFactor = 1;
    public double p;

    public TreeMap<Link, Link> inputOptions = new TreeMap<>(INPUT_COMP);
    public Map<Link, Link> outputOptions = new TreeMap<>(OUTPUT_COMP); // TODO:

    private boolean isQueued;

    public int round;

    public ExcitatoryNeuron targetNeuron;


    public static Comparator<Link> INPUT_COMP = (l1, l2) -> {
        int r = l1.input.state.firedLatest.compareTo(l2.input.state.firedLatest);
        if (r != 0) return r;
        return Activation.INPUT_COMP.compare(l1.actLink, l2.actLink);
    };

    public static Comparator<Link> OUTPUT_COMP = (l1, l2) -> {
        int r = Synapse.OUTPUT_SYNAPSE_COMP.compare(l1.synapse, l2.synapse);
        if (r != 0) return r;
        return Integer.compare(l1.output.getId(), l2.output.getId());
    };

    public Option(Option parent, Activation act, SearchNode sn) {
        this.act = act;
        this.searchNode = sn;
        this.parent = parent;
        this.round = newSearchNode() ? 0 : parent.round + 1;

        if(round > MAX_ROUND) {
            throw new Activation.OscillatingActivationsException(act.getDocument().activationsToString());
        }

        if(parent != null) {
            parent.children.add(this);
        }

        decision = act.getNextDecision(parent, sn);

        link();
        state = computeValueAndWeight(sn);
    }


    public boolean newSearchNode() {
        return parent.searchNode != searchNode;
    }


    public boolean hasChanged() {
        return state != null && parent.state.equalsWithWeights(state);
    }


    public State getState() {
        return state;
    }


    public void setQueued(boolean v) {
        isQueued = v;
    }


    public boolean isQueued() {
        return isQueued;
    }


    public void restoreState(Activation.Mode m) {
        act.currentOption = (m == Activation.Mode.OLD ? parent : this);
    }


    public boolean compare(Option r) {
        return state.equalsWithWeights(r.state);
    }


    public boolean isActive() {
        return state.lb > 0.0;
    }


    private void link() {
        Synapse lastSynapse = null;
        Link maxInputState = null;
        for (Activation.Link al : act.inputLinks.values()) {
            if(al.isInactive()) {
                continue;
            }
            if (lastSynapse != null && lastSynapse != al.getSynapse()) {
                inputOptions.put(maxInputState, maxInputState);
                maxInputState = null;
            }

            Option inputOption = al.getInput().getInputState(al.getSynapse(), act, searchNode);
            if (maxInputState == null || maxInputState.input.state.lb < inputOption.state.lb) {
                maxInputState = new Link(al, inputOption, this);
            }
            lastSynapse = al.getSynapse();
        }
        if (maxInputState != null) {
            inputOptions.put(maxInputState, maxInputState);
        }
    }


    public void setWeight(double weight) {
        this.weight = weight;
    }


    public Map<Link, Link> getLinks(Direction dir) {
        switch(dir) {
            case INPUT:
                return inputOptions;
            case OUTPUT:
                return outputOptions;
        }
        return null;
    }


    private void addLink(Direction dir, Link l) {
        getLinks(dir.getInverted()).put(l, l); // TODO: Warum inverted?
    }


    public void setCacheFactor(int cf) {
        cacheFactor = cf;
    }


    public Activation getAct() {
        return act;
    }


    public void setP(double p) {
        this.p = p;
    }


    public double getP() {
        return getAct().getP(this);
    }


    public void computeRemainingWeight() {
        double sum = 0.0;
        for(Option c: children) {
            sum += c.weight;
        }

        remainingWeight = weight - sum;
    }


// Todo: remove duplicate code!
    public boolean checkSelfReferencing(Option o) {
        Option o1 = getAct().getInputExcitatoryOption(o);
        if(o1 == null) {
            return false;
        } else if(o == o1) {
            return true;
        }

        Fired f1 = o1.getState().firedLatest;
        Fired f2 = o.getState().firedLatest;
        if(f1 == null) {
            return false;
        } else if (f2 != null && f1.compareTo(f2) == 1) {
            return o1.getAct().checkSelfReferencingRecursiveStep(o.getAct(), 0);
        } else {
            return o.getAct().checkSelfReferencingRecursiveStep(o1.getAct(), 0);
        }
    }


    public void traverse(Consumer<Option> f) {
        f.accept(this);
        for(Option c: children) {
            c.traverse(f);
        }
    }


    public void cleanup() {
        parent.children.remove(this);
        parent = null;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" snId:" + (searchNode != null ? searchNode.getId() : "-") + " d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p + " value:" + Utils.round(state.lb));
        return sb.toString();
    }


    @Override
    public int compareTo(Option o) {
        int r = Integer.compare(getAct().getId(), o.getAct().getId());
        if(r != 0) return r;
        return Integer.compare(searchNode.getId(), o.searchNode.getId());
    }




    public static class Link {
        private final Activation.Link actLink;

        private final Option input;
        private final Option output;


        public Link(Activation.Link actLink, Option input, Option output) {
            this.actLink = actLink;
            this.input = input;
            this.output = output;
        }


        public Activation.Link getActivationLink() {
            return actLink;
        }


        public Option getInput() {
            return input;
        }


        public Option getOutput() {
            return output;
        }


        public boolean isNegative(Synapse.State s) {
            return actLink.isNegative(s);
        }


        public boolean isInactive() {
            return actLink.isInactive();
        }


        public boolean isRecurrent() {
            return actLink.isRecurrent();
        }


        public void link() {
            input.addLink(INPUT, this);
            output.addLink(OUTPUT, this);
        }


        public String toString() {
            return actLink + ": " + input + " --> " + output;
        }
    }

}
