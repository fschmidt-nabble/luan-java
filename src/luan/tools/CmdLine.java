package luan.tools;

import java.io.Console;
import java.io.IOException;
import luan.lib.BasicLib;
import luan.Luan;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanTable;
import luan.LuanException;


public class CmdLine {

	public static void main(String[] args) {
		LuanState luan = LuanState.newStandard();
		try {
			LuanFunction standalone = (LuanFunction)luan.get("Basic.standalone");
			luan.JAVA.call(standalone,"standalone",args);
		} catch(LuanException e) {
//			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}

}
