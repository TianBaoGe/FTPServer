import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FTP服务器端
 * @author 林霭良
 */

public class FtpServer {

    private static final String F_DIR = "/home/tianbao/FTPServer"; //文件根目录
    private static String Current_Dir = F_DIR; //记录当前服务器可以使用的并且所在的目录

    /**
     * 服务器端创建文件夹
     *
     * @param command
     */
    public static void mkdir(String command) {

        String path = Current_Dir + "/" + command.substring(6);
        File file = new File(path);
        if (!file.exists()) file.mkdir();
        else {
            System.out.println("mkdir: cannot create directory '" + command.substring(6) + "': File exists");
        }

    }

    /**
     * 服务器端模拟cd命令
     *
     * @param command
     */
    public static void cd(String command) {

        String directory = command.substring(3);

        if (directory.equals("..")) {
            String tempPath = Current_Dir.substring(0, Current_Dir.lastIndexOf("/"));
            if (tempPath.startsWith(F_DIR)) Current_Dir = tempPath;
            else System.out.println("Permission Denied！");
        } else {
            String path = Current_Dir + "/" + directory;

            File file = new File(path);

            if (file.exists()) {
                if (file.isDirectory()) Current_Dir = path;
                else System.out.println(directory + " is not a directory");
            } else {
                System.out.println("cd: dad: No such file or directory");
            }
        }
    }

    /**
     * 模拟linux终端ls命令
     */
    public static void ls() {
        File file = new File(Current_Dir);
        String[] dirStructure = file.list();
        for (String str : dirStructure) {
            System.out.print(str + " ");
        }
        System.out.println();
    }


    /**
     * 模拟linux终端rm命令
     *
     * @param command
     */
    public static void rm(String command) {

        File file = new File(Current_Dir + "/" + command.substring(3));
        if (file.exists()) {
            if (delete(file)) System.out.println("Successfully Deleted");
            else System.out.println("Failed to delete");
        } else {
            System.out.println("rm: cannot remove '" + command.substring(3) + "': No such file or directory");
        }
    }

    /**
     * 配合rm函数，递归删除文件
     *
     * @param file
     * @return boolean类型
     */
    private static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isFile()) {
            return file.delete();
        } else {

            for (File f : file.listFiles()) {
                delete(f);
            }
        }
        return file.delete();
    }

    /**
     * 模拟linux终端pwd命令
     */
    public static void pwd() {
        System.out.println(Current_Dir);
    }


    public static void main(String[] args) throws Exception {

        final String F_DIR = "/home/tianbao/FTPServer"; //文件根目录
        final int Port = 8844; //监听端口号
        AtomicInteger numOfUser = new AtomicInteger(0); //用户的数量

        ServerSocket serverSocket = new ServerSocket(Port); //服务器端的Socket
        Socket client = null;


        System.out.println("220-FTP Server version 1.0 written by Lin TianBao!");
        Scanner in = new Scanner(System.in);
        String command = null;
        while (!((command = in.nextLine()).equals("quit"))) {
            if (command.startsWith("ls")) ls();
            else if (command.startsWith("rm")) rm(command);
            else if (command.startsWith("cd")) cd(command);
            else if (command.startsWith("mkdir")) mkdir(command);
            else if (command.startsWith("pwd")) pwd();
            else if (command.startsWith("run")) {
                System.out.println("FTPServer is runnig");
                while (true) {
                    client = serverSocket.accept();
                    numOfUser.getAndIncrement(); //客户端数量加一
                    Thread t = new Thread(new FtpThread(client, F_DIR, numOfUser));
                    t.start();
                }
            }
        }
    }
}