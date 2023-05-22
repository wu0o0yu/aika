package network.aika.debugger.activations;

import network.aika.debugger.AbstractGraphMouseManager;
import network.aika.debugger.AbstractViewManager;
import org.graphstream.ui.view.util.InteractiveElement;

import java.awt.event.MouseEvent;
import java.util.EnumSet;

public class ActivationGraphMouseManager extends AbstractGraphMouseManager {

    public ActivationGraphMouseManager(AbstractViewManager viewManager) {
        super(viewManager);
    }

    protected void doContextMenuPop(MouseEvent e) {
        ActivationGraphContextMenu menu = new ActivationGraphContextMenu();
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
}
