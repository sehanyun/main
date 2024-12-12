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
                try{
                    LoginData loginData = makeloginData("로그인");
                    sendLoginData(loginData);
                    String msg = sendResult();
                    checkSignIn(msg);
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                }
                catch(IOException e1){
                    System.out.println(e1.getMessage());
                }
            }
        });
        signupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    LoginData loginData = makeloginData("회원가입");
                    sendLoginData(loginData);
                    String msg = sendResult();
                    checkSignUp(paintPanel,msg);
                    bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                }
                catch(IOException e1){System.out.println(e1.getMessage());}
            }
        });

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

    public LoginData makeloginData(String str){
        String idData = login.getText();
        String pwData = password.getText();
        LoginData loginData = new LoginData(idData, pwData, str);
        return loginData;
    }
    public void sendLoginData(LoginData loginData) throws IOException{
        socket = new Socket(address, Integer.parseInt(port));
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        oos.writeObject(loginData);
    }
    public String sendResult() throws IOException{
        String msg = br.readLine();
        serverChat.append(msg + "\n");
        return msg;
    }
    public void checkSignIn(String msg) throws IOException{
        switch(msg){
            case "로그인수락":login();
                break;
            default: serverChat.append("로그인 실패: " + msg + "\n");
                break;
        }
    }
    public void checkSignUp(JPanel paintPanel, String msg){
        switch(msg){
            case "회원가입성공":
                JOptionPane.showMessageDialog(paintPanel, "회원가입에 성공했습니다.","성공!", JOptionPane.PLAIN_MESSAGE);
                break;
            case "회원가입실패":
                JOptionPane.showMessageDialog(paintPanel, "회원가입에 실패했습니다.","실패!", JOptionPane.WARNING_MESSAGE);
                break;
        }
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
        private boolean[][] confirmedHorizontalLines;
        private boolean[][] confirmedVerticalLines;
        private String[][] boxOwner;  // 박스를 완성한 사람 ID
        private String currentPlayerID; // 현재 플레이어 ID
        private int[] currentLine; // [type, row, col] - type: 0=horizontal, 1=vertical

        public DotAndBoxGamePanel(int gridSize) {
            this.gridSize = gridSize;
            this.confirmedHorizontalLines = new boolean[gridSize][gridSize - 1];
            this.confirmedVerticalLines = new boolean[gridSize - 1][gridSize];
            this.boxOwner = new String[gridSize - 1][gridSize - 1];  // 각 박스를 만든 사람의 ID 저장
            this.currentLine = new int[]{-1, -1, -1}; // No line selected initially

            setPreferredSize(new Dimension(500, 500));
            setBackground(Color.WHITE);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleMouseClick(e.getX(), e.getY());
                }
            });
        }

        private void handleMouseClick(int x, int y) {
            int dotSpacing = 50;
            int row = (y - 50) / dotSpacing;
            int col = (x - 50) / dotSpacing;

            // Check and update horizontal lines
            if (row >= 0 && row < gridSize && col >= 0 && col < gridSize - 1) {
                int xStart = 50 + col * dotSpacing;
                int yStart = 50 + row * dotSpacing;

                if (Math.abs(y - yStart) <= 10 && Math.abs(x - xStart - dotSpacing / 2) <= 10) {
                    selectLine(0, row, col);
                    return;
                }
            }

            // Check and update vertical lines
            if (row >= 0 && row < gridSize - 1 && col >= 0 && col < gridSize) {
                int xStart = 50 + col * dotSpacing;
                int yStart = 50 + row * dotSpacing;

                if (Math.abs(x - xStart) <= 10 && Math.abs(y - yStart - dotSpacing / 2) <= 10) {
                    selectLine(1, row, col);
                    return;
                }
            }
        }

        private void selectLine(int type, int row, int col) {
            // Update current line
            currentLine[0] = type;
            currentLine[1] = row;
            currentLine[2] = col;

            // Check if any box is completed
            checkForCompletedBox(row, col);

            // Repaint to reflect temporary selection and box updates
            repaint();
        }

        private void checkForCompletedBox(int row, int col) {
            // Check for horizontal box completion (top-right or bottom-left)
            if (row < gridSize - 1 && col < gridSize - 1) {
                if (confirmedHorizontalLines[row][col] && confirmedHorizontalLines[row + 1][col]
                        && confirmedVerticalLines[row][col] && confirmedVerticalLines[row][col + 1]) {
                    boxOwner[row][col] = currentPlayerID;  // Mark this box as completed by the current player
                    currentPlayerID = currentPlayerID.equals("Player1") ? "Player2" : "Player1"; // Switch player
                    return;  // Once a box is completed, stop further processing in the current turn
                }
            }
        }

        public void setCurrentPlayerID(String id) {
            this.currentPlayerID = id;
        }

        public void confirmLine() {
            // Check if there's a selected line
            if (currentLine[0] != -1) {
                if (currentLine[0] == 0) { // Horizontal line
                    int row = currentLine[1];
                    int col = currentLine[2];
                    confirmedHorizontalLines[row][col] = true; // Mark the line as confirmed
                } else if (currentLine[0] == 1) { // Vertical line
                    int row = currentLine[1];
                    int col = currentLine[2];
                    confirmedVerticalLines[row][col] = true; // Mark the line as confirmed
                }
            }

            // Reset the temporary selection
            currentLine[0] = -1;
            currentLine[1] = -1;
            currentLine[2] = -1;

            // Repaint the panel to update the visuals
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int dotSpacing = 50;
            g.setColor(Color.BLACK);

            // Draw dots
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    int x = 50 + j * dotSpacing;
                    int y = 50 + i * dotSpacing;
                    g.fillOval(x - 5, y - 5, 10, 10);
                }
            }

            // Draw confirmed horizontal lines
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize - 1; j++) {
                    if (confirmedHorizontalLines[i][j]) {
                        int x1 = 50 + j * dotSpacing;
                        int y1 = 50 + i * dotSpacing;
                        int x2 = 50 + (j + 1) * dotSpacing;
                        int y2 = 50 + i * dotSpacing;
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // Draw confirmed vertical lines
            for (int i = 0; i < gridSize - 1; i++) {
                for (int j = 0; j < gridSize; j++) {
                    if (confirmedVerticalLines[i][j]) {
                        int x1 = 50 + j * dotSpacing;
                        int y1 = 50 + i * dotSpacing;
                        int x2 = 50 + j * dotSpacing;
                        int y2 = 50 + (i + 1) * dotSpacing;
                        g.drawLine(x1, y1, x2, y2);
                    }
                }
            }

            // Draw box owners
            for (int i = 0; i < gridSize - 1; i++) {
                for (int j = 0; j < gridSize - 1; j++) {
                    if (boxOwner[i][j] != null) {
                        int x = 50 + j * dotSpacing;
                        int y = 50 + i * dotSpacing;
                        g.drawString(boxOwner[i][j], x + dotSpacing / 4, y + dotSpacing / 2);
                    }
                }
            }

            // Draw temporary selected line
            if (currentLine[0] != -1) {
                g.setColor(Color.RED); // Temporary line color
                if (currentLine[0] == 0) {
                    int row = currentLine[1];
                    int col = currentLine[2];
                    int x1 = 50 + col * dotSpacing;
                    int y1 = 50 + row * dotSpacing;
                    int x2 = 50 + (col + 1) * dotSpacing;
                    int y2 = 50 + row * dotSpacing;
                    g.drawLine(x1, y1, x2, y2);
                } else {
                    int row = currentLine[1];
                    int col = currentLine[2];
                    int x1 = 50 + col * dotSpacing;
                    int y1 = 50 + row * dotSpacing;
                    int x2 = 50 + col * dotSpacing;
                    int y2 = 50 + (row + 1) * dotSpacing;
                    g.drawLine(x1, y1, x2, y2);
                }
            }
        }
    


        public GameState getGameState() {
            GameState gameState = new GameState(gridSize);
            gameState.horizontalLines = confirmedHorizontalLines;
            gameState.verticalLines = confirmedVerticalLines;
            return gameState;
        }

        public void setGameState(GameState gameState) {
            this.confirmedHorizontalLines = gameState.horizontalLines;
            this.confirmedVerticalLines = gameState.verticalLines;
            repaint(); // 보드 상태를 다시 그리기
        }
    }
    public class GameState implements Serializable {
        public boolean[][] horizontalLines;
        public boolean[][] verticalLines;

        public GameState(int gridSize) {
            horizontalLines = new boolean[gridSize][gridSize - 1];
            verticalLines = new boolean[gridSize - 1][gridSize];
        }
    }

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
        	 // 서버에서 받은 첫 번째 턴 주인 정보 처리
            String firstTurnOwner = receiveFirstTurnOwner();

            // 첫 번째 턴 주인에게만 "턴 종료" 버튼 활성화
            if (firstTurnOwner.equals(login.getText())) {
                turn.setEnabled(true); // 첫 번째 턴 주인에게만 활성화
            } else {
                turn.setEnabled(false); // 나머지 플레이어는 비활성화
            }

            // 게임 화면 및 초기화
            displayDotAndBoxGame();
        }

        @Override
        public void login(Client client) {

        }


    }
    //게임 중인 상태.

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

    public static void main(String[] args){
        String address = "localhost";
        String port = "12345";
        new Client(address, port);
        new Client(address, port);
    }

    public void loginSucessConnnect(){
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
    
 // 서버로부터 첫 번째 턴 주인 정보를 받는 메서드
    private String receiveFirstTurnOwner() {
        try {
            // 서버에서 첫 번째 턴 주인 정보 받기
            String msg = br.readLine();
            return msg.split(":")[1].trim(); // "게임시작 - 첫 번째 턴 주인: <user>"에서 <user> 부분만 추출
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    
    private void sendLineToServer(String lineInfo) {
        try {
            bw.write(lineInfo + "\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //상태에 따른 로직 메소드
}
