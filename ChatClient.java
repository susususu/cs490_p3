import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class ChatClient extends Process implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	
	int HEARTBEAT_RATE = 5;
	int THREAD_POOL_CAPACITY = 50;
	int eventNumber;
	int messageNumber;
	ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>> messageSet;
	CopyOnWriteArrayList<Message> PendingSet;
	
	public ChatClient( String serverAddress, int portNumber ) throws IOException {
		this.eventNumber = 1;
		this.messageNumber = 0;
		this.messageSet = new ConcurrentHashMap<String, CopyOnWriteArrayList<Integer>>();
		this.PendingSet = new CopyOnWriteArrayList<Message>();
		
		this.serverAddress = serverAddress;
		this.portNumber = portNumber;
		
		this.serverSocket = new ServerSocket(0);
		this.executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_POOL_CAPACITY);
		
		this.sc = new Scanner(System.in);
		this.group = new ArrayList<Process>();
		
		//connecting to the server
		this.s = new Socket(serverAddress, portNumber);
		
		this.IP = s.getLocalAddress().toString().substring(1);
		this.port = this.serverSocket.getLocalPort();
	}

	public boolean register() throws IOException, ClassNotFoundException {
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
			this.executor.execute(this);
			this.getGroup();
			this.vc = new VectorClock(this.group);
			return true;
		}
		return false;
	}
	
	@Override
	public void run() {
 		while(true) {
			try {
				Socket client = this.serverSocket.accept();
				ObjectInputStream ois;
				try {
					ois = new ObjectInputStream(client.getInputStream());
					Message m = (Message) ois.readObject();
					this.recieve(m);
					client.close();
					Thread.yield();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
 		}
	}
	
	public void rbDeliver(Message m) throws ClassNotFoundException, IOException {
		this.vc.increment(m.sender);
		CopyOnWriteArrayList<Integer> n = null;
		if(this.messageSet.containsKey(m.sender)) {
			n = this.messageSet.get(m.sender);
			if(n.contains(m.messageNumber)) return;
		} else {
			n = new CopyOnWriteArrayList<Integer>();
		}
		System.out.printf("%s : %s\n", m.sender, m.message);
		n.add(m.messageNumber);
		this.messageSet.put(m.sender, n);
		this.bebBroadcast(m);
	}
	
 	public void recieve(Message m) throws Exception {
 		this.PendingSet.add(m);
 		for(Message msg : this.PendingSet) {
 			if(this.vc.compare(msg.VC)) {
 		 		this.rbDeliver(m);
 		 		this.vc.increment(msg.sender);
 		 		this.vc.merge(msg.VC);
 		 		this.PendingSet.remove(msg);
 			} else if(this.vc.clock.size() != msg.VC.clock.size()) {
 				this.getGroup();
 				this.vc.update(this.group);
 			}
 		}
 	}

 		
	public void sendHeartbeat() {
		this.executor.execute(new Runnable() {

			ChatClient c;
			
			@Override
			public void run() {
				try {
					while(true) {
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
	public void getGroup() throws IOException, ClassNotFoundException {
		DataOutputStream dOut = new DataOutputStream(this.s.getOutputStream());
		dOut.writeChar('g');
		dOut.flush();
		
		ObjectInputStream ois = new ObjectInputStream(this.s.getInputStream());
		this.group = (ArrayList<Process>)ois.readObject();
	}
	
	public void prompt() throws IOException, ClassNotFoundException, InterruptedException {
		while(true) {
			System.out.print("> ");
			String s = this.sc.nextLine();
			if(s.equals("exit")) {
				this.s.close();
				System.exit(0);
			} else if(s.equals("get")) {
				this.getGroup();
				for(Process p: this.group) {
					System.out.printf("%s ", p.ID);
				}
				System.out.println();
			} else if(s.startsWith("bc")) {
				this.getGroup();
				String msg = s.substring(3);
				this.vc.increment(this.ID);
				Message m = new Message(this.vc, msg, this.messageNumber++, this.ID);
				this.bebBroadcast(m);
			}  else if (s.equals("test2")) {
				for(int i = 0; i < 3000; i++) {
					Message m = new Message(this.vc, Integer.toString(i), this.messageNumber, this.ID);
					this.messageNumber++;
					this.bebBroadcast(m);
					Thread.sleep(10);
				}
			} else if(s.equals("vc")) {
				System.out.println(this.vc.clock.toString());
			}
		}
	}

	public void bebBroadcast(Message m) throws ClassNotFoundException, IOException {
		this.vc.update(this.group);
		for(Process p : this.group) {
			Socket s;
			try {
				s = new Socket(p.IP, p.port);
				ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
				oos.writeObject(m);
				oos.flush();
				s.close();
			} catch (IOException e) {
				System.out.printf("%s is not online\n", p.ID);
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
