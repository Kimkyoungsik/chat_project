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
                    System.out.println("------------------Sever init----------------.");
                    System.out.println("Sever Host : " + InetAddress.getLocalHost().getHostName());
                    System.out.println("Sever IP : " + InetAddress.getLocalHost().getHostAddress());
                    System.out.println("--------------------------------------------.");

                    // 각 채팅방의 id를 key로, 속한 채팅방 hashmap을 value로 가지고 있는 hash map.
                    // 다중채팅방을 구현하기 위해 사용.
                    HashMap chat_room = new HashMap();
                    // member의 id를 key로, outputstream을 value로 가지는 hash map
                    HashMap open_chat = new HashMap();
                    chat_room.put("open_chat",open_chat);
                    // member의 id를 key로, password를 value로 가지는 hash map
                    HashMap mem_id = new HashMap();
                    // Sever에서 명령어를 처리하기 위한 Thread 생성
                    SeverThread sever = new SeverThread(chat_room);
                    sever.start();
                    // client로 연결을 수용하는것을 영원히 반복
                    while(true){
                           Socket sock = server.accept();
                           ClientThread client = new ClientThread(sock, chat_room, open_chat, mem_id);
                           client.start();
                    }
             }catch(Exception e){
                    System.out.println(e);
             }
       }
}

class SeverThread extends Thread{
      private HashMap chat_room;
      private Scanner sc = new Scanner(System.in);

      public SeverThread(HashMap chat_room) {
            this.chat_room = chat_room;
      }

      public void run() {
            try{
                  String line;      // line buffer
                  help_menu();      // print sever command menu

                  while(true) {
                        line = sc.next(); // line input
                        if(line.indexOf("/list") == 0) {

                        } else if (line.indexOf("/to") == 0) {

                        } else if (line.indexOf("/quit") == 0) {

                        } else if (line.indexOf("/help") == 0) {
                              clearScreen();
                              help_menu();
                        } else {
                              System.out.println("Invalid command");
                        }
                  }

            } catch (Exception e) {
                  System.out.println(e);
            }
      }

      public void sendmsg() {

      }

      public void help_menu() {
            System.out.println("------------------Commands------------------");
            System.out.println("/help");
            System.out.println("/list <mem/chat>");
            System.out.println("/to <mem_id> <message>");
            System.out.println("/quit");
      }

      public void clearScreen() {
            System.out.print("\033[H\033[2J");
      }
}
 
class ClientThread extends Thread{
       private Socket sock;   // ChatThread의 주인인 Client의 소켓
       private String client_ID;     // 이 Client의 ID
       private BufferedReader client_BR;  // 이 client의 bufferedreader
       private PrintWriter client_PW;     // 이 client의 printwriter
       private HashMap cur_chat_room;    // 현재 접속중인 멤버의 Hash map, key : ID, value : outputstream
       private HashMap chat_room;   // key : client_ID, value : chat_room_ID
       private HashMap mem_id;      // key : client_ID, value : password
       private boolean initFlag = false;  // client에서 연결을 종료했는지 확인하기 위한 변수
       private InetAddress client_IP;   // Client의 inetaddress 구조체

       // 생성자
       public ClientThread(Socket sock, HashMap chat_room, HashMap open_chat, HashMap mem_id){

             this.sock = sock;
             this.cur_chat_room = open_chat;
             this.chat_room = chat_room;
             this.mem_id = mem_id;
             this.client_IP = sock.getInetAddress();

             try{
                    // socket으로부터 OutputStream을 구한 뒤 OutputStreamWriter과 PrintWriter으로 변환시켜 준다.
                    // OutputStreamWriter : charater을 byte형태 정보로 변환
                    // PrintWriter : outputstream에 print, println등의 함수를 사용하게 해줌
                    client_PW = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
                    // socket으로부터 정보를 입력받을 버퍼리더를 구해준다.
                    client_BR = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    
                    login();
                    
                   
                    
                    synchronized(cur_chat_room){
                           cur_chat_room.put(this.client_ID, client_PW);
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
                        } else if (line.indexOf("/list mem") == 0) {
                              mem_list();
                        } else if (line.indexOf("/help") == 0) {
                              help_menu();
                        } else if (line.indexOf("/list chat") == 0) {
                              chat_list();
                        } else if (line.indexOf("/make room") == 0) {
                              make_chat_room();
                        } else if (line.indexOf("/enter") == 0) {
                              enter_chat_room(line);
                        } else {
                              broadcast(client_ID + " : " + line,client_ID);
                        }
                    }
             }catch(Exception e){
                    System.out.println(e);
             }finally{
                    // client가 접속을 종료하면 hashmap에서 삭제
                    synchronized(cur_chat_room){
                        cur_chat_room.remove(client_ID);
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

       public void login() {
            String line = null;
            String ID = null;
            String Password = null;
            
            try {
                  client_PW.println("--------------Welcom to chat-------------");
                  client_PW.println("Sign in or Sign up? : <i/u>");
                  client_PW.flush();

                  line = client_BR.readLine();
                  if(line.equals("i")) {
                        // sign in
                        clearScreen(client_PW);
                        client_PW.println("Enter ID : ");
                        client_PW.flush();
                        ID = client_BR.readLine();
                        client_PW.println("Enter PassWord : ");
                        client_PW.flush();
                        Password = client_BR.readLine();

                        if(!mem_id.containsKey(ID)) {
                              clearScreen(client_PW);
                              client_PW.println("Can't find such ID");
                              client_PW.flush();
                              login();
                        } else {
                              if(!mem_id.get(ID).equals(Password)) {
                                    clearScreen(client_PW);
                                    client_PW.println("Incorrect password!");
                                    client_PW.flush();
                                    login();
                              } else {
                                    this.client_ID = ID;
                                    // 다른 member들에게 현재 client가 접속했다고 공지
                                    broadcast(client_ID + "님이 접속했습니다.",client_ID);
                                    // 서버의 콘솔창에 현재 client의 ID와 접속한 ip주소 표시
                                    System.out.println(client_ID+" 회원이 접속했습니다.");
                                    System.out.println("IP : " + client_IP.getHostAddress());
                                    client_PW.flush();
                                    help_menu();
                              }
                        }

                        
                  } else if(line.equals("u")) {
                        // sign up
                        clearScreen(client_PW);
                        client_PW.println("Enter ID : ");
                        client_PW.flush();
                        ID = client_BR.readLine();
                        client_PW.println("Enter PassWord : ");
                        client_PW.flush();
                        Password = client_BR.readLine();

                        mem_id.put(ID, Password);

                        this.client_ID = ID;
                                    // 다른 member들에게 현재 client가 접속했다고 공지
                                    broadcast(client_ID + "님이 접속했습니다.",client_ID);
                                    // 서버의 콘솔창에 현재 client의 ID와 접속한 ip주소 표시
                                    System.out.println(client_ID+" 회원이 접속했습니다.");
                                    System.out.println("IP : " + client_IP.getHostAddress());
                                    client_PW.flush();
                                    help_menu();
                                    

                  } else {
                        clearScreen(client_PW);
                        client_PW.println("invalid command");
                        login();
                  }
            } catch (Exception e) {
                  System.out.print(e);
            }
            


       }
       
       // 채팅방에 상관없이 보낼 수 있게 수정완료
       public void sendmsg(String msg){
            int start = msg.indexOf(" ") +1;
            int end = msg.indexOf(" ", start);
            
            String to = msg.substring(start, end);
            String msg2 = msg.substring(end+1);

            HashMap cur_hash = null;
            PrintWriter to_PW = null;

            try{
                  synchronized ( chat_room) {
                        Iterator chat_hash = chat_room.values().iterator();
                        while(chat_hash.hasNext()) {
                              cur_hash = (HashMap)chat_hash.next();
                              if(cur_hash.containsKey(to)) {
                                    to_PW = (PrintWriter)cur_hash.get(to);
                                    break;
                              }
                        }
                  }
                  
                  if(to_PW != null){
                        to_PW.println(client_ID + "님이 다음의 귓속말을 보내셨습니다. :" +msg2);
                        to_PW.flush();
                  }
            } catch (Exception e) {
                  System.out.println(e);
            }
            
            
       }


       // 현재 id에 출력을 원하지 않을 경우를 제외하기 위해 except_id 추가
      public void broadcast(String msg, String except_id){
            synchronized(cur_chat_room){
                  Collection collection = cur_chat_room.values();
                           
                  Iterator iter = collection.iterator();
                  while(iter.hasNext()){
                        PrintWriter cur_PrintWriter = (PrintWriter)iter.next();
                        if(except_id != null) {
                              if(cur_chat_room.get(except_id) != cur_PrintWriter) {
                                    cur_PrintWriter.println(msg);
                                    cur_PrintWriter.flush();
                              }
                        }  
                  }
            }
      }

      // 접속중인 모든 사용자의 list를 출력 ( 자기 자신 빼고 )
      // 현재 채팅방에서만
      public void mem_list() {
            synchronized(chat_room) {
                  Set id_set = cur_chat_room.keySet();
                  Iterator iter = id_set.iterator();
                  clearScreen(client_PW);
                  client_PW.println("------------online member-----------");
                  while(iter.hasNext()) {
                        String mem_id = (String)iter.next();
                        if(mem_id != client_ID) {
                              client_PW.println(mem_id);
                        }
                  }
                  client_PW.flush();
            }
      }

      public void chat_list() {

            synchronized(chat_room) {
                  Set chat_room_set = chat_room.keySet();
                  Iterator iter = chat_room_set.iterator();

                  clearScreen(client_PW);
                  client_PW.println("------------Chat Room List-----------");
                  while(iter.hasNext()) {
                        client_PW.println(iter.next());
                  }
                  client_PW.flush();
            }
      }
      
      public void make_chat_room() {
            clearScreen(client_PW);
            client_PW.println("------------make new chat-------------");
            client_PW.println("Enter chat name : ");
            client_PW.flush();
            String line = null;
            String ad=null;
            HashMap new_chat = new HashMap();
            try {
                  line = client_BR.readLine();
                  ad=setpath(line);
                   try{
                	  File file = new File(ad);
                	  fw=new FileWriter(file,true);
                	  fw.close();
                  }catch(Exception e){System.out.println(e);}
                  synchronized(chat_room) {
                        chat_room.put(line, new_chat);
                  }
                  synchronized(cur_chat_room) {
                        cur_chat_room.remove(client_ID);
                  }
                  String msg = client_ID + "님이 퇴장하셨습니다";
                  broadcast(msg, null);
                  synchronized(new_chat) {
                        new_chat.put(client_ID,client_PW);
                  }
                  cur_chat_room = new_chat;

                  clearScreen(client_PW);
                  client_PW.println("Entered to " + line);
                  client_PW.flush();
            } catch (Exception e) { System.out.println(e); }
            

      }

      public void enter_chat_room(String line) {
            int start = line.indexOf(" ") +1;
            String chat_name = line.substring(start);
            HashMap new_chat_room = null;

            try{
                  if(chat_room.containsKey(chat_name)) {
                        
                        synchronized(chat_room) {
                              cur_chat_room.remove(client_ID);
                              new_chat_room = (HashMap)chat_room.get(chat_name);
                              cur_chat_room = new_chat_room;
                              new_chat_room.put(client_ID,client_PW);
                        }
                        
                        clearScreen(client_PW);
                        client_PW.println("Entered to " + chat_name);
                        client_PW.flush();
                  } else {
                        client_PW.println("Invalid chat room");
                        client_PW.flush();
                  }
            } catch (Exception e) {
                  client_PW.println(e);
            }
      }
      public void file_trans() {

      }

      public void help_menu() {
            
            clearScreen(client_PW);
            client_PW.println("-------------command-------------");
            client_PW.println("/list <mem/chat>  <current/whole>");
            client_PW.println("/make room");
            client_PW.println("/to <mem_id> <message>");
            client_PW.println("/file");
            client_PW.println("/quit");
            client_PW.println();
            client_PW.flush();
      }
 
 public String setpath(String filename){
    	  this.filename=filename;
    	  filename=String.format("%s",filename);
    	  filepath=String.format("C:\\%s.txt",filename);
    	  return filepath;
      }
      
 public void savechat(String filename,String msg){
    	  this.filename=filename;
    	  String line=msg;
    	  msg.replaceAll("\n", "\r\n");
    	  filepath=String.format("C:\\%s.txt",filename);
    	  try{
    		  fw=new FileWriter(filepath);
    		  fw.write(line);
    		  fw.close();
    	  }catch(Exception e){}
      }
      
 public void readchat(String filename){
    	  this.filename=filename;
    	  filepath=String.format("C:\\%s.txt",filename);
    	  
    	  try{
    		  fr=new FileReader(filepath);
        	  BufferedReader b=new BufferedReader(fr);
        	  String line;
        	  while((line=b.readLine())!=null){
        		  client_PW.println(line);
        	  }
        	  b.close();
    	  }catch(Exception e){}
    	  
    	  
      }

      public static void clearScreen(PrintWriter pw) {  
            pw.print("\033[H\033[2J");  
            pw.flush();  
      } 
      
}
