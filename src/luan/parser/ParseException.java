package luan.parser;

import luan.LuanSource;


public class ParseException extends Exception {
	public final LuanSource src;
	public final int iCurrent;
	public final int iHigh;

	ParseException(String msg,LuanSource src,int iCurrent,int iHigh) {
		super(msg);
		this.src = src;
		this.iCurrent = iCurrent;
		this.iHigh = iHigh;
//System.out.println("iCurrent = "+iCurrent);
//System.out.println("iHigh = "+iHigh);
	}

	private class Location {
		final int line;
		final int pos;

		Location(int index) {
			int line = 0;
			int i = -1;
			while(true) {
				int j = src.text.indexOf('\n',i+1);
				if( j == -1 || j >= index )
					break;
				i = j;
				line++;
			}
			this.line = line;
			this.pos = index - i - 1;
		}
	}

	private String[] lines() {
		return src.text.split("\n",-1);
	}

	public String getFancyMessage() {
		Location loc = new Location(iCurrent);
		String line = lines()[loc.line];
		String msg = getMessage() +  " (line " + (loc.line+1) + ", pos " + (loc.pos+1) + ") in " + src.name + "\n";
		StringBuilder sb = new StringBuilder(msg);
		sb.append( line + "\n" );
		for( int i=0; i<loc.pos; i++ ) {
			sb.append( line.charAt(i)=='\t' ? '\t' : ' ' );
		}
		sb.append("^\n");
		return sb.toString();
	}
}
