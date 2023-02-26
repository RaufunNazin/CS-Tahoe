package task3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class congestionServer {
    private static byte[] toHeader(int seqNum, int ackNum) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(ByteBuffer.allocate(4).putInt(seqNum).array());
            outputStream.write(ByteBuffer.allocate(4).putInt(ackNum).array());
            outputStream.write(ByteBuffer.allocate(1).put((byte) 1).array());
            outputStream.write(ByteBuffer.allocate(1).put((byte) 0).array());
            outputStream.write(ByteBuffer.allocate(2).putShort((short) 12).array());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    private static int[] fromHeader(byte[] segment) {
        int[] header = new int[5];
        ByteBuffer buffer = ByteBuffer.wrap(segment);
        header[0] = buffer.getInt();
        header[1] = buffer.getInt();
        header[2] = buffer.get();
        header[3] = buffer.get();
        header[4] = buffer.getShort();
        return header;
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5001);
            Socket clientSocket = serverSocket.accept();
            int recvBufferSize = 12;
            clientSocket.setReceiveBufferSize(recvBufferSize);
            int expectedSeqNum = 0;
            int ackNum = 0;
            ByteArrayOutputStream receivedData = new ByteArrayOutputStream();

            while (true) {
                byte[] header = new byte[12];
                clientSocket.getInputStream().read(header);
                int[] headerFields = fromHeader(header);
                int seqNum = headerFields[0];
                int rwnd = headerFields[4];
                System.out.println("Seq Num: " + seqNum);

                byte[] data = new byte[rwnd];
                int bytesRead = clientSocket.getInputStream().read(data);
                String str = new String(data, 0, bytesRead);
                System.out.println(str);

                if (bytesRead == -1) {
                    break;
                }

                seqNum = ackNum;

                if (seqNum == expectedSeqNum) {
                    receivedData.write(data);
                    ackNum += bytesRead;
                    expectedSeqNum += bytesRead;

                    byte[] toSendAck = toHeader(seqNum, ackNum);
                    clientSocket.getOutputStream().write(toSendAck);
                } else {
                    byte[] toSendAck = toHeader(seqNum, ackNum);
                    clientSocket.getOutputStream().write(toSendAck);
                }
            }

            String receivedDataStr = receivedData.toString(StandardCharsets.UTF_8);
            System.out.println(receivedDataStr);

            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}