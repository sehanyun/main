import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Random;


public class Server extends JFrame{

    private JTextArea server_display;
    private String address;
    private String port;
    private JButton exit, disconnect, connect;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private Vector<User> users = new Vector<>();
    private Vector<User> waitingRoom = new Vector<>();
    //유저가 매칭버튼을 누르면 매칭방에서 대기하도록 함.
    private Vector<User[]> gameRoom = new Vector<>();
    //매칭방에 두명 이상 존재하면 두명 씩 게임방으로 초대. User[] 한 게임방에 두명씩 정보를 추가해야 되기 때문.

    private HashMap<String,String> userLoginData = new HashMap<>(Map.of(
            "root","1234",
            "root1","4321"
    ));
    //회원관리를 위한 해시맵 컬렉션: 아이디와 비밀번호를 key, value 값으로 설정하여 로그인 확인 시 빠르게 찾을 수 있어 적절하다고 판단.

    public Server(String address, String port){
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setBounds(800, 300, 500, 500);
        this.setTitle("ObjectInputServer GUI");
        this.port = port;
        ServerGUI();
        this.setVisible(true);

    }
    public void ServerGUI(){
        this.getContentPane().add(createDisplayPanel(), BorderLayout.CENTER);
        this.getContentPane().add(createControlPane(), BorderLayout.SOUTH);
    }
    public JPanel createDisplayPanel(){

        JPanel DisplayPanel = new JPanel(new BorderLayout());

        server_display = new JTextArea();
        server_display.setEditable(false);

        DisplayPanel.add(server_display, BorderLayout.CENTER);
        DisplayPanel.add(new JScrollPane(server_display));

        return DisplayPanel;

    }
    public JPanel createControlPane(){
        JPanel controlPanel = new JPanel(new GridLayout(0, 1));
        connect = new JButton("서버시작");
        disconnect = new JButton("서버중단");

        controlPanel.add(connect);
        controlPanel.add(disconnect);
        connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startServer();
                    }
                });
                acceptThread.start();
            }
        });
        disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        return controlPanel;
    }
    public void startServer() {
        ObjectInputStream ois = null;
        BufferedWriter br = null;
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            server_display.append("서버가 시작되었습니다\n");

            while (true) {
                Socket cs = serverSocket.accept();
                 ois = new ObjectInputStream(cs.getInputStream());
                 br = new BufferedWriter(new OutputStreamWriter(cs.getOutputStream()));

                    LoginData data = (LoginData) ois.readObject();
                    String loginId = data.getId();
                    String loginPw = data.getPw();
                    String mode = data.getMode();

                    server_display.append("로그인 전.\n");

                    if ("로그인".equals(mode)) {
                        if (userLoginData.containsKey(loginId) && userLoginData.get(loginId).equals(loginPw)) {
                            br.write("로그인수락\n");
                            br.flush();
                            server_display.append("로그인수락.\n");
                            User user = new User(cs, loginId);
                            users.add(user);
                            user.start();
                        } else {
                            server_display.append("로그인 실패.\n");
                            br.write("로그인실패\n");
                            br.flush();
                        }
                    }
                    else if("회원가입".equals(mode)){
                        if(!userLoginData.containsKey(loginId)){
                            server_display.append("회원가입성공\n");
                            userLoginData.put(loginId, loginPw);
                            br.write("회원가입성공\n");
                            br.flush();
                        }
                        else{
                            br.write("회원가입실패\n");
                            br.flush();
                        }
                    }
            }
        } catch (IOException e) {
            server_display.append("startServer 오류 발생\n");
        }
        catch (ClassNotFoundException e) {
            server_display.append("데이터 읽기 오류\n");
        }
        finally{
            try{
                ois.close();
                br.close();
            }catch(IOException e){
                System.out.println(e.getMessage());
            }
        }
    }
    public void disconnect() {
        try {
            serverSocket.close();
        }
        catch(IOException e){
            server_display.append("서버 종료\n");
        }
    }

    class User extends Thread{
        private Socket socket;
        private Writer chatSender;
        private Reader testReader;
        private String loginId;

        public User(Socket socket, String loginId){
            this.socket =socket;
            this.loginId = loginId;
        }
        @Override
        public void run(){
            chatSend(socket);
        }

        //클라이언트 상태 전달 및 게임 진행 중계.
        private void broadCast(String msg){
            for (User u : users) {
                u.sendMessage(msg);
            }
        }

        private void chatSend(Socket cs){
            //chatSender = null;

            try {
                chatSender = new BufferedWriter(new OutputStreamWriter(cs.getOutputStream()));
                testReader = new BufferedReader(new InputStreamReader(cs.getInputStream()));
                chatSender.write("서버 연결 성공\n");
                chatSender.flush();

                String msg;
                while((msg = ((BufferedReader)testReader).readLine()) != null){
                    processMessage(msg);
                }

            }
            catch(IOException e){
                e.printStackTrace();
            }
            finally {
                cleanupResources(cs);
            }
        }

        private void processMessage(String msg) {
            switch (msg) {
                case "매칭":
                    handleMatching();
                    break;
                case "매칭취소":
                    handleMatchingCancel();
                    break;
                case "게임종료":
                    handleGameEnd();
                    break;
                case "선 확정":
                    handleTurnEnd(msg);  // 선 확정 메시지 처리
                    break;
                default:
                    System.out.println("알 수 없는 메시지: " + msg);
            }
        }
        
        private void updateGameBoard(String lineInfo) {
            // 서버에서 게임판을 업데이트하는 로직 (선 확정)
            // 예를 들어, 선을 그린 사용자와 함께 선 정보를 게임판에 반영합니다.
            server_display.append("게임판 업데이트: " + lineInfo + "\n");
            // 다른 클라이언트들에게 게임판을 업데이트된 내용을 전송
            broadCast("게임판 업데이트: " + lineInfo);
        }
        
        // 매칭 처리
        private void handleMatching() {
            if (waitingRoom.isEmpty()) {
                broadCast(this.loginId + "님이 대기방에 입장했습니다.\n");
                server_display.append(this.loginId + "님이 대기방에 입장했습니다.\n");
                waitingRoom.add(this);
            } else {
                User matchedUser = waitingRoom.remove(0);
                User[] userMatching = {matchedUser, this};
                gameRoom.add(userMatching);

                // 첫 번째 턴 주인 무작위 선택
                User firstTurnOwner = getRandomTurnOwner(userMatching);

                // 턴 주인을 알려주기
                matchedUser.sendMessage("게임시작 - 첫 번째 턴 주인: " + firstTurnOwner.loginId);
                this.sendMessage("게임시작 - 첫 번째 턴 주인: " + firstTurnOwner.loginId);
            }
        }
        // 무작위로 첫 번째 턴 주인 결정
        private User getRandomTurnOwner(User[] userMatching) {
            Random random = new Random();
            return userMatching[random.nextInt(2)]; // 두 사용자 중 무작위로 선택
        }
        
        // 매칭 취소 처리
        private void handleMatchingCancel() {
            synchronized (waitingRoom) {
                waitingRoom.removeIf(user -> user == this);
                broadCast(this.loginId + "님이 매칭을 종료했습니다.");
            }
        }
        // 게임 종료 처리
        private void handleGameEnd() {
            server_display.append(this.loginId + "님의 게임 중 탈주를 확인했습니다.\n");
            synchronized (gameRoom) {
                gameRoom.removeIf(room -> room[0] == this || room[1] == this);
                broadCast(this.loginId + "님이 게임에서 나갔습니다. 게임 방이 해체되었습니다.");
            }
        }
        
        private void handleTurnEnd(String lineInfo) {
            // 선 정보 수신 후 게임판 업데이트
            broadCast("선 확정: " + lineInfo);
            // 게임판을 업데이트하는 로직 추가
            updateGameBoard(lineInfo);
        }
        
        // 리소스 정리
        private void cleanupResources(Socket cs) {
            try {
                if (chatSender != null) chatSender.close();
                if (testReader != null) testReader.close();
                if (cs != null && !cs.isClosed()) cs.close();

                synchronized (waitingRoom) {
                    waitingRoom.removeIf(user -> user == this);
                }
                synchronized (gameRoom) {
                    gameRoom.removeIf(room -> room[0] == this || room[1] == this);
                }

                server_display.append(this.loginId + "님의 클라이언트와의 연결이 종료되었습니다.\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMessage(String msg){
            try{
                chatSender.write(msg+"\n");
                chatSender.flush();
            }
            catch(IOException e){

            }
        }
    }

    public static void main(String[] args){
        String address = "localhost";
        String port = "1234";
        new Server(address , port);
    }
}
