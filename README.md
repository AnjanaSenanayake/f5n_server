
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

### Real Time Sequencing Network
The source code for the Mobile Application can be found [here](https://github.com/SanojPunchihewa/f5n) 

![Diagram](https://github.com/hiruna72/f5n/blob/master/server_mobile_connection.png)

### Quick Start

1) Download a [dataset](https://github.com/nanopore-wgs-consortium/NA12878/blob/master/Genome.md)
2) Pair each reads.fastq(a) and the corresponding (multi)fast5 files. 
3) Compress each pair as a .zip file. (you should now have a folder with many number of .zip files)
4) Run f5n-server
5) Configure the folder path to the folder containing the .zip files.
6) Configure a pipeline
7) Create jobs
8) Connect to f5n-server with mobile phones using the local IP address.

### Features
#### Single Server Socket
Since client-server communication happens only when a job is requested or to get updates of a job, server side is not multi-threaded. Rest of the client requests have to wait while the first client is served. Once a client is served(ex:a job is assigned) the socket is closed. Average service time is minimal thus, we assume mutli-threaded server will not enhance the performance. Also the fact that not all the clients try to connect to the server at the same time supports this. Moreover, there is a considerable time gap between intermediate minIT ouputs. Hence, a client may have to wait until a new job get listed.

#### Flexible File Listener
minIT is changing their output file format continuously. Hence we advice the user to alter the [code](https://github.com/AnjanaSenanayake/f5n_server/blob/106df454fe39f6873115e5e43c196021a847aa99/src/main/java/com/mobilegenomics/f5n/controller/DataController.java#L50) to cater for different scenarios.

## To Do
- [x] stop server & reconfiguration pipeline components
- [x] data set update listner
- [ ] pipeline configuration form validations
- [ ] user messages/warnings/errors to frontend

