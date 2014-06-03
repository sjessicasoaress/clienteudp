package socketUDP;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class MyServerSocket {

    private int porta;
    private ArrayList<MySocket> sockets;
    private boolean ouvindoNovasConexoes;
    private DatagramSocket serverSocket;
    private Thread threadNovasConexoes;
    
    private MyServerSocketListener listener;

    public MyServerSocket(int porta, MyServerSocketListener listener) {
        this.listener = listener;
        this.ouvindoNovasConexoes = true;
        this.porta = porta;
        this.sockets = new ArrayList();
        threadNovasConexoes = new Thread(new Runnable() {
            @Override
            public void run() {
                ouvirNovasConexoes();
            }
        });
        threadNovasConexoes.start();
    }
    
    public void close(){
        ouvindoNovasConexoes = false;
    }

    private void ouvirNovasConexoes() {
        try {
            serverSocket = new DatagramSocket(porta);
            int socketPort = porta +1;
            byte[] dadosRecebimento = new byte[1024];
            DatagramPacket pacoteRecebimento = new DatagramPacket(dadosRecebimento,dadosRecebimento.length);
            while (ouvindoNovasConexoes) {
                serverSocket.receive(pacoteRecebimento);
                InetAddress ipDest = pacoteRecebimento.getAddress();
                int portaDest = pacoteRecebimento.getPort();
                MySocket mySocket = new MySocket(ipDest, portaDest, socketPort++);
                sockets.add(mySocket);
                listener.onNewConnection(mySocket);
            }
        } 
        catch (SocketException e) {
            System.out.println("Porta " + this.porta + " ocupada");
        }
        catch (IOException e) {
            System.out.println("Erro no recebimento do pacote.");
        }
    }

}
