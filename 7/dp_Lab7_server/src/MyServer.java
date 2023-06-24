import dp.scsa.Server;

import java.io.IOException;

public class MyServer {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start(8888);
    }
}