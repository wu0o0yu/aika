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
import org.graphstream.ui.graphicGraph.stylesheet.Style;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.camera.Camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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

//        System.setProperty("org.graphstream.ui", "org.graphstream.ui.swing.util.Display");

        graph = new SingleGraph("0");

        graph.setAttribute("ui.stylesheet",
                "node {" +
                    "size: 20px;" +
//                  "fill-color: #777;" +
//                  "text-mode: hidden;" +
                    "z-index: 1;" +
//                  "shadow-mode: gradient-radial; shadow-width: 2px; shadow-color: #999, white; shadow-offset: 3px, -3px;" +
                    "stroke-mode: plain; stroke-width: 2px;" +
                    "text-size: 20px;" +
                "}" +
                " edge {" +
                    "size: 2px;" +
                    "shape: cubic-curve;" +
                    "z-index: 0;" +
//                  "fill-color: #222;" +
                    "arrow-size: 8px, 5px;" +
                "}");

        graph.setAttribute("ui.antialias");
        graph.setAutoCreate(true);

        /*
                viewer = new SwingViewer(graph, SwingViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        add((DefaultView)viewer.addDefaultView(false, new SwingGraphRenderer()), BorderLayout.CENTER);


      //  viewer = graph.display(false);
//        viewer = new Viewer(graph,Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
//        viewer.disableAutoLayout();
        viewer.enableAutoLayout(new AikaLayout());

        //Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        viewer.getDefaultView().enableMouseOptions();
*/

        viewer = graph.display(false);
       //  Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_SWING_THREAD)

        viewer.getDefaultView().enableMouseOptions();

        viewer.computeGraphMetrics();

        ViewPanel view = (ViewPanel) viewer.getDefaultView();

        view.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!queue.isEmpty()) {
                    QueueEntry<?> qe = queue.pollFirst();
                    qe.process();

                    fromViewer.pump();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println();
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        Camera cam = view.getCamera();

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


    /*
    public void process(Model m) throws InterruptedException {
        while (!queue.isEmpty()) {
            fromViewer.pump(); // or fromViewer.blockingPump(); in the nightly builds

            QueueEntry<?> qe = queue.pollFirst();
            processEntry(qe);
        }
        m.addToN(length());
    }
*/

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

            if(act.getNeuron().isInputNeuron()) {
//                node.setAttribute("layout.frozen");
            }
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
