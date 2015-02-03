package se.alkohest.irkksome.irc;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class SSHConnection extends SSHClient implements ServerConnection {
    private ServerConnection forwardingConnection;

    public SSHConnection(ConnectionData data) {
        super(data);
        this.forwardingConnection = new NormalConnection(data.getHost(), localPort);
    }

    @Override
    public void connect() throws IOException {
        establishConnection();
        forwardingConnection.connect();
    }

    @Override
    public String readLine() throws IOException {
        return forwardingConnection.readLine();
    }

    @Override
    public void write(String s) throws IOException {
        forwardingConnection.write(s);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    protected void postAuthAction() {
        try {
            portForwarder = connection.createLocalPortForwarder(new InetSocketAddress(InetAddress.getLocalHost(), localPort), connectionData.getHost(), connectionData.getPort());
        } catch (IOException e) {
            Log.e(TAG, "could not create portforward", e);
        }
    }

    @Override
    public void close() {
        closeAll();
    }
}
