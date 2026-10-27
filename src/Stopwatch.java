import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

/**
 * Stopwatch Application (Java Swing).
 *
 * <p>Features: start, pause/resume, lap recording and reset, with a display that
 * refreshes smoothly every 10 ms using {@link javax.swing.Timer}.
 *
 * <p>Design notes:
 * <ul>
 *   <li><b>Accuracy:</b> elapsed time is computed from {@link System#nanoTime()}
 *       instead of counting timer ticks, so the stopwatch never drifts even if
 *       the UI thread is briefly busy.</li>
 *   <li><b>Threading:</b> {@code javax.swing.Timer} fires on the Event Dispatch
 *       Thread (EDT), so UI updates are thread-safe without extra locking and
 *       the window never freezes.</li>
 *   <li><b>Separation of concerns:</b> {@link StopwatchModel} holds the timing
 *       logic and knows nothing about Swing; {@link Stopwatch} only builds the UI.</li>
 * </ul>
 */
public final class Stopwatch extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int REFRESH_INTERVAL_MS = 10;

    // Theme colours
    private static final Color BACKGROUND = new Color(0x1E1F26);
    private static final Color SURFACE = new Color(0x2A2C36);
    private static final Color TEXT = new Color(0xF2F2F2);
    private static final Color MUTED_TEXT = new Color(0x9AA0B0);
    private static final Color GREEN = new Color(0x2E9E5B);
    private static final Color AMBER = new Color(0xE0A100);
    private static final Color BLUE = new Color(0x2F7DE1);
    private static final Color RED = new Color(0xD64545);
    private static final Color DISABLED = new Color(0x4A4D5A);

    private final StopwatchModel model = new StopwatchModel();
    private final Timer uiTimer = new Timer(REFRESH_INTERVAL_MS, e -> refreshDisplay());

    private final JLabel timeLabel = new JLabel(StopwatchModel.format(0), SwingConstants.CENTER);
    private final DefaultListModel<String> lapListModel = new DefaultListModel<>();
    private final JButton startPauseButton = createButton("Start", GREEN);
    private final JButton lapButton = createButton("Lap", BLUE);
    private final JButton resetButton = createButton("Reset", RED);

    public Stopwatch() {
        super("Stopwatch");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        registerKeyboardShortcuts();
        updateControls();
        pack();
        setMinimumSize(new Dimension(460, 520));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ UI

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        timeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 52));
        timeLabel.setForeground(TEXT);
        timeLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel buttons = new JPanel(new GridLayout(1, 3, 12, 0));
        buttons.setOpaque(false);
        buttons.add(startPauseButton);
        buttons.add(lapButton);
        buttons.add(resetButton);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(timeLabel, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        JList<String> lapList = new JList<>(lapListModel);
        lapList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        lapList.setBackground(SURFACE);
        lapList.setForeground(TEXT);
        lapList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lapList.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(lapList);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DISABLED), "Laps (newest first)",
                0, 0, new Font(Font.SANS_SERIF, Font.PLAIN, 12), MUTED_TEXT));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(SURFACE);

        root.add(top, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);

        startPauseButton.addActionListener(e -> toggleStartPause());
        lapButton.addActionListener(e -> recordLap());
        resetButton.addActionListener(e -> reset());
        return root;
    }

    private static JButton createButton(String text, Color colour) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(colour);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFocusable(false); // keeps SPACE reserved for the global shortcut
        button.setPreferredSize(new Dimension(120, 44));
        return button;
    }

    /** SPACE = start/pause, L = lap, R = reset. */
    private void registerKeyboardShortcuts() {
        bindKey("SPACE", "toggle", this::toggleStartPause);
        bindKey("L", "lap", this::recordLap);
        bindKey("R", "reset", this::reset);
    }

    private void bindKey(String key, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ------------------------------------------------------------- Actions

    private void toggleStartPause() {
        if (model.isRunning()) {
            model.pause();
            uiTimer.stop();
            refreshDisplay(); // show the exact paused value
        } else {
            model.start();
            uiTimer.start();
        }
        updateControls();
    }

    private void recordLap() {
        if (!model.isRunning()) {
            return;
        }
        StopwatchModel.Lap lap = model.recordLap();
        lapListModel.add(0, String.format("Lap %02d   %s   Total %s",
                lap.number(), StopwatchModel.format(lap.lapMillis()),
                StopwatchModel.format(lap.totalMillis())));
    }

    private void reset() {
        uiTimer.stop();
        model.reset();
        lapListModel.clear();
        refreshDisplay();
        updateControls();
    }

    private void refreshDisplay() {
        timeLabel.setText(StopwatchModel.format(model.elapsedMillis()));
    }

    private void updateControls() {
        boolean running = model.isRunning();
        boolean hasTime = model.elapsedMillis() > 0;

        startPauseButton.setText(running ? "Pause" : (hasTime ? "Resume" : "Start"));
        startPauseButton.setBackground(running ? AMBER : GREEN);

        lapButton.setEnabled(running);
        lapButton.setBackground(running ? BLUE : DISABLED);

        resetButton.setEnabled(hasTime);
        resetButton.setBackground(hasTime ? RED : DISABLED);
    }

    // --------------------------------------------------------------- Model

    /** Pure timing logic with no Swing dependency (easy to test and reuse). */
    static final class StopwatchModel {

        /** A single recorded lap. */
        record Lap(int number, long lapMillis, long totalMillis) { }

        private long accumulatedNanos;
        private long startNanos;
        private boolean running;
        private long lastLapMillis;
        private int lapCount;

        void start() {
            if (!running) {
                startNanos = System.nanoTime();
                running = true;
            }
        }

        void pause() {
            if (running) {
                accumulatedNanos += System.nanoTime() - startNanos;
                running = false;
            }
        }

        void reset() {
            accumulatedNanos = 0;
            startNanos = 0;
            running = false;
            lastLapMillis = 0;
            lapCount = 0;
        }

        Lap recordLap() {
            long total = elapsedMillis();
            Lap lap = new Lap(++lapCount, total - lastLapMillis, total);
            lastLapMillis = total;
            return lap;
        }

        boolean isRunning() {
            return running;
        }

        long elapsedMillis() {
            long nanos = accumulatedNanos + (running ? System.nanoTime() - startNanos : 0);
            return nanos / 1_000_000L;
        }

        /** Formats milliseconds as HH:MM:SS.cc (cc = hundredths of a second). */
        static String format(long millis) {
            long hours = millis / 3_600_000L;
            long minutes = (millis / 60_000L) % 60;
            long seconds = (millis / 1_000L) % 60;
            long hundredths = (millis % 1_000L) / 10;
            return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths);
        }
    }

    // ---------------------------------------------------------------- Main

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Stopwatch().setVisible(true));
    }
}
