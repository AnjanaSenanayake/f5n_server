# f5n_server
This is a job server to distribute and keep track of f5n pipeline jobs to connected f5n clients

## Instructions to deploy server

**1) Clone the repository**

`git clone https://github.com/AnjanaSenanayake/f5n_server.git`

**2) Inside the cloned directory, compile source code**

`mvn install`

**3) Deploy in a jetty server(*Feel free to use any server, jetty dependencies are already added in pom*)**

`mvn jetty:run`

**4) Open [localhost](http://localhost:8080/) in any web browser.**

## To Do
- [x] stop server & reconfiguration pipeline components
- [x] data set update listner
- [ ] user messages/warnings/errors to frontend
