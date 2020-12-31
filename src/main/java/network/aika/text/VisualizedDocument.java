package network.aika.text;

import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.QueueEntry;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.neuron.phase.link.LinkPhase;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import static network.aika.neuron.activation.Fired.NOT_FIRED;

public class VisualizedDocument extends Document implements ViewerListener {

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    private Graph graph;
    private Viewer viewer;
    private ViewerPipe fromViewer;

    public VisualizedDocument(String content) {
        super(content);
        graph = new SingleGraph("0");

        graph.setAttribute("ui.stylesheet",
                "node {\n" +
                "\tsize: 20px;\n" +
//                "\tfill-color: #777;\n" +
//                "\ttext-mode: hidden;\n" +
                "\tz-index: 1;\n" +
//                "\tshadow-mode: gradient-radial; shadow-width: 2px; shadow-color: #999, white; shadow-offset: 3px, -3px;\n" +
                "\tstroke-mode: plain; stroke-width: 2px;\n" +
                "\ttext-size: 20px;\n" +
                "}\n" +
                "\n" +
                "edge {\n" +
                "\tsize: 2px;\n" +
                "\tshape: cubic-curve;\n" +
                "\tz-index: 0;\n" +
//                "\tfill-color: #222;\n" +
                "\tarrow-size: 8px, 5px;\n" +
                "}");

        graph.setAttribute("ui.antialias");
        graph.setAutoCreate(true);
        Viewer viewer = graph.display();
       //  Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_SWING_THREAD)

        viewer.getDefaultView().enableMouseOptions();

        // The default action when closing the view is to quit
        // the program.
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

        // We connect back the viewer to the graph,
        // the graph becomes a sink for the viewer.
        // We also install us as a viewer listener to
        // intercept the graphic events.
        fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(this);
        fromViewer.addSink(graph);
    }

    public void process(Model m) throws InterruptedException {
        while (!queue.isEmpty()) {
            fromViewer.pump(); // or fromViewer.blockingPump(); in the nightly builds

            QueueEntry<?> qe = queue.pollFirst();
            processEntry(qe);
        }
        m.addToN(length());
    }


    private void processEntry(QueueEntry queueEntry) {
        queueEntry.process();

        queueEntry.onProcessEvent();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onActivationEvent(Activation act) {
        Graph g = getGraph();
        String id = "" + act.getId();
        Node node = g.getNode(id);

        if (node == null) {
            node = g.addNode(id);
        }

        node.setAttribute("ui.label", act.getLabel());
        if(act.getFired() != NOT_FIRED) {
            Fired f = act.getFired();
            node.setAttribute("x", f.getInputTimestamp());
            node.setAttribute("y", f.getFired());
        }

        ActivationPhase phase = act.getPhase();
        if(phase != null) {
            phase.updateAttributes(node);
            act.getNeuron().updateAttributes(node);
        } else {
            node.setAttribute("ui.style", "stroke-color: gray;");
        }
    }

    @Override
    public void onLinkEvent(Link l) {
        String inputId = "" + l.getInput().getId();
        String outputId = "" + l.getOutput().getId();
        String edgeId = inputId + "-" + outputId;
        Edge edge = graph.getEdge(edgeId);
        if (edge == null) {
            edge = graph.addEdge(edgeId, inputId, outputId, true);
            l.getSynapse().updateAttributes(edge);
        }
        LinkPhase phase = l.getPhase();
        if(phase != null) {
            phase.updateAttributes(edge);
        }
    }

    public void viewClosed(String id) {
   //     loop = false;
    }

    public void buttonPushed(String id) {
        System.out.println("Button pushed on node "+id);
    }

    public void buttonReleased(String id) {
        System.out.println("Button released on node "+id);
    }

    public void mouseOver(String id) {
        System.out.println("Need the Mouse Options to be activated");
    }

    public void mouseLeft(String id) {
        System.out.println("Need the Mouse Options to be activated");
    }
}
