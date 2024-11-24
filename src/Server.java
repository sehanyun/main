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
            "root","1234"
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
    public void startServer(){
        try {
            serverSocket = new ServerSocket(12345);
            Socket cs = null;
            server_display.append("서버가 시작되었습니다\n");
            while(true){
                cs = serverSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(cs.getInputStream());
                LoginData data = (LoginData) ois.readObject();
                String loginId = data.getId();
                String loginPw = data.getPw();

                User user = new User(cs,loginId);
                users.add(user);
                server_display.append(user.loginId+"님이 입장하셧습니다.");
                //로그인 부분. 로그인할 때 진위여부 파악 기능 추가 예정.
                user.start();
            }
        }
        catch(IOException e){
            server_display.append("startServer오류발생\n");
        }
        catch(ClassNotFoundException e){

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
        private void broadCast(String msg){
            for (User u : users) {
                u.sendMessage(msg);
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

        private void chatSend(Socket cs){
            chatSender = null;

            try {
                chatSender = new BufferedWriter(new OutputStreamWriter(cs.getOutputStream()));
                testReader = new BufferedReader(new InputStreamReader(cs.getInputStream()));

                    try {
                        chatSender.write("서버 연결 성공\n");
                        chatSender.flush();
                        String msg;
                        while((msg = ((BufferedReader)testReader).readLine()) != null){
                            if(msg.equals("매칭을 요청했습니다.")){
                                if(waitingRoom.size() == 0){
                                    broadCast(this.loginId+"님이 대기방에 입장했습니다.");
                                    server_display.append(this.loginId+"님이 대기방에 입장했습니다.\n");
                                    waitingRoom.add(this);
                                }
                                else{
                                    User user = waitingRoom.remove(0);
                                    User[] userMatching = {user,this};
                                    broadCast(user.loginId+"과 "+ this.loginId+"님이 게임을 시작했습니다.");
                                    gameRoom.add(userMatching);
                                }
                            }
                            //매칭버튼을 누르는 경우 매칭방으로 이동. 만약 아무도 매칭방에 없다면 매칭방에서 다른 사람이매칭되기 까지 대기.
                            //매칭방에 두명 이상 있으면 바로 게임방에 두명을 초대해서 게임을 시작.

                            else if(msg.equals("게임종료")){
                                server_display.append(this.loginId+"님의 게임 중 탈주를 확인했습니다.\n");
                                //gameRoom에 자신이 속한 배열을 찾고 제거하는 기능.
                                synchronized (gameRoom) {
                                    // gameRoom을 안전하게 수정하기 위해 동기화
                                    for (int i = 0; i < gameRoom.size(); i++) {
                                        User[] room = gameRoom.get(i);
                                        if (room[0] == this || room[1] == this) {
                                            // 자신이 포함된 방 찾기
                                            gameRoom.remove(i);
                                            //해당 방 제거
                                            broadCast(this.loginId + "님이 게임에서 나갔습니다. 게임 방이 해체되었습니다.");
                                            break;
                                        }
                                    }
                                }
                            }
                            //한 명이 게임을 나가게 된 경우 게임방은 유지할 필요가 없어 게임방을 삭제함. 유저 둘은 매칭 전 상태로 돌아감.
                        }
                    }
                    catch(EOFException e) {
                    }
            }
            catch(IOException e){
                e.printStackTrace();
            }
            finally {
                try{
                    if (chatSender != null) ((BufferedWriter) chatSender).close();
                    if (testReader != null) ((BufferedReader) testReader).close();
                    if (cs != null && !cs.isClosed()) cs.close();
                    //완전한 종료로 서버에 다시 원할히 로그인, 연결 가능하도록 하기 위해 close 선택.
                }
                catch(IOException e){
                    System.out.println(e.getMessage());
                    System.exit(-1);
                }
            }
        }
    }

    public static void main(String[] args){
        String address = "localhost";
        String port = "1234";
        new Server(address , port);
    }
}
