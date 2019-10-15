package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.core.Step;
import com.mobilegenomics.f5n.dto.ErrorMessage;
import com.mobilegenomics.f5n.dto.Response;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.vaadin.ui.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

public class UIController {

    private static WrapperObject[] wrapperObjectsArray;
    private static ArrayList<String> componentsNameList;
    static ArrayList<Argument> arguments;

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

    public static HashMap<String, Step> getSteps() {
        return CoreController.getSteps();
    }

    public static void configurePipelineComponents(TabSheet pipelineComponentsLayout) {
        for (String componentName : componentsNameList) {
            arguments = UIController.getSteps().get(componentName).getArguments();
            for (Argument argument : arguments) {
                if (!argument.isRequired()) {
                    CheckBox checkBox = (CheckBox) findComponentById(pipelineComponentsLayout, "checkbox_" + argument.getArgName());
                    if (checkBox.getValue() && !argument.isFlagOnly()) {
                        TextField argumentInput = (TextField) findComponentById(pipelineComponentsLayout, "textfield_" + argument.getArgName());
                        if (argumentInput != null && !argumentInput.isEmpty()) {
                            argument.setArgValue(argumentInput.getValue());
                            argument.setSetByUser(true);
                        }
                    } else {
                        if (checkBox.getValue() != null && !checkBox.isEmpty()) {
                            argument.setArgValue(checkBox.getValue().toString());
                            argument.setSetByUser(true);
                        }
                    }
                } else {
                    argument.setSetByUser(true);
                }
                System.out.println(argument.getArgValue());
            }
        }
        CoreController.buildCommandString();
        CoreController.createPipeline();
    }

    public static ArrayList<String> getComponentsNameList() {
        componentsNameList = new ArrayList<>();
        return componentsNameList;
    }

    public static void setComponentsNames(String componentsName) {
        componentsNameList.add(componentsName);
    }

    private static Component findComponentById(HasComponents root, String id) {
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

    public static void configureWrapperObjects(String pathToDir) {
        DataController.readFilesFromDir(pathToDir);
        DataController.createWrapperObjects(pathToDir);
    }

    public static void runServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(6677);
            new Thread(() -> {
                while (true) {
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

                            if (clientMessage.getState().equals(State.CONNECT)) {
                                Response<Boolean, Object> response = allocateJobToClient(objectOutStream, socket.getInetAddress().toString());
                                WrapperObject receivedObject = (WrapperObject) response.message;
                                updateGrids(receivedObject);
                            }
                            if (clientMessage.getState().equals(State.SUCCESS)) {
                                WrapperObject receivedObject = receiveMessageFromClient(objectInStream);
                                updateGrids(receivedObject);
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return receivedObject;
    }

    private static Response<Boolean, Object> allocateJobToClient(ObjectOutputStream objectOutStream, String clientAddress) {
        for (WrapperObject wrapperObject : DataController.getIdleWrapperObjectList()) {
            try {
                wrapperObject.setState(State.PENDING);
                wrapperObject.setClientIP(clientAddress);
                objectOutStream.writeObject(wrapperObject);
                objectOutStream.flush();
                System.out.println("To client= " + wrapperObject.toString());
                return new Response<>(true, wrapperObject);
            } catch (IOException e) {
                e.printStackTrace();
                return new Response<>(false, ErrorMessage.COMM_FAIL);
            }
        }
        return new Response<>(false, ErrorMessage.NULL);
    }

    private static void updateGrids(WrapperObject object) {
        if (object.getState().equals(State.PENDING)) {
            DataController.idleListDataProvider.getItems().remove(object);
            DataController.busyListDataProvider.getItems().add(object);
        } else if (object.getState().equals(State.SUCCESS)) {
            DataController.busyListDataProvider.getItems().removeIf(item -> (item.getPrefix().equals(object.getPrefix())));
            DataController.busyListDataProvider.getItems().add(object);
        }
        refreshGrids();
    }

    private static void refreshGrids() {
        if (DataController.idleListDataProvider != null) {
            DataController.idleListDataProvider.refreshAll();
        }
        if (DataController.busyListDataProvider != null) {
            DataController.busyListDataProvider.refreshAll();
        }
    }

    public static String getLocalIPAddress() {
        String ip = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // *EDIT*
                    if (addr instanceof Inet6Address) continue;

                    ip = addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return ip;
    }
}
