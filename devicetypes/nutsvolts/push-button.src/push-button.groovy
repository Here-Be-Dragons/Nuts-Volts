/**
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
metadata {
	definition (name: "Push Button", namespace: "NutsVolts", author: "John.Rucker@Solar-Current.com") {
	
    capability "Actuator"
    capability "Configuration"
    capability "Refresh"
	capability "Sensor"
    capability "Switch"
	capability "Switch Level"
	
	attribute "levelPercent","number"	

	fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008,FF00", outClusters: "0019"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"sent"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"sent"
			state "sent", label: 'wait', icon: "st.motion.motion.active", backgroundColor: "#ffa81e"              
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false, range:"(1..255)") {
			state "level", action:"Switch Level.setLevel"
		}
		valueTile("level", "device.levelPercent", inactiveLabel: false, decoration: "flat") {
			state "level", label: '${currentValue}%'
		}
		
		main(["switch"])
		details(["switch", "level", "levelSliderControl", "refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	//log.debug "parse called with --> $description"
	Map map = [:]
	if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	}
    else if (description?.startsWith('on/off')) {
    	map = parseOnOff(description)
    }
	else if (description?.startsWith('read attr -')) {
		map = parseReportAttributeMessage(description)
	}  
    else {
    	log.debug "No parse method for: $description"
    }
    log.trace map
	return map ? createEvent(map) : null
}

private Map parseReportAttributeMessage(String description) {
	//log.debug "Map parseReportAttributeMessage called with --> $description"
    Map resultMap = [:]
    def descMap = parseDescriptionAsMap(description)
    //log.debug descMap
   
    if (descMap.cluster == "0008" && descMap.attrId == "0000") { 
        resultMap.name = "level"
        resultMap.value = (Integer.parseInt(descMap.value, 16))      
        resultMap.displayed = true  
        
        def cLevel = (int) 100 / (255/resultMap.value)
        sendEvent(name: "levelPercent", value: cLevel, displayed: false) 
    }
    else {
    	log.debug "Attribute match not found for --> $descMap"
    }
    return resultMap
}

private Map parseOnOff(String description) {
	//log.debug "Map parseOnOff called with --> $description"
    Map resultMap = [:]    
    
    if(description?.endsWith("0")) {
        resultMap.name = "switch"
        resultMap.value = "off"
        resultMap.displayed = true
    }    
    else if(description?.endsWith("1")) {
        resultMap.name = "switch"
        resultMap.value = "on"
        resultMap.displayed = true
    }  
    else {
    	log.debug "On/Off match not found for --> $description"
    }    
    return resultMap
}

private Map parseCatchAllMessage(String description) {
	//log.debug "Map parseCatchAllMessage called with --> $description"
    Map resultMap = [:]    
    def cluster = zigbee.parse(description)
    //log.debug cluster
    
    if (cluster.clusterId == 0x0006 && cluster.command == 0x01) {			// command 0x01 = report attribute
    	//log.trace "On Off Cluster report = $cluster.data"
        switch(cluster.data) {
        
        case "[0, 0, 0, 16, 0]":							// Switch is off attribute report   
        resultMap.name = "switch"
        resultMap.value = "off" 
        resultMap.displayed = true
        break     
        
        case "[0, 0, 0, 16, 1]":							// Switch is on attribute report       
        resultMap.name = "switch"
        resultMap.value = "on"   
        resultMap.displayed = true
        break                 
        }
    }
    else if (cluster.clusterId == 0x0006 && cluster.command == 0x0B) {			// command 0x0B = default response to command sent
    	//log.trace "On Off Cluster default response = $cluster.data"
        switch(cluster.data) {
        
        case "[0, 0]":									// Switch acknowledged off command   
        resultMap.name = "switch"
        resultMap.value = "off" 
        resultMap.displayed = true
        break     
        
        case "[1, 0]":									// Switch acknowledged on command        
        resultMap.name = "switch"
        resultMap.value = "on"        
        resultMap.displayed = true
        break                 
        }
    } 
    else if (cluster.clusterId == 0x0008 && cluster.command == 0x0B) {			// command 0x0B = default response to command sent
    	//log.trace "level Cluster default response = $cluster.data"  
        switch(cluster.data) {
        
        case "[0, 0]":															// Level command acknowledged  
        def cLevel = device.currentState("level")?.value as int
        cLevel=(int)100 / (255/cLevel)
        resultMap.name = "levelPercent"
        resultMap.value = cLevel        
        resultMap.displayed = true        
        break                    
        }
   }
    
    else {
    	log.debug "CatchAll match not found for --> $description"
        log.debug "ZigBee.parse --> $cluster"
    }        
    
    return resultMap
}

def on() {
	log.info "on cmd sent"
	"st cmd 0x${device.deviceNetworkId} 1 6 1 {}"
}

def off() {
	log.info "off cmd sent"
	"st cmd 0x${device.deviceNetworkId} 1 6 0 {}"
}

def refresh() {
	log.info "read attributes request sent"
	[
	"st rattr 0x${device.deviceNetworkId} 1 6 0", "delay 500",
    "st rattr 0x${device.deviceNetworkId} 1 8 0"
    ]
}

def setLevel(value) {
	log.info "Level($value) sent"
    sendEvent(name: "level", value: value, displayed: false)    
	def cmds = []
    def level = hexString(Math.round(value))
	cmds << "st cmd 0x${device.deviceNetworkId} 1 8 0 {${level} 0000}"
	cmds
}

def parseDescriptionAsMap(description) {
    (description - "read attr - ").split(",").inject([:]) { map, param ->
        def nameAndValue = param.split(":")
        map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
    }
}

private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}

def configure() {
    log.debug "Binding SEP 0x38 DEP 0x01 Cluster 0x0006 ON/Off cluster to hub"  
    log.debug "Binding SEP 0x38 DEP 0x01 Cluster 0x0008 Level cluster to hub"      
    
    def cmd = []
    cmd << "zdo bind 0x${device.deviceNetworkId} 0x38 0x01 0x0006 {${device.zigbeeId}} {}"		// Bind to end point 0x38 and the On/Off Cluster
    cmd << "delay 150"
    cmd << "zdo bind 0x${device.deviceNetworkId} 0x38 0x01 0x0008 {${device.zigbeeId}} {}"    		// Bind to end point 0x38 and the Level Cluster
    cmd << "delay 1500"       
    
    return cmd + refresh() // send refresh cmds as part of config
}
