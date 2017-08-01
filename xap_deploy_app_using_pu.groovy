def xap_server = "10.8.4.88"
def mgt_port = "8090"

def partitionCount = 1
def hasBackup = true
def requiresIsolation=true

def backupsPerPartition = (hasBackup ? 1 : 0)

def gscHeapSize = "512m"

def appName = "shoppingcart-rest"

def projectDir = "/home/ubuntu/rest-application-example"
def appPuDir = "${projectDir}/rest/target"
def appPuName = "shoppingcart-rest.war"
def appPuPath = "${appPuDir}/${appPuName}"

node {

  //Print Available Host in Service Grid
  stage("Available Hosts Check"){
    def availableHostResponse = 
          httpRequest httpMode:"GET", 
            consoleLogResponseBody:true, 
            contentType:"APPLICATION_JSON",
            url:"http://${xap_server}:${mgt_port}/v1/hosts"

    println("availableHostResponse Response Code "+availableHostResponse.status)
    println("availableHostResponse Content: "     +availableHostResponse.content)
  }

  //Create required GSCs
  stage("Create GSCs"){

    def gscCount = partitionCount

    for (int i = 0; i < partitionCount; i++) {
      // create GSC for primary partition
      def createContainerForPrimaryResponse =  
            httpRequest httpMode:"POST",
              consoleLogResponseBody:true,
              contentType:"APPLICATION_JSON", 
              requestBody:"""{
                "host":"${xap_server}",
                "memory":"${gscHeapSize}"
              }""",
              url:"http://${xap_server}:${mgt_port}/v1/containers"

      println("createContainerResponse Response Code: "+createContainerForPrimaryResponse.status)

      sleep(10)//Create Container is asynchronous

      if (hasBackup) {
        // create GSC for backup partition
        def createContainerForBackupResponse =  
              httpRequest httpMode:"POST", 
                consoleLogResponseBody:true, 
                contentType:"APPLICATION_JSON", 
                requestBody:"""{
                  "host":"${xap_server}",
                  "memory":"${gscHeapSize}"
                }""",
                url:"http://${xap_server}:${mgt_port}/v1/containers"

        println("createContainerResponse Response Code: "+createContainerForBackupResponse.status)

        sleep(10)//Create Container is asynchronous
      }

    }

  }

  //Deploy App using PU
  stage("Deploy App using PU"){
    // first upload PU jar file. The jar file will be an available as a "resource" which has the associated name same as the filename.
    // The file will be placed in the "work/RESTResources" folder under the XAP installation folder
    sh """
      curl -X PUT -F file=@${appPuPath} http://${xap_server}:${mgt_port}/v1/deployments/resources
    """
    
    sleep(10)

    // now deploy the uploaded PU jar 
    def response = 
      httpRequest httpMode:"POST", 
        consoleLogResponseBody:true, 
        contentType:"APPLICATION_JSON", 
        requestBody:"""{
          "name":"${appName}",
          "resource":"${appPuName}",
          "topology":{
            "schema": "partitioned",
            "partitions": ${partitionCount},
            "backupsPerPartition": ${backupsPerPartition}
          },
          "sla":{
            "requiresIsolation": ${requiresIsolation},
            "maxInstancesPerVM": 1
          },
          "contextProperties":{}
        }""",
        url: "http://${xap_server}:${mgt_port}/v1/deployments"

    println("response Code: " + response.status)
    println("response Content: " + response.content)

    sleep(15)
  }


  //Confirm Deployment of App
  stage("Available App Check"){
    def deploymentsResponse =  
      httpRequest httpMode:"GET", 
        consoleLogResponseBody:true, 
        contentType:"APPLICATION_JSON", 
        url:"http://${xap_server}:${mgt_port}/v1/deployments/${appName}"
    
    println("deployments Response Code: " + deploymentsResponse.status)
    println("deploymentsResponse Content: " + deploymentsResponse.content)
  }

}

