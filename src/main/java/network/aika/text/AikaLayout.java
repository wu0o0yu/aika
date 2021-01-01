package network.aika.text;

import org.graphstream.ui.layout.springbox.BarnesHutLayout;
import org.graphstream.ui.layout.springbox.NodeParticle;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;

public class AikaLayout extends SpringBox {

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
        return super.newNodeParticle(id);
    }
}
