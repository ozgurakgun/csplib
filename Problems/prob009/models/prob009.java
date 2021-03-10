/*
 *  CSPLib prob009: Perfect Square Placement
 *  
 *  - The following code is written in Java using the JaCoP solver library (v4.4.0) and Apache Commons
 *  - The program at the end provides a Window to inspect the solution (+ mouse-wheel zoom)
 *  
 *  Arguments: <Problem instance, integer:[0,203]> <Complete Search, boolean>
 *  e.g. $java -classpath .;commons-lang3-*.jar;jacop-4.4.0.jar Solver 179 true
 *  
 *  Author: Theophilus Mouratides (github.com/thmour/)
 *  Date: 7-September-2016
 *  License: MIT
 * 
 */

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jacop.constraints.Constraint;
import org.jacop.constraints.Cumulative;
import org.jacop.constraints.Diff2;
import org.jacop.core.Domain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainMin;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.SimpleSelect;
import org.jacop.search.SmallestDomain;
import org.jacop.search.SmallestMin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Solver {
    public static final int window_size = 650;
    
    private static void Solve(Pair<Integer, Integer[]> data, boolean searchAll) {
        int N = data.getLeft();
        Integer[] S = data.getRight();
        int nsquares = S.length;
        Store store = new Store();

        IntVar[] X = new IntVar[nsquares];
        IntVar[] Y = new IntVar[nsquares];

        IntVar[] W = new IntVar[nsquares];
        IntVar[] H = new IntVar[nsquares];

        IntVar L = new IntVar(store, N, N);

        ArrayUtils.reverse(S);

        for (int i = 0; i < nsquares; i++) {
            X[i] = new IntVar(store, "X" + i, 0, N - S[i]);
            Y[i] = new IntVar(store, "Y" + i, 0, N - S[i]);

            W[i] = new IntVar(store, S[i], S[i]);
            H[i] = new IntVar(store, S[i], S[i]);
        }

        Constraint ctr1 = new Diff2(X, Y, W, H);
        Constraint ctr2 = new Cumulative(X, W, H, L);
        Constraint ctr3 = new Cumulative(Y, W, H, L);
        
        ctr1.impose(store);
        ctr2.impose(store);
        ctr3.impose(store);
        
        Search<IntVar> searchX = new DepthFirstSearch<IntVar>();
        Search<IntVar> searchY = new DepthFirstSearch<IntVar>();
        SelectChoicePoint<IntVar> labelX = new SimpleSelect<>(X, new SmallestMin<>(), new SmallestDomain<>(), new IndomainMin<>());
        SelectChoicePoint<IntVar> labelY = new SimpleSelect<>(Y, new SmallestMin<>(), new SmallestDomain<>(), new IndomainMin<>());
        searchY.setSelectChoicePoint(labelY);
        searchX.addChildSearch(searchY);
        
        if(searchAll)
            searchX.getSolutionListener().searchAll(true);
        
        searchX.getSolutionListener().recordSolutions(true);
        searchY.getSolutionListener().recordSolutions(true);
        searchX.setPrintInfo(false);
        searchY.setPrintInfo(false);
        searchX.labeling(store, labelX);
        for (int sid = 1; sid <= searchX.getSolutionListener().solutionsNo(); sid++) {
            SwingUtilities.invokeLater((new Solver()).new 
                    Window(sid-1, window_size, N, searchX.getSolution(sid), searchY.getSolution(sid), S));
        }
    }
    
    @SuppressWarnings("serial")
    public class Window extends JPanel implements Runnable {
        private AffineTransform tx = new AffineTransform();
        private final int window_size;
        private final int box_size;
        private final int wid;
        private Rectangle2D.Double[] rect;
        Color[] color;
        JLabel[] text;
        
        double adjust(Domain num) {
            return Double.valueOf(num.toString()) * window_size / box_size;
        }
        
        double adjust(Integer num) {
            return (double)num * window_size / box_size;
        }
        
        public Window(int id, int wsize, int bsize, Domain[] X, Domain[] Y, Integer[] S) {
            this.wid = id;
            this.window_size = wsize;
            this.box_size = bsize;
            this.rect = new Rectangle2D.Double[X.length];
            this.color = new Color[X.length];
            this.text = new JLabel[X.length];
            
            Random r = new Random();
            for(int i = 0; i < X.length; i++) {
                text[i] = new JLabel(S[i].toString(), SwingConstants.CENTER);
                rect[i] = new Rectangle2D.Double(adjust(X[i]), adjust(Y[i]), adjust(S[i]), adjust(S[i]));
                color[i] = new Color(
                    r.nextFloat() * 0.65f + 0.35f, 
                    r.nextFloat() * 0.65f + 0.35f,
                    r.nextFloat() * 0.65f + 0.35f
                );
            }
            
            this.addMouseWheelListener(new ZoomHandler());
        }
        
        @Override
        public void paint(Graphics g) {
            super.paint(g);
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape cshape;
            Rectangle crect;
            for(int i = 0; i < rect.length; i++) {
                cshape = tx.createTransformedShape(rect[i]);
                crect = cshape.getBounds();
                g2.setColor(color[i]);
                g2.fill(cshape);
                g2.setColor(new Color(0));
                g2.draw(cshape);
                text[i].setBounds(crect);
            }
        }
        
        @Override
        public void run() {
            JFrame f = new JFrame("Perfect Square Placement | Instance size: " + box_size);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            for(JLabel l : text) {
                f.getContentPane().add(l);
            }
            f.getContentPane().add(this);
            f.setBounds(wid * 50, wid * 50, window_size+7, window_size+30);
            f.setResizable(false);
            f.setVisible(true);
        }
        
        private class ZoomHandler implements MouseWheelListener {
            double scale = 1.0;
            Point2D p1 = null;
            Point2D p2 = null;
            
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                    if(scale == 1) {
                         p1 = e.getPoint();
                         try {
                             p2 = tx.inverseTransform(p1, null);
                         } catch (NoninvertibleTransformException ex) {
                         }
                    }
                    
                    double oldscale = scale;
                    scale -= (0.2 * e.getWheelRotation());
                    scale = Math.min(Math.max(1, scale), 20);
                    if(Math.abs(oldscale - scale) < 0.1) return;

                    tx.setToIdentity();
                    tx.translate(p1.getX(), p1.getY());
                    tx.scale(scale, scale);
                    tx.translate(-p2.getX(), -p2.getY());

                    Window.this.revalidate();
                    Window.this.repaint();
                }
            }
        }
    }

    public static void main(String[] args) {
        @SuppressWarnings("unchecked")
        Pair<Integer, Integer[]>[] instances = new ImmutablePair[] {
            new ImmutablePair<>(112, new Integer[] { 2, 4, 6, 7, 8, 9, 11, 15, 16, 17, 18, 19, 24, 25, 27, 29, 33, 35, 37, 42, 50 }),
            new ImmutablePair<>(110, new Integer[] { 2, 3, 4, 6, 7, 8, 12, 13, 14, 15, 16, 17, 18, 21, 22, 23, 24, 26, 27, 28, 50, 60 }),
            new ImmutablePair<>(110, new Integer[] { 1, 2, 3, 4, 6, 8, 9, 12, 14, 16, 17, 18, 19, 21, 22, 23, 24, 26, 27, 28, 50, 60 }),
            new ImmutablePair<>(139, new Integer[] { 1, 2, 3, 4, 7, 8, 10, 17, 18, 20, 21, 22, 24, 27, 28, 29, 30, 31, 32, 38, 59, 80 }),
            new ImmutablePair<>(147, new Integer[] { 1, 3, 4, 5, 8, 9, 17, 20, 21, 23, 25, 26, 29, 31, 32, 40, 43, 44, 47, 48, 52, 55 }),
            new ImmutablePair<>(147, new Integer[] { 2, 4, 8, 10, 11, 12, 15, 19, 21, 22, 23, 25, 26, 32, 34, 37, 41, 43, 45, 47, 55, 59 }),
            new ImmutablePair<>(154, new Integer[] { 2, 5, 9, 11, 16, 17, 19, 21, 22, 24, 26, 30, 31, 33, 35, 36, 41, 46, 47, 50, 52, 61 }),
            new ImmutablePair<>(172, new Integer[] { 1, 2, 3, 4, 9, 11, 13, 16, 17, 18, 19, 22, 24, 33, 36, 38, 39, 42, 44, 53, 75, 97 }),
            new ImmutablePair<>(192, new Integer[] { 4, 8, 9, 10, 12, 14, 17, 19, 26, 28, 31, 35, 36, 37, 41, 47, 49, 57, 59, 62, 71, 86 }),
            new ImmutablePair<>(110, new Integer[] { 1, 2, 3, 4, 5, 7, 8, 10, 12, 13, 14, 15, 16, 19, 21, 28, 29, 31, 32, 37, 38, 41, 44 }),
            new ImmutablePair<>(139, new Integer[] { 1, 2, 7, 8, 12, 13, 14, 15, 16, 18, 19, 20, 21, 22, 24, 26, 27, 28, 32, 33, 38, 59, 80 }),
            new ImmutablePair<>(140, new Integer[] { 1, 2, 3, 4, 5, 8, 10, 13, 16, 19, 20, 23, 27, 28, 29, 31, 33, 38, 42, 45, 48, 53, 54 }),
            new ImmutablePair<>(140, new Integer[] { 2, 3, 4, 7, 8, 9, 12, 15, 16, 18, 22, 23, 24, 26, 28, 30, 33, 36, 43, 44, 47, 50, 60 }),
            new ImmutablePair<>(145, new Integer[] { 1, 2, 3, 4, 6, 8, 9, 12, 15, 20, 22, 24, 25, 26, 27, 29, 30, 31, 32, 34, 36, 61, 84 }),
            new ImmutablePair<>(180, new Integer[] { 2, 4, 8, 10, 11, 12, 15, 19, 21, 22, 23, 25, 26, 32, 33, 34, 37, 41, 43, 45, 47, 88, 92 }),
            new ImmutablePair<>(188, new Integer[] { 2, 4, 8, 10, 11, 12, 15, 19, 21, 22, 23, 25, 26, 32, 33, 34, 37, 45, 47, 49, 51, 92, 96 }),
            new ImmutablePair<>(208, new Integer[] { 1, 3, 4, 9, 10, 11, 12, 16, 17, 18, 22, 23, 24, 40, 41, 60, 62, 65, 67, 70, 71, 73, 75 }),
            new ImmutablePair<>(215, new Integer[] { 1, 3, 4, 9, 10, 11, 12, 16, 17, 18, 22, 23, 24, 40, 41, 60, 66, 68, 70, 71, 74, 76, 79 }),
            new ImmutablePair<>(228, new Integer[] { 2, 7, 9, 10, 15, 16, 17, 18, 22, 23, 25, 28, 36, 39, 42, 56, 57, 68, 69, 72, 73, 87, 99 }),
            new ImmutablePair<>(257, new Integer[] { 2, 3, 9, 11, 14, 15, 17, 20, 22, 24, 28, 29, 32, 33, 49, 55, 57, 60, 63, 66, 79, 123, 134 }),
            new ImmutablePair<>(332, new Integer[] { 1, 15, 17, 24, 26, 30, 31, 38, 47, 48, 49, 50, 53, 56, 58, 68, 83, 89, 91, 112, 120, 123, 129 }),
            new ImmutablePair<>(120, new Integer[] { 3, 4, 5, 6, 8, 9, 10, 12, 13, 14, 15, 16, 17, 19, 20, 23, 25, 32, 33, 34, 40, 41, 46, 47 }),
            new ImmutablePair<>(186, new Integer[] { 2, 3, 4, 7, 8, 9, 12, 15, 16, 18, 22, 23, 24, 26, 28, 30, 33, 36, 43, 46, 47, 60, 90, 96 }),
            new ImmutablePair<>(194, new Integer[] { 2, 3, 7, 9, 10, 16, 17, 18, 19, 20, 23, 25, 28, 34, 36, 37, 42, 53, 54, 61, 65, 68, 69, 72 }),
            new ImmutablePair<>(195, new Integer[] { 2, 4, 7, 10, 11, 16, 17, 18, 21, 26, 27, 30, 39, 41, 42, 45, 47, 49, 52, 53, 54, 61, 63, 80 }),
            new ImmutablePair<>(196, new Integer[] { 1, 2, 5, 10, 11, 15, 17, 18, 20, 21, 24, 26, 29, 31, 32, 34, 36, 40, 44, 47, 48, 51, 91, 105 }),
            new ImmutablePair<>(201, new Integer[] { 1, 3, 4, 6, 9, 10, 11, 12, 17, 18, 20, 21, 22, 23, 26, 38, 40, 46, 50, 52, 53, 58, 98, 103 }),
            new ImmutablePair<>(201, new Integer[] { 1, 4, 5, 8, 9, 10, 11, 15, 16, 18, 19, 20, 22, 24, 26, 39, 42, 44, 49, 52, 54, 56, 93, 108 }),
            new ImmutablePair<>(203, new Integer[] { 1, 2, 5, 10, 11, 15, 17, 18, 20, 21, 24, 26, 29, 31, 32, 34, 36, 40, 44, 48, 54, 58, 98, 105 }),
            new ImmutablePair<>(247, new Integer[] { 3, 5, 6, 9, 12, 14, 19, 23, 24, 25, 28, 32, 34, 36, 40, 45, 46, 48, 56, 62, 63, 66, 111, 136 }),
            new ImmutablePair<>(253, new Integer[] { 2, 4, 5, 9, 13, 18, 20, 23, 24, 27, 28, 31, 38, 40, 44, 50, 61, 70, 72, 77, 79, 86, 88, 104 }),
            new ImmutablePair<>(255, new Integer[] { 3, 5, 10, 11, 16, 17, 20, 22, 23, 25, 26, 27, 28, 32, 41, 44, 52, 53, 59, 63, 65, 74, 118, 137 }),
            new ImmutablePair<>(288, new Integer[] { 2, 7, 9, 10, 15, 16, 17, 18, 22, 23, 25, 28, 36, 39, 42, 56, 57, 60, 68, 72, 73, 87, 129, 159 }),
            new ImmutablePair<>(288, new Integer[] { 1, 5, 7, 8, 9, 14, 17, 20, 21, 26, 30, 32, 34, 36, 48, 51, 54, 59, 64, 69, 72, 93, 123, 165 }),
            new ImmutablePair<>(290, new Integer[] { 2, 3, 8, 9, 11, 12, 14, 17, 21, 30, 31, 33, 40, 42, 45, 48, 59, 61, 63, 65, 82, 84, 124, 166 }),
            new ImmutablePair<>(292, new Integer[] { 1, 2, 3, 8, 12, 15, 16, 17, 20, 22, 24, 26, 29, 33, 44, 54, 57, 60, 63, 67, 73, 102, 117, 175 }),
            new ImmutablePair<>(304, new Integer[] { 3, 5, 7, 11, 12, 17, 20, 22, 25, 29, 35, 47, 48, 55, 56, 57, 69, 72, 76, 92, 96, 100, 116, 132 }),
            new ImmutablePair<>(304, new Integer[] { 3, 4, 7, 12, 16, 20, 23, 24, 27, 28, 30, 32, 33, 36, 37, 44, 53, 57, 72, 76, 85, 99, 129, 175 }),
            new ImmutablePair<>(314, new Integer[] { 2, 4, 11, 12, 16, 17, 18, 19, 28, 29, 40, 44, 47, 59, 62, 64, 65, 78, 79, 96, 97, 105, 113, 139 }),
            new ImmutablePair<>(316, new Integer[] { 3, 9, 10, 12, 13, 14, 15, 23, 24, 33, 36, 37, 48, 52, 54, 55, 57, 65, 66, 78, 79, 93, 144, 172 }),
            new ImmutablePair<>(326, new Integer[] { 1, 6, 10, 11, 14, 15, 18, 24, 29, 32, 43, 44, 53, 56, 63, 65, 71, 80, 83, 101, 104, 106, 119, 142 }),
            new ImmutablePair<>(423, new Integer[] { 2, 9, 15, 17, 27, 29, 31, 32, 33, 36, 47, 49, 50, 60, 62, 77, 105, 114, 123, 127, 128, 132, 168, 186 }),
            new ImmutablePair<>(435, new Integer[] { 1, 2, 8, 10, 13, 19, 23, 33, 44, 45, 56, 74, 76, 78, 80, 88, 93, 100, 112, 131, 142, 143, 150, 192 }),
            new ImmutablePair<>(435, new Integer[] { 3, 5, 9, 11, 12, 21, 24, 27, 30, 44, 45, 50, 54, 55, 63, 95, 101, 112, 117, 123, 134, 140, 178, 200 }),
            new ImmutablePair<>(459, new Integer[] { 8, 9, 10, 11, 16, 30, 36, 38, 45, 55, 57, 65, 68, 84, 95, 98, 100, 116, 117, 126, 135, 144, 180, 198 }),
            new ImmutablePair<>(459, new Integer[] { 4, 6, 9, 10, 17, 21, 23, 25, 31, 33, 36, 38, 45, 50, 83, 115, 117, 126, 133, 135, 144, 146, 180, 198 }),
            new ImmutablePair<>(479, new Integer[] { 5, 6, 17, 23, 24, 26, 28, 29, 35, 43, 44, 52, 60, 68, 77, 86, 130, 140, 150, 155, 160, 164, 174, 175 }),
            new ImmutablePair<>(147, new Integer[] { 3, 4, 5, 6, 8, 9, 10, 12, 13, 14, 15, 16, 17, 19, 20, 23, 25, 27, 32, 33, 34, 40, 41, 73, 74 }),
            new ImmutablePair<>(208, new Integer[] { 1, 2, 3, 4, 5, 7, 8, 11, 12, 17, 18, 24, 26, 28, 29, 30, 36, 39, 44, 45, 50, 59, 60, 89, 119 }),
            new ImmutablePair<>(213, new Integer[] { 3, 5, 6, 7, 13, 16, 17, 20, 21, 23, 24, 25, 26, 28, 31, 35, 36, 47, 49, 56, 58, 74, 76, 81, 90 }),
            new ImmutablePair<>(215, new Integer[] { 1, 4, 6, 7, 11, 15, 24, 26, 27, 33, 37, 39, 40, 41, 42, 43, 45, 47, 51, 55, 60, 62, 63, 69, 83 }),
            new ImmutablePair<>(216, new Integer[] { 1, 2, 3, 4, 5, 7, 8, 11, 16, 17, 18, 19, 25, 30, 32, 33, 39, 41, 45, 49, 54, 59, 64, 103, 113 }),
            new ImmutablePair<>(236, new Integer[] { 1, 2, 4, 9, 11, 12, 13, 14, 15, 16, 19, 24, 38, 40, 44, 46, 47, 48, 59, 64, 65, 70, 81, 85, 107 }),
            new ImmutablePair<>(242, new Integer[] { 1, 3, 6, 7, 9, 13, 14, 16, 17, 19, 23, 25, 26, 28, 30, 31, 47, 51, 54, 57, 60, 64, 67, 111, 131 }),
            new ImmutablePair<>(244, new Integer[] { 1, 2, 4, 5, 7, 10, 15, 17, 19, 20, 21, 22, 26, 27, 30, 37, 40, 41, 45, 65, 66, 68, 70, 110, 134 }),
            new ImmutablePair<>(252, new Integer[] { 4, 7, 10, 11, 12, 13, 23, 25, 29, 31, 32, 34, 36, 37, 38, 40, 42, 44, 62, 67, 68, 71, 77, 108, 113 }),
            new ImmutablePair<>(253, new Integer[] { 2, 4, 5, 6, 9, 10, 12, 14, 20, 24, 27, 35, 36, 37, 38, 42, 43, 45, 50, 54, 63, 66, 70, 120, 133 }),
            new ImmutablePair<>(260, new Integer[] { 1, 4, 6, 7, 10, 15, 24, 26, 27, 28, 29, 31, 33, 34, 37, 38, 44, 65, 70, 71, 77, 78, 83, 100, 112 }),
            new ImmutablePair<>(264, new Integer[] { 3, 7, 8, 12, 16, 18, 19, 20, 22, 24, 26, 31, 34, 37, 38, 40, 42, 53, 54, 61, 64, 69, 70, 130, 134 }),
            new ImmutablePair<>(264, new Integer[] { 3, 8, 12, 13, 16, 18, 20, 21, 22, 24, 26, 29, 34, 38, 40, 42, 43, 47, 54, 59, 64, 70, 71, 130, 134 }),
            new ImmutablePair<>(264, new Integer[] { 1, 3, 4, 6, 9, 10, 11, 12, 16, 17, 18, 20, 21, 22, 39, 42, 54, 56, 61, 66, 68, 69, 73, 129, 135 }),
            new ImmutablePair<>(265, new Integer[] { 1, 3, 4, 6, 9, 10, 11, 12, 16, 17, 18, 20, 21, 22, 39, 42, 54, 56, 62, 66, 68, 69, 74, 130, 135 }),
            new ImmutablePair<>(273, new Integer[] { 1, 4, 8, 10, 11, 12, 17, 19, 21, 22, 27, 29, 30, 33, 37, 43, 52, 62, 65, 86, 88, 89, 91, 96, 120 }),
            new ImmutablePair<>(273, new Integer[] { 1, 6, 9, 14, 16, 17, 18, 21, 22, 23, 25, 31, 32, 38, 44, 46, 48, 50, 54, 62, 65, 68, 78, 133, 140 }),
            new ImmutablePair<>(275, new Integer[] { 2, 3, 7, 13, 17, 24, 25, 31, 33, 34, 35, 37, 41, 49, 51, 53, 55, 60, 68, 71, 74, 81, 94, 100, 107 }),
            new ImmutablePair<>(276, new Integer[] { 1, 5, 8, 9, 11, 18, 19, 21, 30, 36, 41, 44, 45, 46, 47, 51, 53, 58, 63, 69, 71, 84, 87, 105, 120 }),
            new ImmutablePair<>(280, new Integer[] { 5, 6, 11, 17, 18, 20, 21, 24, 27, 28, 32, 34, 41, 42, 50, 53, 54, 55, 68, 78, 85, 88, 95, 97, 117 }),
            new ImmutablePair<>(280, new Integer[] { 2, 3, 7, 8, 14, 18, 30, 36, 37, 39, 44, 50, 52, 54, 56, 60, 63, 64, 65, 72, 75, 78, 79, 96, 106 }),
            new ImmutablePair<>(284, new Integer[] { 1, 2, 11, 12, 14, 16, 18, 19, 23, 26, 29, 37, 38, 39, 40, 42, 59, 68, 69, 77, 78, 97, 106, 109, 110 }),
            new ImmutablePair<>(286, new Integer[] { 1, 4, 5, 7, 10, 12, 15, 16, 20, 23, 28, 30, 32, 33, 35, 37, 53, 54, 64, 68, 74, 79, 80, 133, 153 }),
            new ImmutablePair<>(289, new Integer[] { 2, 3, 5, 8, 13, 14, 17, 20, 21, 32, 36, 41, 50, 52, 60, 61, 62, 68, 74, 76, 83, 87, 100, 102, 104 }),
            new ImmutablePair<>(289, new Integer[] { 2, 3, 4, 5, 7, 12, 16, 17, 19, 21, 23, 25, 29, 31, 32, 44, 57, 64, 65, 68, 72, 76, 84, 140, 149 }),
            new ImmutablePair<>(290, new Integer[] { 1, 2, 10, 11, 13, 14, 15, 17, 18, 28, 29, 34, 36, 38, 50, 56, 60, 69, 77, 80, 85, 91, 94, 111, 119 }),
            new ImmutablePair<>(293, new Integer[] { 5, 6, 11, 17, 18, 20, 21, 24, 27, 28, 32, 34, 41, 42, 50, 54, 55, 66, 68, 78, 85, 88, 95, 110, 130 }),
            new ImmutablePair<>(297, new Integer[] { 2, 7, 8, 9, 10, 15, 16, 17, 18, 23, 25, 26, 28, 36, 38, 43, 53, 60, 61, 68, 69, 77, 99, 137, 160 }),
            new ImmutablePair<>(308, new Integer[] { 1, 3, 4, 7, 10, 12, 13, 23, 25, 34, 37, 38, 39, 43, 44, 45, 62, 77, 79, 85, 87, 108, 113, 115, 116 }),
            new ImmutablePair<>(308, new Integer[] { 1, 5, 6, 7, 8, 9, 13, 16, 19, 28, 33, 36, 38, 43, 45, 48, 70, 71, 73, 84, 86, 102, 104, 120, 133 }),
            new ImmutablePair<>(309, new Integer[] { 7, 8, 14, 16, 23, 24, 25, 26, 31, 33, 34, 39, 48, 56, 59, 60, 62, 70, 76, 82, 92, 100, 101, 108, 117 }),
            new ImmutablePair<>(311, new Integer[] { 2, 7, 8, 9, 10, 15, 16, 17, 18, 23, 25, 26, 28, 36, 38, 43, 53, 60, 61, 68, 83, 91, 99, 151, 160 }),
            new ImmutablePair<>(314, new Integer[] { 1, 6, 7, 11, 16, 22, 26, 29, 32, 36, 38, 44, 51, 53, 64, 69, 70, 73, 74, 75, 85, 87, 101, 116, 128 }),
            new ImmutablePair<>(316, new Integer[] { 1, 3, 9, 12, 21, 26, 30, 33, 34, 35, 38, 39, 40, 41, 53, 56, 59, 69, 79, 85, 96, 103, 111, 117, 120 }),
            new ImmutablePair<>(317, new Integer[] { 1, 5, 6, 7, 8, 9, 16, 17, 19, 32, 37, 40, 42, 47, 49, 52, 59, 75, 81, 92, 94, 110, 112, 113, 126 }),
            new ImmutablePair<>(320, new Integer[] { 2, 7, 8, 9, 12, 14, 15, 21, 23, 35, 38, 44, 46, 49, 53, 54, 56, 63, 96, 101, 103, 105, 108, 112, 116 }),
            new ImmutablePair<>(320, new Integer[] { 3, 8, 9, 11, 17, 18, 22, 25, 26, 27, 29, 30, 31, 33, 35, 49, 51, 67, 72, 73, 80, 85, 95, 152, 168 }),
            new ImmutablePair<>(320, new Integer[] { 1, 4, 6, 7, 8, 13, 14, 16, 24, 28, 30, 33, 34, 38, 41, 42, 57, 60, 69, 78, 81, 90, 92, 150, 170 }),
            new ImmutablePair<>(320, new Integer[] { 3, 4, 6, 8, 9, 14, 15, 16, 24, 28, 30, 31, 34, 38, 39, 42, 59, 60, 71, 78, 79, 90, 92, 150, 170 }),
            new ImmutablePair<>(322, new Integer[] { 3, 4, 8, 9, 10, 16, 18, 20, 22, 23, 24, 28, 31, 38, 44, 47, 64, 65, 68, 76, 80, 81, 97, 144, 178 }),
            new ImmutablePair<>(322, new Integer[] { 3, 4, 8, 10, 15, 16, 18, 19, 20, 22, 24, 28, 35, 38, 44, 53, 59, 64, 68, 76, 80, 85, 93, 144, 178 }),
            new ImmutablePair<>(323, new Integer[] { 2, 3, 4, 7, 10, 13, 15, 18, 23, 32, 34, 35, 36, 42, 46, 50, 57, 60, 66, 72, 78, 87, 98, 159, 164 }),
            new ImmutablePair<>(323, new Integer[] { 3, 8, 9, 11, 17, 18, 22, 25, 26, 27, 29, 30, 31, 33, 35, 49, 51, 67, 72, 73, 83, 88, 95, 155, 168 }),
            new ImmutablePair<>(323, new Integer[] { 2, 6, 9, 11, 13, 14, 18, 19, 20, 23, 27, 28, 29, 42, 46, 48, 60, 64, 72, 74, 79, 82, 98, 146, 177 }),
            new ImmutablePair<>(325, new Integer[] { 3, 5, 6, 11, 12, 13, 18, 23, 25, 28, 32, 37, 40, 43, 45, 46, 51, 79, 92, 99, 103, 108, 112, 114, 134 }),
            new ImmutablePair<>(326, new Integer[] { 1, 4, 8, 10, 12, 16, 21, 22, 24, 27, 28, 35, 36, 37, 38, 46, 49, 68, 70, 75, 88, 90, 93, 158, 168 }),
            new ImmutablePair<>(327, new Integer[] { 2, 9, 10, 12, 13, 16, 19, 21, 23, 26, 36, 44, 46, 52, 55, 61, 62, 74, 84, 87, 100, 103, 104, 120, 140 }),
            new ImmutablePair<>(328, new Integer[] { 2, 3, 4, 7, 8, 10, 14, 17, 26, 27, 28, 36, 38, 40, 42, 45, 53, 58, 73, 74, 79, 94, 102, 152, 176 }),
            new ImmutablePair<>(334, new Integer[] { 1, 4, 8, 10, 12, 16, 21, 22, 24, 27, 28, 35, 36, 37, 38, 46, 49, 68, 75, 78, 88, 93, 98, 166, 168 }),
            new ImmutablePair<>(336, new Integer[] { 2, 3, 4, 7, 8, 10, 14, 17, 26, 27, 28, 36, 38, 40, 45, 50, 53, 58, 73, 74, 79, 94, 110, 152, 184 }),
            new ImmutablePair<>(338, new Integer[] { 1, 4, 8, 10, 12, 16, 19, 22, 24, 25, 28, 36, 37, 38, 39, 46, 53, 68, 70, 73, 94, 96, 101, 164, 174 }),
            new ImmutablePair<>(338, new Integer[] { 4, 5, 8, 10, 12, 15, 16, 21, 22, 24, 28, 33, 36, 38, 43, 46, 57, 68, 70, 77, 94, 96, 97, 164, 174 }),
            new ImmutablePair<>(340, new Integer[] { 1, 4, 5, 6, 11, 13, 16, 17, 22, 24, 44, 46, 50, 51, 52, 53, 61, 64, 66, 79, 84, 85, 92, 169, 171 }),
            new ImmutablePair<>(344, new Integer[] { 2, 3, 8, 11, 14, 17, 19, 21, 23, 25, 27, 36, 39, 44, 48, 53, 56, 71, 77, 83, 86, 89, 98, 169, 175 }),
            new ImmutablePair<>(359, new Integer[] { 7, 8, 9, 10, 14, 17, 18, 23, 25, 27, 29, 31, 40, 41, 43, 46, 69, 74, 82, 85, 90, 98, 102, 172, 187 }),
            new ImmutablePair<>(361, new Integer[] { 2, 6, 7, 8, 9, 14, 20, 22, 26, 27, 32, 34, 36, 47, 49, 56, 66, 67, 74, 82, 89, 98, 107, 156, 205 }),
            new ImmutablePair<>(363, new Integer[] { 1, 4, 6, 12, 13, 20, 21, 25, 26, 27, 28, 32, 37, 41, 45, 53, 58, 64, 69, 91, 97, 102, 106, 155, 208 }),
            new ImmutablePair<>(364, new Integer[] { 2, 3, 4, 6, 8, 9, 13, 14, 16, 19, 23, 24, 28, 29, 52, 57, 64, 75, 82, 91, 98, 100, 109, 173, 191 }),
            new ImmutablePair<>(367, new Integer[] { 1, 4, 6, 12, 13, 20, 21, 25, 26, 27, 28, 32, 37, 41, 49, 53, 58, 64, 69, 91, 97, 102, 110, 155, 212 }),
            new ImmutablePair<>(368, new Integer[] { 1, 6, 15, 16, 17, 18, 22, 25, 31, 33, 39, 42, 45, 46, 47, 48, 51, 69, 72, 88, 91, 96, 112, 160, 208 }),
            new ImmutablePair<>(371, new Integer[] { 1, 2, 7, 8, 20, 21, 22, 24, 26, 28, 30, 38, 43, 46, 50, 51, 64, 65, 70, 90, 95, 102, 109, 160, 211 }),
            new ImmutablePair<>(373, new Integer[] { 3, 6, 7, 8, 15, 17, 22, 23, 31, 32, 35, 41, 43, 60, 62, 68, 79, 87, 104, 105, 114, 120, 121, 138, 148 }),
            new ImmutablePair<>(378, new Integer[] { 2, 3, 10, 17, 18, 20, 21, 22, 24, 27, 31, 38, 41, 48, 51, 56, 68, 78, 80, 85, 87, 96, 117, 165, 213 }),
            new ImmutablePair<>(378, new Integer[] { 1, 2, 7, 13, 15, 17, 18, 25, 27, 29, 30, 31, 42, 43, 46, 56, 61, 68, 73, 93, 100, 105, 112, 161, 217 }),
            new ImmutablePair<>(380, new Integer[] { 4, 7, 17, 18, 19, 20, 21, 26, 31, 33, 35, 40, 45, 48, 49, 60, 67, 73, 79, 81, 87, 107, 113, 186, 194 }),
            new ImmutablePair<>(380, new Integer[] { 4, 5, 6, 9, 13, 15, 16, 17, 22, 24, 33, 38, 44, 49, 50, 56, 60, 67, 82, 84, 95, 108, 121, 177, 203 }),
            new ImmutablePair<>(381, new Integer[] { 12, 13, 21, 23, 25, 27, 35, 36, 42, 45, 54, 57, 59, 60, 79, 82, 84, 85, 92, 95, 96, 100, 110, 111, 186 }),
            new ImmutablePair<>(384, new Integer[] { 1, 4, 8, 9, 11, 12, 19, 21, 27, 32, 35, 44, 45, 46, 47, 51, 60, 67, 84, 89, 96, 108, 120, 180, 204 }),
            new ImmutablePair<>(384, new Integer[] { 1, 4, 8, 9, 11, 12, 15, 17, 19, 25, 26, 31, 32, 37, 44, 57, 60, 81, 84, 96, 99, 108, 120, 180, 204 }),
            new ImmutablePair<>(384, new Integer[] { 3, 5, 7, 11, 12, 17, 20, 22, 25, 29, 35, 47, 48, 55, 56, 57, 69, 72, 76, 80, 96, 100, 116, 172, 212 }),
            new ImmutablePair<>(385, new Integer[] { 1, 2, 7, 13, 15, 17, 18, 25, 27, 29, 30, 31, 43, 46, 49, 56, 61, 68, 73, 93, 100, 105, 119, 161, 224 }),
            new ImmutablePair<>(392, new Integer[] { 4, 7, 8, 15, 23, 26, 29, 30, 31, 32, 34, 43, 48, 55, 56, 68, 77, 88, 98, 106, 116, 135, 141, 151, 153 }),
            new ImmutablePair<>(392, new Integer[] { 10, 12, 14, 16, 19, 21, 25, 27, 31, 35, 39, 41, 51, 52, 54, 55, 73, 92, 98, 115, 121, 123, 129, 148, 171 }),
            new ImmutablePair<>(392, new Integer[] { 1, 4, 5, 8, 11, 14, 16, 21, 22, 24, 27, 28, 30, 31, 52, 64, 81, 83, 96, 97, 98, 99, 114, 195, 197 }),
            new ImmutablePair<>(393, new Integer[] { 4, 8, 16, 20, 23, 24, 25, 27, 29, 37, 44, 45, 50, 53, 64, 66, 68, 69, 73, 85, 91, 101, 116, 186, 207 }),
            new ImmutablePair<>(396, new Integer[] { 1, 4, 5, 14, 16, 32, 35, 36, 46, 47, 48, 49, 68, 69, 73, 93, 94, 97, 99, 104, 110, 111, 125, 126, 160 }),
            new ImmutablePair<>(396, new Integer[] { 1, 4, 5, 8, 11, 14, 16, 21, 22, 24, 27, 28, 30, 31, 52, 64, 81, 83, 98, 99, 100, 101, 114, 197, 199 }),
            new ImmutablePair<>(396, new Integer[] { 3, 8, 9, 11, 14, 16, 17, 18, 31, 32, 41, 45, 48, 56, 60, 66, 73, 75, 81, 82, 98, 99, 117, 180, 216 }),
            new ImmutablePair<>(398, new Integer[] { 2, 6, 7, 11, 15, 17, 23, 28, 29, 39, 44, 46, 53, 56, 58, 65, 68, 99, 100, 119, 120, 134, 144, 145, 154 }),
            new ImmutablePair<>(400, new Integer[] { 3, 6, 21, 23, 24, 26, 29, 35, 37, 40, 41, 47, 53, 55, 64, 76, 79, 81, 99, 100, 121, 122, 137, 142, 179 }),
            new ImmutablePair<>(404, new Integer[] { 3, 6, 7, 14, 17, 20, 21, 26, 28, 31, 32, 39, 46, 53, 54, 68, 71, 80, 88, 92, 100, 111, 113, 199, 205 }),
            new ImmutablePair<>(404, new Integer[] { 4, 7, 10, 11, 12, 13, 16, 18, 20, 23, 25, 28, 29, 32, 47, 62, 70, 88, 93, 96, 101, 114, 127, 189, 215 }),
            new ImmutablePair<>(408, new Integer[] { 2, 3, 7, 13, 16, 18, 20, 27, 30, 33, 41, 43, 46, 52, 54, 57, 72, 79, 84, 100, 105, 108, 116, 195, 213 }),
            new ImmutablePair<>(412, new Integer[] { 3, 11, 12, 15, 21, 26, 32, 39, 43, 47, 54, 60, 68, 73, 83, 85, 86, 87, 89, 99, 114, 129, 139, 144, 169 }),
            new ImmutablePair<>(413, new Integer[] { 5, 7, 17, 20, 34, 38, 39, 48, 56, 57, 59, 60, 64, 65, 70, 72, 75, 81, 105, 106, 110, 125, 148, 153, 155 }),
            new ImmutablePair<>(416, new Integer[] { 2, 4, 7, 11, 13, 24, 25, 30, 35, 37, 39, 40, 44, 58, 62, 65, 82, 104, 112, 120, 128, 135, 143, 153, 169 }),
            new ImmutablePair<>(416, new Integer[] { 1, 2, 3, 8, 12, 15, 16, 17, 20, 22, 24, 26, 29, 31, 64, 75, 85, 88, 91, 94, 98, 104, 133, 179, 237 }),
            new ImmutablePair<>(421, new Integer[] { 1, 2, 4, 5, 7, 9, 12, 16, 20, 22, 23, 35, 38, 48, 56, 83, 94, 104, 116, 118, 128, 140, 150, 153, 177 }),
            new ImmutablePair<>(421, new Integer[] { 5, 11, 12, 17, 18, 20, 23, 26, 29, 36, 38, 40, 44, 51, 55, 59, 72, 92, 97, 102, 105, 107, 117, 199, 222 }),
            new ImmutablePair<>(422, new Integer[] { 2, 4, 7, 13, 16, 18, 20, 23, 28, 29, 38, 43, 46, 51, 59, 68, 74, 79, 86, 93, 100, 111, 132, 179, 243 }),
            new ImmutablePair<>(425, new Integer[] { 3, 4, 5, 9, 10, 12, 13, 14, 16, 19, 20, 31, 46, 48, 56, 79, 102, 104, 116, 126, 128, 140, 142, 157, 181 }),
            new ImmutablePair<>(441, new Integer[] { 5, 6, 7, 16, 18, 23, 24, 27, 38, 39, 47, 51, 52, 62, 66, 72, 80, 84, 92, 101, 102, 118, 120, 219, 222 }),
            new ImmutablePair<>(454, new Integer[] { 1, 2, 11, 17, 29, 34, 35, 46, 48, 51, 53, 55, 63, 69, 79, 87, 88, 91, 109, 134, 136, 143, 150, 161, 184 }),
            new ImmutablePair<>(456, new Integer[] { 5, 7, 10, 11, 13, 15, 18, 19, 31, 49, 50, 52, 59, 60, 63, 72, 77, 115, 128, 129, 135, 142, 148, 179, 193 }),
            new ImmutablePair<>(465, new Integer[] { 6, 9, 13, 14, 19, 21, 24, 25, 31, 32, 53, 56, 64, 73, 74, 82, 91, 111, 125, 127, 137, 139, 153, 173, 201 }),
            new ImmutablePair<>(472, new Integer[] { 7, 9, 13, 15, 26, 34, 35, 44, 47, 51, 58, 61, 65, 81, 87, 103, 104, 115, 118, 123, 128, 133, 136, 148, 221 }),
            new ImmutablePair<>(477, new Integer[] { 3, 5, 12, 16, 19, 22, 25, 26, 37, 41, 49, 72, 76, 77, 82, 86, 87, 115, 117, 135, 141, 149, 167, 169, 193 }),
            new ImmutablePair<>(492, new Integer[] { 2, 9, 15, 17, 27, 29, 31, 32, 33, 36, 47, 49, 50, 60, 62, 69, 77, 105, 114, 123, 127, 128, 132, 237, 255 }),
            new ImmutablePair<>(492, new Integer[] { 3, 5, 9, 11, 12, 21, 24, 27, 30, 44, 45, 50, 54, 55, 57, 63, 95, 101, 112, 117, 123, 134, 140, 235, 257 }),
            new ImmutablePair<>(503, new Integer[] { 4, 15, 16, 19, 22, 23, 25, 27, 33, 34, 50, 62, 67, 87, 88, 93, 100, 113, 135, 143, 149, 157, 167, 179, 211 }),
            new ImmutablePair<>(506, new Integer[] { 1, 7, 24, 26, 33, 35, 40, 45, 47, 51, 55, 69, 87, 90, 93, 96, 117, 125, 134, 145, 146, 147, 160, 162, 199 }),
            new ImmutablePair<>(507, new Integer[] { 2, 3, 7, 11, 13, 15, 28, 34, 43, 50, 57, 64, 80, 83, 86, 89, 107, 115, 116, 127, 149, 163, 175, 183, 217 }),
            new ImmutablePair<>(512, new Integer[] { 1, 7, 8, 9, 10, 15, 22, 32, 34, 46, 51, 65, 69, 71, 91, 105, 109, 111, 136, 139, 152, 157, 173, 200, 203 }),
            new ImmutablePair<>(512, new Integer[] { 1, 6, 7, 8, 9, 13, 17, 19, 35, 45, 47, 57, 62, 73, 88, 93, 104, 107, 128, 130, 151, 163, 184, 198, 221 }),
            new ImmutablePair<>(513, new Integer[] { 6, 9, 10, 17, 19, 24, 28, 29, 37, 39, 64, 65, 68, 81, 98, 99, 102, 115, 145, 147, 153, 159, 165, 189, 201 }),
            new ImmutablePair<>(517, new Integer[] { 5, 6, 7, 16, 20, 24, 28, 33, 38, 43, 63, 71, 80, 83, 86, 92, 98, 122, 132, 148, 164, 166, 173, 180, 205 }),
            new ImmutablePair<>(524, new Integer[] { 9, 12, 20, 21, 33, 35, 37, 39, 54, 55, 61, 62, 87, 90, 98, 101, 125, 132, 135, 141, 145, 159, 163, 164, 220 }),
            new ImmutablePair<>(527, new Integer[] { 11, 12, 13, 14, 19, 30, 41, 47, 50, 52, 59, 68, 71, 81, 94, 97, 107, 132, 147, 151, 155, 169, 175, 183, 197 }),
            new ImmutablePair<>(528, new Integer[] { 2, 9, 15, 17, 27, 29, 31, 32, 33, 36, 47, 49, 50, 60, 62, 69, 77, 123, 127, 128, 132, 141, 150, 255, 273 }),
            new ImmutablePair<>(529, new Integer[] { 9, 12, 20, 21, 33, 35, 37, 39, 54, 55, 61, 62, 87, 90, 98, 101, 125, 132, 140, 141, 145, 159, 163, 169, 225 }),
            new ImmutablePair<>(531, new Integer[] { 6, 9, 10, 17, 19, 24, 29, 31, 39, 40, 67, 68, 71, 84, 101, 102, 105, 118, 151, 153, 159, 165, 171, 195, 207 }),
            new ImmutablePair<>(532, new Integer[] { 16, 18, 26, 27, 33, 39, 41, 50, 51, 55, 69, 71, 84, 87, 91, 94, 132, 133, 141, 143, 164, 168, 169, 173, 195 }),
            new ImmutablePair<>(534, new Integer[] { 11, 13, 15, 17, 18, 27, 38, 44, 49, 52, 60, 61, 68, 81, 87, 94, 107, 135, 149, 153, 159, 171, 174, 189, 210 }),
            new ImmutablePair<>(535, new Integer[] { 2, 8, 26, 27, 36, 41, 45, 57, 62, 77, 88, 95, 97, 99, 101, 102, 109, 114, 117, 118, 141, 147, 168, 192, 226 }),
            new ImmutablePair<>(536, new Integer[] { 1, 8, 21, 30, 31, 32, 33, 41, 44, 46, 49, 55, 57, 61, 84, 91, 113, 134, 137, 139, 150, 155, 176, 205, 247 }),
            new ImmutablePair<>(536, new Integer[] { 3, 5, 9, 11, 12, 21, 24, 27, 30, 44, 45, 50, 54, 55, 57, 63, 95, 117, 123, 134, 140, 145, 156, 257, 279 }),
            new ImmutablePair<>(540, new Integer[] { 1, 7, 8, 9, 10, 14, 19, 34, 36, 51, 58, 69, 81, 83, 97, 109, 111, 115, 136, 149, 152, 167, 183, 208, 221 }),
            new ImmutablePair<>(540, new Integer[] { 6, 13, 15, 25, 28, 36, 43, 47, 55, 57, 58, 59, 60, 65, 82, 89, 91, 107, 124, 127, 144, 163, 183, 233, 250 }),
            new ImmutablePair<>(540, new Integer[] { 8, 9, 10, 11, 16, 30, 36, 38, 45, 55, 57, 65, 68, 81, 84, 95, 98, 100, 116, 117, 126, 135, 144, 261, 279 }),
            new ImmutablePair<>(540, new Integer[] { 4, 6, 9, 10, 17, 21, 23, 25, 31, 33, 36, 38, 45, 50, 81, 83, 115, 117, 126, 133, 135, 144, 146, 261, 279 }),
            new ImmutablePair<>(541, new Integer[] { 3, 4, 11, 13, 16, 17, 21, 25, 26, 44, 46, 64, 75, 86, 87, 97, 106, 109, 133, 141, 165, 185, 191, 215, 217 }),
            new ImmutablePair<>(541, new Integer[] { 3, 5, 27, 32, 33, 37, 47, 50, 53, 56, 57, 69, 71, 78, 97, 98, 109, 111, 126, 144, 165, 169, 183, 189, 232 }),
            new ImmutablePair<>(544, new Integer[] { 1, 7, 24, 26, 33, 35, 40, 45, 47, 51, 55, 69, 87, 90, 93, 96, 117, 125, 134, 145, 147, 184, 198, 199, 200 }),
            new ImmutablePair<>(544, new Integer[] { 6, 8, 20, 21, 23, 41, 42, 48, 59, 61, 77, 80, 81, 85, 90, 92, 93, 102, 115, 132, 139, 168, 198, 207, 244 }),
            new ImmutablePair<>(547, new Integer[] { 3, 5, 16, 22, 26, 27, 35, 47, 49, 59, 67, 71, 72, 85, 87, 102, 103, 111, 137, 144, 150, 197, 200, 203, 207 }),
            new ImmutablePair<>(549, new Integer[] { 4, 10, 14, 24, 26, 31, 34, 36, 38, 40, 43, 48, 59, 63, 74, 89, 97, 105, 117, 124, 136, 152, 156, 241, 308 }),
            new ImmutablePair<>(550, new Integer[] { 1, 2, 5, 13, 19, 20, 25, 30, 39, 43, 58, 59, 73, 75, 76, 90, 95, 103, 116, 128, 130, 132, 172, 262, 288 }),
            new ImmutablePair<>(550, new Integer[] { 1, 11, 16, 23, 24, 27, 29, 36, 41, 43, 44, 47, 59, 70, 71, 80, 99, 103, 111, 116, 128, 156, 167, 227, 323 }),
            new ImmutablePair<>(551, new Integer[] { 3, 5, 24, 25, 26, 30, 35, 36, 39, 40, 42, 57, 68, 76, 94, 109, 120, 128, 152, 162, 166, 175, 176, 200, 223 }),
            new ImmutablePair<>(552, new Integer[] { 5, 17, 18, 22, 25, 27, 32, 33, 39, 59, 62, 87, 91, 100, 102, 111, 112, 135, 137, 149, 165, 168, 183, 201, 204 }),
            new ImmutablePair<>(552, new Integer[] { 1, 3, 4, 7, 8, 9, 10, 15, 18, 19, 21, 41, 52, 54, 73, 93, 95, 123, 125, 136, 138, 153, 168, 261, 291 }),
            new ImmutablePair<>(556, new Integer[] { 6, 8, 10, 13, 19, 25, 32, 37, 49, 54, 58, 76, 84, 91, 92, 100, 107, 128, 145, 156, 165, 185, 195, 205, 206 }),
            new ImmutablePair<>(556, new Integer[] { 3, 12, 13, 15, 19, 23, 27, 34, 35, 39, 42, 45, 48, 52, 53, 87, 140, 145, 158, 166, 171, 184, 189, 201, 227 }),
            new ImmutablePair<>(556, new Integer[] { 1, 5, 7, 8, 9, 10, 12, 14, 20, 27, 31, 43, 47, 50, 74, 93, 97, 121, 125, 139, 143, 153, 167, 264, 292 }),
            new ImmutablePair<>(562, new Integer[] { 2, 3, 5, 8, 13, 19, 20, 29, 33, 47, 53, 54, 64, 65, 76, 93, 119, 123, 142, 157, 161, 180, 184, 221, 259 }),
            new ImmutablePair<>(570, new Integer[] { 3, 9, 10, 33, 36, 38, 40, 42, 50, 51, 60, 69, 72, 75, 77, 90, 113, 140, 141, 151, 152, 189, 200, 229, 230 }),
            new ImmutablePair<>(575, new Integer[] { 4, 6, 14, 16, 31, 39, 63, 69, 74, 81, 88, 103, 107, 111, 115, 120, 131, 132, 133, 147, 156, 159, 164, 198, 218 }),
            new ImmutablePair<>(576, new Integer[] { 1, 4, 9, 11, 15, 19, 22, 34, 36, 53, 60, 76, 82, 84, 104, 126, 127, 128, 153, 156, 165, 174, 183, 219, 237 }),
            new ImmutablePair<>(576, new Integer[] { 8, 9, 10, 11, 16, 30, 36, 38, 45, 55, 57, 65, 68, 81, 84, 95, 98, 100, 116, 135, 144, 153, 162, 279, 297 }),
            new ImmutablePair<>(576, new Integer[] { 4, 6, 9, 10, 17, 21, 23, 25, 31, 33, 36, 38, 45, 50, 81, 83, 115, 133, 135, 144, 146, 153, 162, 279, 297 }),
            new ImmutablePair<>(580, new Integer[] { 2, 5, 7, 10, 12, 13, 19, 21, 22, 29, 36, 40, 61, 65, 74, 101, 135, 139, 161, 179, 183, 192, 205, 209, 236 }),
            new ImmutablePair<>(580, new Integer[] { 5, 6, 11, 13, 16, 17, 21, 25, 34, 44, 54, 68, 80, 88, 100, 112, 120, 135, 142, 145, 170, 173, 195, 215, 265 }),
            new ImmutablePair<>(580, new Integer[] { 11, 12, 16, 17, 29, 32, 39, 41, 53, 55, 59, 60, 68, 70, 81, 84, 92, 124, 125, 128, 129, 156, 171, 280, 300 }),
            new ImmutablePair<>(593, new Integer[] { 13, 14, 15, 35, 48, 51, 55, 67, 73, 79, 83, 91, 94, 105, 109, 116, 119, 124, 133, 150, 171, 173, 196, 217, 226 }),
            new ImmutablePair<>(595, new Integer[] { 4, 13, 18, 19, 22, 35, 40, 48, 58, 61, 62, 77, 78, 82, 83, 86, 118, 149, 163, 168, 187, 192, 202, 206, 240 }),
            new ImmutablePair<>(601, new Integer[] { 7, 8, 25, 34, 41, 42, 46, 48, 54, 55, 62, 70, 71, 74, 98, 103, 116, 143, 168, 169, 190, 192, 193, 218, 240 }),
            new ImmutablePair<>(603, new Integer[] { 7, 11, 12, 14, 21, 25, 32, 40, 52, 56, 60, 67, 68, 81, 91, 92, 132, 144, 149, 163, 177, 191, 196, 235, 263 }),
            new ImmutablePair<>(603, new Integer[] { 13, 23, 26, 27, 35, 44, 45, 49, 53, 54, 57, 66, 75, 99, 101, 110, 122, 126, 144, 158, 175, 180, 189, 234, 270 }),
            new ImmutablePair<>(607, new Integer[] { 6, 8, 10, 13, 19, 25, 32, 37, 49, 54, 58, 76, 84, 91, 92, 100, 107, 128, 156, 185, 196, 205, 206, 216, 246 }),
            new ImmutablePair<>(609, new Integer[] { 9, 14, 15, 17, 32, 45, 47, 58, 67, 74, 76, 79, 80, 83, 97, 111, 125, 126, 150, 170, 186, 188, 215, 224, 235 }),
            new ImmutablePair<>(611, new Integer[] { 1, 10, 22, 26, 32, 41, 45, 54, 57, 61, 62, 66, 85, 86, 87, 95, 97, 101, 119, 132, 136, 167, 176, 268, 343 }),
            new ImmutablePair<>(614, new Integer[] { 15, 22, 24, 31, 33, 49, 53, 54, 57, 60, 63, 68, 74, 81, 83, 104, 109, 151, 155, 163, 167, 217, 229, 230, 234 }),
            new ImmutablePair<>(634, new Integer[] { 15, 17, 24, 26, 33, 43, 44, 54, 57, 60, 63, 73, 79, 81, 88, 109, 119, 160, 161, 172, 173, 227, 234, 235, 239 }),
            new ImmutablePair<>(643, new Integer[] { 2, 9, 21, 29, 38, 40, 41, 42, 58, 62, 67, 76, 82, 83, 85, 96, 104, 166, 172, 186, 192, 201, 207, 250, 270 }),
            new ImmutablePair<>(644, new Integer[] { 7, 9, 13, 18, 19, 22, 31, 49, 53, 61, 66, 68, 71, 87, 93, 94, 119, 164, 178, 192, 199, 206, 227, 239, 253 }),
            new ImmutablePair<>(655, new Integer[] { 10, 14, 15, 21, 25, 26, 31, 40, 51, 53, 54, 57, 65, 83, 84, 86, 151, 152, 173, 193, 194, 215, 216, 246, 288 }),
            new ImmutablePair<>(661, new Integer[] { 5, 7, 17, 18, 23, 31, 36, 38, 41, 64, 73, 77, 83, 84, 102, 106, 111, 161, 175, 196, 203, 210, 238, 248, 262 })
        };
        int id = 0;
        try {
            if(args.length > 0)
                id = Math.min(Math.max(0, Integer.valueOf(args[0])), 203);
        } catch(NumberFormatException e) {
            System.err.println("Invalid first argument, use an integer eg. 0, 1, ..., 203");
            System.exit(1);
        }
        boolean search_all = false;
        if (args.length > 1) {
            search_all = Boolean.valueOf(args[1]);
        }
        System.out.println("Searching for " + (search_all ? "all distinct solutions" : "a solution") + " for instance #" + id);
        Solve(instances[id], search_all);
    }
}
