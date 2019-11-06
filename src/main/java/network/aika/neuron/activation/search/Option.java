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
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.activation.State;
import network.aika.neuron.excitatory.ExcitatoryNeuron;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static network.aika.Document.MAX_ROUND;
import static network.aika.neuron.activation.link.Link.INPUT_COMP;
import static network.aika.neuron.activation.link.Link.OUTPUT_COMP;
import static network.aika.neuron.activation.search.Decision.UNKNOWN;

/**
 *
 * @author Lukas Molzberger
 */
public class Option implements Comparable<Option> {

    public static final State INITIAL_STATE = new State(0.0, Double.MAX_VALUE, 0.0, null, 0.0);

    private State state = INITIAL_STATE;

    public Activation act;
    public SearchNode searchNode;

    private Option parent;
    private Set<Option> children = new TreeSet<>();

    public Decision decision;

    private double weight;
    public double remainingWeight;
    public int cacheFactor = 1;
    private double p;

    public TreeMap<Link, Option> inputOptions = new TreeMap<>(INPUT_COMP);
    public Map<Link, Option> outputOptions = new TreeMap<>(OUTPUT_COMP); // TODO:

    private boolean isQueued;

    public int round;

    public ExcitatoryNeuron targetNeuron;


    public Option(Option parent, Activation act, SearchNode sn) {
        this.act = act;
        this.searchNode = sn;

        this.parent = parent;

        if(parent != null) {
            parent.children.add(this);
        }

        decision = act.getNextDecision(parent, sn);
    }


    public boolean setState(State s) {
        if(state.equalsWithWeights(s)) {
            return false;
        }

        round++;

        if(round > MAX_ROUND) {
            throw new Activation.OscillatingActivationsException(act.getDocument().activationsToString());
        }

        state = s;
        return true;
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
        return state.value > 0.0;
    }


    public void link() {
        for(Link l: act.getInputLinks().collect(Collectors.toList())) {
            Activation iAct = l.getInput();
            if(iAct.currentOption != null && iAct.currentOption.decision != UNKNOWN && iAct.currentOption.isActive()) {
                link(l, iAct.currentOption);
            }
        }
    }


    public void setWeight(double weight) {
        this.weight = weight;
    }


    public void link(Link l, Option in) {
        inputOptions.put(l, in);
        in.outputOptions.put(l, this);
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
        if(act.getType() != INeuron.Type.INHIBITORY) {
            return p;
        } else {
            if(inputOptions.isEmpty()) {
                return 0.0;
            }
            return inputOptions.firstEntry().getValue().getP();
        }
    }


    public void computeRemainingWeight() {
        double sum = 0.0;
        for(Option c: children) {
            sum += c.weight;
        }

        remainingWeight = weight - sum;
    }



    private Option getInputExcitatoryOption() {
        if(getAct().getType() != INHIBITORY) {
            return this;
        } else {
            if(inputOptions.isEmpty()) {
                return null;
            }

            Option io = inputOptions.firstEntry().getValue();

            return io.getInputExcitatoryOption();
        }
    }

    public boolean checkSelfReferencing(Option o) {
        Option o1 = getInputExcitatoryOption();
        if(o1 == null) {
            return false;
        } else if(o == o1) {
            return true;
        }

        Integer f1 = o1.getState().fired;
        Integer f2 = o.getState().fired;
        if(f1 == null) {
            return false;
        } else if (f2 != null && f1 > f2) {
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
        sb.append(" snId:" + (searchNode != null ? searchNode.getId() : "-") + " d:"  + decision + " cacheFactor:" + cacheFactor + " w:" + Utils.round(weight) + " p:" + p + " value:" + Utils.round(state.value));
        return sb.toString();
    }


    @Override
    public int compareTo(Option o) {
        int r = Integer.compare(getAct().getId(), o.getAct().getId());
        if(r != 0) return r;
        return Integer.compare(searchNode.getId(), o.searchNode.getId());
    }
}
