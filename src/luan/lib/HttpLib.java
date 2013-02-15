package luan.lib;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanElement;
import luan.LuanException;
import luan.LuanTable;


public final class HttpLib {

	public static final String NAME = "http";
	public static final String FN_NAME = "serve_http";

	public static void service(LuanState luan,HttpServletRequest request,HttpServletResponse response)
		throws LuanException, IOException
	{
		LuanFunction fn = (LuanFunction)luan.global.get(FN_NAME);
		ServletOutputStream sout = response.getOutputStream();
		luan.out = new PrintStream(sout);

		LuanTable module = new LuanTable();
		luan.global.put(NAME,module);

		LuanTable parameters = new LuanTable();
		LuanTable parameter_lists = new LuanTable();
		@SuppressWarnings("unchecked")
		Map<String,String[]> paramMap = request.getParameterMap();
		for( Map.Entry<String,String[]> entry : paramMap.entrySet() ) {
			String name = entry.getKey();
			String[] values = entry.getValue();
			parameters.put(name,values[0]);
			parameter_lists.put( name, new LuanTable(Arrays.asList((Object[])values)) );
		}
		module.put("parameters",parameters);
		module.put("parameter_lists",parameter_lists);

		luan.call(fn,LuanElement.JAVA,FN_NAME);
	}
/*
	private final HttpServletRequest request;
	private final HttpServletResponse response;

	private HttpLib(HttpServletRequest request,HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}
*/
}
