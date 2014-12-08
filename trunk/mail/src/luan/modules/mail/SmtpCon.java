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

	public SmtpCon(LuanState luan,LuanTable paramsTbl) throws LuanException {
		Map<Object,Object> params = new HashMap<Object,Object>(paramsTbl.asMap());
		Properties props = new Properties(System.getProperties());

		String host = getString(luan,params,"host");
		if( host==null )
			throw luan.exception( "parameter 'host' is required" );
		props.setProperty("mail.smtp.host",host);

		Object port = params.remove("port");
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

		if( !params.isEmpty() )
			throw luan.exception( "unrecognized parameters: "+params );
	}

	private String getString(LuanState luan,Map<Object,Object> params,String key) throws LuanException {
		Object val = params.remove(key);
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
			Map<Object,Object> mailParams = new HashMap<Object,Object>(mailTbl.asMap());
			MimeMessage msg = new MimeMessage(session);

			String from = getString(luan,mailParams,"from");
			if( from != null )
				msg.setFrom(from);

			String to = getString(luan,mailParams,"to");
			if( to != null )
				msg.setRecipients(Message.RecipientType.TO,to);

			String cc = getString(luan,mailParams,"cc");
			if( cc != null )
				msg.setRecipients(Message.RecipientType.CC,cc);

			String subject = getString(luan,mailParams,"subject");
			if( subject != null )
				msg.setSubject(subject);

			Object body = mailParams.remove("body");
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

			if( !mailParams.isEmpty() )
				throw luan.exception( "unrecognized parameters: "+mailParams );

			Transport.send(msg);
		} catch(MessagingException e) {
			throw luan.exception(e);
		}
	}

}
