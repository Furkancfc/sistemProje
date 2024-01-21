import java.math.BigInteger;
import java.io.Serializable;
import java.security.*;

public class Subscriber implements Serializable {

    private String name;
    private String surname;
    private String email;
    private String password;
    protected boolean isOnline;
    protected Session session; // ! session null olamaz, bos oturum email adresi olmayan session olur.

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

    Subscriber(String name, String surname, String email, String password) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
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
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(password.getBytes());
            BigInteger no = new BigInteger(1, digest);
            String securePassword = no.toString(16);
            password = securePassword;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.password = password;
        }
    }

    public String getPassword() {
        return password;
    }

    public boolean comparepassword(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, digest);
            String md5input = no.toString(16);
            if (md5input.equals(this.password))
                return true;
            else
                return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;

        }
    }
    public void setSession() {
        if (this.session != null && this.isOnline == true) {
            return;
        }
        this.session = new Session(this.email);
        this.isOnline = true;
    }

    public void clearSession() {
        if (session == null && this.isOnline == false) {
            return;
        }
        this.isOnline = false;
        this.session = new Session(null);
        this.session.isActive=false;
    }
}
