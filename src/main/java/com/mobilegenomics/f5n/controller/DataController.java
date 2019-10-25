package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.core.Step;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.vaadin.data.provider.ListDataProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Objects;

public class DataController {

    private static Thread fileMonitorThread = null;
    static ListDataProvider<WrapperObject> idleListDataProvider;
    static ListDataProvider<WrapperObject> busyListDataProvider;
    private static WrapperObject[] wrapperObjectsArray;
    private static ArrayList<WrapperObject> idleWrapperObjectList;
    private static ArrayList<WrapperObject> pendingWrapperObjectList;
    private static String SPLITTER = ".tar";
    private static ArrayList<String> filePrefixes = new ArrayList<>();

    public static ListDataProvider<WrapperObject> setListDataProvider(State state) {
        if (state == State.IDLE) {
            idleListDataProvider = new ListDataProvider(idleWrapperObjectList);
            return idleListDataProvider;
        } else {
            pendingWrapperObjectList = new ArrayList<>();
            busyListDataProvider = new ListDataProvider<>(pendingWrapperObjectList);
            return busyListDataProvider;
        }
    }

    static void readFilesFromDir(String pathToDir) {
        File folder = new File(pathToDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    String[] prefix = file.getName().split(SPLITTER);
                    filePrefixes.add(prefix[0]);
                }
            }
        }
    }

    private static void fileDirMonitorAttach(String pathToDir, boolean isAutomate) {
        if(isAutomate) {
            fileMonitorThread = new Thread(() -> {
                WatchService watchService;
                int filesCount = Objects.requireNonNull(new File(pathToDir).list()).length;
                System.out.println(filesCount);

                try {
                    watchService = FileSystems.getDefault().newWatchService();
                    Path path = Paths.get(pathToDir);
                    path.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);

                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            String file = event.context().toString();
                            String prefix = file.split(".tar")[0];
                            System.out.println("Event kind:" + event.kind() + ". File affected: " + file);
                            filePrefixes.add(prefix);
                            ArrayList<Step> steps = new ArrayList<>(CoreController.getSteps().values());
                            WrapperObject newWrapperObject = new WrapperObject(prefix, State.IDLE, pathToDir, steps);
                            updateGrids(newWrapperObject);
                        }
                        key.reset();
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            fileMonitorThread.start();
        }else {
            fileDirMonitorDetach();
        }
    }

    public static void fileDirMonitorDetach() {
        if (fileMonitorThread != null) {
            try {
                fileMonitorThread.interrupt();
                fileMonitorThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static void createWrapperObjects(String pathToDir, boolean isAutomate) {
        readFilesFromDir(pathToDir);
        idleWrapperObjectList = new ArrayList<>();
        WrapperObject newWrapperObject;
        for (String prefix : filePrefixes) {
            ArrayList<Step> steps = new ArrayList<>(CoreController.getSteps().values());
            newWrapperObject = new WrapperObject(prefix, State.IDLE, pathToDir, steps);
            idleWrapperObjectList.add(newWrapperObject);
        }
        fileDirMonitorAttach(pathToDir, isAutomate);
    }

    public static void updateGrids(WrapperObject object) {
        if (object.getState().equals(State.IDLE)) {
            DataController.idleListDataProvider.getItems().add(object);
        } else if (object.getState().equals(State.PENDING)) {
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

    public static ArrayList<String> getFilePrefixes() {
        return filePrefixes;
    }

    public static ArrayList<WrapperObject> getIdleWrapperObjectList() {
        return idleWrapperObjectList;
    }

    public static ArrayList<WrapperObject> getPendingWrapperObjectList() {
        return pendingWrapperObjectList;
    }
}
