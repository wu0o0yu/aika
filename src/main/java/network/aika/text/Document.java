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
package network.aika.text;

import network.aika.Config;
import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;

/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    private Cursor lastCursor = null;
    private Cursor cursor = null;

    public Document(String content) {
        this(content, null);
    }

    public Document(String content, Config trainingConfig) {
        super(trainingConfig);
        this.content = new StringBuilder();
        if(content != null) {
            this.content.append(content);
        }
    }

    public void moveCursor() {
        lastCursor = cursor;
        cursor = new Cursor();
    }

    public void registerActivation(Activation act) {
        super.registerActivation(act);
    }

    public void addActivationToQueue(Activation act) {
        super.addActivationToQueue(act);

        TextModel tm = (TextModel) act.getNeuron().getModel();
        if(tm.nextTokenInhib.getId().equals(act.getNeuron().getId())) {
            cursor.nextTokenIAct = act;
            cursor.nextTokenPPAct = act.getInputLinks()
                    .findAny()
                    .map(l -> l.getInput())
                    .orElse(null);
        }
    }

    public void append(String txt) {
        content.append(txt);
    }

    public char charAt(int i) {
        return content.charAt(i);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }

    public String toString() {
        return content.toString();
    }

    public String getTextSegment(Integer begin, Integer end) {
        if(begin != null && end != null) {
            return content.substring(
                    Math.max(0, Math.min(begin, length())),
                    Math.max(0, Math.min(end, length()))
            );
        } else {
            return "";
        }
    }

    public static String getText(Activation act) {
        return ((TextReference)act.getReference()).getText();
    }

    public Activation addInput(Neuron n, Reference ref) {
        Activation act = new Activation(this, n);
        act.setReference(ref);

        act.setValue(1.0);
        act.setFired(ref.getBegin());

        act.propagateInput();
        return act;
    }

    public Activation addInput(NeuronProvider n, Reference ref) {
        return addInput(n.getNeuron(), ref);
    }

    public Activation processToken(TextModel m, int begin, int end, String tokenLabel) {
        moveCursor();

        Reference ref = new TextReference(this, begin, end);
        Neuron tokenN = m.lookupToken(ref, tokenLabel);
        Activation tokenPatternAct = addInput(tokenN, ref);
        processActivations();

        return tokenPatternAct;
    }

    public Cursor getLastCursor() {
        return lastCursor;
    }

    public Cursor getCursor() {
        return cursor;
    }
}
