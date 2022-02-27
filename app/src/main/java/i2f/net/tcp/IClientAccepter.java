package i2f.net.tcp;

import java.net.Socket;

public interface IClientAccepter {
    void onClientArrive(Socket sock);
    void onServerClose();
}
