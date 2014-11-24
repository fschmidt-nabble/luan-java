package luan.modules.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import luan.LuanState;
import luan.LuanException;


public class LuanHandler extends AbstractHandler {
	private static final Logger logger = LoggerFactory.getLogger(LuanHandler.class);
	private final LuanState luan;
	private String welcomeFile = "index.html";

	public LuanHandler(LuanState luan) {
		this.luan = luan;
	}

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
		throws IOException
	{
		if( target.endsWith("/") )
			target += welcomeFile;
		try {
			if( !HttpServicer.service(luan,request,response,"site:"+target) )
				return;
			response.setStatus(HttpServletResponse.SC_OK);
		} catch(LuanException e) {
			String err = e.getFullMessage();
			logger.error(err);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,err);
		}
		baseRequest.setHandled(true);
	}

	public void setWelcomeFile(String welcomeFile) {
		this.welcomeFile = welcomeFile;
	}
}
