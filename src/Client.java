import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class Client extends JFrame {

    private String address;
    private String port;
    private JPanel j_paint;
    private JLabel title= new JLabel("제목");
    private JTextField login= new JTextField();
    private JTextField password= new JTextField();
    private JButton signinButton = new JButton("로그인");
    private JButton signupButton = new JButton("생성");
    private JTextArea serverChat;

    public Client(String address, String port){
        this.address = address;
        this.port = port;
        this.setBounds(0, 0, 1000, 550);
        this.setTitle("Client GUI");
        startConnect();
        startGUI();

        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }
    public void startGUI(){
        this.add(new JScrollPane(serverChat));
        this.getContentPane().add(paintPanel(), BorderLayout.CENTER);
        serverChat = new JTextArea(20,10);
        this.getContentPane().add(serverChat, BorderLayout.EAST);
        serverChat.setVisible(false);
    }
    public JPanel paintPanel(){
        JPanel paintPanel = new JPanel();
        paintPanel.setLayout(null);
        title.setBounds(475, 100, 50, 50);
        login.setBounds(400, 300, 140, 25);
        password.setBounds(400, 325, 140, 25);
        signinButton.setBounds(540, 300, 70, 25);
        signupButton.setBounds(540, 325, 70, 25);

        //startButton.setPressedIcon(startPressed);
        //startButton.setContentAreaFilled(false);
        //startButton.setBorderPainted(false);

//        startButton.addActionListener(new InGame());
//        endButton.addActionListener(new ExitGame());
        //시작버튼으로 게임 실행.

        login.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (login.getText().equals("아이디")) {
                    login.setText("");
                    login.setForeground(Color.BLACK);  // 기본 텍스트 색으로 변경
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (login.getText().isEmpty()) {
                    login.setText("아이디");
                    login.setForeground(Color.GRAY);  // placeholder 색으로 변경
                }
            }
            });
        password.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (password.getText().equals("비밀번호")) {
                    password.setText("");
                    password.setForeground(Color.BLACK);  // 기본 텍스트 색으로 변경
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (password.getText().isEmpty()) {
                    password.setText("비밀번호");
                    password.setForeground(Color.GRAY);  // placeholder 색으로 변경
                }
            }
        });

        signinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(login.getText().equals("") || login.getText().equals("아이디")){
                    JOptionPane.showMessageDialog(paintPanel, "경고: 아이디가 잘못되었습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if(password.getText().equals("") || password.getText().equals("비밀번호")){
                    JOptionPane.showMessageDialog(paintPanel, "경고: 비밀번호가 잘못되었습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                title.setVisible(false);
                login.setVisible(false);
                password.setVisible(false);
                signinButton.setVisible(false);
                signupButton.setVisible(false);
                serverChat.setVisible(true);
            }
        });
        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(login.getText().equals("") || login.getText().equals("아이디")){
                    JOptionPane.showMessageDialog(paintPanel, "경고: 생성할 수 없는 아이디입니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(paintPanel, "경고: 회원가입에 성공했습니다.", "경고", JOptionPane.WARNING_MESSAGE);
            }
        });

        paintPanel.add(title);
        paintPanel.add(login);
        paintPanel.add(password);
        paintPanel.add(signinButton);
        paintPanel.add(signupButton);
        return paintPanel;
    }
    public JPanel loginPanel(){
        JPanel login = new JPanel();
        return login;
    }
    public void startConnect(){

    }

    public static void main(String[] args){
        String address = "localhost";
        String port = "1234";
        new Client(address, port);
    }
}
