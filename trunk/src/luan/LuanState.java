package luan;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import luan.interp.LuanCompiler;
import luan.lib.BasicLib;
import luan.lib.PackageLib;
import luan.lib.JavaLib;
import luan.lib.MathLib;
import luan.lib.StringLib;
import luan.lib.TableLib;
import luan.lib.HtmlLib;


public abstract class LuanState implements DeepCloneable<LuanState> {
	public static final String _G = "_G";

	public final LuanBit JAVA = bit(LuanElement.JAVA);

	private LuanTable loaded;
	private LuanTable preload;
	private final List<String> defaultMods;

	public InputStream in = System.in;
	public PrintStream out = System.out;
	public PrintStream err = System.err;

	private final List<MetatableGetter> mtGetters;
	final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();

	protected LuanState() {
		loaded = new LuanTable();
		preload = new LuanTable();
		defaultMods = new ArrayList<String>();
		mtGetters = new ArrayList<MetatableGetter>();
	}

	protected LuanState(LuanState luan) {
		mtGetters = new ArrayList<MetatableGetter>(luan.mtGetters);
		defaultMods = new ArrayList<String>(luan.defaultMods);
	}

	public final LuanState deepClone() {
		return new DeepCloner().deepClone(this);
	}

	@Override public void deepenClone(LuanState clone,DeepCloner cloner) {
		clone.loaded = cloner.deepClone(loaded);
		clone.preload = cloner.deepClone(preload);
	}

	public abstract LuanTable currentEnvironment();

	public final LuanTable loaded() {
		return loaded;
	}

	public final LuanTable preload() {
		return preload;
	}

	public final Object get(String name) {
		String[] a = name.split("\\.");
		LuanTable t = loaded;
		for( int i=0; i<a.length-1; i++ ) {
			Object obj = t.get(a[i]);
			if( !(obj instanceof LuanTable) )
				return null;
			t = (LuanTable)obj;
		}
		return t.get(a[a.length-1]);
	}

	public final Object set(String name,Object value) {
		String[] a = name.split("\\.");
		LuanTable t = loaded;
		for( int i=0; i<a.length-1; i++ ) {
			Object obj = t.get(a[i]);
			if( !(obj instanceof LuanTable) )
				return null;
			t = (LuanTable)obj;
		}
		return t.put(a[a.length-1],value);
	}

	public final void load(String modName,LuanFunction loader) throws LuanException {
		preload.put(modName,loader);
		defaultMods.add(modName);
		PackageLib.require(this,modName);
	}

	public final LuanTable newEnvironment() throws LuanException {
		LuanTable env = new LuanTable();
		for( String modName : defaultMods ) {
			PackageLib.require(this,modName,env);
			LuanTable mod = (LuanTable)loaded.get(modName);
			LuanTable global = (LuanTable)mod.get(_G);
			if( global != null ) {
				for( Map.Entry<Object,Object> entry : global ) {
					env.put( entry.getKey(), entry.getValue() );
				}
			}
		}
		return env;
	}

	public static LuanState newStandard() {
		try {
			LuanState luan = LuanCompiler.newLuanState();
			luan.load(BasicLib.NAME,BasicLib.LOADER);
			luan.load(PackageLib.NAME,PackageLib.LOADER);
			luan.load(JavaLib.NAME,JavaLib.LOADER);
			luan.load(MathLib.NAME,MathLib.LOADER);
			luan.load(StringLib.NAME,StringLib.LOADER);
			luan.load(TableLib.NAME,TableLib.LOADER);
			luan.load(HtmlLib.NAME,HtmlLib.LOADER);
			return luan;
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public final Object[] eval(String cmd,String sourceName,LuanTable env) throws LuanException {
		LuanFunction fn = BasicLib.load(this,cmd,sourceName,env);
		return JAVA.call(fn,null);
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

	public final LuanBit bit(LuanElement el) {
		return new LuanBit(this,el);
	}

	public final Object getHandler(String op,Object obj) {
		LuanTable t = getMetatable(obj);
		return t==null ? null : t.get(op);
	}

}
