package network.aika.debugger.neurons;

import javax.swing.*;

public class NeuronGraphContextMenu extends JPopupMenu {
    JMenuItem anItem;

    public NeuronGraphContextMenu() {
        anItem = new JMenuItem("Click Me!");
        add(anItem);
    }
}