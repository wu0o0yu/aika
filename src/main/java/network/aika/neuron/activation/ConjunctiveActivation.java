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
import network.aika.neuron.conjunctive.ConjunctiveNeuron;
import network.aika.steps.activation.Instantiation;

import static network.aika.fields.Fields.isTrue;

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
                    Instantiation.add(this)
            );
    }

    public CategoryActivation getInputCategory() {
        return inputLinks.values().stream()
                .filter(l -> l.getInput() instanceof CategoryActivation)
                .map(l -> (CategoryActivation) l.getInput())
                .findAny()
                .orElse(null);
    }
    @Override
    public ConjunctiveActivation resolveAbstractInputActivation() {
        return neuron.isAbstract() ?
                getTemplateInstance() :
                this;
    }

    @Override
    public ConjunctiveActivation getTemplateInstance() {
        if(templateInstance == null) {
            CategoryActivation catBS = getInputCategory();

            if (catBS == null)
                return null;

            DisjunctiveLink<?, ConjunctiveActivation<N>, ?> l = catBS.getInput();
            templateInstance = l.getInput();
        }
        return templateInstance;
    }

    public ConjunctiveActivation<N> getTemplate() {
        return template;
    }

    @Override
    public void instantiateTemplate() {
        if(getTemplateInstance() != null)
            return;

        if(!isAbleToInstantiate())
            return;

        N n = (N) neuron.instantiateTemplate(true);

        templateInstance = n.createActivation(thought);
        templateInstance.init(null, null);
        templateInstance.template = this;

        templateInstance.isFired.addEventListener(() -> {
            updateRange();
            n.setLabel(
                    getConfig().getLabel(this)
            );
        });

        getInputLinks()
                .filter(l -> !(l instanceof PositiveFeedbackLink))
                .forEach(l ->
                        l.instantiateTemplateAndCreateLink(
                                l.getInput().resolveAbstractInputActivation(),
                                templateInstance
                        )
                );

        getOutputLinks()
                .filter(l -> l instanceof InhibitoryLink)
                .forEach(l ->
                        l.instantiateTemplateAndCreateLink(
                                templateInstance,
                                l.getOutput().resolveAbstractInputActivation()
                        )
                );


        getOutputLinks()
                .map(l -> l.getOutput())
                .filter(oAct -> oAct.getNeuron().isAbstract())
                .filter(oAct -> isTrue(oAct.isFinalAndFired))
                .forEach(oAct ->
                        Instantiation.add(oAct)
                );
    }

    @Override
    public boolean isUnresolvedAbstract() {
        return neuron.isAbstract() &&
                getTemplateInstance() == null;
    }

    private boolean isAbleToInstantiate() {
        return !getInputLinks()
                .filter(l -> !(l instanceof FeedbackLink))
                .map(l -> l.getInput())
                .anyMatch(iAct -> iAct.isUnresolvedAbstract());
    }
}
