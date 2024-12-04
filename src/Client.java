import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    private ConnectState connectState = new NotConnected();
    //초기 상태는 매칭전 상태.
    private boolean isGameDisplayed = false; // 게임 화면 표시 여부
    private boolean turnInProgress = false; // 현재 턴이 진행 중인지 여부

    public void setConnectState(ConnectState connectState){
        this.connectState = connectState;
    }
    public ConnectState getConnectState(){
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
        paintPanel.add(title);
        paintPanel.add(login);
        paintPanel.add(password);
        paintPanel.add(signinButton);
        paintPanel.add(signupButton);

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
                LoginData loginData = makeloginData("로그인");
                try{
                    sendLoginData(loginData);
                    String msg = br.readLine();
                    serverChat.append(msg + "\n");
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    if ("로그인수락".equals(msg)) {
                        login();
                    } else {
                        serverChat.append("로그인 실패: " + msg + "\n");
                    }
                }
                catch(IOException e1){
                    System.out.println(e1.getMessage());
                }
            }
        });
        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginData loginData = makeloginData("회원가입");
                try{
                    sendLoginData(loginData);
                    String msg = br.readLine();
                    serverChat.append(msg + "\n");
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                    if ("회원가입성공".equals(msg)) {
                        JOptionPane.showMessageDialog(paintPanel, "회원가입에 성공했습니다.", "성공!", JOptionPane.PLAIN_MESSAGE);
                        serverChat.append("회원가입 성공");
                    } else {
                        serverChat.append("회원가입 실패: " + msg + "\n");
                        JOptionPane.showMessageDialog(paintPanel, "회원가입에 실패했습니다.", "실패!", JOptionPane.WARNING_MESSAGE);
                    }
                }
                catch(IOException e1){
                    System.out.println(e1.getMessage());
                }
            }
        });

        return paintPanel;
    }
    public void sendLoginData(LoginData loginData) throws IOException{
        socket = new Socket(address, Integer.parseInt(port));
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        oos.writeObject(loginData);
    }
    public LoginData makeloginData(String str){
        String idData = login.getText();
        String pwData = password.getText();
        LoginData loginData = new LoginData(idData, pwData, str);
        return loginData;
    }
    public void CheckLoginSuccess(){

    }
    public void CheckSignUpSuccess(){

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
    public void startConnect() throws IOException {
        String msg;
        while ((msg = br.readLine()) != null) {
            if(msg.equals("게임시작")){
                startGame();
            }
            //매칭 방에 사람이 두명 이상 존재하면 바로 팀 매칭이 됨.
            serverChat.append(msg+"\n");
            displayDotAndBoxGame();
        }
    }
    private void displayDotAndBoxGame() {
        JPanel gamePanel = new DotAndBoxGamePanel(5);

        // 기존 중앙 패널을 모두 제거하고, 새로운 게임 패널 추가
        this.getContentPane().removeAll();  // 모든 기존 컴포넌트 제거
        this.getContentPane().add(gamePanel, BorderLayout.CENTER);  // 게임판을 중앙에 추가

        // 채팅 패널을 오른쪽에 추가
        this.getContentPane().add(serverChat, BorderLayout.EAST);

        // 컨트롤 패널을 하단에 추가
        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);

        // 레이아웃 갱신
        this.revalidate();
        this.repaint();

        // 메시지는 처음 한 번만 출력
        if (!isGameDisplayed) {
            serverChat.append("게임 화면이 표시되었습니다.\n");
            isGameDisplayed = true; // 플래그 업데이트
        }
    }

    // DotAndBoxGamePanel 클래스
    class DotAndBoxGamePanel extends JPanel {
        private int gridSize;
        private boolean[][] horizontalLines;
        private boolean[][] verticalLines;
        private boolean isPlayerTurn;  // 현재 플레이어 턴인지 여부

        public DotAndBoxGamePanel(int gridSize) {
            this.gridSize = gridSize;
            this.horizontalLines = new boolean[gridSize][gridSize - 1];
            this.verticalLines = new boolean[gridSize - 1][gridSize];
            setPreferredSize(new Dimension(500, 500));
            setBackground(Color.WHITE);
            isPlayerTurn = true;  // 처음에 플레이어 턴으로 설정

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isPlayerTurn) {  // 게임이 진행 중일 때만 선을 선택할 수 있음//xxxxxx
                        handleMouseClick(e.getX(), e.getY());
                    }
                }
            });
        }

        private void handleMouseClick(int x, int y) {
            // 클릭된 위치에서 가장 가까운 선을 찾아서 활성화
            int dotSpacing = 50;
            int row = (y - 50) / dotSpacing;
            int col = (x - 50) / dotSpacing;

            // 클릭된 곳에서 선이 유효한지 확인하고 그리기
            if (row >= 0 && row < gridSize - 1 && col >= 0 && col < gridSize - 1) {
                if (horizontalLines[row][col] == false) {
                    horizontalLines[row][col] = true;
                    repaint();
                }
            }

            // 현재 선을 그린 후, 턴 종료 버튼을 클릭할 수 있도록 처리
            turn.setEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int dotSpacing = 50;
            g.setColor(Color.BLACK);

            // 그리드 점 그리기
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    int x = 50 + j * dotSpacing;
                    int y = 50 + i * dotSpacing;
                    g.fillOval(x - 5, y - 5, 10, 10);  // 점 그리기
                }
            }

            // 수평선 그리기
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize - 1; j++) {
                    if (horizontalLines[i][j]) {
                        int x1 = 50 + j * dotSpacing;
                        int y1 = 50 + i * dotSpacing;
                        int x2 = 50 + (j + 1) * dotSpacing;
                        int y2 = 50 + i * dotSpacing;
                        g.drawLine(x1, y1, x2, y2);  // 수평선 그리기
                    }
                }
            }

            // 수직선 그리기
            for (int i = 0; i < gridSize - 1; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (verticalLines[i][j]) {
                        int x1 = 50 + j * dotSpacing;
                        int y1 = 50 + i * dotSpacing;
                        int x2 = 50 + j * dotSpacing;
                        int y2 = 50 + (i + 1) * dotSpacing;
                        g.drawLine(x1, y1, x2, y2);  // 수직선 그리기
                    }
                }
            }
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
    public void login() throws IOException{connectState.login(this);}
    //통신을 통한 상태변경 메소드

    interface ConnectState {
        public void endGame(Client client);
        public void requestMatching(Client client);
        public void startGame(Client client);
        public void login(Client client) throws IOException;
    }
    //연결상태에 따라 진행. 게임 진행을 어느 상태인지에 따라 관리함. -> 상태에 따라 동일한 버튼 클릭도 다른 반응이 나오기 때문.
    // ex) 매칭 전 매칭 버튼, 게임 중 매칭 버튼 클릭은 다른 반응을 보여야 함.
    class NotConnected implements ConnectState {

        @Override
        public void endGame(Client client) {

        }
        @Override
        public void requestMatching(Client client) {
            try {
                notConnectedToRequestMatching();
            } catch (IOException e) {
                serverChat.append("매칭 요청 중 오류가 발생했습니다.\n");
            }
        }
        @Override
        public void startGame(Client client) {
            serverChat.append("매칭을 먼저 해야합니다.\n");
        }
        @Override
        public void login(Client client){
            loginSucessConnnect();
            startAcceptThread();
        }
    }
    //매칭 전 상태
    class Matching implements ConnectState {

        @Override
        public void endGame(Client client) {

        }

        @Override
        public void requestMatching(Client client) {
            try{
                requestMatchingToNotConnected();
            }
            catch(IOException e){System.out.println(e.getMessage());}
        }

        @Override
        public void startGame(Client client) {requestMatchingToInGame();}

        @Override
        public void login(Client client) {

        }

    }
    //매칭 중인 상태, 게임 진행 전 단계.
    class InGame implements ConnectState {

        @Override
        public void endGame(Client client) {
            try{
                inGameToNotConnected();
            }
            catch(IOException e){
                System.out.println(e.getMessage());
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

        @Override
        public void login(Client client) {

        }


    }
    //게임 중인 상태.

    public static void main(String[] args){
        String address = "localhost";
        String port = "12345";
        new Client(address, port);
//        new Client(address, port);
    }

    public void loginSucessConnnect(){
        title.setVisible(false);
        login.setVisible(false);
        password.setVisible(false);
        signinButton.setVisible(false);
        signupButton.setVisible(false);
        controlPanel.setVisible(true);
    }
    public void startAcceptThread(){
        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(acceptThread == Thread.currentThread()){
                    try{
                        startConnect();
                    }
                    catch(IOException e){

                    }
                }
            }
        });
        acceptThread.start();
    }
    public void notConnectedToRequestMatching() throws IOException{
        bw.write("매칭\n");
        bw.flush();
        serverChat.append("클라: 매칭 중...\n");
        setConnectState(new Matching());
    }
    public void requestMatchingToNotConnected() throws IOException{
        if (connectState.getClass() == new Matching().getClass()) {
            bw.write("매칭취소\n");
            bw.flush();
            serverChat.append("매칭을 취소합니다.\n");
            setConnectState(new NotConnected());
        }
    }
    public void requestMatchingToInGame(){
        serverChat.append("게임을 시작합니다. 곧 색 선택창이 활성화됩니다.\n");
        matching.setEnabled(false);
        backTurn.setEnabled(true);
        turn.setEnabled(true);
        // 상태 전환
        setConnectState(new InGame());
    }
    public void inGameToNotConnected() throws IOException{
        serverChat.append("게임이 종료되었습니다.");
        setConnectState(new NotConnected());
        matching.setEnabled(true);
        backTurn.setEnabled(false);
        turn.setEnabled(false);
        bw.write("게임종료\n");
        bw.flush();
    }
    //상태에 따른 로직 메소드
}