package luan.tools;

import java.io.IOException;
import luan.lib.BasicLib;
import luan.Luan;
import luan.LuanState;
import luan.LuanFunction;
import luan.LuanTable;
import luan.LuanException;
import jline.ConsoleReader;


public class CmdLine {

	public static void main(String[] args) {
		LuanState luan = LuanState.newStandard();
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
						LuanFunction fn = BasicLib.load(luan,cmd,"(command line)",false);
						luan.JAVA.call(fn,null);
					} catch(LuanException e) {
						System.err.println("command line error: "+e.getMessage());
						System.exit(-1);
					}
				} else if( arg.equals("-") ) {
					try {
						BasicLib.do_file(luan,"stdin");
					} catch(LuanException e) {
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
			System.out.println(Luan.version);
		if( i < args.length ) {
			String file = args[i++];
			Object[] varArgs = new Object[args.length-1];
			System.arraycopy(args,1,varArgs,0,varArgs.length);
			LuanTable argsTable = new LuanTable();
			for( int j=0; j<args.length; j++ ) {
				argsTable.put( j, args[j] );
			}
			luan.global().put("arg",argsTable);
			try {
				LuanFunction fn = BasicLib.load_file(luan,file);
				luan.JAVA.call(fn,null,varArgs);
			} catch(LuanException e) {
//				System.err.println("error: "+e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		if( interactive )
			interactive(luan);
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

	static void interactive(LuanState luan) {
		try {
			ConsoleReader console = new ConsoleReader();
			console.setDefaultPrompt("> ");
			while( true ) {
				String input = console.readLine();
				if( input==null )
					break;
				try {
					Object[] rtn = luan.eval(input,"stdin",true);
					if( rtn.length > 0 )
						BasicLib.print(luan,rtn);
				} catch(LuanException e) {
					System.out.println(e.getMessage());
				}
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

}
