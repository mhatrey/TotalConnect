# TotalConnect SmartThings Integration

NOTE: There are many awesome implementations for TotalConnect Alarm by folks in [this thread](https://community.smartthings.com/t/new-app-integration-with-honeywell-totalconnect-alarm-monitoring-system/21248/572) which you should consider if you want full control. I'll try to do justice to them (but I may fail) by listing what they do.

## List of Apps/DeviceHandlers for TotalConnect SmartThings Integration

### > SmartApp based on SmartThings Modes/Smart Home Monitor Status
---
Mode based Code: https://github.com/mhatrey/TotalConnect/blob/master/ModesBased.groovy | Author: @mhatrey

I wrote this SmartApp for simple automation based modes/routines of SmartThings. This is a very minimalistic app and I prefer not to dwindle much besides this, but there's lot more you can do using rest of the apps/device handlers written by folks in this thread. Functionality of the app - 

- SmartThings away mode = Arm Away
- SmartThings night mode = Arm Stay
- SmartThings I'm back, Good Morning mode = Disarm

With this latest version,  I have implemented a way of automatically pulling LocationID & DeviceID, so all you need to enter are your user credentials.

I have also added a SmartApp based on Smart Home Monitor Status (SHM), which will trigger alarming actions based on SHM

Code: https://github.com/mhatrey/TotalConnect/blob/master/SHMBased.groovy | Author: @mhatrey 

### > DeviceHandler that act as Panel as well as Virtualized Lock/Switch
---
Code: https://github.com/Oendaril/TotalConnectAsync/blob/master/TCAsync.groovy | Author: @Oendaril 

This device handler is written to as a SmartThings Device i.e. a virtualized Lock & Light and performs arming actions based on Lock ON/OFF & Switch ON/OFF. Notably

- Switch On = Armed Stay
- Lock On = Armed Away
- Switch/Lock Off = Disarm

After installing the code as [Device Handler](https://graph.api.smartthings.com/ide/devices), you will need to create SmartThings [Device](https://graph.api.smartthings.com/device/list), by clicking "New Device" at the top right and then filling up rest of information. 

Be sure to select "Device Type" by pointing to the Device Handler (Scroll to the bottom, you will TotalConnect Device) created before. Give it a name of your choice and device ID (Anything unique, thats not matching your existing devices)

Once the device is created you will have to edit and fill in preferences that contain your login credentials for TotalConnect. 

To find LocationID & DeviceID @Oendaril has written a crafty SmartApp that you can run in the simulator to fetch that. Install the following code as a SmartApp and get that - https://github.com/Oendaril/TotalConnectAsync/blob/master/TCTesterAsync.groovy

### > Complete TotalConnect Panel
---
Code: Go to [SmartApps](https://graph.api.smartthings.com/ide/apps) > Click Settings (Top Right) > Add a Repository with the following information 
- Owner: jhstroebel
- Name: SmartThings-TCv2
- Branch: master

Author: @jhstroebel
 
This app is a different beast in all together and gives you lot of flexibility. I have not installed this app, so I have no experience using it.

