package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.core.Step;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.vaadin.data.provider.ListDataProvider;

import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;

public class DataController {

    static ListDataProvider<WrapperObject> idleListDataProvider;
    static ListDataProvider<WrapperObject> busyListDataProvider;
    private static Long accumulatedJobProcessTime = 0L;
    private static Long averageProcessingTime = 2700000L;//45 mins
    private static Long userSetTimeout = 0L;
    private static boolean isTimeoutSetByUser = false;
    private static Long successJobs = 0L;
    private static WrapperObject[] wrapperObjectsArray;
    private static ArrayList<WrapperObject> idleWrapperObjectList;
    private static ArrayList<WrapperObject> pendingWrapperObjectList;
    private static String SPLITTER = ".zip";
    private static ArrayList<String> filePrefixes = new ArrayList<>();
    private static WatchService watchService;

    // TODO let the user assign the following values
    private static final long statWatchTimerInMinutes = 1; // 1 minute
    private static int idleJobLimit = 10;

    private static float jobCompletionRate = 0;
    private static float jobFailureRate = 0;
    private static float newJobArrivalRate = 0;
    private static float newJobRequestRate = 0;

    private static int totalRunningJobs = 0;
    private static int totalIdleJobs = 0;
    private static int totalPredictedRunningJobs = 0;
    private static int totalPredictedIdleJobs = 0;

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
        if (isAutomate) {
            new Thread(() -> {
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
                            String prefix = file.split(SPLITTER)[0];
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
            }).start();
        } else {
            fileDirMonitorDetach();
        }
    }

    public static void fileDirMonitorDetach() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void createWrapperObjects(String pathToDir, boolean isAutomate) {
        readFilesFromDir(pathToDir);
        idleWrapperObjectList = new ArrayList<>();
        WrapperObject newWrapperObject;
        ArrayList<Step> steps = new ArrayList<>(CoreController.getSteps().values());
        for (String prefix : filePrefixes) {
            newWrapperObject = new WrapperObject(prefix, State.IDLE, "http://" + getLocalIPAddress() + ":8000/", steps);
            idleWrapperObjectList.add(newWrapperObject);
        }
        fileDirMonitorAttach(pathToDir, isAutomate);
    }

    public static void clearWrapperObjects() {
        getIdleWrapperObjectList().clear();
        getPendingWrapperObjectList().clear();
    }

    public static void updateGrids(WrapperObject object) {
        if (object.getState().equals(State.IDLE)) {
            DataController.idleListDataProvider.getItems().add(object);
        } else if (object.getState().equals(State.PENDING)) {
            DataController.idleListDataProvider.getItems().remove(object);
            DataController.busyListDataProvider.getItems().add(object);
        } else if (object.getState().equals(State.SUCCESS)) {
            boolean isValidClient = DataController.busyListDataProvider.getItems().removeIf(item ->
                    (item.getPrefix().equals(object.getPrefix()) && item.getClientIP().equals(object.getClientIP())));
            if (isValidClient) {
                DataController.busyListDataProvider.getItems().add(object);
            }
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

    public static void configureJobProcessTime(WrapperObject wrapperObject) {
        calculateAccumulateJobProcessTime(wrapperObject);
        incrementSuccessJobs();
        calculateAverageJobProcessingTime();
    }

    public static void calculateAccumulateJobProcessTime(WrapperObject wrapperObject) {
        accumulatedJobProcessTime = accumulatedJobProcessTime + (wrapperObject.getCollectTime() - wrapperObject.getReleaseTime());
    }

    public static void calculateAverageJobProcessingTime() {
        averageProcessingTime = accumulatedJobProcessTime / successJobs;
    }

    public static Long getAverageProcessingTime() {
        return averageProcessingTime / 1000;
    }

    public static void setAverageProcessingTime(Long averageProcessingTime) {
        DataController.averageProcessingTime = averageProcessingTime*1000;
    }

    public static Long getProcessingTime() {
        if(!isTimeoutSetByUser) {
            return averageProcessingTime;
        } else {
            return userSetTimeout;
        }
    }

    public static void incrementSuccessJobs() {
        successJobs = successJobs + 1;
    }

    public static void decrementSuccessJobs() {
        successJobs = successJobs - 1;
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

    public static void calculateStats() {
        calculateJobCompletionRate();
        calculateJobFailureRate();
        calculateNewJobArrivalRate();
        calculateNewJobRequestRate();
    }

    public static float getJobCompletionRate() {
        return jobCompletionRate;
    }

    public static float getJobFailureRate() {
        return jobFailureRate;
    }

    public static float getNewJobArrivalRate() {
        return newJobArrivalRate;
    }

    public static float getNewJobRequestRate() {
        return newJobRequestRate;
    }

    // Job Completion rate is equal 1 / averageProcessingTime
    private static void calculateJobCompletionRate() {
        jobCompletionRate = 1.0f / averageProcessingTime;
    }

    // Job Failure rate is equal to jobs could not complete before timeout
    private static void calculateJobFailureRate() {
        Long elapsedTime;
        int totalTimeOutJobs = 0;
        Iterator<WrapperObject> iterator = pendingWrapperObjectList.iterator();
        while (iterator.hasNext()) {
            WrapperObject wrapperObject = iterator.next();
            if (wrapperObject.getState() == State.PENDING) {
                elapsedTime = (System.currentTimeMillis() - wrapperObject.getReleaseTime()) / 1000;
                if (elapsedTime > getProcessingTime()) {
                    totalTimeOutJobs++;
                }
            }
        }
        jobCompletionRate = totalTimeOutJobs / (float) statWatchTimerInMinutes;
    }

    // Calculate New Job Arrival rate and update total Idle Jobs
    private static void calculateNewJobArrivalRate() {
        if (filePrefixes.size() - totalIdleJobs <= 0) {
            newJobRequestRate = 0;
        } else {
            newJobArrivalRate = (filePrefixes.size() - totalIdleJobs) / (float) statWatchTimerInMinutes;
        }
        totalIdleJobs = filePrefixes.size();
    }

    // Calculate New Job Request rate and update total running Jobs
    private static void calculateNewJobRequestRate() {
        newJobRequestRate = (pendingWrapperObjectList.size() - totalRunningJobs) / (float) statWatchTimerInMinutes;
        totalRunningJobs = pendingWrapperObjectList.size();
    }

//    TODO Call this method at the start of the server
//    DataController.calculateStats();
//
//    TODO Call a timer method to repeatedly calculate the stats and update the UI
//    Timer t = new Timer();
//    t.scheduleAtFixedRate(new TimerTask() {
//    @Override
//        public void run() {
//            DataController.calculateStats();
//            TODO Update UI with the following stats
//            DataController.getJobCompletionRate();
//            DataController.getJobFailureRate();
//            DataController.getNewJobArrivalRate();
//            DataController.getNewJobRequestRate();
//        }
//    }, DataController.statWatchTimerInMinutes*60*1000, DataController.statWatchTimerInMinutes*60*1000);

    // TODO Complete the following two prediction methods relating idleJobLimit
    private static void predictNumberOfIdleJobs(long time) {
        totalPredictedIdleJobs = totalIdleJobs - (int) (newJobRequestRate * time) + (int) (newJobArrivalRate * time) + (int) (jobFailureRate * time);
    }

    private static void predictNumberOfRunningJobs(long time) {
        totalPredictedRunningJobs = totalRunningJobs - (int) (jobCompletionRate * totalRunningJobs) + (int) (newJobRequestRate * time);
    }

}
