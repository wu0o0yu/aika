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

import network.aika.callbacks.EventListener;
import network.aika.callbacks.EventType;
import network.aika.debugger.*;
import network.aika.debugger.activations.layout.ParticleLink;
import network.aika.debugger.activations.particles.ActivationParticle;
import network.aika.debugger.stepmanager.StepManager;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.links.Link;
import network.aika.steps.Step;
import network.aika.text.Document;
import network.aika.utils.Utils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.view.camera.DefaultCamera2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static network.aika.debugger.AbstractGraphManager.STANDARD_DISTANCE_X;
import static network.aika.debugger.AbstractGraphManager.STANDARD_DISTANCE_Y;
import static network.aika.debugger.stepmanager.StepManager.When.*;

/**
 * @author Lukas Molzberger
 */
public class ActivationViewManager extends AbstractViewManager<Activation, ActivationGraphManager> implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(ActivationViewManager.class);

    private Document doc;

    private ActivationConsoleManager consoleManager;

    private AIKADebugManager debugger;

    protected StepManager stepManager;

    protected LayoutState layoutState = new LayoutState();

    public ActivationViewManager(Document doc, ActivationConsoleManager consoleManager, AIKADebugManager debugger) {
        super(doc.getModel());
        this.consoleManager = consoleManager;
        this.debugger = debugger;

        double width = 5 * STANDARD_DISTANCE_X;
        double height = 3 * STANDARD_DISTANCE_Y;

        getCamera().setGraphViewport(-(width / 2), -(height / 2), (width / 2), (height / 2));
        getCamera().setViewCenter(0.20, 0.20, 0.0);

        graphManager = new ActivationGraphManager(graph, doc);

        this.doc = doc;
        doc.addEventListener(this);

        view = initView();
    }

    public StepManager getStepManager() {
        return stepManager;
    }

    public void setStepManager(StepManager stepManager) {
        this.stepManager = stepManager;
    }

    public void pumpAndWaitForUserAction() {
        pump();

        stepManager.waitForClick();
    }

    @Override
    public Component getConsoleManager() {
        return consoleManager;
    }

    public void showElementContext(GraphicElement ge) {
        if(ge instanceof Node) {
            Node n = (Node) ge;

            Activation act = graphManager.getAikaNode(n);
            if(act == null)
                return;

            consoleManager.showSelectedElementContext(act);

        } else if(ge instanceof Edge) {
            Edge e = (Edge) ge;

            Link l = graphManager.getLink(e);
            if(l == null)
                return;

            consoleManager.showSelectedElementContext(l);
        }
    }

    @Override
    public void reactToCtrlSelection(GraphicElement ge) {
        if (ge instanceof Node) {
            Node n = (Node) ge;

            Activation act = graphManager.getAikaNode(n);
            if (act == null)
                return;

            debugger.getNeuronViewManager()
                    .showSelectedNeuron(act.getNeuron());

            debugger.showNeuronView();
        }
    }

    @Override
    public void onElementEvent(EventType et, network.aika.elements.Element e) {
        if(e instanceof Activation<?>)
            onActivationEvent(et, (Activation) e);
        else if(e instanceof Link<?,?,?>)
            onLinkEvent(et, (Link) e);
    }

    public void onActivationEvent(EventType et, Activation act) {
        ActivationParticle p = graphManager.lookupParticle(act);
        p.processLayout(layoutState);
        p.onEvent(et);

        if(!stepManager.stopHere(NEW))
            return;

        pumpAndWaitForUserAction();
    }

    public void onLinkEvent(EventType et, Link l) {
        Edge e = onLinkEvent(l);
        if(e == null)
            return;

        if (!stepManager.stopHere(NEW))
            return;

        pumpAndWaitForUserAction();
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

    }

    public void beforeProcessedEvent(Step s) {
        if(s.getElement() instanceof Activation) {
            beforeActivationProcessedEvent((Activation) s.getElement());
        } else if(s.getElement() instanceof Link) {
            beforeLinkProcessedEvent((Link) s.getElement());
        }

        if (!stepManager.stopHere(BEFORE))
            return;

        pumpAndWaitForUserAction();
    }

    public void afterProcessedEvent(Step s) {
        if(s.getElement() instanceof Activation) {
            afterActivationProcessedEvent((Activation) s.getElement());
        }

        if (!stepManager.stopHere(AFTER))
            return;

        pumpAndWaitForUserAction();
    }

    private void beforeActivationProcessedEvent(Activation act) {
        AbstractParticle p = graphManager.getParticle(act);
        if(p != null)
            p.onEvent(null);
    }

    private void afterActivationProcessedEvent(Activation act) {
        AbstractParticle p = graphManager.getParticle(act);
        if(p != null)
            p.onEvent(null);
    }

    private void highlightCurrentOnly(Element e) {
        if(lastHighlighted != e && lastHighlighted != null) {
            unhighlightElement(lastHighlighted);
        }
    }

    private void beforeLinkProcessedEvent(Link l) {
        onLinkEvent(l);
    }

    private Edge onLinkEvent(Link l) {
        if(l.getInput() == null || l.getOutput() == null)
            return null;

        ParticleLink pl = graphManager.lookupParticleLink(l);
        pl.processLayout();

        highlightCurrentOnly(pl.getEdge());

        pl.onEvent();

        return pl.getEdge();
    }

    public void viewClosed(String id) {
    }

    public Document getDocument() {
        return doc;
    }

    public void dumpNetworkCoordinates() {
        System.out.println("Activations: ");

        System.out.println("camera.setViewPercent(" + Utils.round(getCamera().getViewPercent()) + ");");
        System.out.println("camera.setViewCenter(" + Utils.round(getCamera().getViewCenter().x) + ", " + Utils.round(getCamera().getViewCenter().y) + ", 0);");

        doc.getActivations().forEach(act ->
                dumpActivation(act)
        );
    }

    private void dumpActivation(Activation act) {
        ActivationParticle p = graphManager.getParticle(act);
        if(p != null && p.getPosition() != null) {
            System.out.println("coords.put(" + act.getId() + ", new double[]{" + Utils.round(p.getPosition().x) + ", " + Utils.round(p.getPosition().y) + "});");
        } else {
            System.out.println("//coords.put(" + act.getId() + ", new double[]{ });");
        }
    }

    @Override
    public void moveNodeGroup(Node n, int x, int y) {
        Activation<?> act = getGraphManager().getAikaNode(n);
        if(!(act instanceof PatternActivation))
            return;

        DefaultCamera2D camera = (DefaultCamera2D) getGraphView().getCamera();

        GraphicElement gn = (GraphicElement) n;

        act.getInputLinks()
                .map(l -> l.getInput())
                .map(iAct -> (GraphicElement) getGraphManager().getNode(iAct)) // TODO: fixme
                .forEach(in -> {
                    Point3 p = camera.transformPxToGuSwing(x, y);
                    in.move((in.getX() - gn.getX()) + p.x, (in.getY() - gn.getY()) + p.y, in.getZ());
                });
    }
}
