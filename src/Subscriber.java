import java.io.Serializable;

public class Subscriber implements Serializable {

    private String name;
    private String surname;
    private String email;
    private String password;
    protected boolean isOnline;
    protected Session session;  //! session null olamaz, bos oturum email adresi olmayan session olur.

    Subscriber(Subscriber sub) {
        this.name = sub.name;
        this.surname = sub.surname;
        this.email = sub.email;
        this.password = sub.password;
        this.isOnline = sub.isOnline;
        this.session = sub.session;
    }

    Subscriber(String name, String surname, String email) {
        this.name = name;
        this.surname = surname;
        this.email = email;
    }

    Subscriber(String email, String password) {
        name = null;
        surname = null;
        this.email = email;
        this.setPassword(password);
    }

    public String getFullname() {
        return this.name + " " + this.surname;
    }

    public void setFullname(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getMail() {
        return this.email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setSession() {
        this.session = new Session(this.email);
        this.isOnline = true;
    }

    public void clearSession() {
        this.session = new Session(null);
        this.isOnline = false;
    }
}
