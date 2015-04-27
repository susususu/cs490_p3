import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class ChatClient extends Process implements Runnable {

	ArrayList<Process> group;
	
	//connection to the server
	Socket s;
	
	//Socket for incoming chat
	ServerSocket serverSocket;
	Socket _client;
	
	//ThreadPool
	ThreadPoolExecutor executor;
	
	//universal scanner( for solving the closing warning )
	Scanner sc;
	
	//server attributes
	String serverAddress;
	int portNumber;
	
	//message set
	HashMap<String, LinkedList<Integer>> messageSet;
	
	int HEARTBEAT_RATE = 5;
	int THREAD_POOL_CAPACITY = 50;
	int eventNumber;
	
	public ChatClient( String serverAddress, int portNumber ) throws IOException {
		this.eventNumber = 0;
		
		this.serverAddress = serverAddress;
		this.portNumber = portNumber;
		
		this.serverSocket = new ServerSocket(0);
		this.executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_POOL_CAPACITY);
		
		this.sc = new Scanner(System.in);
		this.group = new ArrayList<Process>();
		this.messageSet = new HashMap<String, LinkedList<Integer>>();
		
		//connecting to the server
		this.s = new Socket(serverAddress, portNumber);
		
		this.IP = s.getLocalAddress().toString().substring(1);
		this.port = this.serverSocket.getLocalPort();
		
	}

	public boolean register() throws IOException {
		System.out.print("Please Enter Your Name: ");
		this.ID = sc.nextLine();
		
		String str = this.ID + "," + this.port + "," + this.IP;
		DataOutputStream dOut = new DataOutputStream(this.s.getOutputStream());
		dOut.writeChar('r');
		dOut.writeUTF(str);
		dOut.flush();
		DataInputStream dIn = new DataInputStream(this.s.getInputStream());
		String reply = dIn.readUTF();
		if(reply.equals("Success")) {
			int i = dIn.readInt();
			this.ProcessID = i;
			return true;
		}
		return false;
	}
	
	@Override
	public void run() {
	}
	
	public void sendHeartbeat() {
		this.executor.execute(new Runnable() {

			ChatClient c;
			
			@Override
			public void run() {
				try {
					while(true) {
						System.out.println("heartbeat");
						DataOutputStream dOut = new DataOutputStream(c.s.getOutputStream());
						dOut.writeChar('h');
						dOut.flush();
						Thread.sleep(HEARTBEAT_RATE * 1000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			public Runnable init(ChatClient c) {
				this.c = c;
				return this;
			}
			
		}.init(this));
	}
	
	@SuppressWarnings("unchecked")
	public void prompt() throws IOException, ClassNotFoundException {
		while(true) {
			System.out.print("> ");
			String s = this.sc.nextLine();
			if(s.equals("exit")) {
				this.s.close();
				System.exit(0);
			} else if(s.equals("get")) {
				DataOutputStream dOut = new DataOutputStream(this.s.getOutputStream());
				dOut.writeChar('g');
				dOut.flush();
				ObjectInputStream ois = new ObjectInputStream(this.s.getInputStream());
				this.group = (ArrayList<Process>)ois.readObject();
				for(Process p: this.group) {
					System.out.printf("%s ", p.ID);
				}
				System.out.println();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
ChatClient cc = null;
		
		if(args.length == 2){
			cc = new ChatClient( args[0], Integer.parseInt(args[1]) );
		} else if(args.length == 1) {
			cc = new ChatClient ( "localhost", Integer.parseInt ( args[0] ) );
		}

		while(true) {
			if(cc.register()) break;
		}
		
		System.out.printf("Client ID: %d\n", cc.ProcessID);
		
		cc.sendHeartbeat();
		cc.prompt();
	}
}
