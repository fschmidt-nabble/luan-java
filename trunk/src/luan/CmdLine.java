package luan;

import java.util.Arrays;
import java.util.Scanner;
import luan.lib.BasicLib;


public class CmdLine {

	public static void main(String[] args) throws Exception {
		LuaState lua = new LuaState();
		BasicLib.register(lua);
		boolean interactive = false;
		int i = 0;
		while( i < args.length ) {
			String arg = args[i];
			if( !arg.startsWith("-") )
				break;
			if( arg.equals("-i") ) {
				interactive = true;
			} else {
				throw new RuntimeException("invalid option: "+arg);
			}
			i++;
		}
		if( i == args.length ) {
			interactive = true;
		} else {
			String file = args[i++];
			Object[] varArgs = new Object[args.length-1];
			System.arraycopy(args,1,varArgs,0,varArgs.length);
			LuaTable argsTable = new LuaTable();
			for( int j=0; j<args.length; j++ ) {
				argsTable.set( new LuaNumber(j), args[j] );
			}
			lua.env().set("arg",argsTable);
			try {
				LuaFunction fn = BasicLib.loadFile(lua,file);
				fn.call(lua,varArgs);
			} catch(LuaException e) {
//				System.out.println(e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		if( interactive )
			interactive(lua);
	}

	static void interactive(LuaState lua) {
		while( true ) {
			System.out.print("> ");
			String input = new Scanner(System.in).nextLine();
			try {
				LuaFunction fn = BasicLib.load(lua,input);
				Object[] rtn = fn.call(lua);
				if( rtn.length > 0 )
					BasicLib.print(rtn);
			} catch(LuaException e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
