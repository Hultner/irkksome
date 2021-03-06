package se.alkohest.irkksome.irc;

public interface ConnectionData {
    public String getPassword();

    public void setPassword(String password);

    public boolean isUseSSH();

    public void setUseSSH(boolean useSSH);

    public int getSshPort();

    public void setSshPort(int sshPort);

    public String getSshPass();

    public void setSshPass(String sshPass);

    public String getSshUser();

    public void setSshUser(String sshUser);

    public String getSshHost();

    public void setSshHost(String sshHost);

    public String getRealname();

    public void setRealname(String realname);

    public String getUsername();

    public void setUsername(String username);

    public String getNickname();

    public void setNickname(String nickname);

    public int getPort();

    public void setPort(int port);

    public String getHost();

    public void setHost(String host);

    public boolean isUseSSL();

    public void setUseSSL(boolean useSSL);
}
