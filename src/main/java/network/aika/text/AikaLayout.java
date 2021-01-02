package network.aika.text;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.layout.springbox.BarnesHutLayout;
import org.graphstream.ui.layout.springbox.NodeParticle;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.layout.springbox.implementations.SpringBoxNodeParticle;

public class AikaLayout extends SpringBox {

    Document doc;
    Graph graph;

    AikaLayout(Document doc, Graph g) {
        this.doc = doc;
        this.graph = g;
    }

    @Override
    public String getLayoutAlgorithmName() {
        return "AikaLayout";
    }

    @Override
    protected void chooseNodePosition(NodeParticle n0, NodeParticle n1) {
        super.chooseNodePosition(n0, n1);
    }

    @Override
    public NodeParticle newNodeParticle(String id) {
        Node n = graph.getNode(id);
        Activation act = doc.getActivation(n.getAttribute("aika.id", Integer.class));

        return new AikaParticle(act, this, id);
    }
}
