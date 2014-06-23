package luan.modules.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import luan.LuanState;
import luan.LuanException;


public class LuanHandler extends AbstractHandler {
	private final LuanState luan;

	LuanHandler(LuanState luan) {
		this.luan = luan;
	}

	public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse response) 
		throws IOException
	{
		try {
			if( !HttpLuan.service(luan,request,response,target) )
				return;
			response.setStatus(HttpServletResponse.SC_OK);
		} catch(LuanException e) {
//e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
		}
		baseRequest.setHandled(true);
	}
}
