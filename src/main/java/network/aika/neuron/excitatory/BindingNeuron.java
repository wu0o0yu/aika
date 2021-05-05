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
import network.aika.neuron.activation.scopes.Scope;
import network.aika.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * @author Lukas Molzberger
 */
public class BindingNeuron extends ExcitatoryNeuron<BindingNeuronSynapse> {
    private static final Logger log = LoggerFactory.getLogger(BindingNeuron.class);

    public static byte type;

    public BindingNeuron() {
        super();
    }

    public BindingNeuron(Model model) {
        super(model);
    }

    @Override
    public Collection<Scope> getInitialScopeTemplates(Direction dir) {
        Templates t = getModel().getTemplates();

        if(dir == Direction.OUTPUT)
            return Set.of(t.B_SAME, t.B_INPUT, t.I_SAME, t.P_SAME);
        else
            return Set.of(t.B_SAME);
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
    public BindingNeuron instantiateTemplate() {
        BindingNeuron n = new BindingNeuron(getModel());
        initFromTemplate(n);

        return n;
    }

    @Override
    public byte getType() {
        return type;
    }
}
