import java.io.Serializable;

public class LoginData implements Serializable {
    private String id;
    private String pw;
    private String mode;
    public LoginData(String id, String pw, String mode) {
        this.id = id;
        this.pw = pw;
        this.mode = mode;
    }
    public String getId(){
        return id;
    }
    public String getPw(){
        return pw;
    }
    public String getMode(){
        return mode;
    }
}
