package network.aika.debugger.graphics;

import org.graphstream.ui.geom.Point2;
import org.graphstream.ui.geom.Vector2;

import java.awt.geom.Path2D;


public class CubicCurveShape {

    Path2D.Double theShape = new Path2D.Double();

    public CubicCurveShape() {
    }

    public void makeSingle(double fromX, double fromY, double toX, double toY, double sox, double soy) {
        double fromx = fromX + sox;
        double fromy = fromY + soy;
        double tox = toX + sox;
        double toy = toY + soy;
        Vector2 mainDir = new Vector2(new Point2(fromX, fromY), new Point2(toX, toY));
        double length = mainDir.length();
        double angle = mainDir.y() / length;
        double c1x = 0.0;
        double c1y = 0.0;
        double c2x = 0.0;
        double c2y = 0.0;

        //     theEdge.

        if(mainDir.y() > 0.0) {
            double fromXDelta;
            double toXDelta;
            if(Math.abs(mainDir.x()) < 0.2) {
                fromXDelta = 0.5 * mainDir.x();
            } else {
                fromXDelta = 0.5 * Math.copySign(0.2, mainDir.x());
            }

            if(Math.abs(mainDir.x()) < 0.5) {
                toXDelta = 0.5 * mainDir.x();
            } else {
                toXDelta = 0.5 * Math.copySign(0.5, mainDir.x());
            }
            c1x = fromx + fromXDelta;
            c2x = tox - toXDelta;

            c1y = fromy + mainDir.y() / 2;
            c2y = toy - mainDir.y() / 2;
        } else {
            c1x = fromx + 0.5 * mainDir.x();
            c2x = tox - 0.5 * mainDir.x();

            c1y = fromy + 0.2;
            c2y = toy - 0.2;
        }

        theShape.reset();
        theShape.moveTo(fromx, fromy);
        theShape.curveTo(c1x, c1y, c2x, c2y, tox, toy);
    }

    public boolean contains(double x, double y, double dist) {
        return theShape.intersects(x - dist, y - dist, 2 * dist, 2 * dist);
    }
}
