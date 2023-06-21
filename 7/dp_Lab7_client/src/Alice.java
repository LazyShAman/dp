import java.io.IOException;
import java.net.Socket;

public class Alice {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Client client = new Client("Alice");
        client.message = "Hi, Bob. How are you?".getBytes();
        Socket socket = client.connectToServer("localhost", 8888);
        client.sendLeafCSR(socket);
        client.receiveCertPack(socket);
        client.verifyLeafCert();
    }
}
