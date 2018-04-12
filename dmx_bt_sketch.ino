#include <DmxSimple.h>
#include <SoftwareSerial.h>

#define CHECK_BYTE_VALUE 99

#define VALUES_PER_CHANNEL 255
#define USING_CHANNELS 64 //can be max 255
#define DMX_DIGITAL_PIN 3

//bluetooth pins
#define BT_RX_PIN 10
#define BT_TX_PIN 11

//flag indicating if arduino should log about new data coming
boolean debug = false;

//serial that reads data from bluetooth module
SoftwareSerial bluetooth_serial(BT_RX_PIN, BT_TX_PIN);

void setup() {
  //setup serial for debug
  Serial.begin(9600);
  Serial.setTimeout(50);

  //setup bt serial
  bluetooth_serial.begin(9600);
  bluetooth_serial.setTimeout(50);

  //setup dmx library
  DmxSimple.usePin(DMX_DIGITAL_PIN);
  DmxSimple.maxChannel(USING_CHANNELS);

  //processing sygnalizator
  pinMode(13, OUTPUT);
  digitalWrite(13, LOW);
}

void loop() {
  //check if we can read some new value from bluetooth
  if(bluetooth_serial.available() >= 3) {
    //indicate that we are processing something from bluetooth connection
    digitalWrite(13, HIGH);
    
    byte check_byte = bluetooth_serial.read();
    int channel = bluetooth_serial.read();
    int value = bluetooth_serial.read();

    //print debug info if debug flag equals true
    if(debug) {
      Serial.print("--- \n");
      Serial.print("check_byte: ");
      Serial.print(check_byte);
      Serial.print(" channel: ");
      Serial.print(channel);
      Serial.print(" value: ");
      Serial.print(value);
      Serial.print(" value_set?: ");
    }

    //check if check byte is a proper one if not we have situation where value was send in wrong order or some byte is lost
    //so program have to skip this value to dont mess some dmx value
    //because current value from bytes with wrong order could be totally random
    if(check_byte == CHECK_BYTE_VALUE) {
      setChannelValue(channel, value);

      if(debug) {
        Serial.print("true");
      }
    } else {
      if(debug) {
        Serial.print("false");
      }

      //clear out software serial data, because problem occured
      //problem is simple some from 3 bytes is missing and order in buffer is destroyed because of that, so program is not able to read any data from buffer anymore
      //so buffer must be cleared and again wait for data and receive it but in proper order now (hopefully)
      while(bluetooth_serial.available() > 0) {
        bluetooth_serial.read();
      }
    }

    if(debug) {
      Serial.print("\n");
    }
  
    //stop indicating processing process
    digitalWrite(13, LOW);
  }
}

void setChannelValue(int channel, int value) {
  DmxSimple.write(channel, value);
}


/**
 * 
 * Old reading from bluetooth value method with bitshifting (combine 2 bytes into one variable)
 * 
 * 
 * 
//reading from bluetooth variables
int value = 0;
boolean parse_new_value = false;

int bitShiftCombine(unsigned char x_high, unsigned char x_low) {
  int combined; 
  combined = x_high;              //send x_high to rightmost 8 bits
  combined = combined << 8;         //shift x_high over to leftmost 8 bits
  combined |= x_low;              //logical OR keeps x_high intact in combined and fills in                                                             
  return combined;
}

//parse new integer if parse_new_value flag is set to true
  if(parse_new_value) {
    int channel = value / (VALUES_PER_CHANNEL + 1);
    int channel_value = value % (VALUES_PER_CHANNEL + 1);
    setChannelValue(channel, channel_value);

    //if debug mode enabled print debug info
    if(debug) {
      Serial.println("---");
      Serial.println(value);
      Serial.print("channel: ");
      Serial.print(channel);
      Serial.print(" value: ");
      Serial.print(channel_value);
      Serial.println("");
    }

    //tell program that new value was parsed allready
    parse_new_value = false;
  }

 */

