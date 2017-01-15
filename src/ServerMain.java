
public class ServerMain {
	
	public ServerMain() { 
		SocketServer socket_server = new SocketServer();
		socket_server.start();
//		new GUI_keyboard(socket_server);
	}
	public static void main(String[] args){
		new ServerMain();
	}

}
