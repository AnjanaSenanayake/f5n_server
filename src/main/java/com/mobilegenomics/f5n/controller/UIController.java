package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.MyUI;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.core.Step;
import com.mobilegenomics.f5n.dto.ErrorMessage;
import com.mobilegenomics.f5n.dto.Response;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.mobilegenomics.f5n.support.FileServer;
import com.vaadin.ui.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class UIController {

    static ArrayList<Argument> arguments;
    private static ServerSocket serverSocket;
    private static WrapperObject[] wrapperObjectsArray;
    private static ArrayList<String> componentsNameList;

    public static void addPipelineSteps(Set<String> checkedPipelineSteps) {
        for (PipelineStep pipelineStep : PipelineStep.values()) {
            if (checkedPipelineSteps.contains(pipelineStep.name())) {
                CoreController.addPipelineStep(pipelineStep);
            }
        }
        CoreController.printList();
        CoreController.configureSteps();
    }

    public static void eraseSelectedPipelineSteps() {
        CoreController.eraseSelectedPipeline();
    }

    public static TreeMap<Integer, Step> getSteps() {
        return CoreController.getSteps();
    }

    //TODO fix format sam paf checkboxes in minmap sequence alignment
    public static void configurePipelineComponents(TabSheet pipelineComponentsLayout) {
        String DATA_SET_PATH = "$DATA_SET_PATH/";
        for (String componentName : componentsNameList) {
            arguments = UIController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments();
            //CheckBox checkBox_prepend = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_prepend" + componentName);
            for (Argument argument : arguments) {
                CheckBox checkBox = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_" + argument.getArgName());
                if (checkBox.getValue() && !argument.isFlagOnly()) {
                    TextField argumentInput = (TextField) findComponentById(pipelineComponentsLayout, componentName + "_textfield_" + argument.getArgName());
                    if (argumentInput != null && !argumentInput.isEmpty()) {
                        argument.setArgValue(DATA_SET_PATH + "" + argumentInput.getValue());
                        argument.setSetByUser(true);
                    }
                } else {
                    if (checkBox.getValue() != null && !checkBox.isEmpty()) {
                        //argument.setArgValue(checkBox.getValue().toString());
                        argument.setSetByUser(true);
                    }
                }

                System.out.println(argument.getArgValue());
            }
        }
        CoreController.buildCommandString();
        //CoreController.createPipeline();
    }

    public static ArrayList<String> getComponentsNameList() {
        componentsNameList = new ArrayList<>();
        return componentsNameList;
    }

    public static void setComponentsNames(String componentsName) {
        componentsNameList.add(componentsName);
    }

    public static Component findComponentById(HasComponents root, String id) {
        for (Component child : root) {
            if (id.equals(child.getId())) {
                return child; // found it!
            } else if (child instanceof HasComponents) { // recursively go through all children that themselves have children
                Component result = findComponentById((HasComponents) child, id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null; // none was found
    }

    public static void configureWrapperObjects(String pathToDir, boolean isAutomate) {
        DataController.createWrapperObjects(pathToDir, isAutomate);
    }

    public static void clearWrapperObjects() {
        DataController.clearWrapperObjects();
    }

    public static void runServer() {
        try {
            serverSocket = new ServerSocket(6677);
            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        //Establishes connection
                        Socket socket = serverSocket.accept();
                        System.out.println("A client is connected: " + socket.getLocalSocketAddress());
                        ObjectInputStream objectInStream = null;
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

    public static void stopServer() {
        try {
            //ToDo catch exceptions
            serverSocket.close();
            FileServer.stopFileServer();
        } catch (IOException e) {
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
                MyUI.jobCompletionRateLabel.setValue(String.valueOf(DataController.getJobCompletionRate()));
                MyUI.jobFailureRateLabel.setValue(String.valueOf(DataController.getJobFailureRate()));
                MyUI.newJobArrivalRateLabel.setValue(String.valueOf(DataController.getNewJobArrivalRate()));
                MyUI.newJobRequestRateLabel.setValue(String.valueOf(DataController.getNewJobRequestRate()));
            }
        }, DataController.statWatchTimerInMinutes * 60 * 1000, DataController.statWatchTimerInMinutes * 60 * 1000);
    }

    public static void resetServerStatisticsCalc() {
        MyUI.jobCompletionRateLabel.setValue(String.valueOf(0));
        MyUI.jobFailureRateLabel.setValue(String.valueOf(0));
        MyUI.newJobArrivalRateLabel.setValue(String.valueOf(0));
        MyUI.newJobRequestRateLabel.setValue(String.valueOf(0));
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
        Long elapsedTime = 0L;
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
}
