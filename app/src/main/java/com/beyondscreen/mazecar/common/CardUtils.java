  
package com.beyondscreen.mazecar.common;

import java.util.Locale;



/** 
 * ClassName:CardUtils 
 * @author   qyl
 * @version      
 */
public class CardUtils {
	  
	    /**
	     * ����byte[]�ж��Ƿ�����ݿ�
	     * @param data
	     * @return true����ݿ�false������ݿ�
	     */
	    public static boolean isIdentityCard(byte[] data) {
	        if(((data[0] >> 7) & 0x1) == 1) {
	            return true;
	        }
	        return false;
	    }
	    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', 
	        '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	    public static String getBlockString(byte[] uid, byte[] block) {
	        	String result = bytesToHex(uid).concat(bytesToHex(block));
	            return "0x"+result.toUpperCase(Locale.US);
	    }
		/**
		 * byte[] to hex string
		 * 
		 * @param bytes
		 * @return
		 */
		private static String bytesToHex(byte[] bytes) {
		    // һ��byteΪ8λ����������ʮ������λ��ʶ
		    char[] buf = new char[bytes.length * 2];
		    int a = 0;
		    int index = 0;
		    for(byte b : bytes) { // ʹ�ó���ȡ�����ת��
		        if(b < 0) {
		            a = 256 + b;
		        } else {
		            a = b;
		        }
		
		        buf[index++] = HEX_CHAR[a / 16];
		        buf[index++] = HEX_CHAR[a % 16];
		    }
		
		    return new String(buf);
		}
}
  