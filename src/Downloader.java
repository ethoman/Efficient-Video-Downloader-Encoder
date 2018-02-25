
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import java.io.*;
import java.util.*;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class Downloader {

	public final static void main(String[] args) throws Exception {
		
		try {
			
	    		/* Set up connection to server */
	    		HttpClient httpclient = HttpClientBuilder.create().build();
	        String url = "http://techslides.com/demos/sample-videos/small.mp4";
	    		HttpHead httpRequest = new HttpHead(url);
	        
	        int contentLength = -1; /* Length of mp4 */
	        String ETag = null; /* ETag of mp4 for integrity check */
	        String ifByteRange = null; /* Check if server supports byte fetch requests */
	        
		        try {
		        		/* Execute response from server, check response */
		        		HttpResponse response = httpclient.execute(httpRequest);
		        		int code = response.getStatusLine().getStatusCode();
		        		if(code != 200 && code != 206)
		        			throw new IOException("Wrong Server Response");
		        		
		        		// Get content length of mp4 file
		        		String contentLengthString = response.getFirstHeader("Content-Length").getValue();
		        		contentLength = Integer.parseInt(contentLengthString);
		        		
		        		// Get ETag for integrity check
		        		ETag = response.getFirstHeader("ETag").getValue().replace("\"", "");
		        		
		        		/* Check if byte requests are available */
		        		ifByteRange = response.getFirstHeader("Accept-Ranges").getValue();
		        } catch (IOException e) {
		            System.out.println(e.getMessage());
		        }   
	        
	        /* Let user know server request was successful */
	        System.out.println("Downloading...");
	       
	        /* Set up local file for output */
	        String outstream = "TheVideo.mp4";
	        File output = new File(outstream);
	        FileOutputStream fos = new FileOutputStream(output);
	        
	        
		    /* n is the number of threads/downloads going on at once -
	           n=1 if the server does not support byte-range get requests */
	        int n = 1;  
	        boolean parallelDownloadEnable = false;
	        if(ifByteRange.equals("bytes")) {
	        		n = 16;
	        		parallelDownloadEnable = true;
	        }
	        
	        /* Create a List of threads to keep track of all the downloads */
	        	List<DownloadMultiThread> threadList = new ArrayList<>();
	        		try {
	        			/* Divide the file into sections to download - make sure there
     				   is no rounding error: */
	        			int length = contentLength/n;
        				length+=contentLength%n;
        				
        				/* Create parallel downloads */
	        			for (int i=0; i<n; i++) {
	        				
	        				/*  Create and keep track of all current theads and downloads */
			            DownloadMultiThread object = new DownloadMultiThread(i, length, url, parallelDownloadEnable);
			            threadList.add(object);
			            
			            // Start each thread
			            object.start();
			        }
	        			
		        } finally {
		        	
		        	// Write all bytes to mp4 file while doing integrity check
		        	HashFunction hf = Hashing.md5();
		        	Hasher hc = hf.newHasher();
			        	for(DownloadMultiThread t : threadList) {
			        			// Wait for thread to finish downloading
			                t.join();
			                
			                // Write to file and update md5 hasher
			                InputStream temp = t.returnBytes();
			                int check = -1;
			                byte[] data = new byte[2048];
			                while((check = temp.read(data)) != -1) {
				                	fos.write(data, 0, check);
				                	hc.putBytes(data, 0, check);
			                }
			            }
			        	
			        	// Flush and close the mp4 file
		        		fos.flush();
		        		fos.close();
		        		
			        	// Turn CheckSum into a String
			        	String myChecksum = hc.hash().toString();
		        		    
		        		/* Check that local file has same length as server one and that
		        		 * md5 integrity check returns true */
		        		if (output.length() != contentLength || !myChecksum.equals(ETag)) {
		        			throw new Exception();
		        		}
		        		/* If everything works, tell the user the download is done */
		        		System.out.println("Download Successful!");
		        		
		        }
		    	}
	    	
	    	catch (Exception e)
	        {
	    			System.out.println("Error in Download");
	        }
	}
}
