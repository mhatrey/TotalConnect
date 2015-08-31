/**
 *  TotalConnect
 *
 *  Copyright 2014 Yogesh Mhatre
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 /*
 Version: v0.2
 Changes
 	- Logic to check if the Arm/DisArm signal actually implemented or not.
 
 */
definition(
    name: "TotalConnect v0.2",
    namespace: "Security",
    author: "Yogesh Mhatre",
    description: "Total Connect App to lock/unlock your home based on your location and mode",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/yogi/TotalConnect/150.png",
    iconX2Url: "https://s3.amazonaws.com/yogi/TotalConnect/300.png")


preferences {
	/*section ("Select mode") {
    	input("mode1", "mode", multiple: false)
    }*/
	section ("Give your credentials") {
    	input("userName", "text", title: "Username", description: "Your username for TotalConnect")
    	input("password", "password", title: "Password", description: "Your Password for TotalConnect")
    	input("ApplicationID", "text", title: "Application ID", description: "Application ID")
    	input("ApplicationVersion", "text", title: "Application Version", description: "Application Version")
	}
}

// SmartThings defaults
def installed() {
	//log.debug "Installed with settings: ${settings}"
    subscribe(location, checkMode)

}

def updated() {
	//log.debug "Updated with settings: ${settings}"
	unsubscribe()
    subscribe(location, checkMode)
}

// Logic for Triggers based on mode change of SmartThings
def checkMode(evt) {
    	if (evt.value == "Away") {
            	log.debug "Mode is set to Away, Performing ArmAway"
            	armAway()   
            }
        else if (evt.value == "Night") {
            	log.debug "Mode is set to Night, Performing ArmStay"
            	armStay()
            }
        else if (evt.value == "Home") {
            	log.debug "Mode is set to Home, Performing Disarm"
            	disarm()
        }
}

// Login Function. Returns SessionID for rest of the functions
def login(token) {
	log.debug "Executed login"
	def paramsLogin = [
    	uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/AuthenticateUserLogin",
    	body: [userName: settings.userName , password: settings.password, ApplicationID: settings.ApplicationID, ApplicationVersion: settings.ApplicationVersion]
    	]
		httpPost(paramsLogin) { responseLogin ->
    	token = responseLogin.data.SessionID 
       }
       log.debug "Smart Things has logged In. SessionID: ${token}" 
    return token
}       

// Logout Function. Called after every mutational command. Ensures the current user is always logged Out.
def logout(token) {
		log.debug "During logout - ${token}"
   		def paramsLogout = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/Logout",
    			body: [SessionID: session]
    			]
   				httpPost(paramsLogout) { responseLogout ->
        		log.debug "Smart Things has successfully logged out"
        	}  
}

// Gets Panel Metadata
Map panelMetaData(token) {
	def alarmCode
    def lastSequenceNumber
    def lastUpdatedTimestampTicks
    def partitionId
	//log.debug "Token at PanelMetadata is " + token
 	def getPanelMetaDataAndFullStatus = [
    									uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/GetPanelMetaDataAndFullStatus",
        								body: [ SessionID: token, LocationID: 395502, LastSequenceNumber: 0, LastUpdatedTimestampTicks: 0, PartitionID: 1]
    ]
   	httpPost(getPanelMetaDataAndFullStatus) {	response -> 
        										lastUpdatedTimestampTicks = response.data.PanelMetadataAndStatus.'@LastUpdatedTimestampTicks'
        										lastSequenceNumber = response.data.PanelMetadataAndStatus.'@ConfigurationSequenceNumber'
        										partitionId = response.data.PanelMetadataAndStatus.Partitions.PartitionInfo.PartitionID
        										alarmCode = response.data.PanelMetadataAndStatus.Partitions.PartitionInfo.ArmingState
                                                
    }
	//log.debug "AlarmCode is " + alarmCode
  return [alarmCode: alarmCode, lastSequenceNumber: lastSequenceNumber, lastUpdatedTimestampTicks: lastUpdatedTimestampTicks]
} //Should return alarmCode, lastSequenceNumber & lastUpdateTimestampTicks

// Arm Function. Performs arming function
def armAway() {        
        	def token = login(token)
            def paramsArm = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/ArmSecuritySystem",
    			body: [SessionID: token, LocationID: 395502, DeviceID: 509622, ArmType: 0, UserCode: '-1']
    			]
   			httpPost(paramsArm) // Arming Function in away mode
            def a = panelMetaData(token)
            //log.debug "Alarm Code in In arm function " + a.alarmCode
            //log.debug "lastSequenceNumber in In arm function " + a.lastSequenceNumber  
            //log.debug "lastUpdatedTimestampTicks in In arm function " + a.lastUpdatedTimestampTicks  
          	def i = 0
            while( a.alarmCode != 10201 ){ 
            	log.debug "Number of times Metadata executed)" + i++
                pause(1000) // One Second Pause to relieve number of retried on while loop
                a = panelMetaData(token)
             }  
           sendPush("Home is now Armed successfully")     
   logout(token)
}

def armStay() {        
        	def token = login(token)
            def paramsArm = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/ArmSecuritySystem",
    			body: [SessionID: token, LocationID: 395502, DeviceID: 509622, ArmType: 1, UserCode: '-1']
    			]
   			httpPost(paramsArm) // Arming function in stay mode
            def a = panelMetaData(token)
            while( a.alarmCode != 10203 ){ 
                pause(1000) // One Second Pause to relieve number of retried on while loop
                a = panelMetaData(token)
             }   
 			sendPush("Home is armed in Night mode")
    logout(token)
}

def disarm() {
			def token = login(token)
				log.debug "During disarming. ${token}"

        	def paramsDisarm = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/DisarmSecuritySystem",
    			body: [SessionID: token, LocationID: 395502, DeviceID: 509622, UserCode: '-1']
    			]
   			httpPost(paramsDisarm)  
            def a = panelMetaData(token)
            while( a.alarmCode != 10200 ){ 
                pause(1000) // One Second Pause to relieve number of retried on while loop
                a = panelMetaData(token)
             }
           sendPush("Home is now Disarmed")
	logout(token)
         
}
