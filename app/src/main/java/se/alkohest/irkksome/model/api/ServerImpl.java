package se.alkohest.irkksome.model.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.alkohest.irkksome.irc.ConnectionData;
import se.alkohest.irkksome.irc.IrcProtocol;
import se.alkohest.irkksome.irc.IrcProtocolFactory;
import se.alkohest.irkksome.irc.IrcProtocolListener;
import se.alkohest.irkksome.irc.IrcProtocolStrings;
import se.alkohest.irkksome.model.api.dao.IrcChannelDAO;
import se.alkohest.irkksome.model.api.dao.IrcMessageDAO;
import se.alkohest.irkksome.model.api.dao.IrcServerDAO;
import se.alkohest.irkksome.model.api.dao.IrcUserDAO;
import se.alkohest.irkksome.model.api.local.IrcChannelDAOLocal;
import se.alkohest.irkksome.model.api.local.IrcMessageDAOLocal;
import se.alkohest.irkksome.model.api.local.IrcServerDAOLocal;
import se.alkohest.irkksome.model.api.local.IrcUserDAOLocal;
import se.alkohest.irkksome.model.entity.IrcChannel;
import se.alkohest.irkksome.model.entity.IrcMessage;
import se.alkohest.irkksome.model.entity.IrcServer;
import se.alkohest.irkksome.model.entity.IrcUser;

public class ServerImpl implements Server, IrcProtocolListener {
    private IrcProtocol ircProtocol;
    private IrcServer ircServer;
    private HilightHelper hilightHelper;
    private IrcChannelDAOLocal channelDAO = new IrcChannelDAO();
    private IrcMessageDAOLocal messageDAO = new IrcMessageDAO();
    private IrcServerDAOLocal serverDAO = new IrcServerDAO();
    private IrcUserDAOLocal userDAO = new IrcUserDAO();
    private ServerCallback listener;
    private String motd = "";

    private IrcChannel activeChannel;
    private ServerDropAcidListener dropListener;

    public ServerImpl(IrcServer ircServer, ConnectionData data) {
        this.ircServer = ircServer;
        ircProtocol = IrcProtocolFactory.getIrcProtocol(data);
        ircProtocol.setListener(this);
        ircProtocol.connect(data.getNickname(), data.getUsername(), data.getRealname(), data.getPassword());
        ircServer.setSelf(serverDAO.getUser(ircServer, data.getNickname()));
        hilightHelper = new HilightHelper();
        // TODO - fix dynamic hilights
        hilightHelper.addHilight(data.getNickname());
    }

    @Override
    public IrcServer getBackingBean() {
        return ircServer;
    }

    @Override
    public void setListener(ServerCallback listener) {
        this.listener = listener;
    }

    @Override
    public void joinChannel(String channelName) {
        ircProtocol.joinChannel((channelName.charAt(0) == '#' ? "" : "#") + channelName);
    }

    @Override
    public void changeNick(String nick) {
        ircProtocol.setNick(nick);
    }

    @Override
    public String getMotd() {
        return motd;
    }

    @Override
    public void disconnect() {
        // TODO - fix custom message
        ircProtocol.disconnect("irkksome <3");
    }

    @Override
    public void setDropListener(ServerDropAcidListener listener) {
        dropListener = listener;
    }

    @Override
    public void leaveChannel(IrcChannel channel) {
        if(channel.getName().charAt(0) == '#') {
            ircProtocol.partChannel(channel.getName());
        }
        serverDAO.removeChannel(ircServer, channel);
        List<IrcChannel> channels = ircServer.getConnectedChannels();
        if (channels.size() != 0) {
            activeChannel = channels.get(channels.size() - 1);
            listener.setActiveChannel(activeChannel);
        } else {
            activeChannel = null;
            listener.showServerInfo(ircServer, motd);
        }
    }

    @Override
    public void sendMessage(IrcChannel channel, String message) {
        ircProtocol.sendChannelMessage(channel.getName(), message);
        IrcMessage ircMessage = messageDAO.create(ircServer.getSelf(), message, new Date());

        channelDAO.addMessage(channel, ircMessage);
        listener.messageReceived();
    }

    @Override
    public java.util.Set<IrcUser> getUsers() {
        return ircServer.getKnownUsers();
    }

    @Override
    public IrcChannel getActiveChannel() {
        return activeChannel;
    }

    @Override
    public void setActiveChannel(IrcChannel ircChannel) {
        if (activeChannel != ircChannel) {
            listener.setActiveChannel(ircChannel);
            listener.updateHilights();
            activeChannel = ircChannel;
        }
    }

    @Override
    public void startQuery(String nick) {
        IrcUser user = serverDAO.getUser(ircServer, nick);
        IrcChannel query = serverDAO.getChannel(ircServer, nick);
        channelDAO.addUser(query, user, "");
        listener.setActiveChannel(query);
        activeChannel = query;
    }

    @Override
    public void showServer() {
        listener.showServerInfo(ircServer, motd);
        activeChannel = null;
    }

    //    ---------------------------------------------------------

    @Override
    public void serverConnected() {
        // TODO - this should only be done when we know we connect to irrsi?
        ircProtocol.sendBacklogRequest(ircServer.getLastMessageTime().getTime()/1000);
    }

    @Override
    public void serverRegistered(String server, String nick) {
        // TODO - this should maybe check if your nick has changed and change it instead of creating new
        ircServer.setSelf(serverDAO.getUser(ircServer, nick));
        listener.showServerInfo(ircServer, motd);
        for (IrcChannel channel : getBackingBean().getConnectedChannels()) {
            joinChannel(channel.getName());
        }
    }

    @Override
    public void nickChanged(String oldNick, String newNick, Date time) {
        if (userDAO.compare(ircServer.getSelf(), oldNick)) {
            ircServer.getSelf().setName(newNick);
        } else {
            IrcUser user = serverDAO.getUser(ircServer, oldNick);
            user.setName(newNick);
            listener.nickChanged(oldNick, user);
        }
        ircServer.setLastMessageTime(time);
    }

    @Override
    public void usersInChannel(String channelName, List<String> users) {
        IrcChannel channel = serverDAO.getChannel(ircServer, channelName);
        for (String u : users) {
            String flag = "";
            if (hasFlag(u)) {
                flag = String.valueOf(u.charAt(0));
                u = u.substring(1);
            }
            IrcUser user = serverDAO.getUser(ircServer, u);
            channelDAO.addUser(channel, user, flag);
            serverDAO.addUser(ircServer, user);
        }
        checkUserUpdate(channel);
    }

    private void checkUserUpdate(IrcChannel channel) {
        if (activeChannel == null ||
                channel.equals(activeChannel)) {
            listener.updateUserList();
        }
    }

    private boolean hasFlag(String user) {
        return user.startsWith(IrcProtocolStrings.FLAG_HALFOP) ||
                user.startsWith(IrcProtocolStrings.FLAG_OP) ||
                user.startsWith(IrcProtocolStrings.FLAG_OWNER) ||
                user.startsWith(IrcProtocolStrings.FLAG_SUPEROP) ||
                user.startsWith(IrcProtocolStrings.FLAG_VOICE);
    }

    @Override
    public void userJoined(String channelName, String nick, Date time) {
        IrcChannel channel = serverDAO.getChannel(ircServer, channelName);
        if (userDAO.compare(ircServer.getSelf(), nick)) {
            listener.setActiveChannel(channel);
            activeChannel = channel;
        } else {
            IrcUser user = serverDAO.getUser(ircServer, nick);
            channelDAO.addUser(channel, user, "");
            listener.userJoinedChannel(channel, user);
        }
        checkUserUpdate(channel);
        ircServer.setLastMessageTime(time);
    }

    @Override
    public void userParted(String channelName, String nick, Date time) {
        IrcChannel channel = serverDAO.getChannel(ircServer, channelName);
        if (!userDAO.compare(ircServer.getSelf(), nick)) {
            IrcUser user = serverDAO.getUser(ircServer, nick);
            channelDAO.removeUser(channel, user);
            listener.userLeftChannel(channel, user);
            checkUserUpdate(channel);
        } else {
            leaveChannel(channel);
        }
        ircServer.setLastMessageTime(time);
    }

    @Override
    public void userQuit(String nick, String quitMessage, Date time) {
        IrcUser user = serverDAO.getUser(ircServer, nick);
        serverDAO.removeUser(ircServer, user);
        List<IrcChannel> channels = new ArrayList<>();
        for (IrcChannel c : ircServer.getConnectedChannels()) {
            String flag = channelDAO.removeUser(c, user);
            if (flag != null) {
                channels.add(c);
            }
        }
        ircServer.setLastMessageTime(time);
        listener.userQuit(user, channels);
    }

    @Override
    public void channelMessageReceived(String channel, String user, String message, Date time) {
        IrcUser ircUser = serverDAO.getUser(ircServer, user);
        IrcMessage ircMessage = messageDAO.create(ircUser, message, time);
        IrcChannel ircChannel;

        // Hilightlogiken ska flyttas till hilights
        if (ircServer.getSelf().getName().equals(channel)) {
            ircChannel = serverDAO.getChannel(ircServer, user);
            ircMessage.setHilight(true);
        } else {
            ircChannel = serverDAO.getChannel(ircServer, channel);
            ircMessage.setHilight(hilightHelper.checkMessage(message));
        }

        if (!ircChannel.equals(activeChannel)) {
            UnreadEntity entity = new UnreadEntity(ircChannel, ircServer);
            dropListener.addUnread(entity, ircMessage.isHilight());
            listener.updateHilights();
        }

        ircServer.setLastMessageTime(time);
        channelDAO.addMessage(ircChannel, ircMessage);
        if (ircChannel == activeChannel && !ircProtocol.isBacklogReplaying()) {
            listener.messageReceived();
        }
    }

    @Override
    public void whoisChannels(String nick, List<String> channels) {

    }

    @Override
    public void whoisRealname(String nick, String realname) {

    }

    @Override
    public void whoisIdleTime(String nick, int seconds) {

    }

    @Override
    public void channelListResponse(String name, String topic, String users) {

    }

    @Override
    public void motdReceived(String motd) {
        this.motd = motd;
    }

    @Override
    public void serverDisconnected() {
        // TODO - this method should try to reconnect if its appropriate?
        dropListener.dropServer(this);
        listener.serverDisconnected();
    }

    @Override
    public void ircError(String errorCode, String message) {
        listener.error(message);
        if ((errorCode.equals(IrcProtocolStrings.ERR_NICKNAMEINUSE) ||
                errorCode.equals(IrcProtocolStrings.ERR_YOUREBANNEDCREEP) ||
                errorCode.equals(IrcProtocolStrings.ERR_ERRONEUSNICKNAME)) &&
                                        ircServer.getSelf() == null) {
            disconnect();
        }
    }
}
