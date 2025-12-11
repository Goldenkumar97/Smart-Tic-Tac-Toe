import javax.swing.*;
import java.awt.*;

public class SmartTicTacToe extends JFrame {
    private final JButton[] cells = new JButton[9];
    private final char[] board = new char[9];
    private boolean playerTurn = true;
    private boolean vsAI = true;
    private String nameX = "Player X", nameO = "Player O";
    private final JLabel status = new JLabel("", SwingConstants.CENTER);

    public SmartTicTacToe() {
        super("Smart Tic Tac Toe");

        // --- SHOW GAME RULES WITH CANCEL OPTION ---
        int rules = JOptionPane.showConfirmDialog(this,
                "GAME RULES\n\n" +
                "1) You are X (Black)\n" +
                "2) Computer/Friend is O (Red)\n" +
                "3) First to form 3 in a row wins\n" +
                "4) Click any box to play\n\nClick OK to continue or Cancel to exit.",
                "Game Rules", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (rules != JOptionPane.OK_OPTION) System.exit(0);

        // --- PLAYER NAME (cancel => exit) ---
        nameX = JOptionPane.showInputDialog(this, "Enter Your Name (X):");
        if (nameX == null) System.exit(0);
        if (nameX.trim().isEmpty()) nameX = "Player X";

        // --- MODE SELECTION (cancel/close => exit) ---
        String[] modes = {"Play vs Smart Computer", "Play with Friend"};
        int mode = JOptionPane.showOptionDialog(this, "Choose how you want to play:",
                "Select Mode", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, modes, modes[0]);
        if (mode == JOptionPane.CLOSED_OPTION) System.exit(0);
        vsAI = (mode == 0);

        if (vsAI) {
            nameO = "Computer (O)";
        } else {
            nameO = JOptionPane.showInputDialog(this, "Enter Friend Name (O):");
            if (nameO == null) System.exit(0);
            if (nameO.trim().isEmpty()) nameO = "Player O";
        }

        // --- UI SETUP ---
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel grid = new JPanel(new GridLayout(3, 3));
        Font f = new Font("SansSerif", Font.BOLD, 48);

        for (int i = 0; i < 9; i++) {
            final int idx = i;
            board[i] = ' ';
            JButton b = new JButton("");
            b.setFont(f);
            b.setFocusPainted(false);
            b.setBackground(Color.WHITE);
            cells[i] = b;
            b.addActionListener(e -> {
                if (board[idx] != ' ') return;
                if (playerTurn) {
                    makeMove(idx, 'X');
                    playerTurn = false;
                    if (!checkEnd()) {
                        if (vsAI) aiMove();
                    }
                } else if (!vsAI) { // friend mode O's turn
                    makeMove(idx, 'O');
                    playerTurn = true;
                }
                updateStatus();
            });
            grid.add(b);
        }

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> resetGame());

        add(status, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
        add(reset, BorderLayout.SOUTH);

        setSize(380, 430);
        setLocationRelativeTo(null);
        setVisible(true);
        updateStatus();
    }

    private void updateStatus() {
        if (evaluate(board) != null) return;
        status.setText(playerTurn ? nameX + " (X) — your turn" :
                (vsAI ? nameO + " (O) — computer thinking..." : nameO + " (O) — your turn"));
    }

    private void makeMove(int idx, char sym) {
        board[idx] = sym;
        cells[idx].setText(String.valueOf(sym));
        cells[idx].setEnabled(false);
        cells[idx].setForeground(sym == 'X' ? Color.BLACK : Color.RED);
    }

    // --- AI (Minimax) ---
    private void aiMove() {
        SwingUtilities.invokeLater(() -> {
            int best = -1, bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) if (board[i] == ' ') {
                board[i] = 'O';
                int s = minimax(board, false);
                board[i] = ' ';
                if (s > bestScore) { bestScore = s; best = i; }
            }
            if (best != -1) makeMove(best, 'O');
            playerTurn = true;
            checkEnd();
            updateStatus();
        });
    }

    private int minimax(char[] b, boolean isMax) {
        Integer score = evaluate(b);
        if (score != null) return score;
        int best = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int i = 0; i < 9; i++) if (b[i] == ' ') {
            b[i] = isMax ? 'O' : 'X';
            int val = minimax(b, !isMax);
            b[i] = ' ';
            best = isMax ? Math.max(best, val) : Math.min(best, val);
        }
        return best;
    }

    // returns +10 if O wins, -10 if X wins, 0 draw, null if not terminal
    private Integer evaluate(char[] b) {
        int[][] win = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };
        for (int[] w : win) {
            if (b[w[0]] != ' ' && b[w[0]] == b[w[1]] && b[w[1]] == b[w[2]]) {
                return (b[w[0]] == 'O') ? +10 : -10;
            }
        }
        for (char c : b) if (c == ' ') return null;
        return 0;
    }

    private boolean checkEnd() {
        Integer e = evaluate(board);
        if (e == null) return false;

        int[] line = winningLine();
        String msg = (e == 10) ? nameO + " (O) wins!" :
                     (e == -10) ? nameX + " (X) wins!" : "Draw!";
        // animate winning line or flash all for draw
        animateWin(line, e == 0, () -> {
            JOptionPane.showMessageDialog(this, msg);
            status.setText(msg);
            for (JButton c : cells) c.setEnabled(false);
        });
        return true;
    }

    // returns winning indices or null if draw/no winner
    private int[] winningLine() {
        int[][] win = {
                {0,1,2},{3,4,5},{6,7,8},
                {0,3,6},{1,4,7},{2,5,8},
                {0,4,8},{2,4,6}
        };
        for (int[] w : win) {
            if (board[w[0]] != ' ' && board[w[0]] == board[w[1]] && board[w[1]] == board[w[2]]) {
                return w;
            }
        }
        return null;
    }

    // Simple flashing animation on winning cells (or all cells if draw).
    // runs callback when animation completes.
    private void animateWin(int[] line, boolean isDraw, Runnable whenDone) {
        final int cycles = 6; // number of flashes
        final int delay = 200; // ms
        Timer t = new Timer(delay, null);
        final int[] count = {0};
        t.addActionListener(e -> {
            boolean on = (count[0] % 2 == 0);
            if (isDraw) {
                // flash all cells
                for (JButton c : cells) c.setBackground(on ? Color.YELLOW : Color.WHITE);
            } else if (line != null) {
                for (int i = 0; i < 9; i++) {
                    if (i == line[0] || i == line[1] || i == line[2]) cells[i].setBackground(on ? Color.YELLOW : Color.WHITE);
                }
            }
            count[0]++;
            if (count[0] > cycles) {
                t.stop();
                // restore neutral backgrounds
                for (JButton c : cells) c.setBackground(Color.WHITE);
                whenDone.run();
            }
        });
        t.setInitialDelay(0);
        t.start();
    }

    private void resetGame() {
        for (int i = 0; i < 9; i++) {
            board[i] = ' ';
            cells[i].setText("");
            cells[i].setEnabled(true);
            cells[i].setBackground(Color.WHITE);
        }
        playerTurn = true;
        updateStatus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmartTicTacToe::new);
    }
}
