package luan.modules.mail;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import luan.Luan;
import luan.LuanState;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanException;


public final class SmtpCon {
	private final Session session;

	public SmtpCon(LuanState luan,LuanTable params) throws LuanException {
		Properties props = new Properties(System.getProperties());

		String host = getString(luan,params,"host");
		if( host==null )
			throw luan.exception( "parameter 'host' is required" );
		props.setProperty("mail.smtp.host",host);

		Object port = params.get("port");
		if( port != null ) {
			String s;
			if( port instanceof String ) {
				s = (String)port;
			} else if( port instanceof Number ) {
				Integer i = Luan.asInteger(port);
				if( i == null )
					throw luan.exception( "parameter 'port' is must be an integer" );
				s = i.toString();
			} else {
				throw luan.exception( "parameter 'port' is must be an integer" );
			}
			props.setProperty("mail.smtp.socketFactory.port", s);
			props.setProperty("mail.smtp.port", s);
		}

		String username = getString(luan,params,"username");
		if( username == null ) {
			session = Session.getInstance(props);
		} else {
			String password = getString(luan,params,"password");
			if( password==null )
				throw luan.exception( "parameter 'password' is required with 'username'" );
			props.setProperty("mail.smtp.auth","true");
			final PasswordAuthentication pa = new PasswordAuthentication(username,password);
			Authenticator auth = new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return pa;
				}
			};
			session = Session.getInstance(props,auth);
		}
	}

	private String getString(LuanState luan,LuanTable params,String key) throws LuanException {
		Object val = params.get(key);
		if( val!=null && !(val instanceof String) )
			throw luan.exception( "parameter '"+key+"' must be a string" );
		return (String)val;
	}

	public LuanTable table() {
		LuanTable tbl = Luan.newTable();
		try {
			tbl.put( "send", new LuanJavaFunction(
				SmtpCon.class.getMethod( "send", LuanState.class, LuanTable.class ), this
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return tbl;
	}


	public void send(LuanState luan,LuanTable mailTbl) throws LuanException {
		try {
			MimeMessage msg = new MimeMessage(session);

			String from = getString(luan,mailTbl,"from");
			if( from != null )
				msg.setFrom(from);

			String to = getString(luan,mailTbl,"to");
			if( to != null )
				msg.setRecipients(Message.RecipientType.TO,to);

			String subject = getString(luan,mailTbl,"subject");
			if( subject != null )
				msg.setSubject(subject);

			Object body = mailTbl.get("body");
			if( body != null ) {
				if( body instanceof String ) {
					msg.setText((String)body);
				} else if( body instanceof LuanTable ) {
					LuanTable bodyTbl = (LuanTable)body;
					Map<Object,Object> map = new HashMap<Object,Object>(bodyTbl.asMap());
					MimeMultipart mp = new MimeMultipart("alternative");
					String text = (String)map.remove("text");
					if( text != null ) {
						MimeBodyPart part = new MimeBodyPart();
						part.setText(text);
						mp.addBodyPart(part);
					}
					String html = (String)map.remove("html");
					if( html != null ) {
						MimeBodyPart part = new MimeBodyPart();
						part.setContent(html,"text/html");
						mp.addBodyPart(part);
					}
					if( !map.isEmpty() )
						throw luan.exception( "invalid body types: " + map );
					msg.setContent(mp);
				} else
					throw luan.exception( "parameter 'body' is must be a string or table" );
			}

			Transport.send(msg);
		} catch(MessagingException e) {
			throw luan.exception(e);
		}
	}

}
