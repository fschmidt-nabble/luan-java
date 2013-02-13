package luan;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import luan.interp.LuanCompiler;
import luan.lib.BasicLib;
import luan.lib.PackageLib;
import luan.lib.JavaLib;
import luan.lib.MathLib;
import luan.lib.StringLib;
import luan.lib.TableLib;
import luan.lib.HtmlLib;


public abstract class LuanState {

	public final LuanTable global = new LuanTable();
	public final LuanTable loaded = new LuanTable();
	public final LuanTable preload = new LuanTable();

	public InputStream in = System.in;
	public PrintStream out = System.out;
	public PrintStream err = System.err;

	private final List<MetatableGetter> mtGetters = new ArrayList<MetatableGetter>();
	final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();


	public Object get(String name) {
		String[] a = name.split("\\.");
		LuanTable t = global;
		for( int i=0; i<a.length-1; i++ ) {
			Object obj = t.get(a[i]);
			if( !(obj instanceof LuanTable) )
				return null;
			t = (LuanTable)obj;
		}
		return t.get(a[a.length-1]);
	}

	public Object set(String name,Object value) {
		String[] a = name.split("\\.");
		LuanTable t = global;
		for( int i=0; i<a.length-1; i++ ) {
			Object obj = t.get(a[i]);
			if( !(obj instanceof LuanTable) )
				return null;
			t = (LuanTable)obj;
		}
		return t.put(a[a.length-1],value);
	}

	public void load(LuanFunction loader,String modName) throws LuanException {
		Object mod = Luan.first(call(loader,LuanElement.JAVA,"loader",modName));
		if( mod == null )
			mod = true;
		loaded.put(modName,mod);
		if( mod instanceof LuanTable )
			global.put(modName,mod);
	}

	public static LuanState newStandard() {
		try {
			LuanState luan = LuanCompiler.newLuanState();
			luan.load(BasicLib.LOADER,BasicLib.NAME);
			luan.load(PackageLib.LOADER,PackageLib.NAME);
			luan.load(JavaLib.LOADER,JavaLib.NAME);
			luan.load(MathLib.LOADER,MathLib.NAME);
			luan.load(StringLib.LOADER,StringLib.NAME);
			luan.load(TableLib.LOADER,TableLib.NAME);
			luan.load(HtmlLib.LOADER,HtmlLib.NAME);
			return luan;
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public Object[] eval(String cmd,String sourceName) throws LuanException {
		LuanFunction fn = BasicLib.load(this,cmd,sourceName);
		return call(fn,null,null);
	}


	public final LuanTable getMetatable(Object obj) {
		if( obj instanceof LuanTable ) {
			LuanTable table = (LuanTable)obj;
			return table.getMetatable();
		}
		for( MetatableGetter mg : mtGetters ) {
			LuanTable table = mg.getMetatable(obj);
			if( table != null )
				return table;
		}
		return null;
	}

	public final void addMetatableGetter(MetatableGetter mg) {
		mtGetters.add(mg);
	}

	public Object[] call(LuanFunction fn,LuanElement calledFrom,String fnName,Object... args) throws LuanException {
		stackTrace.add( new StackTraceElement(calledFrom,fnName) );
		try {
			return fn.call(this,args);
		} finally {
			stackTrace.remove(stackTrace.size()-1);
		}
	}

	public final String checkString(LuanElement el,Object obj) throws LuanException {
		String s = Luan.asString(obj);
		if( s == null )
			throw new LuanException( this, el, "attempt to use a " + Luan.type(obj) + " as a string" );
		return s;
	}

	public final Number checkNumber(LuanElement el,Object obj) throws LuanException {
		Number n = Luan.toNumber(obj);
		if( n == null )
			throw new LuanException( this, el, "attempt to perform arithmetic on a " + Luan.type(obj) + " value" );
		return n;
	}

	public final LuanFunction checkFunction(LuanElement el,Object obj) throws LuanException {
		if( obj instanceof LuanFunction )
			return (LuanFunction)obj;
		throw new LuanException( this, el, "attempt to call a " + Luan.type(obj) + " value" );
	}

	public final String toString(LuanElement el,Object obj) throws LuanException {
		LuanFunction fn = getHandlerFunction(el,"__tostring",obj);
		if( fn != null )
			return checkString( el, Luan.first( call(fn,el,"__tostring",obj) ) );
		return Luan.toString(obj);
	}

	public final LuanFunction getHandlerFunction(LuanElement el,String op,Object obj) throws LuanException {
		Object f = getHandler(op,obj);
		if( f == null )
			return null;
		return checkFunction(el,f);
	}

	public final Object getHandler(String op,Object obj) {
		LuanTable t = getMetatable(obj);
		return t==null ? null : t.get(op);
	}


	public final LuanFunction getBinHandler(LuanElement el,String op,Object o1,Object o2) throws LuanException {
		LuanFunction f1 = getHandlerFunction(el,op,o1);
		if( f1 != null )
			return f1;
		return getHandlerFunction(el,op,o2);
	}

	public final boolean isLessThan(LuanElement el,Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() < n2.doubleValue();
		}
		if( o1 instanceof String && o2 instanceof String ) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2) < 0;
		}
		LuanFunction fn = getBinHandler(el,"__lt",o1,o2);
		if( fn != null )
			return Luan.toBoolean( Luan.first(call(fn,el,"__lt",o1,o2)) );
		throw new LuanException( this, el, "attempt to compare " + Luan.type(o1) + " with " + Luan.type(o2) );
	}
}
