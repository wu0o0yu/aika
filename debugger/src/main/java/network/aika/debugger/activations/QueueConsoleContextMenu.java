package network.aika.debugger.activations;


import javax.swing.*;

public class QueueConsoleContextMenu extends JPopupMenu {

    JMenuItem sortKeyItem;

    public QueueConsoleContextMenu(QueueConsole queueConsole) {
        sortKeyItem = new JMenuItem(queueConsole.getSortKeyVisible().getInverted().getTxt() + " Sort Key");
        sortKeyItem.addActionListener(l -> {
                    queueConsole.setSortKeyVisible(
                            queueConsole.getSortKeyVisible().getInverted()
                    );
                    queueConsole.update();
                }
        );
        add(sortKeyItem);
    }
}