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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.neuron.Synapse;
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.steps.activation.InstantiationA;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveActivation<N extends ConjunctiveNeuron<?, ?>> extends Activation<N> {

    protected ConjunctiveActivation<N>  template;

    protected ConjunctiveActivation<N>  templateInstance;

    public ConjunctiveActivation(int id, Thought t, N n) {
        super(id, t, n);

        if (getConfig().isMetaInstantiationEnabled() && n.isAbstract())
            isFinalAndFired.addEventListener(() ->
                    InstantiationA.add(this)
            );
    }

    @Override
    public ConjunctiveActivation resolveAbstractInputActivation() {
        return neuron.isAbstract() ?
                getTemplateInstance() :
                this;
    }

    @Override
    public ConjunctiveActivation getTemplateInstance() {
        return templateInstance;
    }

    public ConjunctiveActivation<N> getTemplate() {
        return template;
    }

    public void setTemplate(Activation template) {
        this.template = (ConjunctiveActivation<N>) template;
    }

    @Override
    public void instantiateTemplateA() {
        N n = (N) neuron.instantiateTemplate();

        templateInstance = n.createActivation(getThought());
    }

    @Override
    public void instantiateTemplateB() {
        getInputLinks()
                .forEach(l -> {
                    Activation iAct = l.getInput().resolveAbstractInputActivation();
                    if(iAct != null) {
                        Synapse s = l.instantiateTemplate(iAct, templateInstance);
                        s.createLinkFromTemplate(iAct, templateInstance, l);
                    }
                });

        templateInstance.isFired.addEventListener(() -> {
            templateInstance.getNeuron().setLabel(
                    getConfig().getLabel(this)
            );
        });

        templateInstance.initFromTemplate(this);

        getOutputLinks()
                .forEach(l -> {
                    Activation oAct = l.getOutput().resolveAbstractInputActivation();
                    if(oAct != null) {
                        Synapse s = l.instantiateTemplate(templateInstance, oAct);
                        s.createLinkFromTemplate(templateInstance, oAct, l);
                    }
                });
    }
}
