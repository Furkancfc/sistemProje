import java.io.Serializable;
import java.time.Instant;

public class Session implements Serializable {
	public long createTime;
	public long lastlogin;
	public String email;
	public boolean isActive;

	public Session(String email) {
		this.createTime = Instant.now().toEpochMilli();
		this.email = email;
		this.isActive = true;
		this.lastlogin = createTime;
	}
}
