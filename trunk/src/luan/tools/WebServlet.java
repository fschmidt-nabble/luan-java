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
import luan.interp.LuanCompiler;
import luan.lib.HttpLib;
import luan.lib.BasicLib;
import luan.lib.PackageLib;
import luan.lib.MathLib;
import luan.lib.StringLib;
import luan.lib.TableLib;
import luan.lib.HtmlLib;


public class WebServlet extends HttpServlet {

	public static final String HTTP_SERVER = "http_server";

	protected LuanState luanState = null;

	protected void loadLibs(LuanState luan) throws LuanException {
		luan.load(BasicLib.NAME,BasicLib.LOADER);
		luan.load(PackageLib.NAME,PackageLib.LOADER);
		luan.load(MathLib.NAME,MathLib.LOADER);
		luan.load(StringLib.NAME,StringLib.LOADER);
		luan.load(TableLib.NAME,TableLib.LOADER);
		luan.load(HtmlLib.NAME,HtmlLib.LOADER);
	}

	protected LuanState newLuanState() throws LuanException {
		LuanState luan = LuanCompiler.newLuanState();
		loadLibs(luan);
		PackageLib.require(luan,HTTP_SERVER);
		Object fn = luan.global().get(HttpLib.FN_NAME);
		if( !(fn instanceof LuanFunction) )
			throw new LuanException( luan, LuanElement.JAVA, "function '"+HttpLib.FN_NAME+"' not defined" );
		return luan;
	}

	protected  LuanState getLuanState(HttpServletRequest request) throws LuanException {
		synchronized(this) {
			if( luanState == null )
				luanState = newLuanState();
		}
		return luanState.deepClone();
	}

	protected void service(HttpServletRequest request,HttpServletResponse response)
		throws ServletException, IOException
	{
		try {
			LuanState luan = getLuanState(request);
			HttpLib.service(luan,request,response);
		} catch(LuanException e) {
			throw new LuanRuntimeException(e);
		}
	}

}
