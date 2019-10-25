## f5n_server
This is a job server to distribute sequencing tasks among portable devices that run [f5n](https://github.com/SanojPunchihewa/f5n)

### Instructions to deploy server

**1) Clone the repository**

`git clone https://github.com/AnjanaSenanayake/f5n_server.git`

**2) Inside the cloned directory, compile source code**

`mvn install`

**3) Deploy in a jetty server(*Feel free to use any server, jetty dependencies are already added in pom*)**

`mvn jetty:run`

**4) Open [localhost](http://localhost:8080/) in any web browser.**

### Network
The source code for the Mobile Application can be found [here](https://github.com/SanojPunchihewa/f5n) 

![Diagram](https://github.com/hiruna72/f5n/blob/master/server_mobile_connection.png)


### To Do
- stop server & reconfiguration pipeline components
- data set update listner
