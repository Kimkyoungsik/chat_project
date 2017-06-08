import java.net.*;
import java.net.*;
import java.io.*;
import java.util.*;
 
// chat_sever class
// 10001번 포트를 통해 클라이언트와 연결하며 hash map을 통해 클라이언트간의 outputstream을 공유함
public class chat_sever {
       public static void main(String[] args){
             try{
                    ServerSocket server = new ServerSocket(10001);
                    System.out.println("-------------Sever init-------------.");
                    System.out.println("Sever IP : " + InetAddress.getLocalHost().getHostAddress());
                    // member의 id를 key로, outputstream을 value로 가지는 hash map
                    HashMap member = new HashMap();
                    // 각 채팅방의 id를 key로, 속한 채팅방 id을 value로 가지고 있는 hash map.
                    // 다중채팅방을 구현하기 위해 사용.
                    HashMap chat_room = new HashMap();
                    // client로 연결을 수용하는것을 영원히 반복
                    while(true){
                           Socket sock = server.accept();
                           ChatThread client = new ChatThread(sock,member, chat_room);
                           client.start();
                    }
             }catch(Exception e){
                    System.out.println(e);
             }
       }
}
 
class ChatThread extends Thread{
       private Socket sock;   // ChatThread의 주인인 Client의 소켓
       private String client_ID;     // 이 Client의 ID
       private BufferedReader client_BR;  // 이 client의 bufferedreader
       private PrintWriter client_PW;     // 이 client의 printwriter
       private HashMap hm;    // 현재 접속중인 멤버의 Hash map, key : ID, value : outputstream
       private HashMap chat_room;   // key : client_ID, value : chat_room_ID
       private boolean initFlag = false;  // client에서 연결을 종료했는지 확인하기 위한 변수
       private InetAddress client_IP;   // Client의 inetaddress 구조체

       // 생성자
       public ChatThread(Socket sock, HashMap hm, HashMap chat_room){

             this.sock = sock;
             this.hm = hm;
             this.chat_room = chat_room;
             this.client_IP = sock.getInetAddress();

             try{
                    // socket으로부터 OutputStream을 구한 뒤 OutputStreamWriter과 PrintWriter으로 변환시켜 준다.
                    // OutputStreamWriter : charater을 byte형태 정보로 변환
                    // PrintWriter : outputstream에 print, println등의 함수를 사용하게 해줌
                    client_PW = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
                    // socket으로부터 정보를 입력받을 버퍼리더를 구해준다.
                    client_BR = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    // client로부터 ID를 전송받음
                    client_ID = client_BR.readLine();
                   
                    // 다른 member들에게 현재 client가 접속했다고 공지
                    broadcast(client_ID + "님이 접속했습니다.",client_ID);
                    // 서버의 콘솔창에 현재 client의 ID와 접속한 ip주소 표시
                    System.out.println(client_ID+" 회원이 접속했습니다.");
                    System.out.println("IP : " + client_IP.getHostAddress());
                    client_PW.println("------------Sever connected------------");
                    client_PW.flush();
                    help_menu();
                   
                    synchronized(chat_room) {
                          chat_room.put(this.client_ID, "Open_room");
                    }
                    synchronized(hm){
                           hm.put(this.client_ID, client_PW);
                    }
                    initFlag = true;
             }catch(Exception ex){
             System.out.println(ex);
             }
       }
 
       public void run(){
             try{
                    String line = null;
                   
                    // client의 bufferedreader에서 한줄씩 받아옴
                    while((line = client_BR.readLine())!= null){
                           // 명령어를 입력했는지 체크
                           if(line.equals("/quit"))
                                 break;
                           else if(line.indexOf("/to") == 0){
                                 sendmsg(line);
                           } else if (line.indexOf("/mem list") == 0) {
                                 mem_list();
                           } else if (line.indexOf("/help") == 0) {
                                 help_menu();
                           } else if (line.indexOf("/chat list") == 0) {
                                 chat_list();
                           } else {
                                 broadcast(client_ID + " : " + line,client_ID);
                           }
                    }
             }catch(Exception e){
                    System.out.println(e);
             }finally{
                    // client가 접속을 종료하면 hashmap에서 삭제
                    synchronized(hm){
                           hm.remove(client_ID);
                    }
                    broadcast(client_ID+" 님이 접속 종료했습니다.",null);
                    System.out.println(client_ID + "회원이 접속을 종료했습니다.");
                    try{
                           if(sock != null)
                           // 소켓 종료
                                 sock.close();
                    }catch(Exception ex){}
             }
       }
      
       public void sendmsg(String msg){
             int start = msg.indexOf(" ") +1;
             int end = msg.indexOf(" ", start);
            
             if(end != -1){
                    String to = msg.substring(start, end);
                    String msg2 = msg.substring(end+1);
                    Object obj = hm.get(to);
                    if(obj != null){
                           PrintWriter pw = (PrintWriter)obj;
                           pw.println(client_ID + "님이 다음의 귓속말을 보내셨습니다. :" +msg2);
                           pw.flush();
                    }
             }
       }


       // 현재 id에 출력을 원하지 않을 경우를 제외하기 위해 except_id 추가
       public void broadcast(String msg, String except_id){
                    synchronized(hm){
                           Collection collection = hm.values();
                           
                           Iterator iter = collection.iterator();
                           while(iter.hasNext()){
                                 PrintWriter pw = (PrintWriter)iter.next();
                                 if(except_id != null) {
                                    if(hm.get(except_id) != pw) {
                                          pw.println(msg);
                                          pw.flush();
                                    }
                                 }  
                           }
                    }
             }

      // 접속중인 모든 사용자의 list를 출력 ( 자기 자신 빼고 )
      public void mem_list() {
            synchronized(hm) {
                  Set id_set = hm.keySet();
                  Iterator iter = id_set.iterator();
                  Object obj = hm.get(client_ID);
                  PrintWriter pw = (PrintWriter)obj;
                  pw.println("------------online member-----------");
                  while(iter.hasNext()) {
                        String mem_id = (String)iter.next();
                        if(mem_id != client_ID) {
                              pw.println(mem_id);
                        }
                  }
                  pw.flush();
            }
      }

      public void chat_list() {

                  synchronized(chat_room) {
                        Collection chat_room_collection = chat_room.values();
                        Iterator iter = chat_room_collection.iterator();
                        Set unique_room = new HashSet();
                        while(iter.hasNext()) {
                              unique_room.add(iter.next());
                        }
                        iter = unique_room.iterator();

                        PrintWriter pw = (PrintWriter)obj;
                        client_PW.println("------------Chat Room List-----------");
                        while(iter.hasNext()) {
                              String room_id = (String)iter.next();
                              client_PW.println(room_id);
                        }
                        client_PW.flush();
                  }

      }
      public void file_trans() {

      }

      public void help_menu() {
            client_PW.println("-------------command-------------");
            client_PW.println("/mem list");
            client_PW.println("/chat list");
            client_PW.println("/to <mem_id> <message>");
            client_PW.println("/file");
            client_PW.println("/quit");
            client_PW.flush();
      }
      
}
