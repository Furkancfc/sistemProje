import java.io.*;
import java.time.*;
import java.util.*;
import java.sql.*;

public class Database implements Serializable {
	public static class Record extends Subscriber {
		public long createTime;
		public long lastUpdate;
		private Stack<Change> recordLog;

		public Record(Subscriber sub) {
			super(sub);
			this.createTime = Instant.now().toEpochMilli();
			this.lastUpdate = createTime;
			this.recordLog = new Stack<Change>();
		}

		public void setCreateTime(long createTime) {
			this.createTime = createTime;
		}

		public void setLastUpdate(long lastUpdate) {
			this.lastUpdate = lastUpdate;
		}

		public long getCreateTime() {
			return createTime;
		}

		public long getLastUpdate() {
			return lastUpdate;
		}
	}

	public Change change = null;
	public Stack<Change> changeLog = null;
	public HashMap<String, Database.Record> subscriberTable;
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

	public Record modify(String mail, Object object) {
		if (object instanceof Database.Record) {
			Database.Record record = this.subscriberTable.replace(mail, (Database.Record) object);
			this.changeLog.push(this.setChange(new Change(record, "UPDATE", record.lastUpdate)));
			return record;
		} else {
			System.err.println("Wrong type object");
			return null;
		}
	}

	public Record insert(String mail, Object obj) {
		Record record = null;
		if (obj == null) {
			return record;
		} else if (obj.getClass() == (String.class)) {
			Subscriber sub = new Subscriber(null, null, mail);
			sub.setPassword((String) obj);
			sub.setSession();
			this.subscriberTable.put(mail, (record = new Record(sub)));
			record.recordLog.push(this.setChange(new Change(record, "INSERT", record.lastUpdate)));
			return record;
		} else if (obj.getClass() == (Subscriber.class)) {
			Subscriber sub = (Subscriber) obj;
			sub.setSession();
			this.subscriberTable.put(mail, (record = new Record(sub)));
			record.recordLog.push(this.setChange(new Change(record, "INSERT", record.lastUpdate)));
			return record;
		} else if (obj.getClass() == (Record.class)) {
			record = (Record) obj;
			record.setSession();
			this.subscriberTable.put(mail, record);
			record.recordLog.push(this.setChange(new Change(record, "INSERT", record.lastUpdate)));
			return record;
		} else {
			System.out.println("Wrong type object");
			return null;
		}
	}

	public Record delete(String mail) {
		Record record = this.subscriberTable.get(mail);
		if (record != null) {
			// if (record.isOnline != true) {
			// System.out.println("User must login first");
			// return null;
			// } else {
			record = this.subscriberTable.remove(mail);
			record.recordLog.push(this.setChange(new Change(record, "DELETE")));
			return record;
			// }
		} else
			return null;
	}

	public Record login(String mail, String password) {
		Record record = this.subscriberTable.get(mail);
		if (record != null) {
			if (record.comparepassword(password)) {
				record.setSession();
				record.lastUpdate = Instant.now().toEpochMilli();
				record.recordLog.push(this.setChange(new Change(record, "UPDATE", record.lastUpdate)));
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
			record.clearSession();
			record.lastUpdate = Instant.now().toEpochMilli();
			record.recordLog.push(this.setChange(new Change(record, "UPDATE", record.lastUpdate)));
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
		this.change = null;
		this.lastUpdate = Instant.now().toEpochMilli();
		return changeLog.pop();
	}

}
