import java.io.Serializable;


public class Message implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9140687310509422591L;
	
	VectorClock VC;
	String message;
	
	public Message(VectorClock vC, String message) {
		VC = vC;
		this.message = message;
	}

	public VectorClock getVC() {
		return VC;
	}

	public void setVC(VectorClock vC) {
		VC = vC;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
