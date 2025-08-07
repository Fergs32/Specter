package org.fergs.ui.forms;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import org.fergs.objects.Breach;
import org.fergs.ui.AbstractForm;
import org.fergs.utils.JHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

public class BreachGraphForm extends AbstractForm {
    private final String email;
    private final List<Breach> breaches;
    private Point dragOffset;

    private static final int NODE_WIDTH     = 140;
    private static final int NODE_HEIGHT    = 60;
    private static final int INNER_RADIUS   = 180;
    // outer circle for description nodes
    private static final int DESC_WIDTH     = 250;
    private static final int DESC_HEIGHT    = 100;
    private static final int OUTER_RADIUS   = 450;

    public BreachGraphForm(String email, List<Breach> breaches) {
        super("Specter • Breach Map", 900, 700);
        this.email   = email;
        this.breaches = breaches;

        // make the entire window draggable
        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOffset.x,
                        loc.y + e.getY() - dragOffset.y);
            }
        };
        getContentPane().addMouseListener(ma);
        getContentPane().addMouseMotionListener(ma);
    }

    @Override
    protected JPanel createContentPane() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0x1E1E1E));
        return p;
    }

    @Override
    protected void initForm() {
        // close button
        JButton close = JHelper.createHoverButton("X", 20, false);
        close.setPreferredSize(new Dimension(40, 40));
        close.addActionListener(e -> dispose());
        ((JPanel)getContentPane().getComponent(0))
                .add(close, BorderLayout.EAST);
        if (getJMenuBar()!=null) getJMenuBar().setVisible(false);

        // no breaches?
        if (breaches == null || breaches.isEmpty()) {
            JLabel none = new JLabel("<html><b>No breaches for:</b><br/>" + email + "</html>",
                    SwingConstants.CENTER);
            none.setForeground(new Color(0x66FFCC));
            none.setFont(new Font("Consolas", Font.PLAIN, 16));
            getContentRegion().add(none, BorderLayout.CENTER);
            return;
        }


        // build graph
        mxGraph graph = new mxGraph();
        graph.setHtmlLabels(true);
        graph.setCellsResizable(false);
        graph.setCellsEditable(false);
        graph.setCellsDisconnectable(false);
        graph.setCellsSelectable(true);
        graph.setAllowDanglingEdges(false);

        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
            String baseStyle =
                    "shape=rounded;rounded=1;" +
                            "whiteSpace=wrap;horizontal=1;" +
                            "fillColor=#232323;fontColor=#00FF88;" +
                            "strokeColor=#00FF88;strokeWidth=2;" +
                            "fontFamily=Consolas;fontSize=12;";

            // center coordinates
            int cx = OUTER_RADIUS + DESC_WIDTH;
            int cy = OUTER_RADIUS + DESC_HEIGHT;

            // center node
            mxCell emailCell = (mxCell)graph.insertVertex(parent, null,
                    "<html><b>" + email + "</b></html>",
                    cx-100, cy-25, 200, 50,
                    baseStyle + "fontSize=14;fontStyle=1;"
            );

            int n = breaches.size();

            // 1) Inner ring: breach nodes
            mxCell[] breachCells = new mxCell[n];
            for (int i = 0; i < n; i++) {
                Breach b = breaches.get(i);
                double theta = 2*Math.PI*i/n;
                int x = cx + (int)(INNER_RADIUS * Math.cos(theta)) - NODE_WIDTH/2;
                int y = cy + (int)(INNER_RADIUS * Math.sin(theta)) - NODE_HEIGHT/2;
                String label = String.format(
                        "<html><b>%s</b><br/>%s</html>",
                        b.getSite(), b.getPublishDate()
                );
                breachCells[i] = (mxCell)graph.insertVertex(parent, null,
                        label, x, y, NODE_WIDTH, NODE_HEIGHT, baseStyle);
                graph.insertEdge(parent, null, "", emailCell, breachCells[i],
                        "strokeColor=#00FF88;strokeWidth=1.5;");
            }

            // 2) Outer ring: description nodes
            for (int i = 0; i < n; i++) {
                Breach b = breaches.get(i);
                double theta = 2*Math.PI*i/n;
                int dx = cx + (int)(OUTER_RADIUS * Math.cos(theta)) - DESC_WIDTH/2;
                int dy = cy + (int)(OUTER_RADIUS * Math.sin(theta)) - DESC_HEIGHT/2;
                String descHtml = String.format(
                        "<html><b>Description:</b><br/>%s</html>",
                        b.getDescription()
                                .replace("&","&amp;")
                                .replace("<","&lt;")
                                .replace(">","&gt;")
                );
                mxCell descCell = (mxCell)graph.insertVertex(parent, null,
                        descHtml, dx, dy, DESC_WIDTH, DESC_HEIGHT,
                        baseStyle + "fontSize=10;");
                graph.insertEdge(parent, null, "", breachCells[i], descCell,
                        "strokeColor=#00FF88;dashed=1;dashPattern=5 3;");
            }
        } finally {
            graph.getModel().endUpdate();
        }

        // wrap in interactive component
        mxGraphComponent comp = new mxGraphComponent(graph);
        comp.setConnectable(false);
        comp.setBorder(null);
        comp.getViewport().setBackground(new Color(0x1E1E1E));
        comp.setBackground(new Color(0x1E1E1E));

        // panning via left‐drag
        JScrollPane scroll = new JScrollPane(comp);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(0x1E1E1E));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JViewport vp = scroll.getViewport();
        final Point[] md = {null}, vs = {null};
        comp.getGraphControl().addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    md[0] = SwingUtilities.convertPoint(comp.getGraphControl(), e.getPoint(), vp);
                    vs[0] = vp.getViewPosition();
                }
            }
        });
        comp.getGraphControl().addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (md[0] != null) {
                    Point drag = SwingUtilities.convertPoint(comp.getGraphControl(), e.getPoint(), vp);
                    int dx = drag.x - md[0].x, dy = drag.y - md[0].y;
                    vp.setViewPosition(new Point(vs[0].x - dx, vs[0].y - dy));
                }
            }
        });

        // zoom with Ctrl + wheel
        comp.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) comp.zoomIn();
                else                           comp.zoomOut();
                e.consume();
            }
        });

        // center on open
        int cx = OUTER_RADIUS + DESC_WIDTH;
        int cy = OUTER_RADIUS + DESC_HEIGHT;
        SwingUtilities.invokeLater(() -> {
            vp.setViewPosition(new Point(cx - getWidth()/2, cy - getHeight()/2));
        });

        getContentRegion().add(scroll, BorderLayout.CENTER);
    }

    /** helper to find the CENTER region panel */
    private JPanel getContentRegion() {
        Container cp = getContentPane();
        for (Component c : cp.getComponents()) {
            if (BorderLayout.CENTER.equals(((BorderLayout)cp.getLayout()).getConstraints(c))) {
                return (JPanel)c;
            }
        }
        return (JPanel)cp;
    }
}