import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class SocketServer extends Thread{
	/**
     * The port that the server listens on.
     */
    private static final int PORT = 1049;

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<ConnectionPair<PrintWriter,PrintWriter,String>> writers = new HashSet<>();
    private static HashSet<Tuple<Handler,String>> waiting_writers = new HashSet<>();

    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public SocketServer() {
    }
    public void run(){
        System.out.println("The chat server is running.");
        ServerSocket listener;
		try {
			listener = new ServerSocket(PORT);
			CheckHandler ch = new CheckHandler();
			ch.start();
	        try {
	            while (true) {
	            	Handler h = new Handler(listener.accept());
	            	ch.addHandler(h);
	            	System.out.println("new Handler");
	            }
	        } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				System.out.println("listener.close()");
				listener.close();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }
    private static class CheckHandler extends Thread{
    	private HashSet<Handler> waiting_handler = new HashSet<>();
    	private boolean isAdding = false;
    	
    	public void addHandler(Handler h){
    		isAdding = true;
    		waiting_handler.add(h);
    		isAdding = false;
    	}

		@Override
		public void run() {
			while(true){
				if(isAdding)
					try {
						Thread.sleep(1000);
						continue;
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				for(Handler h: waiting_handler){
	            	// Wait for key input
					if(h.key==null) continue;
					if(h.key.length()==0) continue;
	            	boolean skip = false;
	            	waiting_handler.remove(h);
	            	// Checking writers using key or not
	            	for(ConnectionPair<PrintWriter,PrintWriter,String> p: writers){
	            		if(p.k.equals(h.key)){
	            			h.errorHandler("Duplicate key");
	            			skip = true;
	            			break;
	            		}
	            	}
	            	// Match waiting writers
	            	if(!skip){
		            	for(Tuple<Handler,String> p: waiting_writers){
		            		if(p.x.key.equals(h.key)){
		            			ConnectionPair<PrintWriter,PrintWriter,String> pair = new ConnectionPair<PrintWriter,PrintWriter,String>((PrintWriter)p.x.out,h.out,(String)p.k);
		            			h.partner_out = p.x.out;
		            			p.x.partner_out = h.out;
		            			writers.add(pair);
		            			waiting_writers.remove(p);
		            			skip = true;
		            			break;
		            		}
		            	}
	            	}
	            	// Creating waiting objects
	            	if(!skip){
		            	System.out.println("new Handler's key: "+h.key);
		            	Tuple<Handler,String> pair = new Tuple<Handler,String>(h,h.key);
		            	waiting_writers.add(pair);
		            	skip = true;
	            	}
	            	if(skip) break;
	            	System.out.println(writers);
	            	System.out.println(waiting_writers);
				}
			}
		}
    }
    
    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
    	
    	private Thread t;
    	private boolean isRunning = true;

        private String key;
        private Socket socket;
        private PrintWriter out,partner_out;
        private BufferedReader in;
        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
            t = new Thread(this);
        	t.start();
        }
        
        private void endThisThread(){
        	System.out.println(key+" endThisThread()");
            if (partner_out != null) {
                writers.remove(new ConnectionPair<>(out,partner_out,key));
            }
            else if(out != null){
            	waiting_writers.remove(new Tuple<>(this,key));
            }
            
            try {
                this.socket.close();
            } catch (IOException e) {
            	errorHandler("IOException");
            }
        	this.isRunning = false;
    		t.interrupt();
        }
        
        private void errorHandler(String s)
        {
        	if(partner_out!=null){
        		partner_out.println("ERROR");
        	}
        	out.println("ERROR");
        	this.endThisThread();
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {
            	in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                
                // Set the key
                while (isRunning && this.key==null) {
                	this.key = in.readLine();
                }
                while (isRunning && this.key.length()==0) {
                	this.key = in.readLine();
                }
            	System.out.println("Key: "+key);
                // Get commend
                while (isRunning) {
                	String input = in.readLine();
                	if(input.equals("ERROR")|| input==null) {
                		errorHandler(key+" cast ERROR signal");
                		continue;
                	}
                	if(partner_out!=null){
                		partner_out.print(input);
                		System.out.println("From "+key+" to "+partner_out+": "+input);
                	}
                	else{
                		System.out.println("From "+key+": "+input);
                	}
                }
            } catch (IOException e) {
                System.out.println(e);
                errorHandler("IOException");
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if(isRunning) endThisThread();
            }
        }
    }
}
