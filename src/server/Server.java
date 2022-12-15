package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;

public class Server {

    public static void main(String[] args) {
        new Server().run();
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Starting a thread for a new client at " + new Date());
                ClientHandler clientHandler = new ClientHandler(socket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class ClientHandler implements Runnable {

        private final Socket socket;
        private DataOutputStream dataOutputStream;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    try {
                        try {
                            String input = inputFromClient.readUTF();
                            String[] splitInput = input.split(ITEM_SEPARATOR);
                            if (splitInput.length != 4) {
                                writeToClient("");
                                return;
                            }
                            String key = splitInput[0];
                            String domain = splitInput[1];
                            int depth = Integer.parseInt(splitInput[2]);
                            int threads = Integer.parseInt(splitInput[3]);
                            LinkRetriever linkRetriever = new LinkRetriever(domain, depth, threads);
                            Collection<UrlItem> results = linkRetriever.getResults();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (UrlItem item : results) {
                                URL url = item.url;
                                stringBuilder.append(url.getHost())
                                        .append(url.getPath())
                                        .append(ITEM_SEPARATOR)
                                        .append(item.depth)
                                        .append(ROW_SEPARATOR);
                            }
                            writeToClient(key);
                            String[] chunks = stringBuilder.toString().split("(?<=\\G.{" + CHUNK_SIZE + "})");
                            for (String chunk : chunks) {
                                writeToClient(chunk);
                            }
                            writeToClient(key);
                        } catch (SocketException se) {
                            System.err.println(se.getMessage());
                        }
                    } catch (EOFException ex) {
                        inputFromClient.close();
                        dataOutputStream.close();
                        this.socket.close();
                        break;
                    }
                }
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }

        public void writeToClient(String message) throws IOException {
            dataOutputStream.writeUTF(message);
        }
    }

    private static final int PORT = 9898;
    private static final int CHUNK_SIZE = 8000;
    public static final String ITEM_SEPARATOR = ",";
    public static final String ROW_SEPARATOR = ";";
}
