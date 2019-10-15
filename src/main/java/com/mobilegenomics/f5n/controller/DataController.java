package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.vaadin.data.provider.ListDataProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public class DataController {

    static ListDataProvider<WrapperObject> idleListDataProvider;
    static ListDataProvider<WrapperObject> busyListDataProvider;
    private static WrapperObject[] wrapperObjectsArray;
    private static ArrayList<WrapperObject> idleWrapperObjectList;
    private static ArrayList<WrapperObject> pendingWrapperObjectList;
    private static String SPLITTER = ".";
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
                    String[] prefix = file.getName().split(".tar");
                    filePrefixes.add(prefix[0]);
                }
            }
        }
    }

    public static void fileMonitor(String pathToDir) {
        WatchService watchService;
        try {
            int filesCount = new File(pathToDir).list().length;
            System.out.println(filesCount);

            watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(pathToDir);
            path.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    System.out.println(
                            "Event kind:" + event.kind()
                                    + ". File affected: " + event.context() + "count" + event.count());
                    filePrefixes.add(event.context().toString().split(".")[0]);
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void createWrapperObjects(String pathToDir) {
        idleWrapperObjectList = new ArrayList<>();
        WrapperObject newWrapperObject;
        for (String prefix : filePrefixes) {
            newWrapperObject = new WrapperObject("ID_" + prefix, State.IDLE, pathToDir, CoreController.getPipelineComponents());
            idleWrapperObjectList.add(newWrapperObject);
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
