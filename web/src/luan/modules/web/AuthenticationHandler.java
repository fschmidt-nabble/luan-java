package luan.modules.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.B64Code;


public class AuthenticationHandler extends AbstractHandler {
	private final String path;
	private String password = "password";

	public AuthenticationHandler(String path) {
		this.path = path;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
		throws IOException
	{
		if( !target.startsWith(path) )
			return;
		String pwd = getPassword(request);
		if( password.equals(pwd) )
			return;
		response.setHeader("WWW-Authenticate","Basic realm=\""+path+"\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		baseRequest.setHandled(true);
	}

	private static String getPassword(HttpServletRequest request) {
		String auth = request.getHeader("Authorization");
		if( auth==null )
			return null;
		String[] a = auth.split(" +");
		if( a.length != 2 )
			throw new RuntimeException("auth = "+auth);
		if( !a[0].equals("Basic") )
			throw new RuntimeException("auth = "+auth);
		auth = new String(B64Code.decode(a[1]));
		a = auth.split(":");
		if( a.length != 2 )
			throw new RuntimeException("auth = "+auth);
		return a[1];
	}
}
