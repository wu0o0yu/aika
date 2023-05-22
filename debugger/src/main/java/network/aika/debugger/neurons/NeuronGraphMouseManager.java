package network.aika.debugger.neurons;

import network.aika.debugger.AbstractGraphMouseManager;
import network.aika.debugger.AbstractViewManager;
import network.aika.debugger.activations.ActivationGraphContextMenu;

import java.awt.event.MouseEvent;

public class NeuronGraphMouseManager extends AbstractGraphMouseManager {

    public NeuronGraphMouseManager(AbstractViewManager viewManager) {
        super(viewManager);
    }


    protected void doContextMenuPop(MouseEvent e) {
        NeuronGraphContextMenu menu = new NeuronGraphContextMenu();
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
}
