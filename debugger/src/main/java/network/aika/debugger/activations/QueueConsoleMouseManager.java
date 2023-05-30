package network.aika.debugger.activations;

import javax.swing.event.MouseInputListener;
import java.awt.event.MouseEvent;

public class QueueConsoleMouseManager implements MouseInputListener {

    private QueueConsole queueConsole;

    public QueueConsoleMouseManager(QueueConsole queueConsole) {
        this.queueConsole = queueConsole;
    }

    protected void doContextMenuPop(MouseEvent e) {
        QueueConsoleContextMenu menu = new QueueConsoleContextMenu(queueConsole);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger())
            doContextMenuPop(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger())
            doContextMenuPop(e);
    }


    @Override
    public void mouseClicked(MouseEvent e) {

    }


    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
