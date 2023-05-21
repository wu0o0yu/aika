package network.aika.debugger;

import javax.swing.*;

public class GraphContextMenu extends JPopupMenu {
    JMenuItem anItem;

    public GraphContextMenu() {
        anItem = new JMenuItem("Click Me!");
        add(anItem);
    }
}