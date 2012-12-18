package luan;

import java.util.Arrays;
import java.util.Scanner;
import luan.lib.BasicLib;
import luan.interp.LuaCompiler;


public class CmdLine {

	public static void main(String[] args) throws Exception {
		LuaState lua = LuaCompiler.newLuaState();
		BasicLib.register(lua);
		boolean interactive = false;
		boolean showVersion = false;
		int i = 0;
		if( args.length == 0 ) {
			interactive = true;
			showVersion = true;
		} else {
			while( i < args.length ) {
				String arg = args[i];
				if( !arg.startsWith("-") || arg.equals("--") )
					break;
				if( arg.equals("-i") ) {
					interactive = true;
				} else if( arg.equals("-v") ) {
					showVersion = true;
				} else if( arg.equals("-e") ) {
					if( ++i == args.length )
						error("'-e' needs argument");
					String cmd = args[i];
					try {
						LuaFunction fn = BasicLib.load(lua,cmd);
						fn.call(lua);
					} catch(LuaException e) {
						System.err.println("command line error: "+e.getMessage());
						System.exit(-1);
					}
				} else if( arg.equals("-") ) {
					try {
						BasicLib.dofile(lua,null);
					} catch(LuaException e) {
						System.err.println(e.getMessage());
						System.exit(-1);
					}
					System.exit(0);
				} else {
					error("unrecognized option '"+arg+"'");
				}
				i++;
			}
		}
		if( showVersion )
			System.out.println(Lua.version);
		if( i < args.length ) {
			String file = args[i++];
			Object[] varArgs = new Object[args.length-1];
			System.arraycopy(args,1,varArgs,0,varArgs.length);
			LuaTable argsTable = new LuaTable();
			for( int j=0; j<args.length; j++ ) {
				argsTable.put( new LuaNumber(j), args[j] );
			}
			lua.global().put("arg",argsTable);
			try {
				LuaFunction fn = BasicLib.loadfile(lua,file);
				fn.call(lua,varArgs);
			} catch(LuaException e) {
//				System.err.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		if( interactive )
			interactive(lua);
	}

	private static void error(String msg) {
		System.err.println(msg);
		System.err.println("usage: java luan.CmdLine [options] [script [args]]");
		System.err.println("Available options are:");
		System.err.println("  -e stat  execute string 'stat'");
		System.err.println("  -i       enter interactive mode after executing 'script'");
//		System.err.println("  -l name  require library 'name'");
		System.err.println("  -v       show version information");
//		System.err.println("  -E       ignore environment variables");
		System.err.println("  --       stop handling options");
		System.err.println("  -        stop handling options and execute stdin");
		System.exit(-1);
	}

	static void interactive(LuaState lua) {
		while( true ) {
			System.out.print("> ");
			String input = new Scanner(System.in).nextLine();
			try {
				LuaFunction fn = BasicLib.load(lua,input);
				Object[] rtn = fn.call(lua);
				if( rtn.length > 0 )
					BasicLib.print(lua,rtn);
			} catch(LuaException e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
