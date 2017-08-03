definition(
    name: "modeCheck",
    namespace: "Security",
    author: "Yogesh Mhatre",
    description: "Check Mode change",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/yogi/TotalConnect/150.png",
    iconX2Url: "https://s3.amazonaws.com/yogi/TotalConnect/300.png")




// SmartThings defaults
def installed() {
  
    subscribe(location, checkMode)
}

def updated() {
	unsubscribe()
    subscribe(location, checkMode)
}

// Logic for Triggers based on mode change of SmartThings
def checkMode(evt) {
      log.debug "Changed to " + evt.value
    	if (evt.value == "Away") {    
          log.debug "${evt.value} is performed"

        }
        else if (evt.value == "Night") {
          log.debug "${evt.value} is performed"
        }
        else if (evt.value == "Home") {
          log.debug "${evt.value} is performed"
        }

}
