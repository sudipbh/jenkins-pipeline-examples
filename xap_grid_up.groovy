def xap_server = "10.8.4.88"
def mgt_port = "8090"

//Print Available Host in Service Grid
stage("Available Hosts Check"){
  def availableHostResponse =  
    httpRequest httpMode:"GET", consoleLogResponseBody:true, contentType:"APPLICATION_JSON",  url:"http://${xap_server}:${mgt_port}/v1/hosts"

  println("availableHostResponse Response Code "+availableHostResponse.status)
  println("availableHostResponse Content: "     +availableHostResponse.content)
}

//Create a new GSC Container Resource
stage("Create Containers"){

	// create GSC for primary partition
	def createContainerResponse =  httpRequest 
										httpMode:"POST", 
										consoleLogResponseBody:true, 
    									contentType:"APPLICATION_JSON", 
    									requestBody:"{ \"host\": \"${xap_server}\", \"memory\": \"512m\" }", 
    									url:"http://${xap_server}:${mgt_port}/v1/containers"
	// create GSC for backup partition
	def createContainerResponse =  httpRequest 
										httpMode:"POST", 
										consoleLogResponseBody:true, 
    									contentType:"APPLICATION_JSON", 
    									requestBody:"{ \"host\": \"${xap_server}\", \"memory\": \"512m\" }", 
    									url:"http://${xap_server}:${mgt_port}/v1/containers"
    
  println("createContainerResponse Response Code: "+createContainerResponse.status)
  sleep(10)//Create Container is asynchronous
}

