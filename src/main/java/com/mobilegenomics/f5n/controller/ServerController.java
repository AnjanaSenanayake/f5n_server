package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.MyUI;
import com.mobilegenomics.f5n.dto.ErrorMessage;
import com.mobilegenomics.f5n.dto.Response;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.mobilegenomics.f5n.support.FileServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class ServerController {
    private static ServerSocket serverSocket;
    private static MyUI myUI = new MyUI();

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
                                myUI.averageProcessingTimeLabel.setValue(DataController.getAverageProcessingTime() + "s");
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

    public static void startServerStatisticsCalc() {
        DataController.calculateStats();

        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                DataController.calculateStats();
                myUI.jobCompletionRateLabel.setValue(String.valueOf(DataController.getJobCompletionRate()));
                myUI.jobFailureRateLabel.setValue(String.valueOf(DataController.getJobFailureRate()));
                myUI.newJobArrivalRateLabel.setValue(String.valueOf(DataController.getNewJobArrivalRate()));
                myUI.newJobRequestRateLabel.setValue(String.valueOf(DataController.getNewJobRequestRate()));
            }
        }, DataController.statWatchTimerInMinutes * 60 * 1000, DataController.statWatchTimerInMinutes * 60 * 1000);
    }

    public static void resetServerStatisticsCalc() {
        myUI.jobCompletionRateLabel.setValue(String.valueOf(0));
        myUI.jobFailureRateLabel.setValue(String.valueOf(0));
        myUI.newJobArrivalRateLabel.setValue(String.valueOf(0));
        myUI.newJobRequestRateLabel.setValue(String.valueOf(0));
    }
}
