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
package network.aika.neuron.inhibitory;


import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;

import static network.aika.neuron.activation.linker.LinkGraphs.inducePPInhibInputSynapse;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternPartInhibitoryNeuron extends InhibitoryNeuron {

    public static byte type;

    public PatternPartInhibitoryNeuron(Neuron p) {
        super(p);
    }

    public PatternPartInhibitoryNeuron(Model model, String label, Boolean isInputNeuron) {
        super(model, label, isInputNeuron);
    }

    public byte getType() {
        return type;
    }

    @Override
    public byte getOuterType() {
        return PatternPartNeuron.type;
    }

    @Override
    public void induceStructure(Activation act) {
        inducePPInhibInputSynapse.input.follow(act.getINeuron(), act, null, act);
    }

    public String typeToString() {
        return "PP-INHIBITORY";
    }
}
