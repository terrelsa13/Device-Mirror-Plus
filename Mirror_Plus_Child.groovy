/*************
*  Device Mirror Plus Child
*  Copyright 2022 Terrel Allen All Rights Reserved
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU Affero General Public License as published
*  by the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Affero General Public License for more details.
*
*  You should have received a copy of the GNU Affero General Public License
*  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*  WARNING!!!
*  Use at your own risk.
*  Modify at your own risk.
*
*  USAGE
*  Mirror one device with another.
*
*  CHANGE LOG
*  v202110091200
*      -Add non-dimmable switch
*      -Separate dimmable switch from non-dimmable switch
*  v202110031130
*      -fix tamper attribute not updating on slave
*      -fix acceleration attribute not updating on slave
*      -add threeAxis
*  v202110022252
*      -fix humidity attribute not updating on slave
*      -update sethumidityHandler() to humidityHandler()
*      -add hours and minutes to change log dates
*  v202110020000
*      -re-add iconUrl
*      -re-add iconX2Url
*  v20211001000000
*      -add header
*  v20210928000000
*      -initial release w/ battery level, temperature, humidity, etc...
*
*************/

definition(
    name: "Device Mirror Plus Child",
    namespace: "whodunitGorilla",
    parent: "whodunitGorilla:Device Mirror Plus",
    author: "Terrel Allen",
    description: "mirror plus child",
    category: "Convenience",
    importUrl: "https://raw.githubusercontent.com/terrelsa13/Device-Mirror-Plus/master/Mirror_Plus_Child.groovy",
    iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "mainPage")
}


def mainPage() {
    dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
        section("Devices you can mirror: [Light Dimmer], [Light Switch], [Motion Sensor], [Contact Sensor], [Humidity Sensor], [Temperature Sensor], [Battery Level], [Audio], [Single Button], [Fan Control]") {
            // checks if either the master device type or master device has been selected, if not then select a master device type
            if(!masterDeviceType && !master){
                input "masterDeviceType", "enum", title:"Choose Parent Device Type", options: ["Light Dimmer", "Light Switch", "Motion Sensor", "Contact Sensor", "Humidity Sensor", "Temperature Sensor", "Battery Level", "Audio", "Single Button", "Fan Control"], submitOnChange: true
            }
            
            // if either master device or type has been chosen, show the input field for master device
            if(masterDeviceType || master){
                input "master", "capability.${getCapability()}", title: "Select Parent ${masterDeviceType}", submitOnChange: true, required: true

            }

            // if master has been chosen
            if(master){
                // check if master is type button and if it has more than 1 button
                if(masterDeviceType == "Single Button" && master.currentNumberOfButtons != 1){
                    state.error = true
                    state.errorMsg = "Only Single Button Allowed"
                    paragraph "<font color=\"red\">Only Single Button allowed</font>" 
                    return
                }

                state.error = false

                // shows the allowed attributes in paragraphs and adds the list of attributes into masterAttributes
                def masterAttributes = showAndGetMasterAttributes()
                
                // input field for slaves
                input "slaves", "capability.${getCapability()}", title: "Select Child ${masterDeviceType}(s)", submitOnChange: true, required: true, multiple: true
                // if slave(s) has been chosen
                if(slaves){
                    // check if device type is a button
                    if(masterDeviceType == "Single Button"){
                        // check if a slave has more than 1 button and throws error in paragraph if true
                        for(slave in slaves){
                            if(slave.currentNumberOfButtons != 1){
                                state.error = true
                                state.errorMsg = "Only Single Button Allowed"
                                paragraph "<font color=\"red\">Only Single Button allowed</font>" 
                                return
                            }
                        }
                    }
                    // check if it's an endless loop
                    Boolean p = parent.checkSlavesExist(getSlavesId(), getMasterId(), 4, false)
                    if(!p){
                        def slaveList = []
                        for(slave in slaves){
                            def slaveAttributes = getAttributes(slave)
                            for(attribute in slaveAttributes){
                                if(masterAttributes.contains(attribute) && !slaveList.contains(attribute)){
                                    slaveList.add(attribute)
                                    paragraph "<font color=\"green\">[${attribute}] events may be received...</font>"
                                }
                            }
                        }
                        state.error = false
                        input "initializeOnUpdate", "bool", defaultValue: "false", title: "Update slave devices when clicking Done"
                        input "childAppNameIs", "bool", defaultValue: "false", title: "Name the \"Mirror: Child App\" after the first child device"
                        if((slaveList.contains("motion") || slaveList.contains("contact") || slaveList.contains("mute") || slaveList.contains("switch"))){
                            input "reverseEnabled", "bool", defaultValue: "false",  submitOnChange: true, title: "Enable reverse attributes:"
                            if(reverseEnabled){
                                if(slaveList.contains("acceleration")){
                                    input "accelerationReverseEnabled", "bool", defaultValue: "false", title: "Reverse acceleration for child device (e.g. When parent is acceleration active; child will be acceleration inactive.)"
                                }

                                if(slaveList.contains("contact")){
                                    input "contactReverseEnabled", "bool", defaultValue: "false", title: "Reverse contact for child device (e.g. When parent is contact open; child will be contact close.)"
                                }

                                if(slaveList.contains("motion")){
                                    input "motionReverseEnabled", "bool", defaultValue: "false", title: "Reverse motion for child device (e.g. When parent is motion active; child will be motion inactive.)"
                                }

                                if(slaveList.contains("mute")){
                                    input "muteReverseEnabled", "bool", defaultValue: "false", title: "Reverse audio for child device (e.g. When parent is audio mute; child will be audio unmute.)"
                                }

                                if(slaveList.contains("switch")){
                                    log.debug "switch is here"
                                    input "switchReverseEnabled", "bool", defaultValue: "false", title: "Reverse switch for child device (e.g. When parent is switch on; child will be switch off.)"
                                }

                                if(slaveList.contains("tamper")){
                                    input "tamperReverseEnabled", "bool", defaultValue: "false", title: "Reverse tamper for child device (e.g. When parent is tamper detected; child will be tamper clear.)"
                                }
                            }
                        }
                    }
                    else{
                        // sets error state to true and throws error into a paragraph
                        state.error = true
                        state.errorMsg = "Endless Loop - Select A Different Device"
                        paragraph "<font color=\"red\">Select A Different Device</font>" 
                    }
                }
            }
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
    log.info "updated"   
}

def initialize() {
    if(!state.error){
        //list for these attributes
        subscribe(master, "switch", switchHandler) // switch
        subscribe(master, "level", setLevelHandler) // set level
        subscribe(master, "colorTemperature", colorTemperatureHandler) // color temperature
        subscribe(master, "hue", colorHandler) // hue
        subscribe(master, "saturation", saturationHandler) // saturation
        subscribe(master, "motion", motionHandler) // motion sensor
        subscribe(master, "contact", contactHandler) // contact sensor
        subscribe(master, "mute", muteHandler) // mute audio
        subscribe(master, "volume", volumeHandler) // audio volume
        subscribe(master, "pushed", buttonHandler) // button pushed
        subscribe(master, "held", buttonHandler) // button held
        subscribe(master, "doubleTapped", buttonHandler) // button double tapped
        subscribe(master, "released", buttonHandler) // button released
        subscribe(master, "humidity", humidityHandler) // set humidity level
        subscribe(master, "temperature", temperatureHandler) // temperature degrees
        subscribe(master, "illuminance", illuminanceHandler) // illuminance
        subscribe(master, "battery", setBatteryLevelHandler) // battery level
        subscribe(master, "acceleration", accelerationSensorHandler) // acceleration
        subscribe(master, "tamper", tamperAlertHandler) // tamper
        subscribe(master, "threeAxis", threeAxisHandler) // three axis 
        subscribe(master, "speed", fanSpeedHandler) //fan speed

        log.debug "initialize"

        if (childAppNameIs) {
            //Name the "Mirror: Child App" after the fist slave device
            updateAppName("Mirror: ${slaves[0]}")
        }
        else {
            //Name the Mirror: Child App after the master device; this is the default behavior
            updateAppName("Mirror: ${master}")
        }

        // initializes all slave devices to mirror the master on update
        if(initializeOnUpdate) initializeValuesSlaves()
    }
    else{
        updateAppName("ERROR: ${state.errorMsg}")
    }
}


// converts the masterDeviceType input into a valid capability
def getCapability(){
    switch(masterDeviceType){
        case "Light Dimmer":
            return "switchLevel"
            break

        case "Light Switch":
            return "switch"
            break

        case "Motion Sensor":
            return "motionSensor"
            break

        case "Contact Sensor":
            return "contactSensor"
            break

        case "Humidity Sensor":
            return "relativeHumidityMeasurement"
            break

        case "Temperature Sensor":
            return "temperatureMeasurement"
            break

        case "Battery Level":
            return "battery"
            break

        case "Audio":
            return "audioVolume"
            break

        case "Single Button":
            return "pushableButton"
            break

        case "Fan Control":
            return "fanControl"
            break

        default:
            log.debug "getCapability() default case"
            return "default"
            break
    }
}

// name the app
def updateAppName(def string){
    app.updateLabel(string)
}

// switch on and off
def switchHandler(evt) {
    if ((!switchReverseEnabled) || (!reverseEnabled)){
        if(evt.value == "on"){
            slaves?.on()
        }
        else if(evt.value == "off"){
            slaves?.off()
        }
    }
    else if(switchReverseEnabled){
        if(evt.value == "on"){
            slaves?.off()
        }
        else if(evt.value == "off"){
            slaves?.on()
        }
    }
}

// switch set level
def setLevelHandler(evt){
    if(evt.value == "on" || evt.value == "off"){
        //log.debug "it was on or off"
        return
    }

    def level = evt.value.toFloat()
    level = level.toInteger()

    //log.warn evt.getValue()
    slaves?.setLevel(level)
}

// fan speed
def fanSpeedHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            //log.debug "set fan speed to ${evt.value}"
            slave.setSpeed(evt.value)
        }
    }
}

// switch color temperature
def colorTemperatureHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            //log.debug "set color temperature to ${evt.value}"
            slave.setColorTemperature(evt.value)
        }
    }
}

// switch hue
def colorHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            //log.debug "set hue to ${evt.value}"
            slave.setHue(evt.value)
        }
    }
}

// switch saturation
def saturationHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            //log.debug "set saturation to ${evt.value}"
            slave.setSaturation(evt.value)
        }
    }
}

// motion active and inactive
def motionHandler(evt){
    for(slave in slaves){
        try{
            if((!motionReverseEnabled) || (!reverseEnabled)){
                //log.debug "motion is ${evt.value}"
                if(evt.value == "active"){
                    if(slave.hasCommand("motionActive")){
                        slaves?.motionActive()
                    }
                    else if(slave.hasCommand("active")){
                        slaves?.active()
                    }
                }
                else if(evt.value == "inactive"){
                    if(slave.hasCommand("motionInactive")){
                        slaves?.motionInactive()
                    }
                    else if(slave.hasCommand("inactive")){
                        slaves?.inactive()
                    }
                }
            }
            else if (motionReverseEnabled){
                //log.debug "motion is ${evt.value}"
                if(evt.value == "active"){
                    if(slave.hasCommand("motionActive")){
                        slaves?.motionInactive()
                    }
                    else if(slave.hasCommand("active")){
                        slaves?.inactive()
                    }
                }
                else if(evt.value == "inactive"){
                    if(slave.hasCommand("motionInactive")){
                        slaves?.motionActive()
                    }
                    else if(slave.hasCommand("inactive")){
                        slaves?.active()
                    }
                }
            }
        }
        catch(IllegalArgumentException ex){
            log.debug "Command is not supported by device"
        }
    }
}

// contact open and close
def contactHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            if((!contactReverseEnabled) || (!reverseEnabled)){
                //log.debug "set contact sensor to ${evt.value}"
                if((evt.value == "open") || (evt.value == "opened")){
                    slave.open()
                }
                else if((evt.value == "close") || (evt.value == "closed")){
                    slave.close()
                }
            }
            else if(contactReverseEnabled){
                //log.debug "set contact sensor to ${evt.value}"
                if((evt.value == "open") || (evt.value == "opened")){
                    slave.close()
                }
                else if((evt.value == "close") || (evt.value == "closed")){
                    slave.open()
                }
            }
        }
    }
}

// humidity level
def humidityHandler(evt){
    for (slave in slaves){
            if(slave.hasAttribute("${evt.name}")){
                //log.debug "set humidity to ${evt.value}%"
                slave.setRelativeHumidity(evt.value)
        }
    }
}

// temperature degrees
def temperatureHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            //log.debug "set temperature in degress to ${evt.value}"
            slave.setTemperature(evt.value)
        }
    }
}

// illuminance
def illuminanceHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            //log.debug "set illuminance in lux to ${evt.value}"
            slave.setIlluminance(evt.value)
        }
    }
}

// battery level
def setBatteryLevelHandler(evt){
    for(slave in slaves){
        if(slave.hasAttribute("${evt.name}")){
            def level = evt.value.toFloat()
            level = level.toInteger()

            //log.warn evt.getValue()
            //slaves?.setBattery(level)
            slave.setBattery(level)
        }
    }
}

// acceleration active and inactive
def accelerationSensorHandler(evt){
    for(slave in slaves){
        try{
            if((!accelerationReverseEnabled) || (!reverseEnabled)){
                //log.debug "acceleration is ${evt.value}"
                if(evt.value == "active"){
                    if(slave.hasCommand("accelerationActive")){
                        slaves?.accelerationActive()
                    }
                    else if(slave.hasCommand("active")){
                        slaves?.active()
                    }
                }
                else if(evt.value == "inactive"){
                    if(slave.hasCommand("accelerationInactive")){
                        slaves?.accelerationInactive()
                    }
                    else if(slave.hasCommand("inactive")){
                        slaves?.inactive()
                    }
                }
            }
            else if(accelerationReverseEnabled){
                //log.debug "acceleration is ${evt.value}"
                if(evt.value == "active"){
                    if(slave.hasCommand("accelerationActive")){
                        slaves?.accelerationInactive()
                    }
                    else if(slave.hasCommand("active")){
                        slaves?.inactive()
                    }
                }
                else if(evt.value == "inactive"){
                    if(slave.hasCommand("accelerationInactive")){
                        slaves?.accelerationActive()
                    }
                    else if(slave.hasCommand("inactive")){
                        slaves?.active()
                    }
                }
            }
        }
        catch(IllegalArgumentException ex){
            log.debug "Command is not supported by device"
        }
    }
}

// tamper clear and detected
def tamperAlertHandler(evt){
    for(slave in slaves){
        try{
            if((!tamperReverseEnabled) || (!reverseEnabled)){
                //log.debug "tamper alert is ${evt.value}"
                if(evt.value == "detected"){
                    if(slave.hasCommand("tamperDetected")){
                        slaves?.tamperDetected()
                    }
                    else if(slave.hasCommand("detected")){
                        slaves?.active()
                    }
                }
                else if(evt.value == "clear"){
                    if(slave.hasCommand("tamperClear")){
                        slaves?.tamperClear()
                    }
                    else if(slave.hasCommand("clear")){
                        slaves?.inactive()
                    }
                }
            }
            else if(tamperReverseEnabled){
                //log.debug "tamper alert is ${evt.value}"
                if(evt.value == "detected"){
                    if(slave.hasCommand("tamperDetected")){
                        slaves?.tamperClear()
                    }
                    else if(slave.hasCommand("detected")){
                        slaves?.inactive()
                    }
                }
                else if(evt.value == "clear"){
                    if(slave.hasCommand("tamperClear")){
                        slaves?.tamperDetected()
                    }
                    else if(slave.hasCommand("clear")){
                        slaves?.active()
                    }
                }
            }
        }
        catch(IllegalArgumentException ex){
            log.debug "Command is not supported by device"
        }
    }
}

// three axis
def threeAxisHandler(evt){
    for(slave in slaves){
        //log.debug "threeAxis is ${evt.value}"
        if(slave.hasAttribute("${evt.name}")){
            //remove open bracket
            removeBrackets = evt.value.minus("[")
            //remove close bracket
            removeBrackets = removeBrackets.minus("]")
            //split string into an array at ","
            threeAxisArray = removeBrackets.split(",")
            //split strings into arrys at ":"
            xPair = threeAxisArray[0].split(":")
            yPair = threeAxisArray[1].split(":")
            zPair = threeAxisArray[2].split(":")
            //to integers
            int x = xPair[1] as Integer
            int y = yPair[1] as Integer
            int z = zPair[1] as Integer
            //command
            slave.threeAxis(x,y,z)
            //slave.setThreeAxis(evt.value)
        }
    }
}

// audio mute and unmute
def muteHandler(evt){
    if((!muteReverseEnabled) || (!reverseEnabled)){
        if((evt.value == "mute") || (evt.value == "muted")){
            //log.debug "muting children"
            slaves?.mute()
        }
        else{
            //log.debug "unmuting children"
            slaves?.unmute()
        }
    }
    else if(muteReverseEnabled){
        if((evt.value == "mute") || (evt.value == "muted")){
            //log.debug "muting children"
            slaves?.unmute()
        }
        else{
            //log.debug "unmuting children"
            slaves?.mute()
        }
    }
}

// audio volume
def volumeHandler(evt){
    //log.debug "setting the volume to ${evt.value}"
    slaves?.setVolume(evt.value)
}

// button pushed, doubletapped, held and released
def buttonHandler(evt){
    switch(evt.name){
        case "pushed":
            //for(slave in slaves){
                //slave.push(evt.value)
            //}
            slaves?.push(evt.value)
            break

        case "doubleTapped":
            slaves?.doubleTap(evt.value)
            break

        case "held":
            slaves?.hold(evt.value)
            break

        case "released":
            slaves?.release(evt.value)
            break 
    }
}

// returns a map (Attribute name : Value)
def getAttributeValuesMap(def userDevice){
    def attributes = userDevice.getSupportedAttributes()
    def allowedAttributes = getAllowedAttributesList()
    def values = [:]
    for (attribute in attributes){
        if(allowedAttributes.contains(attribute.getName())){
            values.put(attribute.getName(), userDevice.currentValue(attribute.getName()))
        }
    }
    return values
}

// returns a list of command names of userDevice
def getCommands(def userDevice){
    def commands = userDevice.getSupportedCommands()
    def names = []
    for (command in commands){
        names.add(command.getName())
    }
    return names
}

// returns a list of attribute names from userDevice
def getAttributes(def userDevice){
    def attributeNames = []
    def attributes = userDevice.getSupportedAttributes()

    for (attribute in attributes){
        attributeNames.add(attribute.getName())
    }

    return attributeNames
}

// gets a list of the allowed attributes
def getAllowedAttributesList(){
    def allowedList = []
    switch(getCapability()){
        case "switchLevel":
        case "fanControl":
            allowedList = ["switch", "level", "hue", "saturation", "colorTemperature", "speed"]
            break

        case "switch":
            allowedList = ["switch", "hue", "saturation", "colorTemperature", "speed"]
            break

        case "motionSensor":
            allowedList = ["motion", "temperature", "illuminance", "humidity", "acceleration", "tamper", "threeAxis", "battery"]
            break

        case "contactSensor":
            allowedList = ["contact", "temperature", "tamper", "threeAxis", "battery"]
            break

        case "relativeHumidityMeasurement":
            allowedList = ["humidity", "temperature", "battery", "speed"]
            break

        case "temperatureMeasurement":
            allowedList = ["temperature", "humidity", "battery"]
            break

        case "battery":
            allowedList = ["battery"]
            break

        case "audioVolume":
            allowedList = ["mute", "volume"]
            break

        case "pushableButton":
            allowedList = ["doubleTapped", "held", "pushed", "released"]
            break

        default:
            log.debug "getAllowedAttributesList() default case"
            break
    }
    allowedList
}

// shows the allowed attributes of master device in paragraphs and returns the list of attribute names
def showAndGetMasterAttributes(){
    def allowedAttributes = getAllowedAttributesList()
    def attributes = master.getSupportedAttributes()
    def savedAttributes = []
    for (attribute in attributes){
        def attributeName = attribute.getName()
        if(allowedAttributes.contains(attributeName)){
            if(!savedAttributes.contains(attributeName)){
                savedAttributes.add(attributeName)
                paragraph "<font color=\"green\">[${attribute}] events may be forwarded...</font>"
            }
        }
    }
    return savedAttributes
}

def initializeValuesSlaves(){
    def capability = getCapability()
    def map = getAttributeValuesMap(master)
    switch(capability){
        case "switchLevel":
        case "fanControl":
            initializeValuesLightSwitchLevelOrFanControl(map)
            break
        case "switch":
            initializeValuesLightSwitch(map)
            break
        case "motionSensor":
            initializeValuesMotionSensor(map)
            break
        case "contactSensor":
            initializeValuesContactSensor(map)
            break
        case "relativeHumidityMeasurement":
            initializeValuesHumiditySensor(map)
            break
        case "temperatureMeasurement":
            initializeValuesTemperatureSensor(map)
            break
        case "battery":
            initializeBatteryLevel(map)
            break
        case "audioVolume":
            initializeValuesAudio(map)
            break
        default:
            log.info "nothing to initialize"
    }
}

def initializeValuesLightSwitchLevelOrFanControl(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            switch(attributeName){
                case "switch":
                    if ((!switchReverseEnabled) || (!reverseEnabled)){
                        if(attributeValue == "on"){
                            slave.on()
                        }
                        else{
                            slave.off()
                        }
                    }
                    else if(switchReverseEnabled){
                        if(attributeValue == "on"){
                            slave.off()
                        }
                        else{
                            slave.on()
                        }
                    }
                    break
                case "level": 
                    if(slave.hasCommand("setLevel")){
                        slave.setLevel(attributeValue)
                    }
                    break
                case "colorTemperature":
                    if(slave.hasCommand("setColorTemperature")){
                        slave.setColorTemperature(attributeValue)
                    }
                    break
                case "hue":
                    if(slave.hasCommand("setHue")){
                        slave.setHue(attributeValue)
                    }
                    break
                case "saturation":
                    if(slave.hasCommand("setSaturation")){
                        slave.setSaturation(attributeValue)
                    }
                    break
                case "speed":
                    child.setSpeed(attributeValue)
                    break
                default:
                    log.debug "Could not initialize the Light Switch attribute: ${attributeName}"
            }
        }
    }
}

def initializeValuesLightSwitch(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            switch(attributeName){
                case "switch":
                    if ((!switchReverseEnabled) || (!reverseEnabled)){
                        if(attributeValue == "on"){
                            slave.on()
                        }
                        else{
                            slave.off()
                        }
                    }
                    else if(switchReverseEnabled){
                        if(attributeValue == "on"){
                            slave.off()
                        }
                        else{
                            slave.on()
                        }
                    }
                    break
                case "colorTemperature":
                    if(slave.hasCommand("setColorTemperature")){
                        slave.setColorTemperature(attributeValue)
                    }
                    break
                case "hue":
                    if(slave.hasCommand("setHue")){
                        slave.setHue(attributeValue)
                    }
                    break
                case "saturation":
                    if(slave.hasCommand("setSaturation")){
                        slave.setSaturation(attributeValue)
                    }
                    break
                default:
                    log.debug "Could not initialize the Light Switch attribute: ${attributeName}"
            }
        }
    }
}

def initializeValuesMotionSensor(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            if(attributeName == "motion"){
                if((!motionReverseEnabled) || (!reverseEnabled)){
                    if(attributeValue == "active"){
                        if(slave.hasCommand("motionActive")){
                            slave.motionActive()
                        }
                        else if (slave.hasCommand("active")){
                            slave.active()
                        }
                        else{
                            log.debug "0-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "inactive"){
                        if(slave.hasCommand("motionInactive")){
                            slave.motionInactive()
                        }
                        else if (slave.hasCommand("inactive")){
                            slave.inactive()
                        }
                        else{
                            log.debug "1-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
                else if(motionReverseEnabled){
                    if(attributeValue == "active"){
                        if(slave.hasCommand("motionActive")){
                            slave.motionInactive()
                        }
                        else if (slave.hasCommand("active")){
                            slave.inactive()
                        }
                        else{
                            log.debug "2-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "inactive"){
                        if(slave.hasCommand("motionInactive")){
                            slave.motionActive()
                        }
                        else if (slave.hasCommand("inactive")){
                            slave.active()
                        }
                        else{
                            log.debug "3-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
            }
            else if (attributeName == "temperature"){
                slave.setTemperature(attributeValue)
            }
            else if (attributeName == "illuminance"){
                slave.setIlluminance(attributeValue)
            }
            else if (attributeName == "humidity"){
                slave.setRelativeHumidity(attributeValue)
            }
            else if(attributeName == "acceleration"){
                if((!accelerationReverseEnabled) || (!reverseEnabled)){
                    if(attributeValue == "active"){
                        if(slave.hasCommand("accelerationActive")){
                            slave.accelerationActive()
                        }
                        else if (slave.hasCommand("active")){
                            slave.active()
                        }
                        else{
                        log.debug "0-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "inactive"){
                        if(slave.hasCommand("accelerationInactive")){
                            slave.accelerationInactive()
                        }
                        else if (slave.hasCommand("inactive")){
                            slave.inactive()
                        }
                        else{
                        log.debug "1-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
                else if(accelerationReverseEnabled){
                    if(attributeValue == "active"){
                        if(slave.hasCommand("accelerationActive")){
                            slave.accelerationInactive()
                        }
                        else if (slave.hasCommand("active")){
                            slave.inactive()
                        }
                        else{
                        log.debug "2-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "inactive"){
                        if(slave.hasCommand("accelerationInactive")){
                            slave.accelerationActive()
                        }
                        else if (slave.hasCommand("inactive")){
                            slave.active()
                        }
                        else{
                        log.debug "3-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
            }
            else if(attributeName == "tamper"){
                if((!tamperReverseEnabled) || (!reverseEnabled)){
                    if(attributeValue == "detected"){
                        if(slave.hasCommand("tamperDetected")){
                            slave.tamperDetected()
                        }
                        else if (slave.hasCommand("detected")){
                            slave.detected()
                        }
                        else{
                        log.debug "0-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "clear"){
                        if(slave.hasCommand("tamperClear")){
                            slave.tamperClear()
                        }
                        else if (slave.hasCommand("clear")){
                            slave.clear()
                        }
                        else{
                        log.debug "1-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
                else if(tamperReverseEnabled){
                    if(attributeValue == "detected"){
                        if(slave.hasCommand("tamperDetected")){
                            slave.tamperClear()
                        }
                        else if (slave.hasCommand("detected")){
                            slave.clear()
                        }
                        else{
                        log.debug "2-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "clear"){
                        if(slave.hasCommand("tamperClear")){
                            slave.tamperDetected()
                        }
                        else if (slave.hasCommand("clear")){
                            slave.detected()
                        }
                        else{
                        log.debug "3-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
            }
            else if(attributeName == "threeAxis"){
                slave.threeAxis(attributeValue.x,attributeValue.y,attributeValue.z)
            }
            else if (attributeName == "battery"){
                slave.setBattery(attributeValue)
            }
            else{
                log.debug "2-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
            }
        }
    }
}

def initializeValuesContactSensor(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            if(attributeName == "contact"){
                if((!contactReverseEnabled) || (!reverseEnabled)){
                    if((attributeValue == "open") || (attributeValue == "opened")){
                        slave.open()
                    }
                    else if((attributeValue == "close") || (attributeValue == "closed")){
                        slave.close()
                    }
                    else{
                        log.debug "Could not initialize the Contact Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                    }
                }
                else if(contactReverseEnabled){
                    if((attributeValue == "open") || (attributeValue == "opened")){
                        slave.close()
                    }
                    else if((attributeValue == "close") || (attributeValue == "closed")){
                        slave.open()
                    }
                    else{
                        log.debug "Could not initialize the Contact Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                    }
                }
            }
            else if (attributeName == "temperature"){
                slave.setTemperature(attributeValue)
            }
            else if(attributeName == "tamper"){
                if((!tamperReverseEnabled) || (!reverseEnabled)){
                    if(attributeValue == "detected"){
                        if(slave.hasCommand("tamperDetected")){
                            slave.tamperDetected()
                        }
                        else if (slave.hasCommand("detected")){
                            slave.detected()
                        }
                        else{
                        log.debug "0-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "clear"){
                        if(slave.hasCommand("tamperClear")){
                            slave.tamperClear()
                        }
                        else if (slave.hasCommand("clear")){
                            slave.clear()
                        }
                        else{
                        log.debug "1-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
                else if(tamperReverseEnabled){
                    if(attributeValue == "detected"){
                        if(slave.hasCommand("tamperDetected")){
                            slave.tamperClear()
                        }
                        else if (slave.hasCommand("detected")){
                            slave.clear()
                        }
                        else{
                        log.debug "2-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                    else if(attributeValue == "clear"){
                        if(slave.hasCommand("tamperClear")){
                            slave.tamperDetected()
                        }
                        else if (slave.hasCommand("clear")){
                            slave.detected()
                        }
                        else{
                        log.debug "3-Could not initialize the Motion Sensor attribute: [${attributeName}] with value: [${attributeValue}]"
                        }
                    }
                }
            }
            else if (attributeName == "battery"){
                slave.setBattery(attributeValue)
            }
        }
    }
}

def initializeValuesHumiditySensor(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            if (attributeName == "humidity"){
                slave.setRelativeHumidity(attributeValue)
            }
            else if (attributeName == "temperature"){
                slave.setTemperature(attributeValue)
            }
            else if (attributeName == "battery"){
                slave.setBattery(attributeValue)
            }
        }
    }
}

def initializeValuesTemperatureSensor(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            if (attributeName == "temperature"){
                slave.setTemperature(attributeValue)
            }
            else if (attributeName == "humidity"){
                slave.setRelativeHumidity(attributeValue)
            }
            else if (attributeName == "battery"){
                slave.setBattery(attributeValue)
            }
        }
    }
}

def initializeBatteryLevel(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            if (attributeName == "battery"){
                slave.setBattery(attributeValue)
            }
            else{
                log.debug "Could not initialize the Battery Level attribute: [${attributeName}] with value: [${attributeValue}]"
            }
        }
    }
}

def initializeValuesAudio(def attributes){
    attributes.each {attributeName, attributeValue ->
        if(attributeValue == null) return
        //log.debug "attributeName: ${attributeName}    value: ${attributeValue}"
        for(slave in slaves){
            switch(attributeName) {
                case "mute":
                    if((!muteReverseEnabled) || (!reverseEnabled)){
                        if((attributeValue == "mute") || (attributeValue == "muted")){
                            slave.mute()
                        }
                        else{
                            slave.unmute()
                        }
                    }
                    else if(muteReverseEnabled){
                        if((attributeValue == "mute") || (attributeValue == "muted")){
                            slave.unmute()
                        }
                        else{
                            slave.mute()
                        }
                    }
                    break
                case "volume":
                    slave.setVolume(attributeValue)
                    break
                default:
                    log.debug "Could not initialize the Audio attribute: [${attributeName}] with value: [${attributeValue}]"
            }
        }
    }
}

// used in the parent app to check for endless looping
def getMasterId(){
    master.getId()
}

// used in the parent app to check for endless looping
def getSlavesId(){
    def list = []
    for(slave in slaves){
        list.add(slave.getId())
    }
    return list
}
