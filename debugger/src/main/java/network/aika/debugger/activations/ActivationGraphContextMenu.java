package network.aika.debugger.activations;

import javax.swing.*;

public class ActivationGraphContextMenu extends JPopupMenu {
    JMenuItem anItem;

    public ActivationGraphContextMenu() {
        anItem = new JMenuItem("Click Me!");
        add(anItem);
    }
}