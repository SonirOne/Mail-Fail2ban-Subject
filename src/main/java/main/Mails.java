package main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.mail.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class Mails {

    private static final Logger logger = LogManager.getLogger(Mails.class.getName());

    private HashMap<String, IPInfo> sshdIPs;
    private Properties props;

    private Folder inbox;

    public Mails(){
        sshdIPs = new HashMap<>();
        props = getProperties();
    }

    public void readMails() {
        logger.info("Start app...");
        try {
            Store store=openStore();
            Message[] messages = getMessages(store);
            processMessages(messages);
            processSSHDIPs();
            inbox.close(true);
            store.close();
        } catch (NoSuchProviderException nspe) {
            System.err.println("invalid provider name");
        } catch (MessagingException me) {
            System.err.println("messaging exception");
            me.printStackTrace();
        }

        logger.info("Finished.");
    }

    private Store openStore() throws MessagingException {
        String username = props.getProperty("username");
        String password = props.getProperty("password");

        //Connect to the server
        Session session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        //session.setDebug( true);
        Store store = session.getStore();
        store.connect();
        return store;
    }

    private Message[] getMessages(Store store) throws MessagingException {
        //open the inbox folder
        inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        int msgCount = inbox.getMessageCount();

        int msgStart = 1;
        int msgEnd = msgCount;

        int numMessages = 250;
        if (msgCount > numMessages) {
            msgStart = msgCount - numMessages;
            msgEnd = msgCount;
        }

        return inbox.getMessages(msgStart, msgEnd);
    }

    private void processMessages(Message[] messages) throws MessagingException {
        Message msg;
        String subject;
        String ip;

        for (int i = 0; i < messages.length; i++) {

            if (i % 50 == 0) {
                logger.info("Processed "+i+"...");
            }

            msg = messages[i];
            subject = msg.getSubject();
            ip = getIP(subject);
            if (ip != null) {
                if (!sshdIPs.containsKey(ip)) {
                    sshdIPs.put(ip, new IPInfo(ip));
                }

                sshdIPs.get(ip).increment();
            }
        }
    }

    private void processSSHDIPs(){


        Collection<IPInfo> hashMapValues = sshdIPs.values();

        ArrayList<IPInfo> listIPInfo = new ArrayList<IPInfo>();
        listIPInfo.addAll(hashMapValues);
        listIPInfo.sort(new Comparator<IPInfo>() {
            @Override
            public int compare(IPInfo first, IPInfo second) {
                if (first.getCount() > second.getCount()) {
                    return -1;
                }

                if (first.getCount() < second.getCount()) {
                    return 1;
                }

                return 0;
            }
        });

        for (IPInfo info : listIPInfo) {
            System.out.println(info);
        }
    }

    private Properties getProperties() {
        Properties props = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream("mail.properties");

            // load a properties file
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return props;
    }

    private String getIP(String subject) {
        if (subject.contains("sshd: banned")) {
            // [Fail2Ban] sshd: banned - length 24
            int startPos = 24;
            int endPos = subject.indexOf("from");
            return subject.substring(startPos, endPos - 1);
        }

        return null;
    }
}
