import java.io.*;
import java.net.*;
import java.util.*;

public class MusicServer {

  ArrayList<ObjectOutputStream> clientOutputStreams;

  public static void main(String[] args) {
    new MusicServer().go();
  }

  public class ClientHandler implements Runnable {
    ObjectInputStream in;
    Socket clientSocket;

  public ClientHandler(Socket socket) {
      try {
        clientSocket = socket;
        in = new ObjectInputStream(clientSocket.getInputStream());
      } catch (Exception ex) { ex.printStackTrace(); }
    }

      public void run() {
          Object o2 = null;
          Object o1 = null;
          try {

            while ((o1 = in.readObject()) != null) {
              o2 = in.readObject();

              System.out.println("Read message and checkbox boolean state array objects.");
              tellEveryone(o1, o2); // send out the two object to whoever is connected to the server
            }
          } catch (Exception ex) { ex.printStackTrace(); }
      }
  } // close thread

  public void go() {
    clientOutputStreams = new ArrayList<ObjectOutputStream>();

    try {
      ServerSocket serverSocket = new ServerSocket(4242);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOutputStreams.add(out);

        Thread t = new Thread(new ClientHandler(clientSocket));
        t.start();

        System.out.println("Connection established.");
      }
    } catch (Exception ex) { ex.printStackTrace(); }
  }

  public void tellEveryone(Object one, Object two) {
    Iterator it = clientOutputStreams.iterator();
    while (it.hasNext()) {
      try {
        ObjectOutputStream out = (ObjectOutputStream) it.next();
        out.writeObject(one);
        out.writeObject(two);
      } catch (Exception ex) { ex.printStackTrace(); }
    }
  }

}
