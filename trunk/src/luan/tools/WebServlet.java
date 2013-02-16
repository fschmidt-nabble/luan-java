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
		luan.load(BasicLib.LOADER,BasicLib.NAME);
		luan.load(PackageLib.LOADER,PackageLib.NAME);
		luan.load(MathLib.LOADER,MathLib.NAME);
		luan.load(StringLib.LOADER,StringLib.NAME);
		luan.load(TableLib.LOADER,TableLib.NAME);
		luan.load(HtmlLib.LOADER,HtmlLib.NAME);
	}

	protected void loadLuan(LuanState luan) throws LuanException {
		PackageLib.require(luan,HTTP_SERVER);
		Object fn = luan.global().get(HttpLib.FN_NAME);
		if( !(fn instanceof LuanFunction) )
			throw new LuanException( luan, LuanElement.JAVA, "function '"+HttpLib.FN_NAME+"' not defined" );
	}

	protected LuanState newLuanState() throws LuanException {
		LuanState luan = LuanCompiler.newLuanState();
		loadLibs(luan);
		loadLuan(luan);
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
