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

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.Config;
import network.aika.neuron.TNeuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.meta.MetaNeuron;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static network.aika.neuron.Synapse.State.CURRENT;


/**
 *
 * @author Lukas Molzberger
 */
public class InhibitoryNeuron extends TNeuron<InhibitorySynapse> {

    public static final String TYPE_STR = Model.register("NI", InhibitoryNeuron.class);


    protected InhibitoryNeuron() {
        super();
    }


    public InhibitoryNeuron(Neuron p) {
        super(p);
    }


    public InhibitoryNeuron(Model model, String label) {
        super(model, label);
    }


    @Override
    public Fired incrementFired(Fired f) {
        return f;
    }


    public boolean isWeak(Synapse s, Synapse.State state) {
        return s.getWeight(state) < -getBias();
    }


    public String getType() {
        return TYPE_STR;
    }


    public double getTotalBias(Synapse.State state) {
        return getBias(state);
    }


    public ActivationFunction getActivationFunction() {
        return ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
    }

    @Override
    public void addInputSynapse(InhibitorySynapse inhibitorySynapse) {

    }

    @Override
    public void addOutputSynapse(Synapse synapse) {

    }

    @Override
    public void removeInputSynapse(InhibitorySynapse inhibitorySynapse) {

    }

    @Override
    public void removeOutputSynapse(Synapse s) {

    }


    public void propagate(Activation act) {
        super.propagate(act);
        Document doc = act.getDocument();

        Link l = act.inputLinks.firstEntry().getValue();
        Activation pAct = l.getInput();

        pAct.followDown(doc.getNewVisitedId(), (cAct, isConflict) -> {
            if(cAct == pAct) {
                return false;
            }

            Synapse s = act.getNeuron().getOutputSynapse(cAct.getNeuron());

            if(s == null || !s.isRecurrent() || !s.isNegative(CURRENT)) {
                return false;
            }

            Link nl = new Link(s, act, cAct);
            if(cAct.inputLinks.containsKey(nl)) {
                return false;
            }

            doc.getLinker().add(nl);

            return false;
        });

        doc.getLinker().process();
    }


    protected void propagate(Activation iAct, Synapse s) {
        Document doc = iAct.getDocument();
        Activation oAct = new Activation(doc, this, iAct.round);

        oAct.addLink(new Link(s, iAct, oAct));
    }


    public void commit(Collection<? extends Synapse> modifiedSynapses) {
        commitBias();

        for (Synapse s : modifiedSynapses) {
            s.commit();
        }

        setModified();
    }


    public static InhibitoryNeuron induceIncoming(Model m, int threadId, List<ExcitatorySynapse> targetSyns) {
        // TODO: Prüfen, ob schon ein passendes inhibitorisches Neuron existiert.

        InhibitoryNeuron n = new InhibitoryNeuron(m, "");
        n.setBias(0.0);

        for(ExcitatorySynapse es: targetSyns) {
            InhibitorySynapse is = new InhibitorySynapse(es.getPInput(), n.getProvider());

            is.link();

            is.update(null, 1.0);
        }

        n.commit(n.getProvider().getActiveInputSynapses());
        return n;
    }

/*
    public static InhibitoryNeuron induceOutgoing(int threadId, MetaNeuron mn) {
        // TODO: Prüfen, ob schon ein passendes inhibitorisches Neuron existiert.

        InhibitoryNeuron n = new InhibitoryNeuron(mn.getModel(), "");
        n.setBias(0.0);

        int misSynId = n.getNewSynapseId();
        MetaInhibSynapse mis = new MetaInhibSynapse(mn.getProvider(), n.getProvider(), misSynId);
        mis.link();

        mis.update(null, 1.0);

        for(MetaNeuron.MappingLink ml: mn.targetNeurons.values()) {
            ExcitatoryNeuron targetNeuron = ml.targetNeuron;

            int isSynId = n.getNewSynapseId();
            InhibitorySynapse is = new InhibitorySynapse(targetNeuron.getProvider(), n.getProvider(), isSynId);

            is.link();

            is.update(null, ml.nij);
        }

        n.commit(n.getProvider().getActiveInputSynapses());
        return n;
    }
*/


    public void prepareMetaTraining(Config c, Activation o, Function<Activation, ExcitatoryNeuron> callback) {
        // Nothing to do.
    }


    public void train(MetaNeuron mn) {
        for(Synapse s: getProvider().getActiveInputSynapses()) {
            if(s instanceof InhibitorySynapse) {
                InhibitorySynapse is = (InhibitorySynapse) s;
                ExcitatoryNeuron targetNeuron = (ExcitatoryNeuron) is.getInput();

 //               is.update(null, ml.nij);
            }
        }
    }


    public boolean isMature(Config c) {
        return true;
    }

    @Override
    public void dumpStat() {

    }


    public String typeToString() {
        return "INHIBITORY";
    }

}
