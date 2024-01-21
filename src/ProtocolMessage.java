import java.io.*;
public class ProtocolMessage implements Serializable {
	String message = null;
	public ProtocolMessage(String message){
		this.message = message;
	}
}
