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
package network.aika.neuron.bindingsignal;

import network.aika.direction.Direction;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
import static network.aika.neuron.bindingsignal.TransitionMode.*;

/**
 * @author Lukas Molzberger
 */
public class PrimitiveTransition<I extends PrimitiveTerminal, O extends PrimitiveTerminal> implements Transition<I, O> {

    private static final Logger log = LoggerFactory.getLogger(PrimitiveTransition.class);

    protected BiTransition parent = null;
    protected I input;
    protected O output;
    protected TransitionMode mode;
    private Class<? extends Synapse> synClazz;

    protected PrimitiveTransition(I input, O output, TransitionMode mode, Class<? extends Synapse> synClazz) {
        this.input = input;
        this.output = output;
        this.mode = mode;
        this.synClazz = synClazz;

        input.getTransitions(OUTPUT).add(this);
        output.getTransitions(INPUT).add(this);
    }

    public static <I extends PrimitiveTerminal, O extends PrimitiveTerminal> PrimitiveTransition<I, O> transition(
            I input,
            O output,
            TransitionMode transitionMode,
            Class<? extends Synapse> synClazz
    ) {
        return new PrimitiveTransition(input, output, transitionMode, synClazz);
    }

    public BiTransition getParent() {
        return parent;
    }

    public void setParent(BiTransition parent) {
        this.parent = parent;
    }


    @Override
    public void propagate(PrimitiveTerminal fromTerminal, BindingSignal bs, Link l, Activation act) {
        if(bs.getState() != fromTerminal.state)
            return;

        if(!isPropagate())
            return;

        State toState = fromTerminal.type.getPrimitiveTerminal(this).getState();

        BindingSignal nextBS = act.getBindingSignal(bs.getOriginActivation(), toState);

        if(nextBS == null)
            nextBS = new BindingSignal(bs, toState, act);

        nextBS.addParent(l, bs);
    }

    @Override
    public void latentLinking(Synapse ts, BindingSignal... fromBSs) {
        LatentLinking.latentLinking(this, ts, fromBSs[0]);
    }

    @Override
    public void link(Synapse ts, Direction dir, BindingSignal... fromBSs) {
        link(ts, dir, null, fromBSs);
    }

    public void link(Synapse ts, Direction dir, Predicate<BindingSignal> biCheck, BindingSignal... fromBSs) {
        BindingSignal fromBS = fromBSs[0];
        if(!isMatching())
            return;

        Stream<BindingSignal> bsStream = ts.getRelatedBindingSignals(fromBS.getOriginActivation(), this, dir);

        if(biCheck != null)
            bsStream = bsStream.filter(biCheck);

        bsStream.filter(toBS -> fromBS != toBS)
                .filter(toBS ->
                        ts.linkCheck(
                                dir.getInput(fromBS, toBS),
                                dir.getOutput(fromBS, toBS)
                        )
                )
                .forEach(toBS ->
                        ts.link(
                                fromBS.getActivation(),
                                toBS.getActivation(),
                                dir
                        )
                );
    }

    @Override
    public I getInput() {
        return input;
    }

    @Override
    public O getOutput() {
        return output;
    }

    public TransitionMode getMode() {
        return mode;
    }

    public boolean isPropagate() {
        return mode == PROPAGATE_ONLY || mode == MATCH_AND_PROPAGATE;
    }

    public boolean isMatching() {
        return mode == MATCH_ONLY || mode == MATCH_AND_PROPAGATE;
    }

    public String toString() {
        return "Input:" + input +
                " Output:" + output +
                " Mode:" + mode +
                " Synapse:" + synClazz.getSimpleName();
    }
}
