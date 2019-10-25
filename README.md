## f5n_server
This is a job server to distribute sequencing tasks among portable devices that run [f5n](https://github.com/SanojPunchihewa/f5n)

### Instructions to deploy server

1) Clone the repository**

`git clone https://github.com/AnjanaSenanayake/f5n_server.git`

2) Inside the cloned directory, compile source code**

`mvn install`

3) Deploy in a jetty server(*Feel free to use any server, jetty dependencies are already added in pom*)**

`mvn jetty:run`

4) Open [localhost](http://localhost:8080/) in any web browser.**

### Flowchart
![Diagram](https://github.com/hiruna72/f5n_server/blob/master/f5n_server_flow_chart.png)

### Network
The source code for the Mobile Application can be found [here](https://github.com/SanojPunchihewa/f5n) 

![Diagram](https://github.com/hiruna72/f5n/blob/master/server_mobile_connection.png)

### Features
#### Single Server Socket
Since client-server communication happens only when a job is requested or to get updates of a job, server side is not multi-threaded. Rest of the client requests have to wait while the first client is served. Once a client is served(ex:a job is assigned) the socket is closed. Average service time is minimal thus, we assume mutli-threaded server will not enhance the performance. Also the fact that not all the clients try to connect to the server at the same time supports this. Moreover, there is a considerable time gap between intermediate minIT ouputs. Hence, a client may have to wait until a new job get listed.

#### Flexible File Listener
minIT is changing their output file format continuously. Hence we advice the user to alter the [code](https://github.com/AnjanaSenanayake/f5n_server/blob/106df454fe39f6873115e5e43c196021a847aa99/src/main/java/com/mobilegenomics/f5n/controller/DataController.java#L50) to cater for different scenarios.

### To Do
- stop server & reconfiguration pipeline components
- data set update listner
