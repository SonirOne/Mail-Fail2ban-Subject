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

    public void readMails() {

        Properties props = getProperties();
        String username = props.getProperty("username");
        String password = props.getProperty("password");

        logger.info("Start app...");
        try {
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

            //open the inbox folder
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int msgCount = inbox.getMessageCount();

            int msgStart = 1;
            int msgEnd = msgCount;

            int numMessages = 250;
            if (msgCount > numMessages) {
                msgStart = msgCount - numMessages;
                msgEnd = msgCount;
            }

            Message[] messages = inbox.getMessages(msgStart, msgEnd);

            Message msg;
            String subject;
            String ip;
            HashMap<String, IPInfo> sshdIPs = new HashMap<>();
            for (int i = messages.length - 1; i >= 0; i--) {

                if (i % 50 == 0) {
                    logger.info("Processing...");
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

            inbox.close(false);
            store.close();
        } catch (NoSuchProviderException nspe) {
            System.err.println("invalid provider name");
        } catch (MessagingException me) {
            System.err.println("messaging exception");
            me.printStackTrace();
        }

        logger.info("Finished.");
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
