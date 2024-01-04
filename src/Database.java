import java.io.*;
import java.time.*;
import java.util.*;

public class Database implements Serializable {
	static class Record extends Subscriber {
		long createTime;
		long lastUpdate;
		Stack<Change> recordLog;

		public Record(Subscriber sub) {
			super(sub);
			this.createTime = Instant.now().toEpochMilli();
			this.lastUpdate = createTime;
			this.recordLog = new Stack<Change>();
		}
	}

	public Change change = null;
	public Stack<Change> changeLog = null;
	public HashMap<String, Record> subscriberTable;
	public int PORT = 0;
	public long createTime;
	public long lastUpdate;

	public Database(int port) {
		this.subscriberTable = new HashMap<String, Record>();
		subscriberTable.clear();
		this.PORT = port;
		this.changeLog = new Stack<Change>();
		this.createTime = Instant.now().toEpochMilli();
		this.lastUpdate = createTime;
	}

	public Record insert(String mail, Object obj) {
		Record record = null;
		if (obj == null) {
			return null;
		} else if (obj.getClass() == (String.class)) {
			Subscriber sub = new Subscriber(null, null, mail);
			sub.setPassword((String) obj);
			sub.setSession();
			this.subscriberTable.put(mail, (record = new Record(sub)));
			this.setChange(new Change(mail, record.lastUpdate, record) {
			});
			return record;
		} else if (obj.getClass() == (Subscriber.class)) {
			Subscriber sub = (Subscriber) obj;
			sub.setSession();
			this.subscriberTable.put(mail, (record = new Record(sub)));
			this.setChange(new Change(mail, record.lastUpdate, record));
			return record;
		} else if (obj.getClass() == (Record.class)) {
			record = (Record) obj;
			record.setSession();
			this.subscriberTable.put(mail, record = (Record) obj);
			this.setChange(new Change(mail, record.lastUpdate, record));
			return record;
		} else {
			System.out.println("Wrong type object");
			return null;
		}
	}

	public Record delete(String mail) {
		Record record = this.subscriberTable.get(mail);
		if (record.isOnline != true) {
			System.out.println("User must login first");
			return null;
		} else {
			record = this.subscriberTable.remove(mail);
			this.setChange(new Change(mail, record));
			return record;
		}
	}

	public Record login(String mail, String password) {
		Record record = this.subscriberTable.get(mail);
		if (record != null) {
			if (record.getPassword().equals(password)) {
				record.setSession();
				record.lastUpdate = Instant.now().toEpochMilli();
				record.recordLog.push(new Change(this, Instant.now().toEpochMilli(), PORT));
				this.setChange(new Change(mail, record.lastUpdate, record.session));
				return record;
			} else {
				return null;
			}
		} else {
			return null;
		}

	}

	public Record logout(String mail) {
		Record record = this.subscriberTable.get(mail);
		if (record != null) {
			record.isOnline = false;
			record.clearSession();
			record.lastUpdate = Instant.now().toEpochMilli();
			record.recordLog.push(new Change(this, Instant.now().toEpochMilli(), PORT));
			this.setChange(new Change(mail, record.lastUpdate, record.session));
			return record;
		} else
			return record;

	}

	public Change setChange(Change ch) {
		this.change = ch;
		this.lastUpdate = Instant.now().toEpochMilli();
		return changeLog.push(ch);
	}

	public Change clearChange() {
		if (changeLog.peek() == change)
			changeLog.push(change);
		this.change = null;
		this.lastUpdate = Instant.now().toEpochMilli();
		return changeLog.pop();
	}

}
