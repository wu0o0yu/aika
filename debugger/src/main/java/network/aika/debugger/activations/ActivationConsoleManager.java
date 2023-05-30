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
package network.aika.debugger.activations;

import network.aika.Thought;
import network.aika.callbacks.EventListener;
import network.aika.callbacks.EventType;
import network.aika.debugger.AbstractConsoleManager;
import network.aika.debugger.ElementPanel;
import network.aika.elements.Element;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.steps.Step;
import network.aika.text.Document;

import javax.swing.*;
import java.awt.*;


/**
 * @author Lukas Molzberger
 */
public class ActivationConsoleManager extends JSplitPane implements AbstractConsoleManager, EventListener {

    private Document doc;

    private QueueConsole queueConsole;

    private Element currentMainElement = null;
    private Element currentSelectedElement = null;

    protected ActivationPanelMode mode = ActivationPanelMode.CURRENT;

    private ElementPanel activationPanel = new ActivationPanel(this, null);

    private boolean sticky;

    public ActivationConsoleManager(Document doc) {
        super(JSplitPane.VERTICAL_SPLIT);
        this.doc = doc;

        setTopComponent(activationPanel);

        queueConsole = new QueueConsole(this);
        setBottomComponent(getScrollPane(queueConsole));
        setResizeWeight(0.65);

        doc.addEventListener(this);
    }


    public ActivationPanelMode getMode() {
        return mode;
    }

    public void setMode(ActivationPanelMode mode) {
        this.mode = mode;
    }


    public QueueConsole getQueueConsole() {
        return queueConsole;
    }

    public void showSelectedElementContext(Element e) {
        sticky = true;
        update(e);
        updateQueue(null);
    }

    private void update(Element e) {
        Element currentElement = getCurrentElement();
        if(e == currentElement)
            return;

        setCurrentElement(e);

        update();
    }

    public void update() {
        if (activationPanel != null)
            activationPanel.remove();

        ElementPanel newActPanel = ElementPanel.createElementPanel(this, getCurrentElement());

        activationPanel = newActPanel;

        setTopComponent(activationPanel);
    }

    private void setCurrentElement(Element e) {
        if(sticky)
            currentSelectedElement = e;
        else
            currentMainElement = e;
    }

    private Element getCurrentElement() {
        return sticky ?
                currentSelectedElement :
                currentMainElement;
    }

    private void updateQueue(Step s) {
        queueConsole.update(s, getCurrentElement());
    }

    public static JScrollPane getScrollPane(Component comp) {
        JScrollPane scrollPane = new JScrollPane(comp);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(250, 155));
        scrollPane.setMinimumSize(new Dimension(10, 10));

        return scrollPane;
    }

    @Override
    public void onElementEvent(EventType et, Element e) {
        if(e instanceof Activation<?>)
            onActivationEvent(et, (Activation) e);
        else if(e instanceof Link<?,?,?>)
            onLinkEvent(et, (Link) e);
    }


    @Override
    public void onQueueEvent(EventType et, Step s) {
        switch (et) {
            case ADDED:
                queueEntryAddedEvent(s);
                break;
            case BEFORE:
                beforeProcessedEvent(s);
                break;
            case AFTER:
                afterProcessedEvent(s);
                break;
        }
    }

    public void queueEntryAddedEvent(Step s) {
        updateQueue(null);
    }


    public void beforeProcessedEvent(Step s) {
        updateQueue(s);
        update(s.getElement());
    }


    public void afterProcessedEvent(Step s) {
        updateQueue(s);
        update(s.getElement());
    }

    public void onActivationEvent(EventType et, Activation act) {
        update(act);
    }

    public void onLinkEvent(EventType et, Link l) {
        update(l);
    }

    public Thought getThought() {
        return doc;
    }
}
