import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Project {
    private static final Color BACKGROUND = new Color(17, 24, 39);
    private static final Color PANEL = new Color(31, 41, 55);
    private static final Color TEXT = new Color(229, 231, 235);
    private static final Color MUTED = new Color(156, 163, 175);
    private static final Color BLUE = new Color(59, 130, 246);
    private static final Color GREEN = new Color(52, 211, 153);
    private static final Color GOLD = new Color(251, 191, 36);

    private final JComboBox<String> examplePicker = new JComboBox<>(new String[] {
        "Factorial", "Fibonacci", "Sum from 1 to n", "Binary search"
    });
    private final JSpinner inputSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 12, 1));
    private final JLabel inputLabel = new JLabel("n:");
    private final JLabel statusLabel = new JLabel("Choose an example, then press Start.");
    private final JLabel resultLabel = new JLabel("Result: —");
    private final JLabel stepLabel = new JLabel("Step 0 / 0");
    private final JTextArea stackArea = new JTextArea();
    private final JTextArea explanationArea = new JTextArea();
    private final TreePanel treePanel = new TreePanel();
    private final JButton startButton = new JButton("Start");
    private final JButton previousButton = new JButton("◀ Back");
    private final JButton nextButton = new JButton("Next ▶");
    private final JButton playButton = new JButton("▶ Play");
    private final Timer timer = new Timer(650, this::advance);

    private List<TraceEvent> trace = Collections.emptyList();
    private int currentStep;

    private Project() {
        JFrame frame = new JFrame("Recursion Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(1120, 760);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(buildInterface());

        examplePicker.addActionListener(event -> updateInputRange());
        startButton.addActionListener(event -> startTrace());
        previousButton.addActionListener(event -> showStep(currentStep - 1));
        nextButton.addActionListener(event -> showStep(currentStep + 1));
        playButton.addActionListener(event -> togglePlayback());

        updateInputRange();
        refreshStep();
        frame.setVisible(true);
    }

    private JPanel buildInterface() {
        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("Recursion Visualizer");
        title.setForeground(TEXT);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));

        JLabel subtitle = new JLabel("Follow each function call down, then watch the answers return back up.");
        subtitle.setForeground(MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(subtitle, BorderLayout.CENTER);
        root.add(heading, BorderLayout.NORTH);

        JPanel controls = new JPanel();
        controls.setBackground(PANEL);
        controls.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel exampleLabel = new JLabel("Example:");
        exampleLabel.setForeground(TEXT);
        inputLabel.setForeground(TEXT);
        controls.add(exampleLabel);
        controls.add(examplePicker);
        controls.add(inputLabel);
        controls.add(inputSpinner);
        controls.add(startButton);
        controls.add(previousButton);
        controls.add(nextButton);
        controls.add(playButton);

        stackArea.setEditable(false);
        stackArea.setBackground(PANEL);
        stackArea.setForeground(TEXT);
        stackArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        stackArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setBackground(PANEL);
        explanationArea.setForeground(MUTED);
        explanationArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        explanationArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel details = new JPanel(new BorderLayout(0, 10));
        details.setBackground(BACKGROUND);
        details.setPreferredSize(new Dimension(245, 300));
        details.add(section("CALL STACK", new JScrollPane(stackArea)), BorderLayout.CENTER);
        details.add(section("WHAT'S HAPPENING", new JScrollPane(explanationArea)), BorderLayout.SOUTH);
        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81)));
        treeScroll.getViewport().setBackground(BACKGROUND);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(treeScroll, BorderLayout.CENTER);
        center.add(details, BorderLayout.EAST);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        statusLabel.setForeground(TEXT);
        resultLabel.setForeground(GREEN);
        resultLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        stepLabel.setForeground(MUTED);
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(resultLabel, BorderLayout.CENTER);
        footer.add(stepLabel, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(controls, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel section(String heading, JScrollPane content) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(BACKGROUND);
        JLabel label = new JLabel(heading);
        label.setForeground(MUTED);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        panel.add(label, BorderLayout.NORTH);
        content.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81)));
        content.getViewport().setBackground(PANEL);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void updateInputRange() {
        String example = (String) examplePicker.getSelectedItem();
        boolean binarySearch = "Binary search".equals(example);
        inputLabel.setText(binarySearch ? "Target:" : "n:");
        inputSpinner.setModel(binarySearch
            ? new SpinnerNumberModel(18, 0, 34, 1)
            : new SpinnerNumberModel(5, 0, 12, 1));
    }

    private void startTrace() {
        timer.stop();
        playButton.setText("▶ Play");
        int value = ((Number) inputSpinner.getValue()).intValue();
        String example = (String) examplePicker.getSelectedItem();
        Node root;
        try {
            root = buildExample(example, value);
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "Cannot start example",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        treePanel.setRoot(root);
        trace = treePanel.events;
        currentStep = 0;
        statusLabel.setText(example + " — " + (example.equals("Binary search")
            ? "searching a sorted array for " + value
            : "starting with " + value));
        resultLabel.setText("Result: " + root.result);
        refreshStep();
    }

    private Node buildExample(String example, int value) {
        treePanel.events.clear();
        treePanel.allNodes.clear();
        if ("Factorial".equals(example)) {
            return factorial(value, null);
        }
        if ("Fibonacci".equals(example)) {
            if (value > 10) {
                throw new IllegalArgumentException("Fibonacci is limited to n ≤ 10 so the full call tree stays readable.");
            }
            return fibonacci(value, null);
        }
        if ("Sum from 1 to n".equals(example)) {
            return sumTo(value, null);
        }
        int[] values = new int[16];
        for (int i = 0; i < values.length; i++) {
            values[i] = (i + 1) * 2;
        }
        return binarySearch(values, value, 0, values.length - 1, null);
    }

    private Node factorial(int n, Node parent) {
        Node node = beginCall("factorial(" + n + ")", parent);
        if (n <= 1) {
            node.result = 1;
            node.detail = "Base case: 0! and 1! are 1.";
        } else {
            Node child = factorial(n - 1, node);
            node.result = n * child.result;
            node.detail = n + " × " + child.result + " = " + node.result;
        }
        endCall(node);
        return node;
    }

    private Node fibonacci(int n, Node parent) {
        Node node = beginCall("fib(" + n + ")", parent);
        if (n <= 1) {
            node.result = n;
            node.detail = "Base case: fib(" + n + ") = " + n + ".";
        } else {
            Node left = fibonacci(n - 1, node);
            Node right = fibonacci(n - 2, node);
            node.result = left.result + right.result;
            node.detail = left.result + " + " + right.result + " = " + node.result;
        }
        endCall(node);
        return node;
    }

    private Node sumTo(int n, Node parent) {
        Node node = beginCall("sum(" + n + ")", parent);
        if (n <= 0) {
            node.result = 0;
            node.detail = "Base case: sum(0) = 0.";
        } else {
            Node child = sumTo(n - 1, node);
            node.result = n + child.result;
            node.detail = n + " + " + child.result + " = " + node.result;
        }
        endCall(node);
        return node;
    }

    private Node binarySearch(int[] values, int target, int low, int high, Node parent) {
        Node node = beginCall("search(" + low + "…" + high + ")", parent);
        if (low > high) {
            node.result = -1;
            node.detail = "Base case: the range is empty, so the target is absent.";
        } else {
            int middle = low + (high - low) / 2;
            int guess = values[middle];
            node.detail = "Middle value is " + guess + "; target is " + target + ".";
            if (guess == target) {
                node.result = middle;
                node.detail = "Found " + target + " at index " + middle + ".";
            } else if (target < guess) {
                node.detail += " Recurse into the left half.";
                Node child = binarySearch(values, target, low, middle - 1, node);
                node.result = child.result;
            } else {
                node.detail += " Recurse into the right half.";
                Node child = binarySearch(values, target, middle + 1, high, node);
                node.result = child.result;
            }
        }
        endCall(node);
        return node;
    }

    private Node beginCall(String label, Node parent) {
        Node node = new Node(label, parent);
        if (parent != null) {
            parent.children.add(node);
        }
        treePanel.allNodes.add(node);
        treePanel.events.add(new TraceEvent(node, true));
        return node;
    }

    private void endCall(Node node) {
        treePanel.events.add(new TraceEvent(node, false));
    }

    private void togglePlayback() {
        if (trace.isEmpty()) {
            startTrace();
        }
        if (trace.isEmpty()) {
            return;
        }
        if (timer.isRunning()) {
            timer.stop();
            playButton.setText("▶ Play");
        } else if (currentStep < trace.size()) {
            timer.start();
            playButton.setText("Ⅱ Pause");
        } else {
            showStep(0);
            timer.start();
            playButton.setText("Ⅱ Pause");
        }
    }

    private void advance(ActionEvent event) {
        if (currentStep >= trace.size()) {
            timer.stop();
            playButton.setText("▶ Play");
        } else {
            showStep(currentStep + 1);
        }
    }

    private void showStep(int step) {
        timer.stop();
        playButton.setText("▶ Play");
        currentStep = Math.max(0, Math.min(step, trace.size()));
        refreshStep();
    }

    private void refreshStep() {
        List<Node> stack = new ArrayList<>();
        for (int i = 0; i < currentStep; i++) {
            TraceEvent event = trace.get(i);
            if (event.call) {
                stack.add(event.node);
            } else if (!stack.isEmpty()) {
                stack.remove(stack.size() - 1);
            }
        }
        Node active = stack.isEmpty() ? null : stack.get(stack.size() - 1);
        treePanel.setProgress(currentStep, active);
        if (stack.isEmpty()) {
            stackArea.setText(currentStep == 0
                ? "Press Start to build\na recursive call tree."
                : "All calls have returned.");
        } else {
            StringBuilder text = new StringBuilder();
            for (int i = stack.size() - 1; i >= 0; i--) {
                text.append(i == stack.size() - 1 ? "▶ " : "  ")
                    .append(stack.get(i).label).append('\n');
            }
            stackArea.setText(text.toString());
        }
        if (currentStep == 0) {
            explanationArea.setText("Each node is one function call. Step through to see a call pause, make smaller calls, and combine their returned answers.");
        } else {
            TraceEvent event = trace.get(currentStep - 1);
            explanationArea.setText(event.call
                ? "Calling " + event.node.label + ". " + event.node.detail
                : event.node.label + " returns " + event.node.result + ". " + event.node.detail);
        }
        stepLabel.setText("Step " + currentStep + " / " + trace.size());
        previousButton.setEnabled(currentStep > 0);
        nextButton.setEnabled(currentStep < trace.size());
        treePanel.repaint();
    }

    private static final class Node {
        private final String label;
        private final List<Node> children = new ArrayList<>();
        private long result;
        private String detail = "";
        private boolean called;
        private boolean returned;

        private Node(String label, Node parent) {
            this.label = label;
        }
    }

    private static final class TraceEvent {
        private final Node node;
        private final boolean call;

        private TraceEvent(Node node, boolean call) {
            this.node = node;
            this.call = call;
        }
    }

    @SuppressWarnings("serial")
    private static final class TreePanel extends JPanel {
        private final List<Node> allNodes = new ArrayList<>();
        private final List<TraceEvent> events = new ArrayList<>();
        private Node root;
        private Node active;
        private int progress;
        private int maxDepth;

        private TreePanel() {
            setBackground(BACKGROUND);
            setPreferredSize(new Dimension(680, 480));
        }

        private void setRoot(Node root) {
            this.root = root;
            maxDepth = depth(root);
            Dimension size = new Dimension(Math.max(680, leaves(root) * 112 + 50),
                Math.max(480, (maxDepth + 1) * 96 + 55));
            setPreferredSize(size);
            revalidate();
            repaint();
        }

        private void setProgress(int progress, Node active) {
            this.progress = progress;
            this.active = active;
            for (Node node : allNodes) {
                node.called = false;
                node.returned = false;
            }
            for (int i = 0; i < progress; i++) {
                TraceEvent event = events.get(i);
                if (event.call) {
                    event.node.called = true;
                } else {
                    event.node.returned = true;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (root == null) {
                g.setColor(MUTED);
                g.setFont(new Font("SansSerif", Font.PLAIN, 15));
                g.drawString("Start an example to see its recursive call tree.", 28, 42);
            } else {
                int totalLeaves = leaves(root);
                int left = Math.max(0, (getWidth() - totalLeaves * 112) / 2);
                paintLinks(g, root, left, 42, getWidth() / 2, true);
                paintNodes(g, root, left, 42, 0);
                paintLegend(g);
            }
            g.dispose();
        }

        private void paintLinks(Graphics2D g, Node node, double left, int y, double width, boolean isRoot) {
            if (node.children.isEmpty()) {
                return;
            }
            double cursor = left;
            int childY = y + 96;
            for (Node child : node.children) {
                int childLeaves = leaves(child);
                double childWidth = childLeaves * 112.0;
                double childCenter = cursor + childWidth / 2;
                g.setColor(child.called ? new Color(75, 85, 99) : new Color(55, 65, 81));
                g.setStroke(new BasicStroke(1.4f));
                g.drawLine((int) width, y + 19, (int) childCenter, childY);
                paintLinks(g, child, cursor, childY, childCenter, false);
                cursor += childWidth;
            }
        }

        private void paintNodes(Graphics2D g, Node node, double left, int y, int depth) {
            double cursor = left;
            double center;
            if (node.children.isEmpty()) {
                center = left + 56;
            } else {
                double first = -1;
                double last = -1;
                for (Node child : node.children) {
                    double childLeaves = leaves(child);
                    double childWidth = childLeaves * 112.0;
                    double childCenter = cursor + childWidth / 2;
                    if (first < 0) {
                        first = childCenter;
                    }
                    last = childCenter;
                    paintNodes(g, child, cursor, y + 96, depth + 1);
                    cursor += childWidth;
                }
                center = (first + last) / 2;
            }

            int x = (int) center - 47;
            int boxY = y - 2;
            Color fill = node == active ? BLUE : node.returned ? new Color(6, 78, 59)
                : node.called ? new Color(120, 53, 15) : PANEL;
            Color border = node == active ? new Color(147, 197, 253)
                : node.returned ? GREEN : node.called ? GOLD : new Color(75, 85, 99);
            g.setColor(fill);
            g.fillRoundRect(x, boxY, 94, 42, 12, 12);
            g.setColor(border);
            g.setStroke(new BasicStroke(node == active ? 2.5f : 1.2f));
            g.drawRoundRect(x, boxY, 94, 42, 12, 12);

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            drawCentered(g, node.label, (int) center, boxY + 17, TEXT);
            String state = node.returned ? "= " + node.result : node.called ? "running…" : "waiting";
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            drawCentered(g, state, (int) center, boxY + 33, node.returned ? GREEN : MUTED);
        }

        private void paintLegend(Graphics2D g) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(MUTED);
            g.drawString("Waiting", 15, getHeight() - 18);
            g.setColor(GOLD);
            g.drawString("Running", 86, getHeight() - 18);
            g.setColor(GREEN);
            g.drawString("Returned", 165, getHeight() - 18);
        }

        private void drawCentered(Graphics2D g, String text, int centerX, int y, Color color) {
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(color);
            g.drawString(text, centerX - metrics.stringWidth(text) / 2, y);
        }

        private int depth(Node node) {
            int result = 0;
            for (Node child : node.children) {
                result = Math.max(result, 1 + depth(child));
            }
            return result;
        }

        private int leaves(Node node) {
            if (node.children.isEmpty()) {
                return 1;
            }
            int result = 0;
            for (Node child : node.children) {
                result += leaves(child);
            }
            return result;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Project::new);
    }
}