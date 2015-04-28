import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class VectorClock implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6436157940209221790L;
	ConcurrentHashMap<String, Integer> clock;
	
	public VectorClock(ArrayList<Process> g) {
		this.clock = new ConcurrentHashMap<String, Integer>();
		for(Process p : g) {
			this.clock.put(p.ID, 0);
		}
	}
	
	public void update(ArrayList<Process> g) {
		for(Process p : g) {
			if(!this.clock.containsKey(p.ID)) {
				this.clock.put(p.ID, 0);
			}
		}
	}
	
	public void increment(String s) {
		int i = this.clock.get(s);
		i++;
		this.clock.put(s, i);
	}
	
	public void merge(VectorClock v) {
		for(String key : v.clock.keySet()) {
			if(!this.clock.containsKey(key)) {
				this.clock.put(key, v.clock.get(key));
			}
		}
		
		for(Entry<String, Integer> e : v.clock.entrySet()) {
			int v1 = e.getValue();
			String k = e.getKey();
			int v2 = this.clock.get(k);
			int v3 = (v1 >= v2) ? v1 : v2;
			this.clock.put(k, v3);
		}
	}
	
	public boolean compare(VectorClock v) {
//		System.out.println(this.clock.toString());
//		System.out.println(v.clock.toString());
		
		if(v.clock.size() > this.clock.size()) {
			return false;
		}
		
		boolean LE_flag = true;
		boolean L_flag = false;
		
		for(Entry<String, Integer> e : v.clock.entrySet()) {
			int v1 = e.getValue();
			String k = e.getKey();
			int v2 = this.clock.get(k);
			if(v2 > v1) {
				LE_flag = false;
				break;
			}
			if(v1 > v2) {
				L_flag = true;
			}
		}
		
		if(LE_flag && L_flag) return true;
		
		boolean equal_flag = true;
		
		for(Entry<String, Integer> e : v.clock.entrySet()) {
			int v1 = e.getValue();
			String k = e.getKey();
			int v2 = this.clock.get(k);
			if(v2 != v1) {
				equal_flag = false;
			}
		}
		
		if(equal_flag) return true;
		
		boolean flag1 = false;
		boolean flag2 = false;
		for(Entry<String, Integer> e : v.clock.entrySet()) {
			int v1 = e.getValue();
			String k = e.getKey();
			int v2 = this.clock.get(k);
			if(v1 > v2) flag1 = true;
			if(v1 < v2) flag2 = true;
		}
		
		if(flag1 && flag2) return true;
		
		return false;
	}
}
