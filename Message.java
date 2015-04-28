import java.io.Serializable;


public class Message implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9140687310509422591L;
	
	VectorClock VC;
	String message;
	int messageNumber;
	String sender;
	
	public Message(VectorClock vC, String message, int no, String sender) {
		VC = vC;
		this.message = message;
		this.messageNumber = no;
		this.sender = sender;
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
