package luan;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import luan.impl.LuanCompiler;
import luan.modules.BasicLuan;
import luan.modules.PackageLuan;


public abstract class LuanState implements DeepCloneable<LuanState> {
	private final LuanBit JAVA = bit(LuanElement.JAVA);

	public LuanException exception(Object msg) {
		return JAVA.exception(msg);
	}

	public Object call(LuanFunction fn) throws LuanException {
		return call(fn,null,LuanFunction.NOTHING);
	}

	public Object call(LuanFunction fn,String fnName) throws LuanException {
		return call(fn,fnName,LuanFunction.NOTHING);
	}

	public Object call(LuanFunction fn,Object[] args) throws LuanException {
		return call(fn,null,args);
	}

	public Object call(LuanFunction fn,String fnName,Object[] args) throws LuanException {
		return JAVA.call(fn,fnName,args);
	}

	public LuanFunction checkFunction(Object obj) throws LuanException {
		return JAVA.checkFunction(obj);
	}

	public String toString(Object obj) throws LuanException {
		return JAVA.toString(obj);
	}

	public String repr(Object obj) throws LuanException {
		return JAVA.repr(obj);
	}

	public boolean isLessThan(Object o1,Object o2) throws LuanException {
		return JAVA.isLessThan(o1,o2);
	}



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

	public final void globalImport(String modName) {
		try {
			Object mod = PackageLuan.require(this,modName);
			global.put(modName,mod);
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public static LuanState newStandard() {
		try {
			LuanState luan = LuanCompiler.newLuanState();
			luan.globalImport("Package");
			BasicLuan.do_file(luan,"java:luan/init.luan");
			return luan;
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public final Object eval(String cmd) {
		return eval(cmd,new LuanTable());
	}

	public final Object eval(String cmd,LuanTable env) {
		try {
			LuanFunction fn = BasicLuan.load(this,cmd,"eval",env,true);
			return call(fn);
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
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
