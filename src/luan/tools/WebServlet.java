package luan.tools;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import luan.LuanState;
import luan.LuanException;
import luan.LuanRuntimeException;
import luan.LuanFunction;
import luan.LuanElement;
import luan.DeepCloner;
import luan.interp.LuanCompiler;
import luan.lib.HttpLib;
import luan.lib.BasicLib;
import luan.lib.PackageLib;
import luan.lib.MathLib;
import luan.lib.StringLib;
import luan.lib.TableLib;
import luan.lib.HtmlLib;


public class WebServlet extends HttpServlet {

	protected LuanState luanState = null;

	protected LuanState newLuanState() throws LuanException {
		LuanState luan = LuanCompiler.newLuanState();
		BasicLib.load(luan);
		PackageLib.load(luan);
		MathLib.load(luan);
		StringLib.load(luan);
		TableLib.load(luan);
		HtmlLib.load(luan);
		return luan;
	}

	@Override protected void service(HttpServletRequest request,HttpServletResponse response)
		throws ServletException, IOException
	{
		try {
			synchronized(this) {
				if( luanState == null ) {
					luanState = newLuanState();
					HttpLib.load(luanState);
				}
			}
			LuanState luan = new DeepCloner().deepClone(luanState);
			service(request,response,luan);
		} catch(LuanException e) {
			throw new LuanRuntimeException(e);
		}
	}

	protected void service(HttpServletRequest request,HttpServletResponse response,LuanState luan)
		throws ServletException, IOException, LuanException
	{
		HttpLib.service(luan,request,response);
	}

}
