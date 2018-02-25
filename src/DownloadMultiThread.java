import java.io.*;
import java.net.*;
import com.google.common.io.FileBackedOutputStream;

class DownloadMultiThread extends Thread {
	int bytes; // Total amount of bytes to download in this thread
	String url = ""; // URL of the mp4 file
	int offset; // The section of the mp4 file to download
	boolean enable; // Are byte requests enabled?
	FileBackedOutputStream output; // Where bytes are stored from server
	
	public DownloadMultiThread (int offset, int bytes, String url, boolean enable) {
		this.enable = enable;
		this.url += url;
		this.bytes = bytes;
		this.offset = offset;
		output = new FileBackedOutputStream(256);
	}
	
	public void run()
    {
		int start = offset*bytes; // Where to start the chunk download
		int end = ((offset+1)*bytes-1); // Where to end the chunk download
		int count = 0;
		
		// Thread has 3 tries to download the chunk correctly in case there is an error
		while (true) {
	        try {
		        	InputStream in = null; // The InputStream
	        		try {
		        		/* Connect to server */
		        		URL theUrl = new URL(url);
			 	    URLConnection urlConn = theUrl.openConnection();
			 	    
			 	    if (enable) // If possible, engage in parallel downloads of the file
			 	    		urlConn.setRequestProperty("Range", "bytes=" + start + "-" + end + "");
			 	    
			 	    in = urlConn.getInputStream();  // Get InputStream
	        		}
	        		catch (IOException e) {
			            System.out.println(e.getMessage());
			    }
	        		
		 	    /* Start reading in the mp4 */
		        	byte[] buffer = new byte[2048];
		        int check = -1;  
	            
		        /* Write the bytes in this section of the mp4 to FileBackedOutputStream */
	            while ((check = in.read(buffer)) != -1) {
	            		output.write(buffer, 0, check);
	            }
	            
	            /* Close InputStream */
	            in.close();
	            
	            /* Break if chunk was successfully read to EOF */
	            if (check == -1) break;
	        }
	        
	        catch (Exception e)
	        {	/* Retry the chunk download three times before throwing an error */
	        		if(++count < 3) {
	        			try {
	        				output.reset();
	        				} 
	        			catch(IOException g) {
	        				System.out.println ("Error reseting faulty download");
	        				}
			        	continue;
			    }
	            /* Throwing an exception if something doesn't work */
	            System.out.println ("Error during download");
	        }
	    }
    }
	
	/* Give the download manager a way to retrieve downloaded bytes from this thread */
	public InputStream returnBytes() {
		InputStream i = null;
			try {
				i = output.asByteSource().openBufferedStream();
			} 
			catch(IOException e) { 
				System.out.println("Error fetching bytes"); 
			}
		return i;
	}	
}
