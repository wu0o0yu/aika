package network.aika.text;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.phase.Phase;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

public class VisualizedDocument extends Document {

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    private Graph graph;

    public VisualizedDocument(String content) {
        super(content);
        graph = new SingleGraph("0");
    }

    @Override
    protected void processEntry(QueueEntry queueEntry) {
        queueEntry.process();
        if (queueEntry instanceof Link) {
            Link link = (Link) queueEntry;
            String inputId = "" + link.getInput().getId();
            String outputId = "" + link.getOutput().getId();
            String edgeId = inputId + "-" + outputId;
            Edge edge = graph.getEdge(edgeId);
            if (edge == null) {
                edge = graph.addEdge(edgeId, inputId, outputId);
            }

        } else if (queueEntry instanceof Activation) {
            Activation activation = (Activation) queueEntry;
            String id = "" + activation.getId();
            Node node = graph.getNode(id);

            if (node == null) {
                node = graph.addNode(id);
            }
            node.setAttribute("ui.label", activation.getLabel());
            Phase phase = queueEntry.getPhase();
            String color = phase == null ? "gray" : phase.getColor();
            node.setAttribute("ui.style", "fill-color: " + color + ";");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
