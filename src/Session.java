import java.io.Serializable;
import java.time.Instant;

public class Session implements Serializable {
	public String email;
	public long createTime;
	public long lastlogin;
	public boolean isActive;

	public Session(String email) {
		Instant now = Instant.now();
		this.createTime = now.toEpochMilli() *1_000_000 + now.getNano();
		this.email = email;
		this.isActive = true;
		this.lastlogin = createTime;
	}
}
