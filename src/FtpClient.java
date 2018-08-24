import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.DataTruncation;
import java.text.DecimalFormat;
import java.util.Scanner;

/**
 * FTP服务器的客户端
 * @author 林霭良
 */

public class FtpClient {

    private String Server_IP; //服务器IP地址，这里用作本地测试
    private int Server_Port; //服务器端口
    private Socket client;
    private boolean Loggin = false;

    private BufferedReader br;
    private PrintWriter pw;
    private Scanner in = new Scanner(System.in);
    private static DecimalFormat df = null;
    private final String LOGIN_WARNING = "530 Please log in with USER and PASS first.";


    //显示服务器发来的一句话
    public void showMessage(){
        try {
            String reply = br.readLine();
            System.out.println(reply);
        } catch (IOException e) {
        }
    }


    public FtpClient(String Server_IP, int Server_Port) throws Exception {

        this.Server_IP = Server_IP;
        this.Server_Port = Server_Port;
        this.client = new Socket(Server_IP, Server_Port);

        InputStream in = null;
        OutputStream out = null;

        try {
            in = client.getInputStream();
            out = client.getOutputStream();
        } catch (IOException e) {
            //日志记录，以后再补充
        }

        br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        pw = new PrintWriter(out);
    }

    public void user(String command) {

        pw.println(command);
        pw.flush();

        showMessage();
    }

    public void pass(String command) {

        pw.println(command);
        pw.flush();

        try {
            String reply = br.readLine();
            System.out.println(reply);
            if(reply.equals("230 Logged on")) Loggin = true;
        } catch (IOException e) {
        }
    }

    public void pwd() {

        pw.println("pwd");
        pw.flush();

        showMessage();
    }

    public void cd(String command){


        pw.println(command);
        pw.flush();

        showMessage();
    }

    public void download(String command) {

        pw.println(command);
        pw.flush();
        if(Loggin) {
            System.out.println("Please Enter The Stored Path");
            String path = in.nextLine();

            try (Socket fileSocket = new Socket(Server_IP, 6622);) {
                DataInputStream dis = new DataInputStream(fileSocket.getInputStream());

                // 文件名和长度
                String fileName = dis.readUTF();
                long fileLength = dis.readLong();
                File directory = new File(path);
                if (!directory.exists()) {
                    directory.mkdir();
                }
                File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                FileOutputStream fos = new FileOutputStream(file);

                // 开始接收文件
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                    fos.write(bytes, 0, length);
                    fos.flush();
                }
                System.out.println("======== 文件接收成功 [File Name：" + fileName + "] [Size：" + getFormatFileSize(fileLength) + "] ========");
                dis.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            showMessage();
        }
        else showMessage();
    }

    public void sent(String command) {

        pw.println(command);
        pw.flush();

        if(Loggin) {

            String path = command.substring(5).trim();

            showMessage();

            try {
                File file = new File(path);
                if (file.exists()) {
                    try (Socket fileSocket = new Socket(Server_IP, 6622);
                         FileInputStream fis = new FileInputStream(file);
                         DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());) {

                        //文件名和长度
                        dos.writeUTF(file.getName());
                        dos.flush();
                        dos.writeLong(file.length());
                        dos.flush();

                        // 开始传输文件
                        System.out.println("======== 开始上传文件 ========");
                        byte[] bytes = new byte[1024];
                        int length = 0;
                        long progress = 0;
                        while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                            dos.write(bytes, 0, length);
                            dos.flush();
                            progress += length;
                            System.out.print("| " + (100 * progress / file.length()) + "% |");
                        }
                        System.out.println();
                        System.out.println("======== 文件上传完毕 ========");
                    }
                }

                showMessage();

            } catch (Exception e) {
            }
        }
        else showMessage();
    }

    public void ls() {


        pw.println("LS");
        pw.flush();
        if (Loggin) {
            showMessage();
            String list;
            try {
                while (!((list = br.readLine()).equals("ojbk"))) {
                    System.out.println(list);
                }
            } catch (IOException e) {
            }
        } else showMessage();
    }

    public void list() {

        pw.println("list");
        pw.flush();
        if(Loggin) {
            showMessage();
            String list;
            try {
                while (!((list = br.readLine()).equals("ojbk"))) {
                    System.out.println(list);
                }
            } catch (IOException e) {
            }
        }
        else showMessage();
    }

    public void num(){

        pw.println("num");
        pw.flush();
        showMessage();
    }

    public void UnrecognizedCommand(String command){
        pw.println(command);
        pw.flush();
        showMessage();
    }

    public void quit(){

        pw.println("quit");
        pw.flush();
        showMessage();
    }

    private static String getFormatFileSize(long length) {
        double size = ((double) length) / (1 << 30);
        if (size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if (size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if (size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }


    public static void main(String[] args) {

        try {
            FtpClient test = new FtpClient("localhost", 8844);
            String command;
            Scanner In = new Scanner(System.in);
            Boolean statu = true;
            test.showMessage();
            test.showMessage();
            while(statu){
                command = In.nextLine();
                if(command.toUpperCase().startsWith("USER")) test.user(command);
                else if(command.toUpperCase().startsWith("PASS")) test.pass(command);
                else if(command.toUpperCase().startsWith("PWD")) test.pwd();
                else if(command.toUpperCase().startsWith("CD")) test.cd(command);
                else if(command.toUpperCase().startsWith("DOWNLOAD")) test.download(command);
                else if(command.toUpperCase().startsWith("SENT")) test.sent(command);
                else if(command.toUpperCase().startsWith("LIST")) test.list();
                else if(command.toUpperCase().startsWith("QUIT")) {test.quit();statu = false;}
                else if(command.toUpperCase().startsWith("NUM")) test.num();
                else if(command.toUpperCase().startsWith("LS")) test.ls();
                else test.UnrecognizedCommand(command);
            }
        } catch (Exception e) {

        }
    }
}