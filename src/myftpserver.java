import javax.imageio.IIOException;
import java.io.*;
import java.net.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class myftpserver {

    // Vector to store active clients
    static Vector<HandleClient> ar = new Vector<>();

    //Add the files under operation over here
    public static ArrayList<String> fileList = new ArrayList<String>();

    //Variable to store all the commands to be terminated
    //check how to make this variable sychronized so that multiple thread cannot access it at the same time.
    //TODO
    public static List list = new ArrayList();
    // counter for clients
    static int i = 0;

    //server socket for get
    public static ServerSocket getserverSocket;
    //server socket for put
    public static ServerSocket putserverSocket;

    //start sockets for put and get
    public myftpserver(){
        try {
            //create a socket for get and wait for incoming get commands
            this.getserverSocket = new ServerSocket(8001);

            //create a socket for put and wait for incoming get commands
            this.putserverSocket = new ServerSocket(8000);
        } catch (IOException e){
            e.printStackTrace();
            System.out.println("IOException Occured");
            System.exit(1);
        }
    }



    public static void main(String[] args) throws IOException {

        //check if command line inputs are correct
        if (args.length != 2) {
            System.out.println("Input in following format :\n myftpserver <nport> <tport>");
            System.exit(1);
        }
        //nport
        int nport = Integer.parseInt(args[0]);
        //tport
        int tport = Integer.parseInt(args[1]);

        //create object of the class and get sockets for get and put
        myftpserver server = new myftpserver();


        //Once the myftpserver program is invoked, it should create two threads.
        //The first thread will create a socket on the first port number and wait for incoming clients.
        Thread firstT = new NormalServer(new ServerSocket(nport), server.getserverSocket, server.putserverSocket);
        firstT.start();

        //The second thread will create a socket on the second port and wait for incoming terminate commands.
        Thread secondT = new TerminateServer(new ServerSocket(tport));
        secondT.start();
    }
}

//Second thread invokes terminate process
class TerminateServer extends Thread{
    private Socket tclientSocket;
    private final ServerSocket tserverSocket;

    public TerminateServer(ServerSocket tserverSocket) throws IOException {
        this.tserverSocket = tserverSocket;
    }

    @Override
    public void run() {
        try {
            handleTerminateSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleTerminateSocket() throws IOException, InterruptedException{
        //while loop to accept multiple clients
        while (true) {
            //Client discovered on tport. Accept connection
            this.tclientSocket = this.tserverSocket.accept();
            HandleTerminateClient handleTerminateClient = new HandleTerminateClient(this.tclientSocket, this.tserverSocket);

            // Create a new Thread with this object.
            Thread tt = new Thread(handleTerminateClient);
            tt.start();
        }
    }
}

class HandleTerminateClient implements Runnable {
    private Socket tclientSocket;
    private ServerSocket tserverSocket;

    public HandleTerminateClient(Socket tclientSocket, ServerSocket tserverSocket) {
        this.tclientSocket = tclientSocket;
        this.tserverSocket = tserverSocket;
    }

    @Override
    public void run() {
        try {
            handleTerminateClientCommands();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleTerminateClientCommands() throws IOException, InterruptedException {
        PrintWriter tsendtoClient = new PrintWriter(tclientSocket.getOutputStream(), true);

        //read input from client
        BufferedReader tclientInput = new BufferedReader(new InputStreamReader(tclientSocket.getInputStream()));

        //get the path of server file.
        String spath = System.getProperty("user.dir");

        //accepts input from client
        String inputClient;
        while ((inputClient = tclientInput.readLine()) != null) {
            //quit implementation
            if (inputClient.equals("quit")) {
                //CLOSE THE SOCKET
                this.tclientSocket.close();
                break;
            }

            //terminate implementation
            //Note: If clients gives any thread id which is not related to get or
            //put and if there is a thread with that Id, the program will go in deadlock!
            if(inputClient.equals("terminate")){
                //receive id to be terminated
                long id = Long.parseLong(tclientInput.readLine());
                //receive the host details
                String host = tclientInput.readLine();
                //receive the port of normal server
                int nport = Integer.parseInt(tclientInput.readLine());

                Socket connectToNormalSocket = new Socket(host, nport);
                PrintWriter putsendtoServer = new PrintWriter(connectToNormalSocket.getOutputStream(), true);

                //read input from Normal server
                BufferedReader putreadServer = new BufferedReader(new InputStreamReader(connectToNormalSocket.getInputStream()));

                //send the command id to normnal server
                putsendtoServer.println("interrupt");
                putsendtoServer.println(id);

                //skipping the first two responses as they do not matter to terminate command
                putreadServer.readLine();
                putreadServer.readLine();
                //receive response from the normal server
                String response = putreadServer.readLine();
                //reply to the client
                tsendtoClient.println(response);

                //Since work of the terminate socket is done! close it!
                connectToNormalSocket.close();
                continue;
            }
        }
    }
}

//First thread invokes normal Server process
class NormalServer extends Thread {
    private Socket nclientSocket;
    private final ServerSocket nserverSocket;
    private ServerSocket getserverSocket;
    private ServerSocket putserverSocket;

    public NormalServer(ServerSocket nserverSocket, ServerSocket getserverSocket, ServerSocket putserverSocket) throws IOException {
        this.nserverSocket = nserverSocket;
        this.getserverSocket = getserverSocket;
        this.putserverSocket = putserverSocket;
    }

    @Override
    public void run() {
        try {
            handleNormalSocket();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleNormalSocket() throws IOException, InterruptedException{

        //while loop to accept multiple clients
        while (true){

            //Client discovered on nport. Accept connection
            this.nclientSocket = this.nserverSocket.accept();
            Runnable handleClient = new HandleClient(this.nclientSocket, this.nserverSocket, this.getserverSocket, this.putserverSocket);

            // Create a new Thread with this object.
            Thread t = new Thread(handleClient);
            t.start();
        }
    }
}

class HandleClient implements Runnable{
    private Socket nclientSocket;
    private ServerSocket nserverSocket;

    private ServerSocket getserverSocket;
    private ServerSocket putserverSocket;

    public HandleClient(Socket nclientSocket, ServerSocket nserverSocket, ServerSocket getserverSocket, ServerSocket putserverSocket){
        this.nclientSocket = nclientSocket;
        this.nserverSocket = nserverSocket;
        this.getserverSocket = getserverSocket;
        this.putserverSocket = putserverSocket;
    }

    @Override
    public void run() {
        try {
            handleClientCommands();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleClientCommands() throws IOException, InterruptedException{
        PrintWriter nsendtoClient = new PrintWriter(nclientSocket.getOutputStream(), true);

        //read input from client
        BufferedReader nclientInput = new BufferedReader(new InputStreamReader(nclientSocket.getInputStream()));

        //Initiating conversation
        nsendtoClient.println("handleClientCommands Responding");//optimize and remove in the end
        nsendtoClient.println("Initiating conversation");//remove this as well in the end

        //get the path of server file.
        String spath = System.getProperty("user.dir");

        //accepts input from client
        String inputClient;
        while ((inputClient = nclientInput.readLine()) != null) {
            //terminate simulation
            if (inputClient.equals("interrupt")){
                //initialise thread variable;
                Thread t = new Thread();
                //receive the command id to be terminated
                int id = Integer.parseInt(nclientInput.readLine());

                //Iterate over all threads to check if thread with the above id exists or not
                Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                for(Thread thread : setOfThread){
                    if (thread.getId() == id){
                        //add the id to the global list
                        myftpserver.list.add(id);
                        //store the thread in variable t
                        t = thread;
                    }
                }

                //if the id is not stored then it implies, it could not find the required thread in pool of threads!
                if (!myftpserver.list.contains((Object) id)){
                    nsendtoClient.println("Either process is finished or the id is incorrect");
                    continue;
                }

                //wait till the thread is terminated by itself
                while(t.isAlive()){
                    Thread.currentThread().sleep(100);
                }

                //thread is finally dead!
                if(t.isAlive() != true){
                    nsendtoClient.println("Terminated Successfully");
                }else {
                    nsendtoClient.println("Oops! Something went wrong!");
                }
                continue;
            }

            //cd implementation
            if (inputClient.equals("cd")){
                //get the path to cd into
                String dir = nclientInput.readLine();

                // if cd . is received
                if (dir.equals(".")) {
                    nsendtoClient.println("Not a directory");
                    continue;
                }
                //if cd .. is requested
                if (dir.equals("..")) {
                    //get absolute path
                    String path = spath;
                    File file = new File(path);

                    //get parent directories path and set it as new path
                    String newpath = file.getParentFile().toString();
                    spath = newpath;
                    //send the new path to client and be ready for input from client
                    nsendtoClient.println(newpath.toString());
                    continue;
                } else {

                    //get the absolute path of the requested directory
                    String pwd = spath + "/" +dir;
                    File file2 = new File(pwd);

                    //check if its a directory
                    if (file2.isDirectory()) {
                        //get the new path and send to client
                        spath = file2.toString();
                        nsendtoClient.println(file2.toString());
                        continue;
                    } else {
                        //reply to client that it's not a directory
                        nsendtoClient.println("Not a directory");
                    }
                }
            }

            //if over is received here just continue as it is not supposed to be received here
            //Optimize here so that over is not received in any scenario.
            if (inputClient.equals("over")) continue;

            //delete implementation
            if(inputClient.equals("delete")){

                //receive name of file to be deleted
                String dir = nclientInput.readLine();

                //get full path of the file to be deleted
                String pathName = spath + "/" + dir;
                Path pathofFile = Paths.get(pathName);
                try{
                    //delete file if exists
                    if(Files.deleteIfExists(pathofFile)){
                        nsendtoClient.println(dir +" is deleted.");
                        continue;
                    } else {
                        nsendtoClient.println(dir+" does not exist.");
                        continue;
                    }
                }
                catch (DirectoryNotEmptyException e){
                    nsendtoClient.println("Directory not empty.");
                    continue;
                }
            }
            //get implementation
            if(inputClient.equals("get")){

                //get file name to be copied to client
                String dir = nclientInput.readLine();

                // check whether file exits or not
                String path = System.getProperty("user.dir") + "/" + dir;
                Path pathofFile = Paths.get(path);

                if(!Files.exists(pathofFile)){
                    nsendtoClient.println("error");
                    continue;
                }

                File file = new File(dir);
                char[] filechararray = new char[(int)file.length()];

                //fbr for reading file
                BufferedReader fbr = new BufferedReader(new FileReader(file));

                //read file in filechararray
                fbr.read(filechararray, 0, filechararray.length);
                fbr.close();
                //send the length of the char array
                nsendtoClient.println(filechararray.length);

                //send content of filechararray to client
                nsendtoClient.write(filechararray, 0, filechararray.length);

                //send over indicating end of file
                nsendtoClient.println("over");

                //send completion acknowledgement.
                nsendtoClient.println("copied to client");
                continue;
            }

            //get implementation wtih "&" appended
            if(inputClient.equals("Get2")) {
                //get file name to be copied to client
                String dir = nclientInput.readLine();

                // check whether file exits or not
                String path = System.getProperty("user.dir") + "/" + dir;
                Path pathofFile = Paths.get(path);

                if (!Files.exists(pathofFile)) {
                    nsendtoClient.println("error");
                    continue;
                }

                //check if the file is present in the list
                boolean inUse = myftpserver.fileList.contains(dir);

                //wait until file is ready to use
                while(inUse){
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException e){
                        System.out.println("InterruptedException occured");
                    }
                    if(myftpserver.fileList.contains(dir) == false)
                        inUse = false;
                }

                //if file is not present in the list, add it till we need the file
                myftpserver.fileList.add(dir);

                File file = new File(dir);
                char[] filechararray = new char[(int) file.length()];

                //send the length of the char array
                nsendtoClient.println(filechararray.length);

                Runnable handleClientGet = new HandleClient(nclientSocket, nserverSocket, getserverSocket, putserverSocket);

                //create a new thread for asynchronous execution
                Thread getthread = new Get(handleClientGet, nclientInput, nsendtoClient, file, filechararray);
                getthread.start();

                //get the current thread id to be returned to client
                long getthreadId = getthread.getId();
                nsendtoClient.println(getthreadId);

                //We do not need continue over here!
            }

            //put implementation
            if(inputClient.equals("put")){
                //name of the file to be copied to server
                String dir = nclientInput.readLine();

                //size of the file in int
                int arraylength = Integer.parseInt(nclientInput.readLine());

                //create char array of length provided by client
                char[] filebytearray = new char[arraylength];

                //read into the array
                nclientInput.read(filebytearray, 0, arraylength);
                File file = new File(System.getProperty("user.dir") + "/" + dir);

                //fbr to write into file and then close writer.
                BufferedWriter fbr = new BufferedWriter(new FileWriter(file));
                fbr.write(filebytearray, 0, arraylength);
//                fbr.flush();//newly added
                fbr.close();

                //wait for input indicating end of file
                nclientInput.readLine().equals("over");
                //send acknowledgement
                nsendtoClient.println("copied to server");
                continue;
            }

            //put implementation with & appended
            if(inputClient.equals("Put2")){
                Runnable handleClient = new HandleClient(nclientSocket, nserverSocket, getserverSocket, putserverSocket);
                //create a new thread for asynchronous execution
                Thread putthread = new Put(handleClient, nclientInput, nsendtoClient);
                putthread.start();

                //get the current thread id to be returned to client
                long threadId = putthread.getId();
                nsendtoClient.println(threadId);

                //do not need continue here;
            }

            //mkdir implementation
            if(inputClient.equals("mkdir")){
                //name of the directory to be created
                String dir = nclientInput.readLine();

                String pathName = spath + "/" + dir;
                File mkdir = new File(pathName);

                //create the new directory
                if(mkdir.mkdirs()){
                    //successful
                    nsendtoClient.println("Directory "+mkdir+" is created.");
                } else {
                    // unsuccessful
                    nsendtoClient.println("Directory "+mkdir+" is not created.");
                }
                continue;
            }

            //pwd implementation
            if (inputClient.equals("pwd")) {

                //get the current path
                String pwd = spath;

                //return the path to client
                nsendtoClient.println(pwd);
                continue;
            }

            //ls implementation
            if (inputClient.equals("ls")) {
                //get current directory
                File dir = new File(spath);

                //get all the subdirectories of present directory
                String childs[] = dir.list();
                nsendtoClient.println("null");

                //send all the directories to the client
                for (String child : childs) {
                    nsendtoClient.println(child);
                }
                //indicate end of the list
                nsendtoClient.println("ls finished");
                continue;
            }

            //quit implementation
            if(inputClient.equals("quit")){

                //CLose the client socket
                this.nclientSocket.close();
                break;
            }

            //If anything unexpected received
            nsendtoClient.println("ok");
        }
    }
    //get implementation
    public  void getImplement(BufferedReader nclientInput, PrintWriter nsendtoClient, File file, char[] filechararray) throws IOException {

        //put command discovered. Accept connection
        Socket getclientSocket = this.getserverSocket.accept();

        PrintWriter getsendtoClient = new PrintWriter(getclientSocket.getOutputStream(), true);
        BufferedReader getclientInput = new BufferedReader(new InputStreamReader(getclientSocket.getInputStream()));

        //fbr for reading file
        BufferedReader fbr = new BufferedReader(new FileReader(file));

        //read file in filechararray
        fbr.read(filechararray, 0, filechararray.length);

        //write 1000 bytes/characters in one iteration
        int n = filechararray.length/1000;
        int i;

        //receive thread id from client
        long id = Long.parseLong(getclientInput.readLine());

        //After transferring, 1000 bytes/characters check if termination of the thread is requested
        for ( i=1; i<=n; i++){
            getsendtoClient.write(filechararray, (i-1)*1000, 1000);
            getsendtoClient.flush();
            for (Object l: myftpserver.list) {
                //check if the current thread is listed for termination.
                if ((int) l == Thread.currentThread().getId()) {
                    //Iterate over all threads to check if thread with the above id exists or not
                    Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                    for (Thread thread : setOfThread) {
                        if (thread.getId() == id) {
                            //terminate the client thread!
                            thread.interrupt();
                            //close writer (which may indicate end of file)
                            getsendtoClient.close();
                        }
                    }

                    //remove file from the list
                    myftpserver.fileList.remove(file.getName().toString());

                    //close sockets
                    getclientSocket.close();
                    return;
                }
            }

        }

        //send the last remaining bytes/characters (which in total are less than 1000)
        getsendtoClient.write(filechararray, (i-1)*1000, filechararray.length - n*1000);
//        getsendtoClient.flush();
        //send over indicating end of file
        getsendtoClient.println("over");

        //send completion acknowledgement.
        getsendtoClient.println("copied to client");

        //close the sockets
        getclientSocket.close();

        //remove file from the list
        myftpserver.fileList.remove(file.getName().toString());
    }

    //put implementation
    public void putImplement(BufferedReader nclientInput, PrintWriter nsendtoClient) throws IOException {
        //put command discovered. Accept connection
        Socket putclientSocket = this.putserverSocket.accept();

        PrintWriter putsendtoClient = new PrintWriter(putclientSocket.getOutputStream(), true);
        BufferedReader putclientInput = new BufferedReader(new InputStreamReader(putclientSocket.getInputStream()));

        //name of the file to be copied to server
        String dir = putclientInput.readLine();

        //check if the file is present in the list
        boolean inUse = myftpserver.fileList.contains(dir);

        //wait until file is ready to use
        while(inUse){
            try{
                Thread.sleep(100);
            } catch (InterruptedException e){
                System.out.println("InterruptedException occured");
            }
            if(myftpserver.fileList.contains(dir) == false)
                inUse = false;
        }

        //if file is not present in the list, add it till we need the file
        myftpserver.fileList.add(dir);

        //size of the file in int
        int arraylength = Integer.parseInt(putclientInput.readLine());

        //receive thread id from client
        long id = Long.parseLong(putclientInput.readLine());

        //create char array of length provided by client
        char[] filebytearray = new char[arraylength];

        //read 1000 bytes/characters in one iteration
        int n = arraylength/1000;
        int i;

        //After every 1000 bytes/characters check if termination of the thread is requested
        for ( i=1; i<=n; i++){
            putclientInput.read(filebytearray, (i-1)*1000, 1000);
            for (Object l : myftpserver.list) {
                //check if the current thread is listed for termination.
                if ((int) l == Thread.currentThread().getId()) {
                    //Iterate over all threads to check if thread with the above id exists or not
                    Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                    for (Thread thread : setOfThread) {
                        if (thread.getId() == id) {
                            //terminate the client thread!
                            thread.interrupt();
                            //remove file from the list
                            myftpserver.fileList.remove(dir);
                            putsendtoClient.close();
                        }
                    }

                    //close sockets
                    putclientSocket.close();
                    return;
                }
            }
        }
        putclientInput.read(filebytearray, (i-1)*1000, arraylength - n*1000);

        //shouldnt it be received over here?
        putclientInput.readLine().equals("over");

//        //delete me
//        for (char j: filebytearray) {
//            System.out.print(j);
//        }
        File file = new File(System.getProperty("user.dir") + "/" + dir);

        //fbr to write into file and then close writer.
        BufferedWriter fbr = new BufferedWriter(new FileWriter(file));
        fbr.write(filebytearray, 0, arraylength);
        fbr.close();

        //send acknowledgement
        putsendtoClient.println("copied to server");

        //close the sockets
        putclientSocket.close();

        //remove file from the list
        myftpserver.fileList.remove(dir);
    }
}

class Put extends Thread{
    HandleClient handleClient;
    BufferedReader nclientInput;
    PrintWriter nsendtoClient;
    //constructor
    Put(Runnable handleClient, BufferedReader nclientInput, PrintWriter nsendtoClient){
        this.handleClient = (HandleClient) handleClient;
        this.nclientInput = nclientInput;
        this.nsendtoClient = nsendtoClient;
    }
    @Override
    public void run()
    {
        try {
            handleClient.putImplement(nclientInput, nsendtoClient);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class Get extends Thread{
    HandleClient handleClient;
    BufferedReader nclientInput;
    PrintWriter nsendtoClient;
    File file;
    char[] filechararray;

    //constructor
    Get(Runnable handleClient, BufferedReader nclientInput, PrintWriter nsendtoClient, File file, char[] filechararray){
        this.handleClient = (HandleClient) handleClient;
        this.nclientInput = nclientInput;
        this.nsendtoClient = nsendtoClient;
        this.file = file;
        this.filechararray = filechararray;
    }
    @Override
    public void run()
    {
        try {
            handleClient.getImplement(nclientInput, nsendtoClient, this.file, this.filechararray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
