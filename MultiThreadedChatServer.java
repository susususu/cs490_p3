import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MultiThreadedChatServer implements Runnable{
	
	//pool size
		int THREAD_POOL_CAPACITY = 50;
		
		//main server attributes
		ServerSocket serverSocket;
		static ArrayList<Process> group;
		static ConcurrentHashMap<String, Long> heart_beat;
		ThreadPoolExecutor executor;
		int port;
		static int ClientIDCounter;
		
		//child server attributes
		Socket _client;
		String name;
		DataInputStream dIn;
		
		
		//create the main server
		public MultiThreadedChatServer(int port) {
			this.executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_POOL_CAPACITY);
			this.port = port;
			
			//create the server socket
			try {
				this.serverSocket = new ServerSocket(port);
			} catch ( Exception e ) {
			}
			
			group = new ArrayList<Process>();
			heart_beat = new ConcurrentHashMap<String, Long>();
			ClientIDCounter = 0;
		}
		
		//child server constructor
		public MultiThreadedChatServer(Socket client) throws IOException {
			this._client = client;
			this.dIn = new DataInputStream(client.getInputStream());
		}
		
		private synchronized static boolean removeFromGroup(Process s) {
			return MultiThreadedChatServer.group.remove(s);
		}
		
		public synchronized boolean modifyGroup(String s) throws IOException {
			String[] tok = s.split(",");
			System.out.println(s);
			for(Process m : MultiThreadedChatServer.group) {
				if(m.ID.equals(tok[0])) {
					String str = "Failed";
					DataOutputStream dOut = new DataOutputStream(this._client.getOutputStream());
					dOut.writeUTF(str);
					dOut.flush();
					return false;
				}
			}
			
			//success
			Process p = new Process();
			p.ID = tok[0];
			p.port = Integer.parseInt(tok[1]);
			p.IP = tok[2];
			MultiThreadedChatServer.group.add(p);
			String str = "Success";
			DataOutputStream dOut = new DataOutputStream(this._client.getOutputStream());
			dOut.writeUTF(str);
			dOut.flush();
			dOut.writeInt(ClientIDCounter);
			ClientIDCounter++;
			dOut.flush();
			return true;
		}
		
		public void checkingHeartbeat() {
			
			this.executor.execute(new Runnable() {

				@Override
				public void run() {
					while(true) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
						}
						System.out.println("Checking Heartbeat");
						
						ArrayList<Process> g = new ArrayList<Process>();
						
						Long time = System.currentTimeMillis();
						for(Process s : MultiThreadedChatServer.group) {
							Long t = MultiThreadedChatServer.heart_beat.get(s.ID);
							if(time - t > 10000) {
								g.add(s);
							}
						}
						for(Process s : g) {
							MultiThreadedChatServer.heart_beat.remove(s.ID);
							if( !removeFromGroup(s)) {
								System.out.println("removing failed");
							}
							System.out.printf("%s removed\n", s);
						}
					}
				}
				
			});
		}
	
		@Override
		public void run() {
			try {
				while(true) {
					char flag = this.dIn.readChar();
					System.out.printf("%c\n", flag);
					if(flag == 'r') {
						String m = this.dIn.readUTF();
						this.modifyGroup(m);
						String[] tok = m.split(",");
						this.name = tok[0];
						MultiThreadedChatServer.heart_beat.put(this.name, System.currentTimeMillis());
					} else if(flag == 'g') {
						System.out.println(MultiThreadedChatServer.group.toString());
						ObjectOutputStream oos = new ObjectOutputStream(this._client.getOutputStream());
						oos.reset();
						oos.writeObject(MultiThreadedChatServer.group);
						oos.flush();
					} else if(flag == 'h') {
						System.out.println("heartbeat");
						MultiThreadedChatServer.heart_beat.put(this.name, System.currentTimeMillis());
					}
				}
			} catch (Exception e) {
				System.out.printf("Connection to %s lost\n", this.name);
				Thread.yield();
			}
		}
		
		public void startServer() {
			System.out.println("Start!");
			this.checkingHeartbeat();
			while (true) {
				try {
					Socket _client = this.serverSocket.accept();
					MultiThreadedChatServer ms = new MultiThreadedChatServer(_client);
					this.executor.execute(ms);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public static void main(String[] args) {
			if(args.length != 1) {
				System.err.println("need port number");
				System.exit(1);
			}
			MultiThreadedChatServer ms = new MultiThreadedChatServer(Integer.parseInt(args[0]));
			ms.startServer();
		}
}
