# Stopwatch Application (Java Swing)

A desktop stopwatch built with Java Swing. It measures elapsed time with
hundredth-of-a-second precision and supports start, pause/resume, laps and reset.

Developed as part of the Java Programming Virtual Internship at Pynexia Technology.

## Features

- **Start / Pause / Resume** a single toggle button
- **Lap recording** shows lap time and total time, newest lap first
- **Reset** clears the time and all laps
- **Smooth display** refreshes every 10 ms (`HH:MM:SS.cc`)
- **Keyboard shortcuts**: `SPACE` start/pause, `L` lap, `R` reset
- Buttons enable/disable automatically based on state (e.g. Lap only while running)

## Technical Highlights

| Concept | Implementation |
|---|---|
| Timer functionality | `javax.swing.Timer` fires every 10 ms on the Event Dispatch Thread |
| Threading basics | UI updates stay on the EDT (thread-safe); app starts via `SwingUtilities.invokeLater` |
| Accuracy | Elapsed time derived from `System.nanoTime()`, so there is no drift from missed ticks |
| Clean design | `StopwatchModel` (logic) is separate from the Swing UI |
| Event-driven UI | `ActionListener` lambdas and `InputMap`/`ActionMap` key bindings |

## Requirements

- JDK 17 or newer

## How to Run

```bash
cd src
javac Stopwatch.java
java Stopwatch
```

Or open `src/Stopwatch.java` in IntelliJ IDEA / Eclipse / VS Code and run it.

## Project Structure

```
stopwatch-application/
├── src/
│   └── Stopwatch.java
├── .gitignore
└── README.md
```

## Testing Checklist

- Start, wait, pause: time freezes at the exact value
- Resume: time continues from where it stopped
- Record several laps: lap times add up to the total
- Reset: display returns to `00:00:00.00` and laps clear
- Let it run for several minutes: no lag or drift

## Author

Chetana Langhi
