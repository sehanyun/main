import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {

    private String address;
    private String port;
    private JLabel title= new JLabel("제목");
    private JTextField login= new JTextField();
    private JTextField password= new JTextField();
    private JButton signinButton = new JButton("로그인");
    private JButton signupButton = new JButton("생성");
    private JTextArea serverChat;
    private Socket socket;
    private BufferedWriter bw;
    private BufferedReader br;
    private Thread acceptThread;
    //계속 서버에서의 메시지를 받고 연결을 유지하기 위해 멀티 스레드 결정.

    private JPanel controlPanel;
    private JButton matching = new JButton("매칭");
    private JButton backTurn = new JButton("무르기");
    private JButton turn = new JButton("턴종료");
    private JButton exit = new JButton("나가기");

    private connectState connectState = new NotConnected();
    //초기 상태는 매칭전 상태.

    public void setConnectState(Client.connectState connectState){
        this.connectState = connectState;
    }
    public Client.connectState getConnectState(){
        return this.connectState;
    }

    public Client(String address, String port){
        this.address = address;
        this.port = port;
        this.setBounds(0, 0, 1000, 550);
        this.setTitle("Client GUI");
        startGUI();

        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }
    public void startGUI(){
        this.add(new JScrollPane(serverChat));
        this.getContentPane().add(paintPanel(), BorderLayout.CENTER);

        serverChat = new JTextArea(20,10);
        this.getContentPane().add(serverChat, BorderLayout.EAST);

        controlPanel = controlPanel();
        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
        controlPanel.setVisible(false);
    }
    public JPanel paintPanel(){
        JPanel paintPanel = new JPanel();
        paintPanel.setLayout(null);
        title.setBounds(475, 100, 50, 50);
        login.setBounds(400, 300, 140, 25);
        password.setBounds(400, 325, 140, 25);
        signinButton.setBounds(540, 300, 70, 25);
        signupButton.setBounds(540, 325, 70, 25);

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
                String idData = login.getText();
                String pwData = password.getText();
                LoginData loginData = new LoginData(idData, pwData);

                title.setVisible(false);
                login.setVisible(false);
                password.setVisible(false);
                signinButton.setVisible(false);
                signupButton.setVisible(false);
                controlPanel.setVisible(true);

                acceptThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        while(acceptThread == Thread.currentThread()){
                            try{
                                startConnect(loginData);
                                title.setVisible(true);
                                login.setVisible(true);
                                password.setVisible(true);
                                signinButton.setVisible(true);
                                signupButton.setVisible(true);
                                controlPanel.setVisible(false);
                            }
                            catch(IOException e){
                            }
                            finally {
                                try{
                                    acceptThread = null;
                                    socket.close();
                                }
                                catch(IOException e){

                                }
                            }
                        }
                    }
                });
                acceptThread.start();
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
    public JPanel controlPanel(){
        JPanel controlPanel = new JPanel(new GridLayout(0,4));
        matching.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestMatching();
            }
        });
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                endGame();
            }
        });
        backTurn.setEnabled(false);
        turn.setEnabled(false);
        controlPanel.add(matching);
        controlPanel.add(backTurn);
        controlPanel.add(turn);
        controlPanel.add(exit);
        return controlPanel;
    }
    public void startConnect(LoginData loginData) throws IOException {
            socket = new Socket(address, Integer.parseInt(port));
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(loginData);
            String msg;
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            while ((msg = br.readLine()) != null) {
                if(msg.contains("님이 게임을 시작했습니다.")){
                    startGame();
                }
                //매칭 방에 사람이 두명 이상 존재하면 바로 팀 매칭이 됨.
                serverChat.append(msg+"\n");
            }
    }
    public void requestMatching(){
        connectState.requestMatching(this);
    }
    public void startGame(){
        connectState.startGame(this);
    }
    public void endGame(){
        connectState.endGame(this);
    }

    interface connectState {
        public void endGame(Client client);
        public void requestMatching(Client client);
        public void startGame(Client client);
    }
    //연결상태에 따라 진행. 게임 진행을 어느 상태인지에 따라 관리함. -> 상태에 따라 동일한 버튼 클릭도 다른 반응이 나오기 때문.
    // ex) 매칭 전 매칭 버튼, 게임 중 매칭 버튼 클릭은 다른 반응을 보여야 함.
    class NotConnected implements connectState {

        @Override
        public void endGame(Client client) {

        }

        @Override
        public void requestMatching(Client client) {
            try {
                bw.write("매칭을 요청했습니다.\n");
                bw.flush();
                serverChat.append("클라: 매칭중...\n");
                serverChat.append("매칭 상태가 되었습니다.\n");
                client.setConnectState(new Matching());
            }
            catch(IOException e){

            }
        }

        @Override
        public void startGame(Client client) {
            serverChat.append("매칭을 먼저 해야합니다.\n");
        }
    }
    //매칭 전 상태
    class Matching implements connectState {

        @Override
        public void endGame(Client client) {

        }

        @Override
        public void requestMatching(Client client) {
            serverChat.append("이미 매칭 중입니다.\n");
        }

        @Override
        public void startGame(Client client) {
            serverChat.append("게잉을 시작합니다. 곧 색을 선택창이 활성화됩니다.\n");
            matching.setEnabled(false);
            backTurn.setEnabled(true);
            turn.setEnabled(true);
            client.setConnectState(new InGame());
        }
    }
    //매칭 중인 상태, 게임 진행 전 단계.
    class InGame implements connectState {

        @Override
        public void endGame(Client client) {
            serverChat.append("게임이 종료되었습니다.");
            client.setConnectState(new NotConnected());
            try{
                matching.setEnabled(true);
                backTurn.setEnabled(false);
                turn.setEnabled(false);
                bw.write("게임종료\n");
                bw.flush();
            }
            catch(IOException e){

            }
        }

        @Override
        public void requestMatching(Client client) {
            serverChat.append("게임이 이미 진행 중입니다.\n");
        }

        @Override
        public void startGame(Client client) {
            serverChat.append("게임이 이미 진행 중입니다.\n");
        }
    }
    //게임 중인 상태.

    public static void main(String[] args){
        String address = "localhost";
        String port = "12345";
        new Client(address, port);
        new Client(address, port);
    }
}