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
package network.aika.debugger.neurons;

import network.aika.Model;
import network.aika.debugger.AbstractParticleLink;
import network.aika.debugger.AbstractViewManager;
import network.aika.direction.Direction;
import network.aika.elements.neurons.Neuron;
import network.aika.elements.synapses.Synapse;
import network.aika.utils.Utils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.ui.graphicGraph.GraphicElement;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static network.aika.debugger.AbstractGraphManager.STANDARD_DISTANCE_X;

/**
 * @author Lukas Molzberger
 */
public class NeuronViewManager extends AbstractViewManager<Neuron, NeuronGraphManager> {

    protected NeuronConsoleManager consoleManager;

    public NeuronViewManager(Model m, NeuronConsoleManager consoleManager) {
        super(m);

        this.consoleManager = consoleManager;
        graphManager = new NeuronGraphManager(graph);

        viewer.enableAutoLayout(graphManager);

        view = initView();
    }

    @Override
    public void showElementContext(GraphicElement ge) {
        if(ge instanceof Node) {
            Node n = (Node) ge;

            Neuron neuron = graphManager.getAikaNode(n);
            if (neuron == null)
                return;

            if(consoleManager != null)
                consoleManager.showSelectedElementContext(neuron);

        } else if(ge instanceof Edge) {
            Edge e = (Edge) ge;

            Synapse s = graphManager.getLink(e);
            if(s == null)
                return;

            consoleManager.showSelectedElementContext(s);
        }
    }

    public void showSelectedNeuron(Neuron n) {
        double[] x = new double[] {0.0};

        drawNeuron(n, 0.0, 0.0);

        drawInputSynapses(n);
        drawOutputSynapses(n);
    }


    public void expandNeuron(Neuron<?> n) {
        List<? extends Synapse> outputSyns = n.getOutputSynapsesAsStream()
                .limit(20)
                .toList();

        double count = outputSyns.size();

        int i = 0;
        for(Synapse s: outputSyns) {
            drawExpandedSynapse(
                    n,
                    s,
                    Direction.OUTPUT,
                    getRelativePosition(i++, count),
                    STANDARD_DISTANCE_X
            );
        }

        List<Synapse> inputSyns = n.getInputSynapsesAsStream()
                .limit(20)
                .toList();

        count = inputSyns.size();

        i = 0;
        for(Synapse s: inputSyns) {
            drawExpandedSynapse(
                    n,
                    s,
                    Direction.INPUT,
                    getRelativePosition(i++, count),
                    -STANDARD_DISTANCE_X
            );
        }
    }

    private double getRelativePosition(int pos, double count) {
        double stepSize = STANDARD_DISTANCE_X / Math.pow(count, 0.8);
        return (pos * stepSize) - (((count - 1) * stepSize) / 2.0);
    }

    private void drawExpandedSynapse(Neuron<?> n, Synapse s, Direction dir, double relX, double relY) {
        Node currentNode = graphManager.getNode(n);
        drawNeuron(
                dir.getNeuron(s),
                (Double) currentNode.getAttribute("x") + relX,
                ((Double) currentNode.getAttribute("y")) + relY
        );
        drawSynapse(s);
    }

    @Override
    public void reactToCtrlSelection(GraphicElement ge) {
        if (ge instanceof Node) {
            Node node = (Node) ge;

            Neuron n = graphManager.getAikaNode(node);
            if (n == null)
                return;

            expandNeuron(n);
        }
    }

    public void viewClosed(String id) {
        //     loop = false;
    }

    public void updateGraphNeurons() {
 //       updateGraphNeurons(getNeurons());
    }

    public void updateGraphNeurons(Collection<Neuron> neurons) {
        double[] x = new double[] {0.0};

        neurons.forEach(n -> {
            drawNeuron(n, x[0], 0.0);
            x[0] += STANDARD_DISTANCE_X;
        });

        neurons.forEach(n -> {
            drawInputSynapses(n);
            drawOutputSynapses(n);
        });
    }

    private Collection<Neuron> getNeurons() {
        Collection<Neuron> neurons = getModel()
                .getActiveNeurons()
                .stream()
                .map(p -> p.getNeuron())
                .toList();
        return neurons;
    }

    public void dumpNetworkCoordinates() {
        System.out.println("Neurons: ");

        System.out.println("camera.setViewPercent(" + Utils.round(getCamera().getViewPercent()) + ");");
        System.out.println("camera.setViewCenter(" + Utils.round(getCamera().getViewCenter().x) + ", " + Utils.round(getCamera().getViewCenter().y) + ", 0);");

        getNeurons()
                .forEach(n -> {
                    NeuronParticle p = graphManager.getParticle(n);
                    if(p != null) {
                        System.out.println("coords.put(" + n.getId() + "l, new double[]{" + Utils.round(p.getPosition().x) + ", " + Utils.round(p.getPosition().y) + "});");
                    }
                }
        );
    }

    @Override
    public void moveNodeGroup(Node n, int x, int y) {

    }


    @Override
    public Component getConsoleManager() {
        return consoleManager;
    }

    protected void drawNeuron(Neuron<?> n, double x, double y) {
        NeuronParticle np = graphManager.lookupParticle(n);
        np.updateNode(x, y);
    }

    protected void drawInputSynapses(Neuron<?> n) {
        n.getInputSynapses()
                .forEach(s ->
                        drawSynapse(s)
                );
    }

    protected void drawOutputSynapses(Neuron<?> n) {
        n.getOutputSynapses()
                .forEach(s ->
                        drawSynapse(s)
                );
    }

    protected Edge drawSynapse(Synapse s) {
        if(graphManager.getNode(s.getInput()) == null || graphManager.getNode(s.getOutput()) == null)
            return null;

        AbstractParticleLink pl = graphManager.lookupParticleLink(s);

        return pl.getEdge();
    }
}
