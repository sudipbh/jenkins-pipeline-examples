def xap_server = "10.8.4.88"
def mgt_port = "8090"

def partitionCount = 1
def hasBackup = true
def requiresIsolation=true

def backupCount = (hasBackup ? 1 : 0)

def gscHeapSize = "512m"

def spaceName = "mySpace"

//Print Available Host in Service Grid
stage("Available Hosts Check"){
  def availableHostResponse =  
    httpRequest httpMode:"GET", consoleLogResponseBody:true, contentType:"APPLICATION_JSON",  url:"http://${xap_server}:${mgt_port}/v1/hosts"

  println("availableHostResponse Response Code "+availableHostResponse.status)
  println("availableHostResponse Content: "     +availableHostResponse.content)
}

//Create required GSCs
stage("Create GSCs"){

  def gscCount = partitionCount

  for (int i = 0; i < partitionCount; i++) {
    // create GSC for primary partition
    def createContainerForPrimaryResponse =  httpRequest httpMode:"POST", consoleLogResponseBody:true, contentType:"APPLICATION_JSON", 
                        requestBody:"""
                          {
                            "host": "${xap_server}",
                            "memory": "${gscHeapSize}"
                          }
                        """,
                        url:"http://${xap_server}:${mgt_port}/v1/containers"
    println("createContainerResponse Response Code: "+createContainerForPrimaryResponse.status)
    sleep(10)//Create Container is asynchronous

    if (hasBackup) {
      // create GSC for backup partition
      def createContainerForBackupResponse =  httpRequest httpMode:"POST", consoleLogResponseBody:true, contentType:"APPLICATION_JSON", 
                          requestBody:"""
                          {
                            "host": "${xap_server}",
                            "memory": "${gscHeapSize}"
                          }
                          """,
                          url:"http://${xap_server}:${mgt_port}/v1/containers"

      println("createContainerResponse Response Code: "+createContainerForBackupResponse.status)
      sleep(10)//Create Container is asynchronous
    }

  }

}

//Deploy PU
node {
  stage("Deploy PU"){
    def XAP_PATH = "/home/ubuntu/xap121"
    def PU_PATH = "/home/ubuntu/tmp"
    def PU_NAME = "shoppingcart-space.jar"
    sh "${XAP_PATH}/bin/gs.sh deploy -max-instances-per-vm 1 -requires-isolation true -cluster total_members=${partitionCount},${backupCount} ${PU_PATH}/${PU_NAME}"
  }
}

//Confirm Deployment of Empty Space 
stage("Available Spaces Check"){
  def deploymentsResponse =  httpRequest httpMode:"GET",  
                                                consoleLogResponseBody:true, 
                                                contentType:"APPLICATION_JSON", 
                                                  url:"http://${xap_server}:${mgt_port}/v1/deployments"
  
  println("deployments Response Code: "+deploymentsResponse.status)
  println("deploymentsResponse Content: "      +deploymentsResponse.content)
}
