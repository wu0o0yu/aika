package network.aika.debugger.activations;


import javax.swing.*;

public class QueueConsoleContextMenu extends JPopupMenu {

    JMenuItem sortKeyItem;

    public QueueConsoleContextMenu(QueueConsole queueConsole) {
        sortKeyItem = new JMenuItem(queueConsole.getSortKey().getInverted().getTxt() + " Sort Key");
        sortKeyItem.addActionListener(l -> {
                    queueConsole.setSortKey(
                            queueConsole.getSortKey().getInverted()
                    );
                    queueConsole.update();
                }
        );
        add(sortKeyItem);
    }
}