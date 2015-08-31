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
 Version: v0.3
 Changes [August 30, 2015]
 	- Logic to check if the Arm/DisArm signal actually implemented or not.
    	- User dont have to input LocationID & DeviceID. Its been capatured from the response now.
 
 */
definition(
    name: "TotalConnect v0.3",
    namespace: "Security",
    author: "Yogesh Mhatre",
    description: "Total Connect App to lock/unlock your home based on your location and mode",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/yogi/TotalConnect/150.png",
    iconX2Url: "https://s3.amazonaws.com/yogi/TotalConnect/300.png")

preferences {
	section ("Give your Total Connect credentials. Recommended to make another user for SmartThings") {
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

Map getSessionDetails(token) {
	def locationId
    def deviceId
 	def getSessionParams = [
    						uri: "https://rs.alarmnet.com/tc21api/tc2.asmx/GetSessionDetails",
        					body: [ SessionID: token, ApplicationID: '14588', ApplicationVersion: '3.2.2']
    ]
   	httpPost(getSessionParams) { responseSession -> 
        						 locationId = responseSession.data.Locations.LocationInfoBasic.LocationID
        						 deviceId = responseSession.data.Locations.LocationInfoBasic.DeviceList.DeviceInfoBasic.DeviceID
        									
    }
	log.debug "Location ID: ${locationId}, DeviceId: ${deviceId}"
  return [locationId: locationId, deviceId: deviceId]
} // Should return LocationID & DeviceID

// Arm Function. Performs arming function
def armAway() {        
        	def token = login(token)
            def details = getSessionDetails (token) // Get Location & Device ID
            def paramsArm = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/ArmSecuritySystem",
    			body: [SessionID: token, LocationID: details.locationId, DeviceID: details.deviceId, ArmType: 0, UserCode: '-1']
    			]
   			httpPost(paramsArm) // Arming Function in away mode
            def metaData = panelMetaData(token) // Get AlarmCode
            while( metaData.alarmCode != 10201 ){ 
                pause(1500) // One Second Pause to relieve number of retried on while loop
                metaData = panelMetaData(token)
             }  
           sendPush("Home is now Armed successfully")     
   logout(token)
}

def armStay() {        
        	def token = login(token)
            def details = getSessionDetails (token) // Get Location & Device ID

            def paramsArm = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/ArmSecuritySystem",
    			body: [SessionID: token, LocationID: details.locationId, DeviceID: details.deviceId, ArmType: 1, UserCode: '-1']
    			]
   			httpPost(paramsArm) // Arming function in stay mode
            def metaData = panelMetaData(token) // Gets AlarmCode
            while( metaData.alarmCode != 10203 ){ 
                pause(1500) // One Second Pause to relieve number of retried on while loop
                metaData = panelMetaData(token)
             }   
 			sendPush("Home is armed in Night mode")
    logout(token)
}

def disarm() {
			def token = login(token)
            def details = getSessionDetails (token) // Get Location & Device ID

        	def paramsDisarm = [
    			uri: "https://rs.alarmnet.com/TC21API/TC2.asmx/DisarmSecuritySystem",
    			body: [SessionID: token, LocationID: details.locationId, DeviceID: details.deviceId, ArmType: 0, UserCode: '-1']
    			]
   			httpPost(paramsDisarm)  
            def metaData = panelMetaData(token) // Gets AlarmCode
            while( metaData.alarmCode != 10200 ){ 
                pause(1500) // One Second Pause to relieve number of retried on while loop
                metaData = panelMetaData(token)
             }
           sendPush("Home is now Disarmed")
	logout(token)
         
}
