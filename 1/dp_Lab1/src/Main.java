import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class Main {
    static int elems = 64;
    static long[] oPositions = new long[elems];
    static String fileContent;
    static ArrayList<String> compareList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            fileContent = Files.readString(Paths.get("leasing.txt"));
            generateSHA1("leasing.txt");
            fileContent = Files.readString(Paths.get("leasing1.txt"));
            generateSHA1("leasing1.txt");

            if (compareList.get(0).equals(compareList.get(1))) {
                System.out.println("File leasing.txt equals leasing1.txt by SHA1 hash code.");
            }

            int count = 0;
            int k = 0;
            for (int i = 0; i < fileContent.length(); i++) {
                if (fileContent.charAt(i) == 'о') {
                    count++;
                    if (count <= elems) {
                        oPositions[k] = i;
                        k++;
                    } else break;
                }
            }

            for (int i = 1; i <= Math.pow(2, 14); i++) {
                createFile(i);
            }

            String firstElement = compareList.get(0); // получить первый элемент массива
            for (int i = 1; i < compareList.size(); i++) {
                String currentElement = compareList.get(i);
                if (currentElement.equals(firstElement))
                    System.out.println("Найдена коллизия с файлом № " + i + ":" + currentElement);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void createFile(int fileNumber) {
        StringBuilder temp = new StringBuilder(fileContent);

        for (int i = elems - 1; i >= 0; i--) {
            int bit = (fileNumber >> i) & 1;
            if (bit == 1)
                temp.setCharAt(Math.toIntExact(oPositions[i]), 'o');
        }

        try {
            String path = "src/txt/" + fileNumber + ".txt";
            Files.write(Paths.get(path), temp.toString().getBytes(), StandardOpenOption.CREATE);

            generateSHA1(path);
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    protected static void generateSHA1(String path) {
        try {
            ProcessBuilder builder = new ProcessBuilder("openssl", "dgst", "-sha1", path);

            // сообщает процессу объединять стандартный вывод и стандартный поток ошибок в один поток,
            // который мы можем прочитать в Java
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // для чтения вывода процесса
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // читаем каждую строку вывода процесса и выводим ее в консоль
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                addToCompare(line);
            }

            // чтобы дождаться завершения процесса и получить код ошибки, если таковой имеется
            int exitCode = process.waitFor();
            System.out.println("Exited with error code " + exitCode);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addToCompare(String output) {
        compareList.add(output.substring(output.indexOf("= ") + 2));
    }
}
