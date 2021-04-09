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
import network.aika.neuron.activation.direction.Direction;
import network.aika.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static network.aika.neuron.activation.Scope.*;

/**
 * @author Lukas Molzberger
 */
public class PatternPartNeuron extends ExcitatoryNeuron<PatternPartSynapse> {
    private static final Logger log = LoggerFactory.getLogger(PatternPartNeuron.class);

    public static byte type;

    public PatternPartNeuron() {
        super();
    }

    public PatternPartNeuron(Model model) {
        super(model);
    }

    @Override
    public Set<ScopeEntry> getInitialScopes(Direction dir) {
        Set<ScopeEntry> result = new TreeSet<>();
        result.add(new ScopeEntry(0, PP_SAME));
        if(dir == Direction.OUTPUT) {
            result.add(new ScopeEntry(1, PP_INPUT));
            result.add(new ScopeEntry(2, I_SAME));
            result.add(new ScopeEntry(3, P_SAME));
        }
        return result;
    }

    @Override
    public boolean allowTemplatePropagate(Activation act) {
        Neuron n = act.getNeuron();

        if(n.isInputNeuron())
            return false;

        Utils.checkTolerance(act.getOutputGradientSum());

        return true;
    }

    @Override
    public PatternPartNeuron instantiateTemplate() {
        PatternPartNeuron n = new PatternPartNeuron(getModel());
        initFromTemplate(n);

        return n;
    }

    @Override
    public byte getType() {
        return type;
    }
}
