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

import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;

/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    public Document(String content) {
        super();
        this.content = new StringBuilder();
        if(content != null) {
            this.content.append(content);
        }
    }

    public void registerActivation(Activation act) {
        super.registerActivation(act);
    }

    @Override
    public void linkInputRelations(Activation act) {
        TextReference ref = act.getReference();
        TextModel tm = act.getNeuron().getModel();
        if(tm.getNextTokenInhib().getId().equals(act.getNeuron().getId())) {
            ref.nextTokenIAct = act;
            ref.nextTokenBAct = act.getInputLinks()
                    .findAny()
                    .map(Link::getInput)
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
        TextReference ref = act.getReference();
        return ref.getText();
    }

    public Activation addInput(Neuron n, int begin, int end) {
        return addInput(n, new TextReference(this, begin, end));
    }

    public Activation addInput(NeuronProvider n, int begin, int end) {
        return addInput(n.getNeuron(), begin, end);
    }

    public Activation addInput(Neuron n, Reference ref) {
        Activation act = createActivation(n);
        act.initInput(ref);
        return act;
    }

    public Activation addInput(NeuronProvider n, Reference ref) {
        return addInput(n.getNeuron(), ref);
    }

    @Deprecated
    public Activation processToken(TextModel m, TextReference lastRef, int begin, int end, String tokenLabel) {
        TextReference ref = new TextReference(this, begin, end);
        Neuron tokenN = m.lookupToken(tokenLabel);
        Activation tokenPatternAct = addInput(tokenN, ref);
        linkConsecutiveTokens(lastRef, ref);

        return tokenPatternAct;
    }

    public void linkConsecutiveTokens(TextReference prevToken, TextReference nextToken) {
        if (prevToken == null)
            return;
        prevToken.setNext(nextToken);
        nextToken.setPrevious(prevToken);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(content);
        sb.append("\n");
        sb.append(super.toString());
        return sb.toString();
    }
}
