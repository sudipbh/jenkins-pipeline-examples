def xap_server = "10.8.4.88"
def mgt_port = "8090"

def partitionCount = 1
def hasBackup = true
def requiresIsolation=true

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
stage("Create GSCs") {

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

//Deploy an Empty Space (Datagrid) to available Container Resource
stage("Deploy Empty Space"){
	def spaceUrl = "http://${xap_server}:${mgt_port}/v1/spaces?name=${spaceName}&partitions=${partitionCount}&backups=${hasBackup}&requiresIsolation=${requiresIsolation}"
	println("spaceUrl = [" + spaceUrl + "]")
	def deployEmptySpaceResponse = httpRequest httpMode:"POST", 
                                              consoleLogResponseBody:true, 
                                              contentType:"APPLICATION_JSON", 
                                              url: "${spaceUrl}"
	println("deployEmptySpaceResponse Response Code: "+deployEmptySpaceResponse.status)
	sleep(10)//Deploy Space is asynchronous
}

//Confirm Deployment of Empty Space 
stage("Available Spaces Check"){
	def availableSpacesResponse =  httpRequest httpMode:"GET",  
                                            		consoleLogResponseBody:true, 
                                            		contentType:"APPLICATION_JSON", 
                                              		url:"http://${xap_server}:${mgt_port}/v1/spaces"
  
  println("availableSpacesResponse Response Code: "+availableSpacesResponse.status)
  println("availableSpacesResponse Content: "      +availableSpacesResponse.content)
}
