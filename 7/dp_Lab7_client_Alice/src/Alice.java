import dp.scsa.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static dp.scsa.Tools.chooseFile;

public class Alice {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Client client = new Client("Alice");
        Socket socket = client.connectToServer("localhost", 8888);
        client.createCertificate(socket);

        try {
            // Создаем сокет для подключения к Бобу
            Socket socketP2P = new Socket("localhost", 1234);

            // Получаем потоки ввода/вывода для обмена данными с Бобом
            ObjectOutputStream outputStream = new ObjectOutputStream(socketP2P.getOutputStream());
            outputStream.writeObject(client.getClientLogin());
            ObjectInputStream inputStream = new ObjectInputStream(socketP2P.getInputStream());
            String penFriend = (String) inputStream.readObject();

            // Цикл для отправки и получения файлов
            while (true) {
                String filePath = chooseFile();
                if (filePath == null) {
                    socketP2P.close();
                    break;
                }
                client.setMessage(filePath);
                client.createSignature();

                // Отправляем документ Бобу
                client.sendFiles(socketP2P);

                System.out.println();

                // Получаем документ от Боба
                if (!socketP2P.isClosed())
                    client.receiveFiles(socketP2P, penFriend);
                else
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
