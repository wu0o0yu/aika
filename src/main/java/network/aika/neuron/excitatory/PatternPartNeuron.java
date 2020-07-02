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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron(NeuronProvider p) {
        super(p);
    }

    public PatternPartNeuron(Model model, String label, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
    }

    @Override
    public byte getType() {
        return type;
    }

    public double propagateRangeCoverage(Activation iAct) {
        return 0.0; //getPrimaryInput() == iAct.getNeuron() ? iAct.rangeCoverage : 0.0;
    }

    public Neuron induceNeuron(Activation act) {
        return new PatternNeuron(getModel(), "", "", false);
    }

    public Synapse induceSynapse(Activation iAct, Activation oAct) {
        return new ExcitatorySynapse(iAct.getNeuron(), oAct.getNeuron());
    }

    private double getInputProbability() {
        return inputSynapses
                .values()
                .stream()
                .map(s -> s.getInput())
                .filter(n -> n instanceof PatternNeuron)
                .mapToDouble(n -> n.getP())
                .max()
                .getAsDouble();
    }


    private double getPatternProbability() {
        return outputSynapses
                .values()
                .stream()
                .map(s -> s.getOutput())
                .filter(n -> n instanceof PatternNeuron)
                .mapToDouble(n -> n.getP())
                .average()
                .getAsDouble();
    }

    public double getCost(Sign s) {
        double pXi = getInputProbability();
        double pXo = getPatternProbability();

        double pXio = frequency / getN();
        double pXioIndep = pXi * pXo;

        if (s == Sign.POS) {
            return Math.log(pXio) - Math.log(pXioIndep);
        } else {
            return Math.log(1.0 - pXio) - Math.log(1.0 - pXioIndep);
        }
    }
}
