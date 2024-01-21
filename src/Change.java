import java.io.*;
import java.time.*;

public class Change implements Serializable {
	Object content; // id of change
	Object operation;	// what changed
	long timestamp;

	public Change(Object content, Object operation) {
		this.content = content;
		this.operation = operation;
		this.timestamp = Instant.now().toEpochMilli();
	}

	public Change(Object content, Object operation, long timestamp) {
		this.content = content;
		this.timestamp = timestamp;
		this.operation = operation;
	}
}
