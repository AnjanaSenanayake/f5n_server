package com.mobilegenomics.f5n.support;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {

    private static ServerSocket serverSocket = null;
    private static Socket socket = null;
    private static ExecutorService executor;

    public static void startFileServer(int port, String fileDir) {
        try {
            serverSocket = new ServerSocket(port);
            new Thread(() -> {
                while (true) {
                    try {
                        socket = serverSocket.accept();
                        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
                        PrintStream pout = new PrintStream(out);
                        System.out.println("Client Connected from: " + socket.getInetAddress());
                        Thread t = new Thread(new CLIENTConnection(socket, fileDir));
                        t.start();
                    } catch (Exception e) {
                        System.err.println("Error in connection attempt.");
                    }
                }
            }).start();
        } catch (IOException e) {
            //ToDo catch exceptions
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

class CLIENTConnection implements Runnable {

    private String fileDir;
    private Socket clientSocket;
    private BufferedReader in = null;

    public CLIENTConnection(Socket client, String fileDir) {
        this.clientSocket = client;
        this.fileDir = fileDir;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String outGoingFileName = in.readLine();
            sendFile(outGoingFileName);
        } catch (IOException ex) {

        }
    }

    public void sendFile(String request) {
        try {
            //handle file read
            String fileName = request.substring(4, request.length()-9).trim();
            File myFile = new File(fileDir+"/"+fileName);

            InputStream fis = new FileInputStream(myFile);
            OutputStream out = clientSocket.getOutputStream();

            byte[] buffer = new byte[(int) myFile.length()];
            while (fis.available()>0)
                out.write(buffer, 0, fis.read(buffer));
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }
}