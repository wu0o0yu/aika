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
package network.aika.elements.activations;

import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.elements.neurons.ConjunctiveNeuron;
import network.aika.fields.FieldLink;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static network.aika.callbacks.EventType.UPDATE;
import static network.aika.fields.Fields.isTrue;
import static network.aika.fields.Fields.scale;


/**
 *
 * @author Lukas Molzberger
 */
public abstract class ConjunctiveActivation<N extends ConjunctiveNeuron<?, ?>> extends Activation<N> {

    protected ConjunctiveActivation<N>  template;

    protected List<ConjunctiveActivation<N>> templateInstances;

    private boolean initialized = true;

    public ConjunctiveActivation(int id, Thought t, N n) {
        super(id, t, n);
    }


    @Override
    protected void connectWeightUpdate() {
        negUpdateValue = scale(
                this,
                "-updateValue",
                -1.0,
                updateValue
        );

        FieldLink.link(
                updateValue,
                getNeuron().getBias()
        );
    }

    public ConjunctiveActivation getActiveTemplateInstance() {
        return getTemplateInstancesStream()
                .filter(act -> isTrue(act.getIsFired()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public ConjunctiveActivation<N> resolveAbstractInputActivation() {
        return neuron.isAbstract() ?
                getActiveTemplateInstance() :
                this;
    }

    public List<ConjunctiveActivation<N>> getTemplateInstances() {
        if(templateInstances == null)
            templateInstances = new ArrayList<>();

        return templateInstances;
    }

    public Stream<ConjunctiveActivation<N>> getTemplateInstancesStream() {
        return getTemplateInstances().stream();
    }

    public ConjunctiveActivation<N> getTemplate() {
        return template;
    }

    public void setTemplate(Activation template) {
        this.template = (ConjunctiveActivation<N>) template;
    }

    public void addTemplateInstance(ConjunctiveActivation instanceAct) {
        getTemplateInstances().add(instanceAct);
    }

    public void linkTemplateAndInstance(ConjunctiveActivation instanceAct) {
        addTemplateInstance(instanceAct);
        instanceAct.setTemplate(this);
    }

    @Override
    public void instantiateTemplateNodes() {
        N n = (N) neuron.instantiateTemplate();

        ConjunctiveActivation<N> ti = n.createActivation(getThought());
        linkTemplateAndInstance(ti);
        ti.initialized = false;

        if(thought.getInstantiationCallback() != null)
            thought.getInstantiationCallback().onInstantiation(ti);
    }

    @Override
    public void instantiateTemplateEdges() {
        ConjunctiveActivation<N> ti = getActiveTemplateInstance();

        getInputLinks()
                .forEach(l ->
                    l.instantiateTemplate(
                            l.getInput().resolveAbstractInputActivation(),
                            ti
                    )
                );

        ti.getNeuron().setLabel(
                getConfig().getLabel(this)
        );

        ti.initDummyLinks();
        ti.initFromTemplate();

        getOutputLinks()
                .filter(l -> !l.getOutput().getNeuron().isAbstract())
                .forEach(l ->
                    l.instantiateTemplate(
                            ti,
                            l.getOutput().resolveAbstractInputActivation()
                    )
                );
    }

    public void initFromTemplate() {
        if(initialized)
            return;

        template.copyState(this);
        fired = template.fired;
        connect(Direction.INPUT, false, false);
        thought.onElementEvent(UPDATE, this);

        initialized = true;
    }
}
