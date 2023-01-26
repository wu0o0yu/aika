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
package network.aika.elements.links;

import network.aika.direction.Direction;
import network.aika.elements.activations.*;
import network.aika.elements.synapses.BindingCategoryInputSynapse;
import network.aika.elements.synapses.BindingCategorySynapse;
import network.aika.elements.synapses.PatternCategorySynapse;


/**
 * @author Lukas Molzberger
 */
public class BindingCategoryInputLink extends BindingNeuronLink<BindingCategoryInputSynapse, BindingCategoryActivation> {

    public BindingCategoryInputLink(BindingCategoryInputSynapse s, BindingCategoryActivation input, BindingActivation output) {
        super(s, input, output);
    }

    @Override
    public void instantiateTemplate(BindingCategoryActivation iAct, BindingActivation oAct) {
        if(iAct == null || oAct == null)
            return;

        Link l = iAct.getInputLink(oAct.getNeuron());

        if(l != null)
            return;

        BindingCategorySynapse s = new BindingCategorySynapse();
        s.initFromTemplate(oAct.getNeuron(), iAct.getNeuron(), synapse);

        synapse.copyState(s);
        s.connect(Direction.INPUT, false, false);
        s.connect(Direction.OUTPUT, false, true);

        s.createLinkFromTemplate(oAct, iAct, this);
    }

    @Override
    public void addInputLinkingStep() {
        super.addInputLinkingStep();

        input.getInputLinks()
                .map(l -> (ConjunctiveActivation)l.getInput())
                .forEach(act ->
                        output.linkTemplateAndInstance(act)
                );
    }
}