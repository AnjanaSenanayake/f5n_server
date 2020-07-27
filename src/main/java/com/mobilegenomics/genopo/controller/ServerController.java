package com.mobilegenomics.genopo.controller;

import com.mobilegenomics.genopo.MyUI;
import com.mobilegenomics.genopo.dto.ErrorMessage;
import com.mobilegenomics.genopo.dto.Response;
import com.mobilegenomics.genopo.dto.State;
import com.mobilegenomics.genopo.dto.WrapperObject;
import com.mobilegenomics.genopo.support.FileServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerController {
    private static ServerSocket serverSocket;
    private MyUI myUI;

    public void initiateServerUISettings(MyUI myUI) {
        this.myUI = myUI;
    }

    public static void stopServer() {
        try {
            //ToDo catch exceptions
            serverSocket.close();
            FileServer.stopFileServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessageToClient(WrapperObject wrapperObject, ObjectOutputStream objectOutStream) {
        try {
            objectOutStream.writeObject(wrapperObject);
            objectOutStream.flush();
            System.out.println("To client= " + wrapperObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static WrapperObject receiveMessageFromClient(ObjectInputStream objectInStream) {
        WrapperObject receivedObject = null;
        try {
            receivedObject = (WrapperObject) objectInStream.readObject();
            System.out.println("From client= " + receivedObject.toString());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return receivedObject;
    }

    private static Response<Boolean, Object> allocateJobToClient(ObjectOutputStream objectOutStream, String clientAddress) {
        try {
            WrapperObject wrapperObject = DataController.getIdleWrapperObjectList().get(0);
            wrapperObject.setState(State.PENDING);
            wrapperObject.setClientIP(clientAddress);
            wrapperObject.setReleaseTime(System.currentTimeMillis());
            objectOutStream.writeObject(wrapperObject);
            objectOutStream.flush();
            DataController.getIdleWrapperObjectList().removeIf(item -> (item.getPrefix().equals(wrapperObject.getPrefix())));
            System.out.println("To client" +
                    " = " + wrapperObject.toString());
            return new Response<>(true, wrapperObject);
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            return new Response<>(false, ErrorMessage.COMM_FAIL);
        }
    }

    private static void assessTimedOutJobs() {
        long elapsedTime;
        ArrayList<WrapperObject> list = DataController.getPendingWrapperObjectList();
        Iterator<WrapperObject> iterator = list.iterator();
        while (iterator.hasNext()) {
            WrapperObject wrapperObject = iterator.next();
            if (wrapperObject.getState() == State.PENDING) {
                elapsedTime = (System.currentTimeMillis() - wrapperObject.getReleaseTime()) / 1000;
                if (elapsedTime > DataController.getProcessingTime()) {
                    iterator.remove();
                    wrapperObject.setState(State.IDLE);
                    DataController.idleListDataProvider.getItems().add(wrapperObject);
                }
            }
        }
    }

    public static void runServer() {
        try {
            serverSocket = new ServerSocket(6677);
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        //Establishes connection
                        Socket socket = serverSocket.accept();
                        socket.setSoTimeout(2000); // Time out after 2000 milli seconds
                        System.out.println("A client is connected: " + socket.getLocalSocketAddress());
                        ObjectInputStream objectInStream;
                        objectInStream = new ObjectInputStream(socket.getInputStream());
                        if (objectInStream.available() == 0) {
                            WrapperObject clientMessage = (WrapperObject) objectInStream.readObject();
                            System.out.println("From client= " + clientMessage.toString());

                            ObjectOutputStream objectOutStream = new ObjectOutputStream(socket.getOutputStream());
                            WrapperObject replyObject = new WrapperObject();
                            replyObject.setState(State.ACK);
                            sendMessageToClient(replyObject, objectOutStream);

                            if (clientMessage.getState().equals(State.REQUEST)) {
                                assessTimedOutJobs();
                                Response<Boolean, Object> response = allocateJobToClient(objectOutStream, socket.getInetAddress().toString());
                                WrapperObject sentJobObject = (WrapperObject) response.message;
                                DataController.updateGrids(sentJobObject);
                            }
                            if (clientMessage.getState().equals(State.COMPLETED)) {
                                WrapperObject receivedObject = receiveMessageFromClient(objectInStream);
                                receivedObject.setCollectTime(System.currentTimeMillis());
                                DataController.updateGrids(receivedObject);
                                DataController.configureJobProcessTime(receivedObject);
                                receivedObject = DataController.addServerSideReport(receivedObject);
                                DataController.writeSummaryLogToFile(receivedObject.getPrefix(), receivedObject.getResultSummery());
                                MyUI.averageProcessingTimeLabel.setValue(DataController.getAverageProcessingTime() + "s");
                                System.out.println("Average Processing Time: " + DataController.getAverageProcessingTime() + " s");
                            }
                            objectOutStream.close();
                        }
                        objectInStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
