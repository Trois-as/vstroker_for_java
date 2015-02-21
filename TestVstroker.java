/*--------------------------------------------------------------------------------
File : TestVstroker.java
----------------------------------------------------------------------------------
* Read the raw data from a Vstroker, and display them on a CLI 
* Version : 0
* Project :  
* License : 
* Ressources : commons-lang3-3.2.1.jar, libusb4java-1.2.0-xxx.jar, usb4java-1.2.0.jar
----------------------------------------------------------------------------------
* Modifications     :
--------------------------------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.*;
import java.lang.Math;

import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;
import org.usb4java.BufferUtils;
import org.usb4java.EndpointDescriptor;


public class TestVstroker {

      /** The vendor ID of the Vstroker. */
      private static final short VENDOR_ID = 0x0451;
  
      /** The product ID of the Vstroker. */
      private static final short PRODUCT_ID = 0x55a5;
      
      /** The input endpoint of the Vstroker. */
      private static final byte IN_ENDPOINT = (byte) 0x81;
  
      /** The communication timeout in milliseconds. */
      private static final int TIMEOUT = 5000;   
      

      public static Device findUSBModule() {
      
            // Read the USB device list
            DeviceList list = new DeviceList();
            int result = LibUsb.getDeviceList(null, list);
            if (result < 0) {
              throw new RuntimeException("Unable to get device list. Result=" + result);
            }
    
            try {
               // Iterate over all devices and scan for the USB Key
               for (Device device: list) {
                  DeviceDescriptor descriptor = new DeviceDescriptor();
                  result = LibUsb.getDeviceDescriptor(device, descriptor);
                  if (result < 0) {
                      throw new RuntimeException("Unable to read device descriptor. Result=" + result);
                  }
                  if (descriptor.idVendor() == VENDOR_ID && descriptor.idProduct() == PRODUCT_ID) { 
                    return device;
                  }  
               }
            }
            finally {
                   // Ensure the allocated device list is freed
                   LibUsb.freeDeviceList(list, true);
            }
    
            // No USB Key found
            return null;         
      }
      
      public static ByteBuffer read(DeviceHandle handle, int size) {
      
            ByteBuffer buffer = BufferUtils.allocateByteBuffer(size).order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer transferred = BufferUtils.allocateIntBuffer();
            int result = LibUsb.interruptTransfer(handle, IN_ENDPOINT, buffer,transferred, TIMEOUT);
            
            if (result != LibUsb.SUCCESS) {
              throw new LibUsbException("Unable to read data", result);
            }

            return buffer;
      } 
      
      public static int uncryptByte(int crypted_word, int xor_byte) {
      
            int uncrypted_word_int;
            int uncrypter_data_1 = 0xf;
            int uncrypter_data_2 = 4;
            
            uncrypted_word_int = (((crypted_word & uncrypter_data_1) << uncrypter_data_2) | (crypted_word >> uncrypter_data_2)) ^ xor_byte;
            
            return uncrypted_word_int;
      }
      
      public static int make_signed_int(int uncrypted_word_1, int uncrypted_word_2) {
          
            int double_decrypted_word = uncrypted_word_1 | (uncrypted_word_2 << 8);
            if (double_decrypted_word > Math.pow(2, 15)) {
              double_decrypted_word = double_decrypted_word - (int)Math.pow(2, 15);
            }

            return double_decrypted_word;
      }       
      
      public static void main(String[] args) {
      
            long debut_chrono;
            long end_chrono;
            long duration_ms;
            int duration_sec = Integer.parseInt(args[0]);
            long actual_chrono;
            
            // Initialize the libusb context
            int result = LibUsb.init(null);
            if (result != LibUsb.SUCCESS) {
              throw new LibUsbException("Unable to initialize libusb", result);
            }
    
            // Search for the USB key and stop when not found
            Device device = findUSBModule();
            if (device == null) {
              System.err.println("USB key not found.");
              System.exit(1);
            }
            
            // Open Vstroker device
            final DeviceHandle handle = LibUsb.openDeviceWithVidPid(null, VENDOR_ID, PRODUCT_ID);
            if (handle == null) {
              System.err.println("Vstroker device not found.");
              System.exit(1);
            }        
    
            try {
               // Claim interface
               result = LibUsb.claimInterface(handle, 0);
               if (result != LibUsb.SUCCESS) {
                 throw new LibUsbException("Unable to claim interface", result);
               }
               
               duration_ms = duration_sec * 1000;
               debut_chrono = java.lang.System.currentTimeMillis();
               end_chrono = debut_chrono + duration_ms;
               actual_chrono = java.lang.System.currentTimeMillis();
               while (actual_chrono <= end_chrono) {
                    // Receive the a raw trame from the USB key and put it in a buffer
                    ByteBuffer trame = read(handle, 10); // 10 is the wMaxPacketSize of the Vstroker
                    byte[] crypt_array; // byte array with crypted data
                    crypt_array = new byte[10];
                    int[] uncrypt_array; // store decrypted data of the trame 
                    uncrypt_array = new int[10];
                    trame.get(crypt_array); // Dump of the buffers data into a byte array that allow us to manipulate the data sends by the Vstroker
                    
                    int crypted_first_word = (int)crypt_array[0];
                    int crypted_word_int;
                    int uncrypted_word_int;
                    int double_decrypted_word_1;
                    int double_decrypted_word_2;
                    int final_data;
                    
                    for(int i=1 ; i<5 ; i++) {
                       crypted_word_int = (int)crypt_array[i];        
                       uncrypted_word_int = uncryptByte(crypted_word_int, crypted_first_word);
                       uncrypt_array[i] = uncrypted_word_int;
                    }

                    // Interleaving of byte 2 and 3 of the trame in one bigger word and 4 and 5 in another
                    double_decrypted_word_1 = make_signed_int(uncrypt_array[1], uncrypt_array[2]);
                    double_decrypted_word_2 = make_signed_int(uncrypt_array[3], uncrypt_array[4]);
            
                    // Making a directly usable data :
                    final_data = double_decrypted_word_1 + (- double_decrypted_word_2);
                    System.out.println(final_data);
                    actual_chrono = java.lang.System.currentTimeMillis();
               }   
            }
            finally
            {
                LibUsb.close(handle);
            }
    
            // Deinitialize the libusb context
            LibUsb.exit(null);
      }
}  
                 
