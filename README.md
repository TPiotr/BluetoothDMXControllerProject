# What this is
System that allows setting DMX channels values using wireless bluetooth connection using electronic circuit made from arduino,
bluetooth module, DMX module and smartphone with Android system. Android app written in Java, arduino sketch avaliable in arduino folder ([arduino sketch link](arduino/dmx_bt_sketch.ino)). 

(Current android app alows to operate with only first 8 channels, could be easily extendend by adding offset in [writeDMXValue method, line 323](app/src/main/java/dmx/main/DMXConsoleActivity.java))

(Arduino circuit allows to use max 256 channels (because channel index is send as one byte of data))

## How communication works
1 command is build from **3 bytes (1 byte - check byte (default 99), 2 byte - channel index, 3 byte - channel value)** 

Check byte is used to prevent situation where some of 3 bytes is lost and arduino will read remaining bytes as totally random value

## Features
- Save, load values of seekbars
- Color choosing by selecting color from color picker

## Video
[![Dmx showcase video](http://img.youtube.com/vi/GR3v3LT25qU/0.jpg)](http://www.youtube.com/watch?v=GR3v3LT25qU)

## Screenshots
![Wololo](/../master/screenshots/dmx11.png?raw=true "Wolololo!")
![Wololo](/../master/screenshots/dmx3.png?raw=true "Wolololo!")
![Wololo](/../master/screenshots/dmx4.png?raw=true "Wolololo!")
