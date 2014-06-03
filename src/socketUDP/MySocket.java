
package socketUDP;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import javax.swing.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;

public class MySocket {
    
    public class LiberaAckPerformed extends AbstractAction{
        @Override
        public void actionPerformed(ActionEvent e) {
            aguardandoAckEnvio = false;
        }
    }
    
    private boolean primeiraMensagem;
    private InetAddress ipDest;
    private int portaDest;
    private int portaOrig;
    private Thread threadOuvindo;
    private Thread threadEnviando;
    private boolean conectado;
    private DatagramSocket serverSocket;
    
    private final int TEMPO_ESPERA_ENVIO = 10000;
    
    //buffer
    private final int BUFFER_SIZE = 2;
    private LinkedList<String> bufferEntrada;
    private LinkedList<String> bufferSaida;
    private int ackEntrada;
    private int ackSaida;
    private boolean aguardandoAckEnvio;
    
    //Timer
    private Timer timer;
    
    private final String MENSAGEM_ACEPT_CONNECTION = "##CONEXAO_ACEITA##";
    
    public MySocket(InetAddress ipDest, int portaDest, int portaOrig) throws SocketException {
        
        this.primeiraMensagem = true;
        this.timer = new Timer(TEMPO_ESPERA_ENVIO, new LiberaAckPerformed());
        this.aguardandoAckEnvio = false;
        this.conectado = true;
        this.ipDest = ipDest;
        this.portaDest = portaDest;
        this.portaOrig = proximaPortaAberta(portaOrig);
        this.serverSocket = new DatagramSocket(this.portaOrig);
        this.bufferEntrada = new LinkedList();
        this.bufferSaida = new LinkedList();
        this.ackEntrada = 0;
        this.ackSaida = 0;
        threadEnviando = new Thread(new Runnable() {
            @Override
            public void run() {
                enviando();
            }
        });
        threadOuvindo = new Thread(new Runnable() {
            @Override
            public void run() {
                ouvindo();
            }
        });
        threadEnviando.start();
        threadOuvindo.start();
        enviarConf();
    }
    
    public void enviarConf(){
        bufferSaida.addLast(MENSAGEM_ACEPT_CONNECTION);
    }
    
    private void checkConfig(DatagramPacket pacote){
        if(pacote.getAddress().equals(this.ipDest)){
            this.portaDest = pacote.getPort();
        }
    }
    
    private int ackEnv(String mensagem){
        if(mensagem.length() >= 2 && mensagem.charAt(0) == 'a' && (mensagem.charAt(1) == '0' || mensagem.charAt(1) == '1')){
            return Integer.parseInt(""+mensagem.charAt(1));
        }
        return -1;
    }
    
    private int ackRcv(String mensagem){
        if(mensagem.isEmpty() || (mensagem.charAt(0) != '0' && mensagem.charAt(0) != '1')){
            return -1;
        }
        return Integer.parseInt(""+mensagem.charAt(0));
    }
    
    private String checkAckEnvio(String mensagem){
        int ack = ackEnv(mensagem);
        if(ack != -1){
            timer.stop();
            this.aguardandoAckEnvio = false;
            mensagem = mensagem.substring(2);
            if(ack == ackSaida){
                ackSaida = (ackSaida + 1) % BUFFER_SIZE;
                if(!this.bufferSaida.isEmpty())
                    this.bufferSaida.removeLast();
            }
        }
        return mensagem;
    }
    
    private String checkAckRecebimento(String mensagem) throws IOException{
        int ackRcv = ackRcv(mensagem);
        if(ackRcv != -1){
            DatagramPacket pacoteEnvio = new DatagramPacket(("a"+ackEntrada).getBytes(), 2, ipDest,portaDest);
            serverSocket.send(pacoteEnvio);
            if(ackRcv == ackEntrada){
                ackEntrada = (ackEntrada + 1) % BUFFER_SIZE;
                mensagem = mensagem.substring(1);
                if(!this.primeiraMensagem){
                    bufferEntrada.addFirst(mensagem);
                }
                primeiraMensagem = false;
            }
        }
        return mensagem;
    }
    
    private void ouvindo(){
        while(this.conectado){
            try {
                byte[] dadoRecebimento = new byte[1024];
                DatagramPacket pacoteRecebimento = new DatagramPacket(dadoRecebimento, dadoRecebimento.length);
                serverSocket.receive(pacoteRecebimento);
                if(pacoteRecebimento.getAddress().equals(this.ipDest)){
                    checkConfig(pacoteRecebimento);
                    String mensagemRecebimento = new String(pacoteRecebimento.getData()).trim();
                    mensagemRecebimento = checkAckEnvio(mensagemRecebimento);
                    mensagemRecebimento = checkAckRecebimento(mensagemRecebimento);
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MySocket.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (IOException ex) {
                Logger.getLogger(MySocket.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    protected void enviando(){
        while(this.conectado){
            if(!this.aguardandoAckEnvio && !bufferSaida.isEmpty()){
                if(bufferSaida.getLast() == null){
                    bufferSaida.removeLast();
                    bufferSaida.addLast("");
                }
                try {
                    byte[] dadosEnvio = (this.ackSaida + bufferSaida.getLast()).getBytes();
                    DatagramPacket pacoteEnvio = new DatagramPacket(dadosEnvio, dadosEnvio.length, ipDest,portaDest); 
                    serverSocket.send(pacoteEnvio);
                    this.aguardandoAckEnvio = true;
                    timer.restart();
                } catch (IOException ex) {
                    Logger.getLogger(MySocket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MySocket.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public InetAddress getIpDest() {
        return ipDest;
    }

    public void setIpDest(InetAddress ipDest) {
        this.ipDest = ipDest;
    }

    public int getPortaDest() {
        return portaDest;
    }

    public void setPortaDest(int portaDest) {
        this.portaDest = portaDest;
    }

    public int getPortaOrig() {
        return portaOrig;
    }

    public void setPortaOrig(int portaOrig) {
        this.portaOrig = portaOrig;
    }
    
    public  String receberMensagem() throws InterruptedException{
        while(bufferEntrada == null || bufferEntrada.isEmpty()){
            Thread.sleep(10);
        }
        return bufferEntrada.removeLast();
    }
    
    public void enviarMensagem(String mensagem){
        bufferSaida.addFirst(mensagem);
    }
    
    public static int proximaPortaAberta(int porta) {
        for (int i = porta; i < 65535; i++) {
            try {
                DatagramSocket ds = new DatagramSocket(i);
                ds.close();
                return i;
            } 
            catch (IOException ex) {}
        }
        return -1;
    }
}
