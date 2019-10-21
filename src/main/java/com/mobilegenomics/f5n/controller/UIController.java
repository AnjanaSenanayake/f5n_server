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

    static ArrayList<Argument> arguments;
    private static ServerSocket serverSocket;
    private static WrapperObject[] wrapperObjectsArray;
    private static ArrayList<String> componentsNameList;
    private static boolean isLive;

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
            //CheckBox checkBox_prepend = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_prepend" + componentName);
            for (Argument argument : arguments) {
                CheckBox checkBox = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_" + argument.getArgName());
                if (checkBox.getValue() && !argument.isFlagOnly()) {
                    TextField argumentInput = (TextField) findComponentById(pipelineComponentsLayout, componentName + "_textfield_" + argument.getArgName());
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

    public static void runServer() {
        isLive = true;
        try {
            serverSocket = new ServerSocket(6677);
            new Thread(() -> {
                while (isLive) {
                    try {
                        //Establishes connection
                        if (serverSocket.isClosed()) {
                            isLive = false;
                        } else {
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
                                    Response<Boolean, Object> response = allocateJobToClient(objectOutStream, socket.getInetAddress().toString());
                                    WrapperObject receivedObject = (WrapperObject) response.message;
                                    DataController.updateGrids(receivedObject);
                                }
                                if (clientMessage.getState().equals(State.COMPLETED)) {
                                    WrapperObject receivedObject = receiveMessageFromClient(objectInStream);
                                    DataController.updateGrids(receivedObject);
                                }
                                objectOutStream.close();
                            }
                            objectInStream.close();
                        }
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
            serverSocket.close();
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
        WrapperObject wrapperObject = DataController.getIdleWrapperObjectList().get(0);
        try {
            wrapperObject.setState(State.PENDING);
            wrapperObject.setClientIP(clientAddress);
            objectOutStream.writeObject(wrapperObject);
            objectOutStream.flush();
            DataController.getIdleWrapperObjectList().removeIf(item -> (item.getPrefix().equals(wrapperObject.getPrefix())));
            System.out.println("To client= " + wrapperObject.toString());
            return new Response<>(true, wrapperObject);
        } catch (IOException e) {
            e.printStackTrace();
            return new Response<>(false, ErrorMessage.COMM_FAIL);
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
                while (addresses.hasMoreElements()) {
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
