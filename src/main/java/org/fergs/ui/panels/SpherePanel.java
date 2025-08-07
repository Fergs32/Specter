package org.fergs.ui.panels;

import io.opentelemetry.sdk.metrics.data.PointData;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Random;

public class SpherePanel extends JPanel {

    private final java.util.List<Vec3> landPoints = new ArrayList<>();
    private double angleY = 0;
    private Timer ticker;
    private final Vec3 cam = new Vec3(0, 0, -300);
    private final Color landColor = new Color(100, 200, 100);
    private final Color coastColor = new Color(150, 255, 150);
    private double rotationSpeed = Math.PI / 360;
    private double rotationProgress = 0;

    public SpherePanel(int numPoints) {
        setBackground(new Color(0x1E1E1E));
        setOpaque(true);

        Random rnd = new Random();
        while (landPoints.size() < numPoints) {
            double u = rnd.nextDouble() * 2 - 1;
            double phi = rnd.nextDouble() * 2 * Math.PI;
            double r = Math.sqrt(1 - u * u);
            double x = r * Math.cos(phi);
            double y = u;
            double z = r * Math.sin(phi);

            double lat = Math.asin(y);
            if (Math.abs(lat) < 0.7 && (Math.sin(phi * 5) > 0.3 || Math.abs(lat) < 0.2)) {
                landPoints.add(new Vec3(x, y, z));
            }
        }

    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(500, 500);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        final Graphics2D g = (Graphics2D) g0;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        final int w = getWidth(), h = getHeight();
        final double sinY = Math.sin(angleY), cosY = Math.cos(angleY);

        final double centerDepth = -cam.z;
        final double radius = 250.0;
        final double focal = 200;
        final double projRadius = (radius * focal) / (focal + centerDepth);

        g.setColor(new Color(100, 100, 100, 80));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval((int) (w / 2 - projRadius), (int) (h / 2 - projRadius),
                (int) (2 * projRadius), (int) (2 * projRadius));

        final java.util.List<PointData> pointData = new ArrayList<>();
        for (final Vec3 p : landPoints) {
            double px = p.x * radius;
            double py = p.y * radius;
            double pz = p.z * radius;

            double x = px * cosY + pz * sinY;
            double z = -px * sinY + pz * cosY;
            double y = py;

            double zx = x - cam.x;
            double zy = y - cam.y;
            double zz = z - cam.z;

            if (zz < 0) continue;

            double scale = focal / (focal + zz);
            int sx = (int) (zx * scale + w / 2);
            int sy = (int) (zy * scale + h / 2);

            pointData.add(new PointData(sx, sy, scale, zz));
        }

        pointData.sort((a, b) -> Double.compare(b.depth, a.depth));

        for (final PointData p : pointData) {

            final double sizeFactor = 0.8 + 0.2 * Math.sin(angleY + p.x * 0.01);
            int size = (int) (5 * p.scale * sizeFactor);
            size = Math.max(1, Math.min(6, size));

            final Ellipse2D point = new Ellipse2D.Double(p.x - size / 2.0, p.y - size / 2.0, size, size);

            final float brightness = (float) Math.max(0.5, Math.min(1.0, p.scale * 1.1));
            final Color pointColor = new Color(
                    (int) (landColor.getRed() * brightness),
                    (int) (landColor.getGreen() * brightness),
                    (int) (landColor.getBlue() * brightness)
            );

            g.setColor(pointColor);
            g.fill(point);

            if (size > 2) {
                g.setColor(new Color(
                        (int) (coastColor.getRed() * brightness),
                        (int) (coastColor.getGreen() * brightness),
                        (int) (coastColor.getBlue() * brightness),
                        150
                ));
                g.setStroke(new BasicStroke(0.7f));
                g.draw(point);

            }
        }
    }

    public void start() {
        ticker = new Timer(16, e -> {
            rotationProgress += 0.01;
            rotationSpeed = Math.PI / 360 * (0.8 + 0.2 * Math.sin(rotationProgress));
            angleY += rotationSpeed;
            repaint();
        });
        ticker.start();
    }

    public void stop() {
        ticker.stop();
    }

    private static class Vec3 {
        double x, y, z;

        Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class PointData {
        int x, y;
        double scale, depth;

        PointData(int x, int y, double scale, double depth) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.depth = depth;
        }
    }

}

