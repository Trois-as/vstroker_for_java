# vstroker_for_java

TestVstroker.java is a java class to test your Vstroker and display raw data from the accelerometer in
a command prompt.

Many thanks to qdot which have published his driver for the Vstroker in python : https://github.com/qdot/libvstroker.
It saves a lot of time of reverse engineering.

How to use :
- download usb4java at http://usb4java.org
- compile TestVstroker.java with :

If you are under unix:
javac -cp '.:commons-lang3-3.2.1.jar:usb4java-1.2.0.jar:libusb4java-1.2.0-xxx*' TestVstroker.java

If you are under windows:
javac -cp .;commons-lang3-3.2.1.jar;usb4java-1.2.0.jar;libusb4java-1.2.0-xxx* TestVstroker.java


Run the program with this :

Under unix:
java -cp '.:commons-lang3-3.2.1.jar:usb4java-1.2.0.jar:libusb4java-1.2.0-xxx*' TestVstroker 10**

Under windows:
java -cp .;commons-lang3-3.2.1.jar;usb4java-1.2.0.jar;libusb4java-1.2.0-xxx* TestVstroker 10**

* replace libusb4java-1.2.0-xxx by the corresponding jar of your OS and CPU architecture : by example
on windows 7 64bits use libusb4java-1.2.0-windows-x86_64.jar and on 32 linux OS use 
libusb4java-1.2.0-linux-x86.jar.

** this parameters is the number of seconds to run the test

