---
layout: default
theme: cosmo
title: APVS
---
###**APVS** is the ATLAS Procedures Visualizer System for the Wireless Safety System.


----
----
__APVS WEBSERVER__

 **GUI** 

The web client application of apvs is written in java (then converted in javascript) and it connects to the server via [Atmosphere](https://github.com/Atmosphere/atmosphere), the Asynchronous WebSocket/Comet Framework.
The user interface is providing the supervisor with the following information:

Sensor Data,Video, Plots, 





**apvs-asterisk** 

Asterisk is a communication server. It is the communication toolkit used for real-time communication in apvs project. 
  Although it is read-only, it is upgradeable. 
  Asterisk handles all the low level details of sending and receiving data using lots of different communication protocols. 
  For a communications application to work, you need the communications server connected to communication services (VoIP or PSTN).
  Voice over IP (VoIP) is a methodology and group of technologies for the delivery of voice communications and multimedia sessions over Internet Protocol (IP) networks, such as the Internet.
  For people to access your communications system you need phone numbers or VoIP URIs that send calls to your server.
  The server components are handling all of the low level details of the underlying protocols.
  Your application doesn't have to worry about the byte alignment, the packet size, the codec or any of the   thousands of other critical details that make the application work. 
  This is the power of an engine.
  Asterisk-Java is the free Java library for Asterisk PBX (projet builder) integration.
  
  For more information visit:
  [1](https://wiki.asterisk.org/wiki/display/AST/AMI+v2+Specification#AMIv2Specification-Introduction),
  [2](https://wiki.asterisk.org/wiki/display/AST/Asterisk+as+a+Swiss+Army+Knife+of+Telephony),
  [3](http://www.voip-info.org/wiki/view/Asterisk+manager+API)



**apv-charts**

In order to create the plots we are using GWT Highcharts which is a comprehensive API enabling the use of Highcharts within a GWT application. 

  [GWT Highcharts](http://www.moxiegroup.com/moxieapps/gwt-highcharts/)   ,the moxie group   deliver the jar file is a freely available open source library that provides an elegant and feature complete approach for including Highcharts 
  and Highstock visualizations within a GWT application using pure Java code (including GWT widget libraries, such as SmartGWT or Ext GWT). 

  Highcharts is a charting library written in pure JavaScript, offering intuitive, interactive charts to your own website or web application.
  They support line, spline, area, areaspline, column, bar, pie and scatter chart types.

  




![alt edjiwefiuw](json_byte2packets.svg)


**apvs-configuration**

APVS configuration is an apache configuration.

If needed to update commons-configuration which is needed in order to read the option file vs configuration you need to do the following:

compile commons-configuration in ~/cern/svn/
copy commons-configuration-2.0-SNAPSHOT.jar to current dir with svn version number
copy commons-configuration-2.0-SNAPSHOT-sources.jar to current dir with svn version number
copy pom.xml to current dir as commons-configuration-2.0-SNAPSHOT-pom.xml with svn version number



**apvs-converter**

**apvs-daq-server** 

The code for Data Acquisition.
There are several classes under apvs-daq-server --> src/main/java --> ch.cern.atlas.apvs.daq.server :

**a) DaqServer**

In order to make a measurement available at an outport we have to create a filter to the bus (see filterport description)

Json Message Encoder 
Replaying decoder makes sure that the json message is fully received.
 
**b) DatabaseWriter**
connecting to the bus, taking every message and storing in the DB.

There are four types in json:

**1)** general configuration (in json is key values pairs) and tells you which access point it is connected, which dosimeter it is actually connected to.

**2)** measurement conf. shows the limits for the measurement (thresholds)

**3)** events  --->alarms, shut down , boot, switch on/off, events happen occasionally.

**4)** measurements in regular basis.


**c) DebugHandler:** what goes on and off the bus, it is saved in a log file, for every message.

**d) EventFilter:** filtering events.

**e) Filter:** the interface for filtering the measurements.

**f) TypeEventFilter**

It is not currently being used.

**g) ValueFilter**
check through "messagetothebus" declaration if needed to be used.


To define an inport for receiving data and an outport to send data, edit the APVS.properties file of the apvs-daq-server. 

*Example:*
 
*APVS.daq.inport=10123*

*APVS.daq.outport=10124*

*APVS.daq.filterport=10125*

> Additionally, a filter port was created in order to send specific needed data to a port (filterport).

see photo 2 







