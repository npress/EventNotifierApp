import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;


public class SendMailUsingTLS {
	
	
		String d_email = "nema.press@sleepygiant.com",
		d_password = Pass.get(),
		d_host = "exchange.zerolag.com",
		d_port = "25",
		m_to = "nema.press@sleepygiant.com",
		m_subject = "Testing",
		m_text = "Hey, this is the testing email.";

	public SendMailUsingTLS()
	{
		Properties props = new Properties();
		props.put("mail.smtp.user", d_email);
		props.put("mail.smtp.host", d_host);
		props.put("mail.smtp.port", d_port);
		props.put("mail.smtp.starttls.enable","true");
		props.put("mail.smtp.auth", "false");
		//props.put("mail.smtp.debug", "true");
		props.put("mail.smtp.socketFactory.port", d_port);
		//props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");
	
		SecurityManager security = System.getSecurityManager();

	try
	{
		Authenticator auth = new SMTPAuthenticator();
		Session session = Session.getInstance(props, auth);
		//session.setDebug(true);
	
		MimeMessage msg = new MimeMessage(session);
		msg.setText(m_text);
		msg.setSubject(m_subject);
		msg.setFrom(new InternetAddress(d_email));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(m_to));
		Transport.send(msg);
	}
	catch (Exception mex)
	{
		mex.printStackTrace();
	}
	}

	public static void main(String[] args)
	{
		SendMailUsingTLS tlsMailApp = new SendMailUsingTLS();
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator
	{
		public PasswordAuthentication getPasswordAuthentication()
		{
			return new PasswordAuthentication(d_email, d_password);
		}
	}
	
}
