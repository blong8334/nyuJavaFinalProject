package client;

import server.Server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.UUID;

public class Client extends JFrame {
    private DataOutputStream toServer = null;
    private DataInputStream fromServer = null;
    private Socket socket = null;
    private JTable resultsTable;
    private JTextField domainInput;
    private boolean isLoading;
    private JTextField depthInput;
    private JButton submitButton;
    private JButton clearButton;
    private DefaultTableModel defaultTableModel;
    private final String key;
    private JTextField threadsInput;
    private JTextArea clientFeedback;
    private StringBuilder feedbackString;

    public static void main(String[] args) {
        JFrame frame = new Client();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public Client() {
        super("Link Lookup");
        key = UUID.randomUUID().toString();
        feedbackString = new StringBuilder();
        isLoading = false;
        createPanel();
        this.setSize(1500, 1000);
        connect();
    }

    public void connect() {
        try {

            socket = new Socket("localhost", 9898);
            toServer = new DataOutputStream(socket.getOutputStream());
            updateClientFeedback("Connected to server");
            new Thread(new Runner(key)).start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public class Runner implements Runnable {
        private final String key;
        public Runner(String key) {
            this.key = key;
        }
        public void run() {
            try {
                fromServer = new DataInputStream(socket.getInputStream());
                boolean buildingResponse = false;
                StringBuilder responseBuilder = new StringBuilder();
                int results = 0;
                while (true) {
                    String message = fromServer.readUTF();
                    if (message.equals(key)) {
                        if (!buildingResponse) {
                            responseBuilder = new StringBuilder();
                            buildingResponse = true;
                            continue;
                        }
                        buildingResponse = false;
                    } else {
                        responseBuilder.append(message);
                        continue;
                    }
                    String[] splitMessage = responseBuilder.toString().split(Server.ROW_SEPARATOR);
                    for (String s : splitMessage) {
                        String[] splitRow = s.split(Server.ITEM_SEPARATOR);
                        if (splitRow.length != 2) {
                            continue;
                        }
                        results++;
                        defaultTableModel.addRow(splitRow);
                    }
                    updateClientFeedback("Found " + results + " links");
                    results = 0;
                    isLoading = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));
        JPanel clientFeedbackPanel = new JPanel();
        clientFeedbackPanel.setLayout(new BoxLayout(clientFeedbackPanel, BoxLayout.X_AXIS));
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.X_AXIS));

        JLabel domainLabel = new JLabel();
        domainLabel.setText("Domain:");
        domainInput = new JTextField();
        Dimension maxSize = new Dimension();
        maxSize.setSize(1000, 100);
        domainInput.setMaximumSize(maxSize);
        JLabel depthLabel = new JLabel();
        depthLabel.setText("Depth:");
        depthInput = new JTextField();
        depthInput.setMaximumSize(maxSize);
        JLabel threadsLabel = new JLabel();
        threadsLabel.setText("Threads:");
        threadsInput = new JTextField();
        threadsInput.setText(Integer.toString(CORES));
        threadsInput.setMaximumSize(maxSize);

        inputPanel.add(domainLabel);
        inputPanel.add(domainInput);
        inputPanel.add(depthLabel);
        inputPanel.add(depthInput);
        inputPanel.add(threadsLabel);
        inputPanel.add(threadsInput);

        submitButton = new JButton();
        submitButton.setText("Submit");
        submitButton.addActionListener(new SubmitButtonListener());
        clearButton = new JButton();
        clearButton.setText("Clear");
        clearButton.addActionListener(new ClearButtonListener());
        actionsPanel.add(submitButton);
        actionsPanel.add(clearButton);

        JLabel clientFeedbackLabel = new JLabel();
        clientFeedbackLabel.setText("Client Feedback:");
        clientFeedback = new JTextArea();
        Dimension maxSizeFeedback = new Dimension();
        maxSize.setSize(1500, 300);
        clientFeedback.setMaximumSize(maxSizeFeedback);
        clientFeedback.setEditable(false);
        updateClientFeedback("Setting threads to default value of available processors");
        clientFeedbackPanel.add(clientFeedbackLabel);
        clientFeedbackPanel.add(new JScrollPane(clientFeedback));

        defaultTableModel = new DefaultTableModel();
        defaultTableModel.addColumn("URL");
        defaultTableModel.addColumn("Depth");
        resultsTable = new JTable(defaultTableModel);
        resultsTable.setGridColor(Color.GRAY);
        resultsTable.setAutoCreateRowSorter(true);
        resultsPanel.add(new JScrollPane(resultsTable));

        mainPanel.add(inputPanel);
        mainPanel.add(actionsPanel);
        mainPanel.add(clientFeedbackPanel);
        mainPanel.add(resultsPanel);
        add(mainPanel);
    }

    class SubmitButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (isLoading) {
                return;
            }
            defaultTableModel.setRowCount(0);
            String domain = domainInput.getText().trim();
            String depth = depthInput.getText().trim();
            String threads = threadsInput.getText().trim();
            if (!domain.startsWith("https://") && !domain.startsWith("http://")) {
                updateClientFeedback("URL must start with https:// or http://");
                return;
            }
            try {
                Integer.parseInt(depth);
            } catch (NumberFormatException ex) {
                updateClientFeedback("Invalid depth provided");
                return;
            }
            try {
                Integer.parseInt(threads);
            } catch (NumberFormatException ex) {
                updateClientFeedback("Invalid threads provided");
                return;
            }
            try {
                isLoading = true;
                toServer.writeUTF(key +
                        Server.ITEM_SEPARATOR +
                        domain +
                        Server.ITEM_SEPARATOR +
                        depth +
                        Server.ITEM_SEPARATOR +
                        threads);
                updateClientFeedback("Loading...");
            } catch (IOException ex) {
                updateClientFeedback(ex.getMessage());
                System.err.println(ex.getMessage());
            }
        }
    }

    class ClearButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            defaultTableModel.setRowCount(0);
            domainInput.setText("");
            depthInput.setText("");
            threadsInput.setText(Integer.toString(CORES));
            feedbackString = new StringBuilder();
            updateClientFeedback("Setting threads to default value of available processors");
        }
    }

    private void updateClientFeedback(String text) {
        feedbackString.append(new Date()).append(" ").append(text).append("\n");
        clientFeedback.setText(feedbackString.toString());
    }

    private static final int CORES = Runtime.getRuntime().availableProcessors();
}
