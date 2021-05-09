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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Reference;

/**
 *
 * @author Lukas Molzberger
 */
public class TextReference implements Reference {

    private Document doc;
    private int begin;
    private int end;

    private TextReference previous;
    private TextReference next;

    public Activation nextTokenBAct;
    public Activation nextTokenIAct;

    public TextReference(Document doc, int begin, int end) {
        assert begin < end;

        this.doc = doc;
        this.begin = begin;
        this.end = end;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public String getText() {
        return doc.getTextSegment(begin, end);
    }

    public TextReference getPrevious() {
        return previous;
    }

    public void setPrevious(TextReference previous) {
        this.previous = previous;
    }

    public TextReference getNext() {
        return next;
    }

    public void setNext(TextReference next) {
        this.next = next;
    }

    @Override
    public double length() {
        return end - begin;
    }

    @Override
    public Reference add(Reference ir) {
        return new TextReference(
                doc,
                Math.min(begin, ir.getBegin()),
                Math.max(end, ir.getEnd())
        );
    }

    @Override
    public Thought getThought() {
        return doc;
    }

    public String toString() {
        return "Ref(" + begin + "," + end + ")";
    }
}
