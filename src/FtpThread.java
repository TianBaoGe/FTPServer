import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务器线程类
 * @author 林霭良
 */

public class FtpThread implements Runnable {

    //服务器端的数据
    private Socket socketClient; //客户端的socket
    private String dir = null; //绝对路径，对应在服务器上的具体位置
    private String pDir = "/"; //相对路径，展示给客户的文件位置
    private AtomicInteger numOfUser; //记录当前FTP服务器的用户数量
    private static DecimalFormat df = null;

    //客户端的数据
    private String username = "not logged in";//客户端用户名
    private String password = ""; //客户端的登录密码
    private boolean loginStatus = false; //客户端的登录验证状态
    private final String LOGIN_WARNING = "530 Please log in with USER and PASS first.";//这是用户没有登录的警示信息
    private String command = ""; //这是客户端发来的命令，类似于shell命令


    //构造函数
    public FtpThread(Socket socketClient, String dir, AtomicInteger numOfUser) {

        this.socketClient = socketClient;
        this.dir = dir;
        this.numOfUser = numOfUser;
    }

    /**
     * 格式化文件大小
     * @param length
     * @return 数据大小字符串
     */
    private String getFormatFileSize(long length) {
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

    @Override
    public void run(){


        //该服务器端口是为了与客户端进行文件传送的

        InputStream in = null;
        OutputStream out = null;

        try {

            in = socketClient.getInputStream();
            out = socketClient.getOutputStream();
        } catch (IOException e) {
            //日志记录，以后再补充
        }

        //br,接受客户端发来的信息
        //pw，向客户端打印信息
        BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
        PrintWriter pw = new PrintWriter(out);


        //欢迎界面
        pw.println("220-FTP Server version 1.0 written by Lin TianBao!");
        pw.flush();
        SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        pw.println(DF.format(new Date()));
        pw.flush();

        //判断并执行客户端发来的命令
        boolean statu = true;
        while (statu) {

            try {
                //获取用户输入的命令
                command = br.readLine();
                if(null == command) break;
            } catch (IOException e) {
                pw.println("331 Failed to get command");
                pw.flush();
                statu = false;
            }

            //USER登录命令,
            //命令格式：user 用户名
            if (command.toUpperCase().startsWith("USER")) {
                username = command.substring(5).trim();
                if (username.equals("")) {
                    pw.println("501 Syntax error");
                    pw.flush();
                    username = "not logged in";
                } else {
                    pw.println("331 Password required for " + username);
                    pw.flush();
                }
                loginStatus = false;
            }

            //PASS输入密码
            //命令格式：pass 密码
            else if (command.toUpperCase().startsWith("PASS")) {
                password = command.substring(5).trim();
                if (username.equals("root") && password.equals("19970719")) {
                    pw.println("230 Logged on");
                    pw.flush();
                    loginStatus = true;//修改登录状态，表示登录成功
                } else {
                    pw.println("530 Login or password incorrect!");
                    pw.flush();
                }
            }

            //PWD命令，打印出当前所在目录，为FTP服务器的相对目录
            else if (command.toUpperCase().startsWith("PWD")) {
                if (loginStatus) {
                    pw.println(pDir + "\" is current directory");
                    pw.flush();
                } else {
                    pw.println(LOGIN_WARNING);
                    pw.flush();
                }
            }

            //CD命令，类似于Linux shell的cd命令
            //命令格式：cd 目录名
            else if (command.toUpperCase().startsWith("CD")) {
                if (loginStatus) {
                    String str = command.substring(3).trim();
                    if (str.equals("")) {
                        pw.println("250 Broken client detected,missing argument to CD. +\"" + pDir + "\" is current directory.");
                        pw.flush();
                    }
                    else if(str.equals("..")){

                        if (!dir.equals("/home/tianbao/FTPServer")) {

                            String tempPath = dir.substring(0, dir.lastIndexOf("/"));
                            dir = tempPath;
                            if (pDir.lastIndexOf("/") == 0) pDir = "/";
                            else pDir = pDir.substring(0, pDir.lastIndexOf("/"));
                        }
                        pw.println("250 CD successful. \"" + pDir + "\" is current directory.");
                        pw.flush();
                    }
                    else {
                        //判断目录是否存在
                        String tmpDir = dir + "/" + str;
                        File file = new File(tmpDir);
                        if (file.exists()) {
                            //目录存在
                            if(file.isDirectory()) {
                                dir = dir + "/" + str; //很重要，更改当前的绝对路径
                                if (pDir.equals("/")) {
                                    pDir = pDir + str;
                                } else {
                                    pDir = pDir + "/" + str; //这里也很重要，更改当前的相对路径
                                }
                                pw.println("250 CD successful. \"" + pDir + "\" is current directory.");
                                pw.flush();
                            }
                            else{
                                pw.println(str + " is not a directory");
                                pw.flush();
                            }
                        } else {
                            //目录不存在
                            pw.println("550 CD failed: No such file or directory");
                            pw.flush();
                        }
                    }
                } else {
                    pw.println(LOGIN_WARNING);
                    pw.flush();
                }
            }


            //DOWNLOAD命令
            //上传文件到客户端
            else if (command.toUpperCase().startsWith("DOWNLOAD")) {
                if (loginStatus) {
                    String str = command.substring(9).trim();
                    if (str.equals("")) {
                        pw.println("501 Syntax error");
                        pw.flush();
                    } else {
                        try (ServerSocket fileServer = new ServerSocket(6622);){
                            File file = new File(dir + "/" + str);
                            if (file.exists()) {
                                try (Socket socketFile = fileServer.accept();
                                     FileInputStream fis = new FileInputStream(file);
                                     DataOutputStream dos = new DataOutputStream(socketFile.getOutputStream());) {

                                    //文件名和长度
                                    dos.writeUTF(file.getName());
                                    dos.flush();
                                    dos.writeLong(file.length());
                                    dos.flush();

                                    // 开始传输文件
                                    System.out.println("======== 开始传输文件 ========");
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
                                    System.out.println("======== 文件传输成功 ========");
                                }
                                //pw = new PrintWriter(socketClient.getOutputStream());
                                pw.println("226 Transfer OK!");
                                pw.flush();
                            } else {
                                pw.println(str + " File Not Exists！");
                                pw.flush();
                            }
                        } catch (Exception e) {
                            pw.println("503 Bad sequence of commands");
                            pw.flush();
                            //写入日志
                        }
                    }
                } else {
                    pw.println(LOGIN_WARNING);
                    pw.flush();
                    //写入日志
                }
            }

            //SENT命令,即客户端上传文件到FTP服务器
            //send和load都有问题，没关闭！！！！！
            else if (command.toUpperCase().startsWith("SENT")) {
                if (loginStatus) {

                    try {
                        pw.println("150 Opening data channel for file transfer.");
                        pw.flush();

                        try (ServerSocket fileServer = new ServerSocket(6622);
                             Socket socketFile = fileServer.accept();
                             DataInputStream dis = new DataInputStream(socketFile.getInputStream());) {
                            // 文件名和长度
                            String fileName = dis.readUTF();
                            long fileLength = dis.readLong();
                            File directory = new File(dir);
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
                        }
                        pw.println("226 Transfer OK");
                        pw.flush();

                    } catch (Exception e) {
                        pw.println("503 Bad sequence of commands.");
                        pw.flush();
                    }
                } else {
                    pw.println(LOGIN_WARNING);
                    pw.flush();
                    //写入日志
                }
            }

            //LS命令
            else if (command.toUpperCase().startsWith("LS")) {
                if (loginStatus) {
                    try {
                        pw.println("150 Opening data channel for directory list.");
                        pw.flush();
                        File file = new File(dir);
                        String[] dirStructure = file.list();
                        for (String str : dirStructure) {
                            pw.println(str);
                            pw.flush();
                        }
                        pw.println("ojbk");
                        pw.flush();

                    } catch (Exception e) {
                        pw.println("503 Bad sequence of commands.");
                        pw.flush();
                    }
                } else {
                    pw.println(LOGIN_WARNING);
                    pw.flush();
                    //记录日志
                }
            }

            //LIST命令
            else if (command.toUpperCase().startsWith("LIST")) {
                if (loginStatus) {
                    try {
                        pw.println("150 Opening data channel for directory list.");
                        pw.flush();
                        FtpUtil.getDetailList(pw, dir);
                        pw.println("ojbk");
                        pw.flush();
                    } catch (Exception e) {
                        pw.println("503 Bad sequence of commands");
                        pw.flush();
                    }
                } else {
                    pw.println(LOGIN_WARNING);
                    pw.flush();
                    //写入日志
                }
            }

            //num命令，显示当前客户端数量
            else if (command.toUpperCase().startsWith("NUM")) {

                pw.println(numOfUser);
                pw.flush();
            }

            //quit命令，关闭所有资源，关闭线程让出控制权
            else if (command.toUpperCase().startsWith("QUIT")) {

                try {
                    pw.println("221 GoodBye!");
                    pw.flush();
                    numOfUser.getAndDecrement();//用户端数量减一
                    in.close();
                    out.close();
                    Thread.currentThread();
                    Thread.sleep(1000);//这里日后要修改
                    statu = false;
                } catch (InterruptedException e) {
                } catch (IOException e) {
                }
            }

            //非法命令
            else {
                pw.println("500 Syntax error,command unrecognized!");
                pw.flush();
                //写入日志
            }
        }


        try {
            br.close();
            socketClient.close();
            pw.close();
            //if(s != null) tempSocket.close();
        } catch (IOException e) {
            //写入日志
        }

    }
}
