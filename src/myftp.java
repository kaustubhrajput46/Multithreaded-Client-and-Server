import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
public class myftp {

    public static String host;

    public static void main(String[] args) throws IOException {
        //check if command line inputs are correct
        if (args.length != 3) {
            System.err.println(
                    "Insert in the format given ahead: myftp <host> <nport> <tport>");
            System.exit(1);
        }

        //host system ip
        String host = args[0];

        //setting up the global host so that it can be used in different class
        myftp.host = host;

        //nport
        int nport = Integer.parseInt(args[1]);
        //tport
        int tport = Integer.parseInt(args[2]);

        //connect with server using host and port.
        Socket nsocket = null;
        Socket tsocket = null;
        try {
            //connect to nport
            nsocket = new Socket(host, nport);
            //connect to tport
            tsocket = new Socket(host, tport);
        } catch (ConnectException e) {
            System.out.println("Connection refused: connect.\nCheck if server is up and listening.");
            System.exit(1);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host.");
            System.exit(1);
        }

        PrintWriter nsendtoServer = new PrintWriter(nsocket.getOutputStream(), true);
        PrintWriter tsendtoServer = new PrintWriter(tsocket.getOutputStream(), true);

        //read input from Normal server
        BufferedReader nreadServer = new BufferedReader(new InputStreamReader(nsocket.getInputStream()));
        //read input from Terminate server
        BufferedReader treadServer = new BufferedReader(new InputStreamReader(tsocket.getInputStream()));

        //read user commands from command line
        BufferedReader readCmd = new BufferedReader(new InputStreamReader(System.in));

        //remove this from here and also from normal server simultaneously
        System.out.println("Normal Server : " + nreadServer.readLine());

        String userInput = "null";
        String serverReply;
        String serverReplyls;

        while((serverReply = nreadServer.readLine()) != null){

            //code for ls
            if(userInput.equals("ls")){
                while (!(serverReplyls = nreadServer.readLine()).equals("ls finished")){
                    if(!serverReplyls.equals("null"))
                        System.out.println(serverReplyls);
                }
            }
            // check if  else part is needed
            else if (!serverReply.equals("ok")){
                System.out.println(serverReply);
            }

            // accept user input
            System.out.print("myftp> ");
            userInput = readCmd.readLine();

            //handle single word commands
            //code for pwd, quit
            String[] str = userInput.split(" ");
            if(str.length == 1){
                //if userInput equals pwd, ls, quit then forward to server.
                //send rubbish for all the unsupported inputs
                if(userInput.equals("pwd")) nsendtoServer.println(userInput);
                else if (userInput.equals("ls")) nsendtoServer.println(userInput);
                else if (userInput.equals("quit")) {
                    nsendtoServer.println(userInput);
                    tsendtoServer.println(userInput);
                }
                else nsendtoServer.println("rubbish");

                //quit received stop client
                if(userInput.equals("quit")) {
                    nsocket.close();
                    break;
                }
            }

            //handle two argument commands
            if(str.length == 2) {

                //code for mkdir/delete/cd
                if (str[0].equals("mkdir") || str[0].equals("delete") || str[0].equals("cd")) {
                    //send first parameter
                    nsendtoServer.println(str[0]);
                    //send second parameter
                    nsendtoServer.println(str[1]);

                    //response from server
                    System.out.println(nreadServer.readLine());

                    //send this to keep the communication going!
                    nsendtoServer.println("rubbish");
                    continue;
                }
                //code for terminate
                if (str[0].equals("terminate")) {

                    //send terminate first
                    tsendtoServer.println(str[0]);
                    //send id to be terminated
                    tsendtoServer.println(str[1]);
                    //send the host string
                    tsendtoServer.println(myftp.host);
                    //send the normal_server port
                    tsendtoServer.println(nport);

                    //response from the server
                    String response = treadServer.readLine();
                    System.out.println(response);

                    //send this to keep the communication going!
                    nsendtoServer.println("rubbish");
                    continue;
                }

                //code for put
                if (str[0].equals("put")) {
                    //check if file exists
                    String path = System.getProperty("user.dir") + "/" + str[1];
                    Path pathofFile = Paths.get(path);
                    if (!Files.exists(pathofFile)) {
                        System.out.println("File does not exits.");

                        //send this to keep the communication going!
                        nsendtoServer.println("sry");
                        continue;
                    }

                    //send put first
                    nsendtoServer.println(str[0]);
                    //send file name to be copied at server side
                    nsendtoServer.println(str[1]);

                    File file = new File(str[1]);
                    char[] filechararray = new char[(int) file.length()];

                    //send size of filechararray to server
                    nsendtoServer.println(filechararray.length);

                    //fbr for reading file
                    BufferedReader fbr = new BufferedReader(new FileReader(file));

                    //write file in filechararray
                    fbr.read(filechararray, 0, filechararray.length);

                    //send content of filechararray to server
                    nsendtoServer.write(filechararray, 0, filechararray.length);

                    //send over indicating end of file
                    nsendtoServer.println("over");
                    continue;
                }

                //code for get
                if (str[0].equals("get")) {

                    //send get first
                    nsendtoServer.println(str[0]);

                    //send file name to be copied from server side
                    nsendtoServer.println(str[1]);

                    String status = nreadServer.readLine();
                    if (status.equals("error")) {
                        System.out.println("File does not exist.");
                        nsendtoServer.println("sry -- message to termninate the while loop");
                        continue;
                    }
                    //name of the file to be copied from server
                    File file = new File(System.getProperty("user.dir") + "/" + str[1]);

                    //size of the file in int
                    int arraylength = Integer.parseInt(status);

                    //create char array of length provided by server
                    char[] filebytearray = new char[arraylength];

                    //read into the array
                    nreadServer.read(filebytearray, 0, arraylength);
                    File fileToBeRead = new File(System.getProperty("user.dir") + "/" + str[1]);

                    //fbr to write into file and then close writer.
                    BufferedWriter fbr = new BufferedWriter(new FileWriter(fileToBeRead));
                    fbr.write(filebytearray, 0, arraylength);
                    fbr.close();

                    //wait for input indicating end of file
                    nreadServer.readLine().equals("over");
                    continue;
                }

                //future scope
                //remaining commands if any
                //send first parameter
                nsendtoServer.println(str[0]);
                //send second parameter
                nsendtoServer.println(str[1]);
            }

            //support from commands with 3 parameters e.g. put <filename> &
            //code implementation
            if(str.length == 3) {
                if (str[0].equals("put")) {
                    //check if file exists
                    String path = System.getProperty("user.dir") + "/" + str[1];
                    Path pathofFile = Paths.get(path);
                    if(!Files.exists(pathofFile)){
                        System.out.println("File does not exits.");
                        nsendtoServer.println("sry");
                        continue;
                    }

                    //send put informing put operation
                    nsendtoServer.println("Put2");

                    //wait for server to get the command Id.
                    long threadId = Long.parseLong(nreadServer.readLine());
                    System.out.println("Command Id :"+threadId);

                    //creating a new socket with the server for put communication
                    Socket putSocket = new Socket(myftp.host, 1000);

                    //create a new thread for the execution of put
                    //Paramter 1 : file name > str[1]
                    //Paramter 2 : sendtoServer > nsendtoServer
                    //Paramter 3 : tsendtoServer > tsendtoServer
                    //Paramter 4 : nreadServer > nreadServer
                    //Paramter 5 : treadServer > treadServer
                    //Paramter 6 : putSocket > putSocket
                    Thread put = new PutImplement(str[1], nsendtoServer, tsendtoServer, nreadServer, treadServer, putSocket);
                    put.start();
                    continue;
                }

                if(str[0].equals("get")){

                    //send get first as Get2
                    nsendtoServer.println("Get2");

                    //send file name to be copied from server side
                    nsendtoServer.println(str[1]);

                    String status = nreadServer.readLine();
                    if (status.equals("error")) {
                        System.out.println("File does not exist.");
                        nsendtoServer.println("sry -- message to termninate the while loop");
                        continue;
                    }

                    //size of the file in int
                    int arraylength = Integer.parseInt(status);

                    //wait for server to get the command Id.
                    long threadId = Long.parseLong(nreadServer.readLine());
                    System.out.println("Command Id :"+threadId);

                    //creating a new socket with the server for put communication
                    Socket getSocket = new Socket(myftp.host, 1000);

                    //create a thread for execution of get
                    // Paramter 1 : file name > str[1]
                    //Paramter 2 : sendtoServer > nsendtoServer
                    //Paramter 3 : tsendtoServer > tsendtoServer
                    //Paramter 4 : nreadServer > nreadServer
                    //Paramter 5 : treadServer > treadServer
                    //Paramter 6 : getSocket > getSocket
                    //Paramter 7 : arraylength > arraylength
                    Thread get = new GetImplement(str[1], nsendtoServer, tsendtoServer, nreadServer, treadServer, getSocket, arraylength);
                    get.start();
                    continue;

                }
            } else
                if(str.length != 1)
                    //do this to keep the communication going!
                    nsendtoServer.println(userInput);
        }// end while
    }
}

//Thread supporting put command
class PutImplement extends Thread{
    private PrintWriter nsendtoServer;
    private PrintWriter tsendtoServer;
    private BufferedReader nreadServer;
    private BufferedReader treadServer;
    private String filename;
    private Socket putSocket;

    //constructor to take inputs
    public PutImplement(String filename, PrintWriter nsendtoServer, PrintWriter tsendtoServer, BufferedReader nreadServer, BufferedReader treadServer,Socket putSocket){
        this.nsendtoServer = nsendtoServer;
        this.tsendtoServer = tsendtoServer;
        this.nreadServer = nreadServer;
        this.treadServer = treadServer;
        this.filename = filename;
        this.putSocket = putSocket;
    }

    @Override
    public void run() {
        try {
            putImplementation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void putImplementation() throws IOException {

        PrintWriter putsendtoServer = new PrintWriter(putSocket.getOutputStream(), true);

        //read input from Normal server
        BufferedReader putreadServer = new BufferedReader(new InputStreamReader(this.putSocket.getInputStream()));

        //send the file name to be put on server
        putsendtoServer.println(filename);
        File file = new File(filename);
        char[] filechararray = new char[(int)file.length()];

        //send size of filechararray to server
        putsendtoServer.println(filechararray.length);

        //fbr for reading file
        BufferedReader fbr = new BufferedReader(new FileReader(file));

        //write file in filechararray
        fbr.read(filechararray, 0, filechararray.length);

        //send content of filechararray to server
        putsendtoServer.write(filechararray, 0, filechararray.length);
        putsendtoServer.println("over");

        //get the acknowledgement from server
        putreadServer.equals("copied to server");

        //close the socket
        this.putSocket.close();
    }
}

//Thread supporting get command
class GetImplement extends Thread{
    private PrintWriter nsendtoServer;
    private PrintWriter tsendtoServer;
    private BufferedReader nreadServer;
    private BufferedReader treadServer;
    private String filename;
    private Socket getSocket;
    private int arraylength;

    //constructor to take inputs
    public GetImplement(String filename, PrintWriter nsendtoServer, PrintWriter tsendtoServer, BufferedReader nreadServer, BufferedReader treadServer,Socket getSocket, int arraylength){
        this.nsendtoServer = nsendtoServer;
        this.tsendtoServer = tsendtoServer;
        this.nreadServer = nreadServer;
        this.treadServer = treadServer;
        this.filename = filename;
        this.getSocket = getSocket;
        this.arraylength = arraylength;
    }

    @Override
    public void run() {
        try {
            getImplementation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void getImplementation() throws IOException {

        PrintWriter getsendtoServer = new PrintWriter(getSocket.getOutputStream(), true);

        //read input from Normal server
        BufferedReader getreadServer = new BufferedReader(new InputStreamReader(this.getSocket.getInputStream()));


        //name of the file to be copied from server
        File file = new File(System.getProperty("user.dir") + "/" + filename);

        //create char array of length provided by server
        char[] filebytearray = new char[arraylength];

        //read into the array
        getreadServer.read(filebytearray, 0, arraylength);
        File fileToBeRead = new File(System.getProperty("user.dir") + "/" + filename);

        //fbr to write into file and then close writer.
        BufferedWriter fbr = new BufferedWriter(new FileWriter(fileToBeRead));
        fbr.write(filebytearray, 0, arraylength);
        fbr.close();

        //wait for input indicating end of file
        getreadServer.readLine().equals("over");

        //get the acknowledgement from server
        getreadServer.equals("copied to server");

        //close the socket
        this.getSocket.close();
    }
}
