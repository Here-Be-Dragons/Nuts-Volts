/**
 *  Panel Meter Management
 *
 *  Copyright 2015 John Rucker
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
definition(
    name: "Analog Panel Meter Manager",
    namespace: "NutsVolts",
    author: "John Rucker",
    description: "Select data for and send it to your analog panel meter",
    category: "My Apps",
    iconUrl: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo.png",
    iconX2Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo2x.png",
    iconX3Url: "http://coopboss.com/images/SmartThingsIcons/coopbossLogo3x.png")


preferences {
    section("Description", hideable:true, hidden:true) {
        paragraph 	"Analog Panel Meter Manager allows you to select a unique data source for each of your 4 " +
        			"panel meters.  First you will need to select the 4 panel meter to send the data to. Then " +
        			"you can select a unique device and data attribute for each meter."
    }
    
    section("Select panel meter") {              
        input(name: "alogMeter", type: "capability.switchLevel", title: "Select the analog panel meter to display the data", required: true)    
    }    

    (1..4).each() { n ->
        section("Analog Meter ${n}") {
            input "dataSource_${n}", "capability.sensor", title:"Source device", multiple:false, required:false
            input "dataType_${n}", "enum", title: "Data type", metadata: [values: ["windSpeed", "windDir", "humidity", "temperature"]], multiple:false, required:false 
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {   
    (1..4).each() { n ->   	
    	def dataSource = settings."dataSource_${n}"
        def dataType = settings."dataType_${n}"
        def methodToCall = "updateMeter_${n}"
        
		log.trace "subscribing method ${methodToCall} to ${dataSource} : ${dataType} event."           
		subscribe(dataSource, dataType, methodToCall)
    }        
}

def updateMeter_1(evt){
	log.trace "New event for Meter 1 from ${dataSource_1} ${evt.name} = ${evt.value}"
	def dataToSend = [:]
    dataToSend.epNum = "0x38"
    dataToSend.value = (int)(evt.value as float)
	alogMeter.sendLevelToEP(dataToSend)
}

def updateMeter_2(evt){
	log.trace "New event for Meter 2 from ${dataSource_2} ${evt.name} = ${evt.value}"
	def dataToSend = [:]
    dataToSend.epNum = "0x39"
    dataToSend.value = (int)(evt.value as float)
	alogMeter.sendLevelToEP(dataToSend)
}

def updateMeter_3(evt){
	log.trace "New event for Meter 3 from ${dataSource_3} ${evt.name} = ${evt.value}"
	def dataToSend = [:]
    dataToSend.epNum = "0x40"
    dataToSend.value = (int)(evt.value as float)
	alogMeter.sendLevelToEP(dataToSend)
}

def updateMeter_4(evt){
	log.trace "New event for Meter 4 from ${dataSource_4} ${evt.name} = ${evt.value}"
	def dataToSend = [:]
    dataToSend.epNum = "0x41"
    dataToSend.value = (int)(evt.value as float)
	alogMeter.sendLevelToEP(dataToSend)
}