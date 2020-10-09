# Multithreaded FTP Client and Server


### In this project, multi-threaded client and server are introduced. The client is able to handle multiple commands  (from same user) simultaneously and server is able to handle multiple commands from multiple clients. The client executable is called “myftp” and the server executable is called “myftpserver”.
### The client and server support below set of commands
###     -- get, put, delete, ls, cd, mkdir, pwd, quit, terminate.
### The syntax of the command is indicated in the parenthesis.
 
1. get (get <remote_filename>) -- Copy file with the name <remote_filename> from remote directory to local directory. 
2. put (put <local_filename>) -- Copy file with the name <local_filename> from local directory to remote directory. 
3. delete (delete <remote_filename>) – Delete the file with the name <remote_filename> from the remote directory. 
4. ls (ls) -- List the files and subdirectories in the remote directory. 
5. cd (cd <remote_direcotry_name> or cd ..) – Change to the <remote_directory_name > on the remote machine or change to the parent directory of the current directory 
6. mkdir (mkdir <remote_directory_name>) – Create directory named <remote_directory_name> as the sub-directory of the current working directory on the remote machine. 
7. pwd (pwd) – Print the current working directory on the remote machine. 
8. quit (quit) – End the FTP session. 
9. terminate <commad-ID> -- terminate the command identified by <command-ID>.



#### FTP Server (myftpserver program)  - The server program takes two command line parameters, which are the port numbers where the server waits on (one for normal commands and another for the “terminate” command). The port for normal commands are henceforth referred to as “nport” and the terminate port is referred to as “tport”. Once the myftpserver program is invoked, it creates two threads. The first thread creates a socket on the first port number and waits for incoming clients. The second thread creates a socket on the second port and waits for incoming “terminate” commands. 
#### When a client connects to the normal port, the server spawns off a new thread to handles the client commands, it then starts accepting commands and executes them. The directory where the server program resides is the current working directory for each incoming client connection (i.e., if the first command entered by a client is “pwd” it sees the path of the directory where the server program resides).  
#### However, when the serve gets a “get” or a “put” command, it immediately sends back a command ID to the client. This is for the clients to use when they need to terminate a currently-running command. Furthermore, the threads executing the “get” and “put” commands, periodically (after transferring 1000 bytes) check the status of the command to see if the client needs the command to be terminated. If so, it stops transferring data, deletes any files that were created and becomes ready to execute more commands. The server is able to handle the consistency issues arising out of concurrency. 

#### When a client connects to the “tport”, the server accepts the terminate command. The command identifies (using the command-ID) which command needs to be terminated. It sets the status of that command to “terminate” so that the thread executing that command will notice it and gracefully terminate. 

#### FTP Client: The ftp client program takes three command line parameters the machine name where the server resides, the normal port number, and the terminate port number. Once the client starts up, it displays a prompt “mytftp>”. It then accepts and execute commands by relaying the commands to the server and displaying the results and error messages where appropriate. The client terminates when the user enters the “quit” command. 
#### However, if any command is appended with a “&” sign (e.g., get file1.txt &), then this command is  executed in a separate thread. The main thread continues to wait for more commands (i.e., it is not blocked for the other threads to complete). For “get” and “put” commands, the client displays the command-ID received from the server. When the user enters a terminate command, the client uses the tport to relay the command to the server. The client also cleans up any files that were created as a result of commands that were terminated.

### To run the client server programs :
	Compile the files using : javac <filnename>
	Run the programs in different terminals.
	server program terminal: java myftpserver [Normal Port] [Terminate Port]
	client program terminal: java myftp [IP address of server system] [Normal Port] [Terminate Port]

### Note : use new terminal for every new client!
