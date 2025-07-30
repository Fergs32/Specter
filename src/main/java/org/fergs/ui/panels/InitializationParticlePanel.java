package org.fergs.ui.panels;

import org.fergs.objects.Flake;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InitializationParticlePanel extends JPanel implements ActionListener {
    private final List<Flake> flakes = new ArrayList<>();
    private final Random rand = new Random();
    private final Timer timer;
    private boolean init = false;

    public InitializationParticlePanel() {
        setBackground(new Color(0x1E1E1E));
        timer = new Timer(40, this);
        timer.start();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!init) {
                    initFlakes();
                    init = true;
                }
            }
        });
    }

    private void initFlakes() {
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < 10; i++) {
            int size = 6 + rand.nextInt(6);
            flakes.add(new Flake(rand.nextInt(w), rand.nextInt(h), size));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0xCCFFFF));
        for (Flake f : flakes) {
            AffineTransform old = g2.getTransform();
            g2.translate(f.x, f.y);
            g2.rotate(f.angle);
            drawSnowflake(g2, f.size);
            g2.setTransform(old);
        }
        g2.dispose();
    }

    private void drawSnowflake(Graphics2D g2, int r) {
        int arms = 6;
        double angleStep = Math.PI * 2 / arms;
        for (int i = 0; i < arms; i++) {
            double angle = i * angleStep;
            int x2 = (int) (Math.cos(angle) * r);
            int y2 = (int) (Math.sin(angle) * r);
            g2.drawLine(0, 0, x2, y2);

            double branchBase = r * 0.6;
            int bx = (int) (Math.cos(angle) * branchBase);
            int by = (int) (Math.sin(angle) * branchBase);
            double ba1 = angle + angleStep * 0.2;
            double ba2 = angle - angleStep * 0.2;
            int br = (int) (r * 0.3);
            g2.drawLine(bx, by,
                    bx + (int) (Math.cos(ba1) * br),
                    by + (int) (Math.sin(ba1) * br));
            g2.drawLine(bx, by,
                    bx + (int) (Math.cos(ba2) * br),
                    by + (int) (Math.sin(ba2) * br));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int w = getWidth(), h = getHeight();
        for (Flake f : flakes) {
            f.y += f.speed;
            f.x += (int) (Math.sin(f.phase) * f.drift);
            f.angle += f.rotationSpeed;
            f.phase += f.phaseSpeed;
            if (f.y > h) {
                f.y = -f.size;
                f.x = rand.nextInt(w);
            }
        }
        repaint();
    }
}
