package org.fergs.ui.notifications;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;



@Getter @Setter
public final class ToastNotification {
    private static final List<JWindow> toasts = new ArrayList<>();
    private static final int GAP = 10;

    private JFrame owner;
    private String title;
    private String message;
    private Color background;
    private Color titleColor;
    private Color messageColor;
    private Font titleFont;
    private Font messageFont;
    private int width = 320;
    private int height = 90;
    private int fadeInStep;
    private int fadeOutStep;
    private int duration;
    private Icon icon;

    private ToastNotification() {}

    public static ToastNotification builder(JFrame owner) {
        ToastNotification tn = new ToastNotification();
        tn.owner = owner;
        return tn;
    }

    public static ToastNotification builder(Component ownerComponent) {
        final ToastNotification tn = new ToastNotification();

        final Window window = SwingUtilities.getWindowAncestor(ownerComponent);
        if (window instanceof JFrame) {
            tn.owner = (JFrame) window;
        } else {
            throw new IllegalArgumentException("Unable to resolve parent JFrame from the provided component.");
        }
        return tn;
    }

    public ToastNotification setTitle(String title) {
        this.title = title;
        return this;
    }

    public ToastNotification setMessage(String message) {
        this.message = message;
        return this;
    }

    public ToastNotification setBackground(Color background) {
        this.background = background;
        return this;
    }

    public ToastNotification setTitleColor(Color titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public ToastNotification setMessageColor(Color messageColor) {
        this.messageColor = messageColor;
        return this;
    }

    public ToastNotification setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
        return this;
    }

    public ToastNotification setMessageFont(Font messageFont) {
        this.messageFont = messageFont;
        return this;
    }

    public ToastNotification setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public ToastNotification setFadeInStep(int fadeInStep) {
        this.fadeInStep = fadeInStep;
        return this;
    }

    public ToastNotification setFadeOutStep(int fadeOutStep) {
        this.fadeOutStep = fadeOutStep;
        return this;
    }

    public ToastNotification setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public void show() {
        if (owner == null || title == null) {
            throw new IllegalStateException("Owner frame and title are required.");
        }

        final JWindow win = new JWindow(owner);
        win.setSize(width, height);
        win.setBackground(new Color(0,0,0,0));

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(titleColor, 2),
                new EmptyBorder(8,12,8,12)
        ));

        if (icon != null) {
            final JLabel iconLabel = new JLabel(icon);
            panel.add(iconLabel, BorderLayout.WEST);
        }

        final JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(titleFont);
        titleLbl.setForeground(titleColor);

        final JLabel msgLbl = new JLabel("<html>" + message.replace("\n","<br>") + "</html>");
        msgLbl.setFont(messageFont);
        msgLbl.setForeground(messageColor);

        final JPanel textPanel = new JPanel(new GridLayout(2,1));
        textPanel.setOpaque(false);
        textPanel.add(titleLbl);
        textPanel.add(msgLbl);

        panel.add(textPanel, BorderLayout.CENTER);
        win.add(panel);

        toasts.add(win);
        win.setOpacity(0f);
        win.setVisible(true);

        repositionAll();

        fadeIn(win);
    }

    private void fadeIn(JWindow win) {
        new Timer(fadeInStep, e -> {
            float op = win.getOpacity() + 0.05f;
            if (op >= 1f) {
                win.setOpacity(1f);
                ((Timer) e.getSource()).stop();
                new Timer(duration, evt -> fadeOut(win)).start();
            } else win.setOpacity(op);
        }).start();
    }

    private void fadeOut(JWindow win) {
        new Timer(fadeOutStep, e -> {
            float op = win.getOpacity() - 0.05f;
            if (op <= 0f) {
                win.setOpacity(0f);
                ((Timer) e.getSource()).stop();
                win.dispose();
                toasts.remove(win);
                repositionAll();
            } else win.setOpacity(op);
        }).start();
    }

    private void repositionAll() {
        Point loc = owner.getLocationOnScreen();
        for (int i = 0; i < toasts.size(); i++) {
            JWindow w = toasts.get(i);
            int x = loc.x + owner.getWidth() - width - GAP;
            int y = loc.y + GAP + i * (height + GAP);
            w.setLocation(x, y);
        }
    }
}
