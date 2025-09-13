package org.fergs.modules.impl.tempemail;

import org.fergs.Specter;
import org.fergs.modules.AbstractModule;
import org.fergs.objects.TempEmail;
import org.fergs.objects.TempMessage;
import org.fergs.ui.forms.SpecterForm;
import org.fergs.ui.notifications.ToastNotification;
import org.fergs.ui.scroll.CyberScrollPane;
import org.fergs.utils.JHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TempEmailUI provides a user interface for creating and managing temporary email addresses
 * using the mail.tm service. It allows users to create disposable emails, view incoming messages,
 * and manage multiple email accounts simultaneously.
 *
 * @Author Fergs32
 */
public final class TempEmailUI extends AbstractModule {
    private final JPanel ui;
    private final JPanel emailsPanel;
    private final JPanel messagesPanel;
    private final JTextArea messageContentArea;
    private final JLabel statusLabel;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private Timer pollingTimer;
    private TempEmail currentSelectedEmail;
    private TempMessage currentSelectedMessage;

    public TempEmailUI() {
        super("temp-email", "Create and manage temporary email addresses");

        ui = new JPanel(new BorderLayout(10, 10));
        ui.setBackground(new Color(0x1E1E1E));

        emailsPanel = new JPanel();
        emailsPanel.setLayout(new BoxLayout(emailsPanel, BoxLayout.Y_AXIS));
        emailsPanel.setBackground(new Color(0x1E1E1E));

        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(new Color(0x1E1E1E));

        messageContentArea = new JTextArea();
        messageContentArea.setEditable(false);
        messageContentArea.setBackground(new Color(0x1E1E1E));
        messageContentArea.setForeground(new Color(0xCCCCCC));
        messageContentArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        messageContentArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        statusLabel = new JLabel("Ready to create temporary emails");
        statusLabel.setForeground(new Color(0x66FFCC));
        statusLabel.setFont(new Font("Consolas", Font.ITALIC, 12));

        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();

        ui.add(topPanel, BorderLayout.NORTH);
        ui.add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topPanel.setBackground(new Color(0x1E1E1E));

        JButton createEmailBtn = JHelper.createFancyHoverButton("Create Email", 14, true);
        createEmailBtn.addActionListener(e -> createNewTempEmail());

        JButton refreshBtn = JHelper.createFancyHoverButton("Refresh", 14, true);
        refreshBtn.addActionListener(e -> refreshEmails());

        JToggleButton autoRefreshBtn = new JToggleButton("Auto-Refresh");
        styleToggleButton(autoRefreshBtn);
        autoRefreshBtn.addActionListener(e -> toggleAutoRefresh(autoRefreshBtn.isSelected()));

        JButton clearBtn = JHelper.createFancyHoverButton("Clear All", 14, false);
        clearBtn.addActionListener(e -> clearAllEmails());

        topPanel.add(createEmailBtn);
        topPanel.add(refreshBtn);
        topPanel.add(autoRefreshBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(clearBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);

        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(0x1E1E1E));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(0x1E1E1E));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x66FFCC), 1),
            "Active Email Addresses",
            0, 0,
            new Font("Consolas", Font.BOLD, 14),
            new Color(0x66FFCC)
        ));

        JScrollPane emailsScroll = new CyberScrollPane(emailsPanel);
        emailsScroll.setPreferredSize(new Dimension(350, 0));
        leftPanel.add(emailsScroll, BorderLayout.CENTER);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setBackground(new Color(0x1E1E1E));
        rightSplit.setDividerLocation(250);
        rightSplit.setDividerSize(3);

        JPanel messagesContainer = new JPanel(new BorderLayout());
        messagesContainer.setBackground(new Color(0x1E1E1E));
        messagesContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x66FFCC), 1),
            "Inbox Messages",
            0, 0,
            new Font("Consolas", Font.BOLD, 14),
            new Color(0x66FFCC)
        ));

        JScrollPane messagesScroll = new CyberScrollPane(messagesPanel);
        messagesContainer.add(messagesScroll, BorderLayout.CENTER);

        JPanel contentContainer = new JPanel(new BorderLayout());
        contentContainer.setBackground(new Color(0x1E1E1E));
        contentContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x66FFCC), 1),
            "Message Content",
            0, 0,
            new Font("Consolas", Font.BOLD, 14),
            new Color(0x66FFCC)
        ));

        JScrollPane contentScroll = new JScrollPane(messageContentArea);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.getViewport().setBackground(new Color(0x1E1E1E));
        contentContainer.add(contentScroll, BorderLayout.CENTER);

        rightSplit.setTopComponent(messagesContainer);
        rightSplit.setBottomComponent(contentContainer);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setBackground(new Color(0x1E1E1E));
        mainSplit.setLeftComponent(leftPanel);
        mainSplit.setRightComponent(rightSplit);
        mainSplit.setDividerLocation(350);
        mainSplit.setDividerSize(3);

        centerPanel.add(mainSplit, BorderLayout.CENTER);
        return centerPanel;
    }

    private void styleToggleButton(JToggleButton button) {
        button.setFont(new Font("Consolas", Font.PLAIN, 12));
        button.setBackground(new Color(0x2A2A2A));
        button.setForeground(new Color(0x66FFCC));
        button.setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
        button.setFocusPainted(false);

        button.addItemListener(e -> {
            if (button.isSelected()) {
                button.setBackground(new Color(0x66FFCC));
                button.setForeground(new Color(0x1E1E1E));
                button.setBorder(BorderFactory.createLineBorder(new Color(0x66FFCC), 2));
            } else {
                button.setBackground(new Color(0x2A2A2A));
                button.setForeground(new Color(0x66FFCC));
                button.setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
            }
        });
    }

    private void createNewTempEmail() {
        statusLabel.setText("Creating new temporary email...");

        new SwingWorker<TempEmail, Void>() {
            @Override
            protected TempEmail doInBackground() throws Exception {
                TempEmailImpl impl = new TempEmailImpl();
                return impl.createTempEmail();
            }

            @Override
            protected void done() {
                try {
                    TempEmail email = get();
                    if (email != null) {
                        addEmailToPanel(email);
                        statusLabel.setText("Email created successfully: " + email.address());

                        ToastNotification.builder(SpecterForm.frame)
                                .setBackground(new Color(0x2A2A2A))
                                .setTitleColor(new Color(0x00FF88))
                                .setMessageColor(new Color(0xF5F5F5))
                                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                                .setSize(300, 100)
                                .setFadeInStep(25)
                                .setFadeOutStep(35)
                                .setDuration(3500)
                                .setTitle("Email Created")
                                .setMessage("New temporary email:\n" + email.address())
                                .show();
                    } else {
                        statusLabel.setText("Failed to create email");
                        showErrorNotification("Failed to create temporary email");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                    showErrorNotification("Error creating email: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void addEmailToPanel(TempEmail email) {
        JPanel emailCard = createEmailCard(email);
        emailsPanel.add(emailCard);
        emailsPanel.add(Box.createVerticalStrut(8));
        emailsPanel.revalidate();
        emailsPanel.repaint();
    }

    private JPanel createEmailCard(TempEmail email) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setBackground(new Color(0x2A2A2A));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x66FFCC), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);

        JLabel emailIcon = new JLabel("📧");
        emailIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        infoPanel.add(emailIcon, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);

        JLabel addressLabel = new JLabel(email.address());
        addressLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        addressLabel.setForeground(new Color(0x66FFCC));

        JLabel createdLabel = new JLabel("Created: " + email.createdAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        createdLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        createdLabel.setForeground(new Color(0xBBBBBB));

        textPanel.add(addressLabel);
        textPanel.add(createdLabel);
        infoPanel.add(textPanel, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actionsPanel.setOpaque(false);

        JButton copyBtn = createIconButton("📋", "Copy to clipboard");
        copyBtn.addActionListener(e -> copyToClipboard(email.address()));

        JButton deleteBtn = createIconButton("🗑️", "Delete email");
        deleteBtn.addActionListener(e -> deleteEmail(email, card));

        actionsPanel.add(copyBtn);
        actionsPanel.add(deleteBtn);

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(actionsPanel, BorderLayout.EAST);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectEmail(email);
                highlightSelectedCard(card);
            }
        });

        return card;
    }

    private JButton createIconButton(String icon, String tooltip) {
        JButton btn = new JButton(icon);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setBackground(new Color(0x333333));
        btn.setForeground(new Color(0x66FFCC));
        btn.setBorder(BorderFactory.createLineBorder(new Color(0x555555), 1));
        btn.setFocusPainted(false);
        btn.setToolTipText(tooltip);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(0x444444));
                btn.setBorder(BorderFactory.createLineBorder(new Color(0x66FFCC), 1));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(0x333333));
                btn.setBorder(BorderFactory.createLineBorder(new Color(0x555555), 1));
            }
        });

        return btn;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);

        ToastNotification.builder(SpecterForm.frame)
                .setBackground(new Color(0x2A2A2A))
                .setTitleColor(new Color(0x00FF88))
                .setMessageColor(new Color(0xF5F5F5))
                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                .setSize(300, 100)
                .setFadeInStep(25)
                .setFadeOutStep(35)
                .setDuration(3500)
                .setTitle("Email Copied")
                .setMessage("Email copied to clipboard!")
                .show();
    }

    private void deleteEmail(TempEmail email, JPanel card) {
        int result = JOptionPane.showConfirmDialog(
                ui,
                "Are you sure you want to delete this email address?\n" + email.address(),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    TempEmailImpl impl = new TempEmailImpl();
                    return impl.deleteEmail(email.id(), email.token());
                }

                @Override
                protected void done() {
                    try {
                        boolean success = get();
                        if (success) {
                            emailsPanel.remove(card);
                            emailsPanel.revalidate();
                            emailsPanel.repaint();

                            if (currentSelectedEmail != null && currentSelectedEmail.id().equals(email.id())) {
                                currentSelectedEmail = null;
                                messagesPanel.removeAll();
                                messageContentArea.setText("");
                                messagesPanel.revalidate();
                                messagesPanel.repaint();
                            }

                            statusLabel.setText("Email deleted successfully");
                        } else {
                            showErrorNotification("Failed to delete email");
                        }
                    } catch (Exception ex) {
                        showErrorNotification("Error deleting email: " + ex.getMessage());
                    }
                }
            }.execute();
        }
    }

    private void selectEmail(TempEmail email) {
        currentSelectedEmail = email;
        statusLabel.setText("Selected: " + email.address());
        loadMessages(email);
    }

    private void highlightSelectedCard(JPanel selectedCard) {
        for (Component comp : emailsPanel.getComponents()) {
            if (comp instanceof JPanel panel) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x66FFCC), 1),
                        new EmptyBorder(10, 10, 10, 10)
                ));
            }
        }

        selectedCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x00FF88), 2),
                new EmptyBorder(9, 9, 9, 9)
        ));
    }

    private void loadMessages(TempEmail email) {
        messagesPanel.removeAll();
        messageContentArea.setText("Loading messages...");

        new SwingWorker<List<TempMessage>, Void>() {
            @Override
            protected List<TempMessage> doInBackground() throws Exception {
                TempEmailImpl impl = new TempEmailImpl();
                return impl.getMessages(email.id(), email.token());
            }

            @Override
            protected void done() {
                try {
                    List<TempMessage> messages = get();
                    displayMessages(messages);
                    messageContentArea.setText(messages.isEmpty() ?
                        "No messages received yet.\nMessages will appear here automatically." :
                        "Select a message to view its content.");
                } catch (Exception ex) {
                    messageContentArea.setText("Error loading messages: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void displayMessages(List<TempMessage> messages) {
        messagesPanel.removeAll();

        if (messages.isEmpty()) {
            JLabel noMessages = new JLabel("📭 No messages yet", SwingConstants.CENTER);
            noMessages.setFont(new Font("Consolas", Font.ITALIC, 16));
            noMessages.setForeground(new Color(0x888888));
            messagesPanel.add(noMessages);
        } else {
            for (TempMessage message : messages) {
                JPanel messageCard = createMessageCard(message);
                messagesPanel.add(messageCard);
                messagesPanel.add(Box.createVerticalStrut(5));
            }
        }

        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    private JPanel createMessageCard(TempMessage message) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setBackground(new Color(0x252525));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x444444), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setOpaque(false);

        JLabel subjectLabel = new JLabel(message.subject().length() > 40 ?
            message.subject().substring(0, 37) + "..." : message.subject());
        subjectLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 13));
        subjectLabel.setForeground(new Color(0x66FFCC));

        JLabel fromLabel = new JLabel("From: " + message.from());
        fromLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        fromLabel.setForeground(new Color(0xBBBBBB));

        JLabel dateLabel = new JLabel(message.receivedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
        dateLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        dateLabel.setForeground(new Color(0x888888));

        infoPanel.add(subjectLabel);
        infoPanel.add(fromLabel);
        infoPanel.add(dateLabel);

        JLabel statusIndicator = new JLabel(message.isRead() ? "👁️" : "📩");
        statusIndicator.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(statusIndicator, BorderLayout.EAST);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                viewMessage(message);
                highlightSelectedMessage(card);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(0x2A2A2A));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x66FFCC), 1),
                        new EmptyBorder(7, 9, 7, 9)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(new Color(0x252525));
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x444444), 1),
                        new EmptyBorder(8, 10, 8, 10)
                ));
            }
        });

        return card;
    }

    private void viewMessage(TempMessage message) {
        if (currentSelectedEmail == null) return;

        messageContentArea.setText("Loading message content...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                TempEmailImpl impl = new TempEmailImpl();
                return impl.getMessageContent(currentSelectedEmail.id(), message.id(), currentSelectedEmail.token());
            }

            @Override
            protected void done() {
                try {
                    String content = get();
                    StringBuilder display = new StringBuilder();
                    display.append("Subject: ").append(message.subject()).append("\n");
                    display.append("From: ").append(message.from()).append("\n");
                    display.append("Date: ").append(message.receivedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"))).append("\n");
                    display.append("─".repeat(60)).append("\n\n");
                    display.append(content);

                    messageContentArea.setText(display.toString());
                    messageContentArea.setCaretPosition(0);
                } catch (Exception ex) {
                    messageContentArea.setText("Error loading message content: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void highlightSelectedMessage(JPanel selectedCard) {
        for (Component comp : messagesPanel.getComponents()) {
            if (comp instanceof JPanel panel) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0x444444), 1),
                        new EmptyBorder(8, 10, 8, 10)
                ));
            }
        }

        selectedCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x00FF88), 2),
                new EmptyBorder(7, 9, 7, 9)
        ));
    }

    private void refreshEmails() {
        if (currentSelectedEmail != null) {
            loadMessages(currentSelectedEmail);
            statusLabel.setText("Messages refreshed for: " + currentSelectedEmail.address());
        } else {
            statusLabel.setText("Select an email to refresh messages");
        }
    }

    private void toggleAutoRefresh(boolean enabled) {
        if (enabled) {
            startAutoRefresh();
            statusLabel.setText("Auto-refresh enabled (30s interval)");
        } else {
            stopAutoRefresh();
            statusLabel.setText("Auto-refresh disabled");
        }
    }

    private void startAutoRefresh() {
        if (pollingTimer != null) {
            pollingTimer.stop();
        }

        pollingTimer = new Timer(30000, e -> {
            if (currentSelectedEmail != null) {
                loadMessages(currentSelectedEmail);
            }
        });
        pollingTimer.start();
        isPolling.set(true);
    }

    private void stopAutoRefresh() {
        if (pollingTimer != null) {
            pollingTimer.stop();
            pollingTimer = null;
        }
        isPolling.set(false);
    }

    private void clearAllEmails() {
        int result = JOptionPane.showConfirmDialog(
                ui,
                "Are you sure you want to clear all email addresses?\nThis will remove all emails from the display.",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            emailsPanel.removeAll();
            messagesPanel.removeAll();
            messageContentArea.setText("");
            currentSelectedEmail = null;
            stopAutoRefresh();

            emailsPanel.revalidate();
            emailsPanel.repaint();
            messagesPanel.revalidate();
            messagesPanel.repaint();

            statusLabel.setText("All emails cleared");
        }
    }

    private void showErrorNotification(String message) {
        ToastNotification.builder(SpecterForm.frame)
                .setBackground(new Color(0xFF4444))
                .setTitleColor(Color.WHITE)
                .setMessageColor(Color.WHITE)
                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                .setSize(300, 100)
                .setFadeInStep(25)
                .setFadeOutStep(35)
                .setDuration(4000)
                .setTitle("❌ Error")
                .setMessage(message)
                .show();
    }

    @Override
    public void onEnable() {
    }


    @Override
    public void onDisable() {
        stopAutoRefresh();
    }

    @Override
    public void onLoad(JFrame frame) {
        ToastNotification.builder(frame)
                .setBackground(new Color(0x2A2A2A))
                .setTitleColor(new Color(0x00FF88))
                .setMessageColor(new Color(0xF5F5F5))
                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                .setSize(255, 85)
                .setFadeInStep(25)
                .setFadeOutStep(35)
                .setDuration(3500)
                .setTitle("⚡ Module Loaded")
                .setMessage("Name: Temporary Email\nStatus: ✓ Active")
                .show();
    }

    @Override
    public JPanel getUI() {
        return ui;
    }
}
