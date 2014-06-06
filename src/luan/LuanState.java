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
import luan.lib.BinaryLib;
import luan.lib.IoLib;


public abstract class LuanState implements DeepCloneable<LuanState> {
	public final LuanBit JAVA = bit(LuanElement.JAVA);

	private LuanTable global;
	private LuanTable loaded;
	private LuanTable preload;
	private LuanTable searchers;

	private final List<MetatableGetter> mtGetters;
	final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();

	protected LuanState() {
		global = new LuanTable();
		global.put("_G",global);
		loaded = new LuanTable();
		preload = new LuanTable();
		searchers = new LuanTable();
		mtGetters = new ArrayList<MetatableGetter>();
	}

	protected LuanState(LuanState luan) {
		mtGetters = new ArrayList<MetatableGetter>(luan.mtGetters);
	}

	public final LuanState deepClone() {
		return new DeepCloner().deepClone(this);
	}

	@Override public void deepenClone(LuanState clone,DeepCloner cloner) {
		clone.global = cloner.deepClone(global);
		clone.loaded = cloner.deepClone(loaded);
		clone.preload = cloner.deepClone(preload);
		clone.searchers = cloner.deepClone(searchers);
	}

	public abstract LuanTable currentEnvironment();

	public final LuanTable global() {
		return global;
	}

	public final LuanTable loaded() {
		return loaded;
	}

	public final LuanTable preload() {
		return preload;
	}

	public final LuanTable searchers() {
		return searchers;
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
		Object mod = PackageLib.require(this,modName);
		if( mod==null )
			throw new RuntimeException();
		global.put(modName,mod);
	}

	public static LuanState newStandard() {
		try {
			LuanState luan = LuanCompiler.newLuanState();
			BasicLib.load(luan);
			PackageLib.load(luan);
			MathLib.load(luan);
			StringLib.load(luan);
			TableLib.load(luan);
			HtmlLib.load(luan);
			BinaryLib.load(luan);
			IoLib.load(luan);
			BasicLib.do_java_resource(luan,"luan/lib/init.luan");
			JavaLib.load(luan);
			return luan;
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public final Object eval(String cmd,String sourceName,boolean interactive) throws LuanException {
		LuanFunction fn = BasicLib.load(this,cmd,sourceName,interactive);
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
