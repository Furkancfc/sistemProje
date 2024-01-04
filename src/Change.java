import java.io.*;
import java.time.*;

public class Change implements Serializable {
	Object chcontent;
	long chtime;
	Object chlocation;

	public Change(Object content, Object location) {
		this.chcontent = content;
		this.chtime = Instant.now().toEpochMilli();
		this.chlocation = location;
	}

	public Change(Object content, long chtime, Object location) {
		this.chcontent = content;
		this.chtime = chtime;
		this.chlocation = location;
		if (content == null || chtime == 0 || location == null) {
			return;
		}
	}
}
