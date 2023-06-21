import java.io.IOException;
import java.net.Socket;

public class Bob {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Client client = new Client("Bob");
        client.message = "Oh, Alice? Hello!".getBytes();
        Socket socket = client.connectToServer("localhost", 8888);
        client.sendLeafCSR(socket);
        client.receiveCertPack(socket);
        client.verifyLeafCert();
    }
}
