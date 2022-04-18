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
package network.aika.steps.activation;

import network.aika.direction.Direction;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.*;
import network.aika.neuron.bindingsignal.BindingSignal;
import network.aika.neuron.bindingsignal.State;
import network.aika.neuron.bindingsignal.Transition;

import static network.aika.direction.Direction.INPUT;
import static network.aika.direction.Direction.OUTPUT;
/**
 * The job of the linking phase is to propagate information through the network by creating the required activations and links.
 * Each activation and each link have an corresponding neuron or synapse respectively. Depending on the data set in the
 * document, a neuron might have several activations associated with it. During propagation an input activation
 * causes the creating of a link in one or more output synapses and the creation of an output activation. Initially the value
 * of the input activation and the weight of the synapse might not suffice to activate the output activation. But that might
 * change later on as more input links are added to the activation. New input links are added by the closeCycle method. This
 * method is called by the visitor which follows the links in the activation network to check that both input and output
 * activation of a new link refer to the same object in the input data set.
 *
 * @author Lukas Molzberger
 */
public class Linking  {

    public static void link(Synapse ts, Direction dir, BindingSignal fromBS, BindingSignal toBS) {
        BindingSignal inputBS = dir.getInput(fromBS, toBS);
        BindingSignal outputBS = dir.getOutput(fromBS, toBS);

        link(ts, inputBS, outputBS);
    }

    public static void link(Direction dir, Synapse ts, BindingSignal<?> fromBS) {
        Neuron toNeuron = dir.getNeuron(ts);

        fromBS.getRelatedBindingSignal(ts, toNeuron)
                .filter(toBS -> fromBS != toBS)
                .forEach(toBS ->
                        link(ts, dir, fromBS, toBS)
                );

        if(dir == OUTPUT) {
            //           propagate(fromBS, ts);
            link(ts, fromBS, null);
        }
    }
/*
    public static Activation link(Synapse ts, BindingSignal inputBS, BindingSignal outputBS) {
        if(!ts.linkingCheck(inputBS, outputBS))
            return null;

        Link l = ts.createLink(inputBS, outputBS);
        return l.getOutput();
    }
*/
    public static Activation link(Synapse ts, BindingSignal iBS, BindingSignal oBS) {
        if(iBS.getActivation().getNeuron().isNetworkInput() && !ts.networkInputsAllowed(INPUT))
            return null;

        if(oBS.getActivation().getNeuron().isNetworkInput() && !ts.networkInputsAllowed(OUTPUT))
            return null;

        Transition t = ts.getTransition(iBS, Direction.OUTPUT, false);
        if(t == null)
            return null;

        State oState = t.next(Direction.OUTPUT);
        if(oState == null || oState != oBS.getState())
            return null;

        if(ts.linkExists(iBS.getActivation(), oBS.getActivation()))
            return null;

        if(!(ts.isRecurrent() || Link.isCausal(iBS.getActivation(), oBS.getActivation())))
            return null;

        if(!ts.linkingCheck(iBS, null))
            return null;

        Activation inputAct = iBS.getActivation();

        if(oBS == null) {
            if(!ts.propagatedAllowed(inputAct))
                return null;

            oBS = iBS.propagate(ts);
            if(oBS == null)
                return null;

            Activation outputAct = ts.getOutput().createActivation(inputAct.getThought());
            outputAct.init(ts, inputAct);

            oBS.init(outputAct);
            outputAct.addBindingSignal(oBS);
        }

        Link l = ts.createLink(iBS, oBS);

        oBS.setLink(l); // <- TODO
        return l.getOutput();
    }
}
