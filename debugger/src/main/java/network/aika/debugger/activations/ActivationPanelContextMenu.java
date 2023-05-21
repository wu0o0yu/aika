package network.aika.debugger.activations;

import javax.swing.*;

public class ActivationPanelContextMenu extends JPopupMenu {

    ActivationPanel actPanel;

    JMenuItem currentVsSelectedItem;

    public ActivationPanelContextMenu(ActivationPanel actPanel) {
        this.actPanel = actPanel;

        currentVsSelectedItem = new JMenuItem("Selected");
        currentVsSelectedItem.addActionListener(l -> {
            currentVsSelectedItem.setText("Bla");
        });
        add(currentVsSelectedItem);
    }
}