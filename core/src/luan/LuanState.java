package luan;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import luan.impl.LuanCompiler;
import luan.modules.BasicLuan;
import luan.modules.PackageLuan;
import luan.modules.JavaLuan;
import luan.modules.StringLuan;
import luan.modules.BinaryLuan;


public abstract class LuanState implements DeepCloneable<LuanState> {

	final List<StackTraceElement> stackTrace = new ArrayList<StackTraceElement>();

	private LuanTableImpl registry;
	private LuanTableImpl global;
	private LuanTableImpl metatable;  // generic metatable

	protected LuanState() {
		registry = new LuanTableImpl();
		global = new LuanTableImpl();
		global.put("_G",global);
		global.put("java",JavaLuan.javaFn);
		metatable = newMetatable();
	}

	protected LuanState(LuanState luan) {}

	@Override public void deepenClone(LuanState clone,DeepCloner cloner) {
		clone.registry = cloner.deepClone(registry);
		clone.global = cloner.deepClone(global);
		clone.metatable = cloner.deepClone(metatable);
	}

	public abstract LuanTable currentEnvironment();
	public abstract LuanSource currentSource();

	public final LuanTable registry() {
		return registry;
	}

	public final LuanTable global() {
		return global;
	}

	public static LuanState newStandard() {
		try {
			LuanState luan = LuanCompiler.newLuanState();
			PackageLuan.require(luan,"luan:Basic");
			PackageLuan.require(luan,"luan:Io");
//			BasicLuan.do_file(luan,"classpath:luan/init.luan");
			return luan;
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	public final Object eval(String cmd) {
		return eval(cmd,new LuanTableImpl());
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
		if( obj==null )
			return null;
		if( obj instanceof LuanTable ) {
			LuanTable table = (LuanTable)obj;
			return table.getMetatable();
		}
		return metatable;
	}

	public final LuanBit bit(LuanElement el) {
		return new LuanBit(this,el);
	}

	public final Object getHandler(String op,Object obj) {
		LuanTable t = getMetatable(obj);
		return t==null ? null : t.get(op);
	}

	public final Object getHandler(String op,LuanTable table) {
		LuanTable t = table.getMetatable();
		return t==null ? null : t.get(op);
	}

	private static LuanTableImpl newMetatable() {
		LuanTableImpl metatable = new LuanTableImpl();
		try {
			metatable.put( "__index", new LuanJavaFunction(
				LuanState.class.getMethod("__index",LuanState.class,Object.class,Object.class), null
			) );
			metatable.put( "__newindex", new LuanJavaFunction(
				LuanState.class.getMethod("__newindex",LuanState.class,Object.class,Object.class,Object.class), null
			) );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return metatable;
	}

	public static Object __index(LuanState luan,Object obj,Object key) throws LuanException {
		if( obj instanceof String ) {
			Object rtn = StringLuan.__index(luan,(String)obj,key);
			if( rtn != null )
				return rtn;
		}
		if( obj instanceof byte[] ) {
			Object rtn = BinaryLuan.__index(luan,(byte[])obj,key);
			if( rtn != null )
				return rtn;
		}
		return JavaLuan.__index(luan,obj,key);
	}

	public static void __newindex(LuanState luan,Object obj,Object key,Object value) throws LuanException {
		JavaLuan.__newindex(luan,obj,key,value);
	}

	// convenience methods

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
}
