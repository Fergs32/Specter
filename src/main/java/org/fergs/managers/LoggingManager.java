package org.fergs.managers;

import lombok.Getter;
import lombok.Setter;
import org.fergs.objects.LogEntry;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.*;

@Getter @Setter
public final class LoggingManager {
    private final Logger logger = Logger.getLogger("org.fergs.Specter");
    private static LoggingManager instance;

    private JFrame consoleFrame;
    private JTextArea consoleArea;
    private JScrollPane scrollPane;

    private Path logFilePath;
    private boolean fileLoggingEnabled = true;
    private boolean consoleWindowEnabled = true;

    private Thread loggingThread;
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    private final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Private constructor to prevent instantiation from outside.
     */
    private LoggingManager() {
        System.out.println("Initializing LoggingManager...");
        try {
            initialize();
            System.out.println("LoggingManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize LoggingManager: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("LoggingManager initialization failed", e);
        }
    }

    /**
     * Get the singleton instance of LoggingManager
     */
    public static synchronized LoggingManager getInstance() {
        if (instance == null) {
            System.out.println("Creating LoggingManager instance...");
            instance = new LoggingManager();
        }
        return instance;
    }

    /**
     * Initialize the logging manager with console window and file setup
     */
    private void initialize() {
        setupFileLogging();
        setupConsoleWindow();
        setupBackgroundLogging();
        configureJavaLogger();
    }

    /**
     * Setup file logging with daily rotation
     */
    private void setupFileLogging() {
        try {

            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
                System.out.println("Created logs directory");
            }

            String logFileName = String.format("specter-%s.log",
                    LocalDateTime.now().format(FILE_DATE_FORMAT));
            logFilePath = logsDir.resolve(logFileName);

            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
                System.out.println("Created log file: " + logFilePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to setup file logging: " + e.getMessage());
            fileLoggingEnabled = false;
        }
    }

    /**
     * Setup the separate console window for log output
     */
    private void setupConsoleWindow() {
        SwingUtilities.invokeLater(() -> {
            consoleFrame = new JFrame("Specter - Log Console");
            consoleFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            consoleFrame.setSize(800, 600);

            consoleArea = new JTextArea();
            consoleArea.setEditable(false);
            consoleArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            consoleArea.setBackground(Color.BLACK);
            consoleArea.setForeground(Color.GREEN);
            consoleArea.setCaretColor(Color.GREEN);

            scrollPane = new JScrollPane(consoleArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            JButton clearButton = new JButton("Clear");
            clearButton.addActionListener(e -> consoleArea.setText(""));

            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(clearButton);

            consoleFrame.add(scrollPane, BorderLayout.CENTER);
            consoleFrame.add(buttonPanel, BorderLayout.SOUTH);

            consoleFrame.setLocationRelativeTo(null);
        });
    }

    /**
     * Setup background thread for async logging
     */
    private void setupBackgroundLogging() {
        loggingThread = new Thread(this::processLogEntries, "LoggingThread");
        loggingThread.setDaemon(true);
        loggingThread.start();
    }

    /**
     * Configure the Java logger to use our custom handler
     */
    private void configureJavaLogger() {
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);

        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (isLoggable(record)) {
                    queueLogEntry(new LogEntry(
                            record.getLevel(),
                            record.getMessage(),
                            record.getThrown(),
                            record.getParameters()
                    ));
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        });
    }

    /**
     * Process log entries from the queue in background thread
     */
    private void processLogEntries() {
        while (running) {
            try {
                LogEntry entry = logQueue.take();
                String formattedMessage = formatLogMessage(entry);

                if (consoleWindowEnabled) {
                    SwingUtilities.invokeLater(() -> appendToConsole(formattedMessage));
                }

                if (fileLoggingEnabled) {
                    writeToFile(formattedMessage);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error processing log entry: " + e.getMessage());
            }
        }
    }

    /**
     * Queue a log entry for background processing
     */
    private void queueLogEntry(LogEntry entry) {
        try {
            boolean ok = logQueue.offer(entry);
            if (!ok) {
                log(Level.WARNING, "Log queue is full or not accepting entries??");
                writeToFile(formatLogMessage(entry));
            }
        } catch (Exception e) {
            System.err.println("Failed to queue log entry: " + e.getMessage());
        }
    }

    /**
     * Format a log message with timestamp, level, and content
     */
    private String formatLogMessage(LogEntry entry) {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("] ");

        sb.append(String.format("%-7s", entry.level.getName()));
        sb.append(" - ");

        String message = entry.message;
        if (entry.params != null && entry.params.length > 0) {
            try {
                message = String.format(message, entry.params);
            } catch (Exception ignored) {}
        }
        sb.append(message);

        if (entry.throwable != null) {
            sb.append("\n").append(getStackTrace(entry.throwable));
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getSimpleName())
                .append(": ")
                .append(throwable.getMessage());

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\n    at ").append(element.toString());
        }

        if (throwable.getCause() != null) {
            sb.append("\nCaused by: ").append(getStackTrace(throwable.getCause()));
        }

        return sb.toString();
    }

    /**
     * Append message to console window
     */
    private void appendToConsole(String message) {
        if (consoleArea != null) {
            consoleArea.append(message);
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());

            int maxLines = 10000;
            int lines = consoleArea.getLineCount();
            if (lines > maxLines) {
                try {
                    int linesToRemove = lines - maxLines;
                    int endOffset = consoleArea.getLineEndOffset(linesToRemove - 1);
                    consoleArea.replaceRange("", 0, endOffset);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Write message to log file
     */
    private void writeToFile(String message) {
        try {
            // Check if we need to rotate the log file (daily rotation)
            String currentDate = LocalDateTime.now().format(FILE_DATE_FORMAT);
            String expectedFileName = String.format("specter-%s.log", currentDate);

            if (!logFilePath.getFileName().toString().equals(expectedFileName)) {
                // Rotate to new file
                logFilePath = logFilePath.getParent().resolve(expectedFileName);
                if (!Files.exists(logFilePath)) {
                    Files.createFile(logFilePath);
                }
            }

            Files.write(logFilePath, message.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Show the console window
     */
    public void showConsoleWindow() {
        if (consoleFrame != null) {
            SwingUtilities.invokeLater(() -> {
                consoleFrame.setVisible(true);
                consoleFrame.toFront();
            });
        }
    }

    /**
     * Hide the console window
     */
    public void hideConsoleWindow() {
        if (consoleFrame != null) {
            SwingUtilities.invokeLater(() -> consoleFrame.setVisible(false));
        }
    }

    /**
     * Logs a message at the specified level.
     */
    public void log(final @NotNull Level level, final @NotNull String message) {
        logger.log(level, message);
    }

    /**
     * Logs a message with an associated Throwable.
     */
    public void log(final @NotNull Level level, final @NotNull String message, final @NotNull Throwable thrown) {
        logger.log(level, message, thrown);
    }

    /**
     * Logs a formatted message with parameters.
     */
    public void log(final @NotNull Level level, final @NotNull String message, final @NotNull Object... params) {
        logger.log(level, message, params);
    }

    public void debug(String message) { log(Level.FINE, message); }
    public void debug(String message, Object... params) { log(Level.FINE, message, params); }
    public void info(String message) { log(Level.INFO, message); }
    public void info(String message, Object... params) { log(Level.INFO, message, params); }
    public void warn(String message) { log(Level.WARNING, message); }
    public void warn(String message, Object... params) { log(Level.WARNING, message, params); }
    public void warn(String message, Throwable thrown) { log(Level.WARNING, message, thrown); }
    public void error(String message) { log(Level.SEVERE, message); }
    public void error(String message, Object... params) { log(Level.SEVERE, message, params); }
    public void error(String message, Throwable thrown) { log(Level.SEVERE, message, thrown); }

    /**
     * Shutdown the logging manager
     */
    public void shutdown() {
        running = false;
        if (loggingThread != null) {
            loggingThread.interrupt();
        }
        if (consoleFrame != null) {
            SwingUtilities.invokeLater(() -> consoleFrame.dispose());
        }
    }
}