package luan.interp;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import org.parboiled.BaseParser;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.parboiled.support.Var;
import org.parboiled.support.StringVar;
import org.parboiled.support.StringBuilderVar;
import org.parboiled.support.ValueStack;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.annotations.Cached;
import luan.Luan;
import luan.LuanState;
import luan.LuanSource;


class LuanParser extends BaseParser<Object> {

	LuanSource source;

	LuanSource.Element se(int start) {
		return new LuanSource.Element(source,start,currentIndex());
	}

	static final String _ENV = "_ENV";

	static final class Frame {
		final Frame parent;
		final List<String> symbols = new ArrayList<String>();
		int stackSize = 0;
		int loops = 0;
		boolean isVarArg = false;
		final List<String> upValueSymbols = new ArrayList<String>();
		final List<UpValue.Getter> upValueGetters = new ArrayList<UpValue.Getter>();

		Frame() {
			this.parent = null;
			upValueSymbols.add(_ENV);
			upValueGetters.add(UpValue.globalGetter);
		}

		Frame(Frame parent) {
			this.parent = parent;
		}

		int stackIndex(String name) {
			int i = symbols.size();
			while( --i >= 0 ) {
				if( symbols.get(i).equals(name) )
					return i;
			}
			return -1;
		}

		int upValueIndex(String name) {
			int i = upValueSymbols.size();
			while( --i >= 0 ) {
				if( upValueSymbols.get(i).equals(name) )
					return i;
			}
			if( parent==null )
				return -1;
			i = parent.stackIndex(name);
			if( i != -1 ) {
				upValueGetters.add(new UpValue.StackGetter(i));
			} else {
				i = parent.upValueIndex(name);
				if( i == -1 )
					return -1;
				upValueGetters.add(new UpValue.NestedGetter(i));
			}
			upValueSymbols.add(name);
			return upValueSymbols.size() - 1;
		}
	}

	static final UpValue.Getter[] NO_UP_VALUE_GETTERS = new UpValue.Getter[0];

	int nEquals;
	Frame frame = new Frame();

	boolean nEquals(int n) {
		nEquals = n;
		return true;
	}

	List<String> symbols() {
		return frame.symbols;
	}

	int symbolsSize() {
		return frame.symbols.size();
	}

	boolean addSymbol(String name) {
		frame.symbols.add(name);
		if( frame.stackSize < symbolsSize() )
			frame.stackSize = symbolsSize();
		return true;
	}

	boolean addSymbols(List<String> names) {
		frame.symbols.addAll(names);
		if( frame.stackSize < symbolsSize() )
			frame.stackSize = symbolsSize();
		return true;
	}

	int stackIndex(String name) {
		return frame.stackIndex(name);
	}

	boolean popSymbols(int n) {
		List<String> symbols = frame.symbols;
		while( n-- > 0 ) {
			symbols.remove(symbols.size()-1);
		}
		return true;
	}

	int upValueIndex(String name) {
		return frame.upValueIndex(name);
	}

	boolean incLoops() {
		frame.loops++;
		return true;
	}

	boolean decLoops() {
		frame.loops--;
		return true;
	}

	Chunk newChunk(int start) {
		return new Chunk( se(start), (Stmt)pop(), frame.stackSize, symbolsSize(), frame.isVarArg, frame.upValueGetters.toArray(NO_UP_VALUE_GETTERS) );
	}

	Rule Target() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			Spaces(false),
			start.set(currentIndex()),
			FirstOf(
				Sequence(
					ExpList(false),
					push( new ReturnStmt( se(start.get()), (Expressions)pop() ) ),
					push( newChunk(start.get()) ),
					EOI
				),
				Sequence(
					action( frame.isVarArg = true ),
					Block(),
					push( newChunk(start.get()) ),
					EOI
				)
			)
		);
	}

	Rule Block() {
		Var<List<Stmt>> stmts = new Var<List<Stmt>>(new ArrayList<Stmt>());
		Var<Integer> stackStart = new Var<Integer>(symbolsSize());
		return Sequence(
			Optional( Stmt(stmts) ),
			ZeroOrMore(
				StmtSep(stmts),
				Optional( Stmt(stmts) )
			),
			push( newBlock(stmts.get(),stackStart.get()) )
		);
	}

	Stmt newBlock(List<Stmt> stmts,int stackStart) {
		int stackEnd = symbolsSize();
		popSymbols( stackEnd - stackStart );
		if( stmts.isEmpty() )
			return Stmt.EMPTY;
		if( stmts.size()==1 && stackStart==stackEnd )
			return stmts.get(0);
		return new Block( stmts.toArray(new Stmt[0]), stackStart, stackEnd );
	}

	Rule StmtSep(Var<List<Stmt>> stmts) {
		return Sequence(
			FirstOf(
				';',
				Sequence(
					Optional( "--", ZeroOrMore(NoneOf("\r\n")) ),
					EndOfLine()
				),
				Sequence(
					OutputStmt(),
					stmts.get().add( (Stmt)pop() )
				)
			),
			Spaces(false)
		);
	}

	Rule EndOfLine() {
		return FirstOf("\r\n", '\r', '\n');
	}

	Rule Stmt(Var<List<Stmt>> stmts) {
		return FirstOf(
			LocalStmt(stmts),
			Sequence(
				FirstOf(
					ReturnStmt(),
					FunctionStmt(),
					LocalFunctionStmt(),
					BreakStmt(),
					GenericForStmt(),
					NumericForStmt(),
					TryStmt(),
					DoStmt(),
					WhileStmt(),
					RepeatStmt(),
					IfStmt(),
					SetStmt(),
					ExpressionsStmt()
				),
				stmts.get().add( (Stmt)pop() )
			)
		);
	}

	Rule OutputStmt() {
		Var<Integer> start = new Var<Integer>();
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>(new ExpList.Builder());
		return Sequence(
			start.set(currentIndex()),
			"%>", Optional( EndOfLine() ),
			ZeroOrMore(
				FirstOf(
					Sequence(
						OneOrMore(
							TestNot("<%"),
							ANY
						),
						addToExpList(builder.get(),new ConstExpr(match()))
					),
					Sequence(
						"<%=", Spaces(false),
						Expr(false),
						addToExpList(builder.get()),
						"%>"
					)
				)
			),
			"<%", Spaces(false),
			push( new OutputStmt( se(start.get()), builder.get().build() ) )
		);
	}

	Rule ReturnStmt() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Keyword("return",false), Expressions(false),
			push( new ReturnStmt( se(start.get()), (Expressions)pop() ) )
		);
	}

	Rule FunctionStmt() {
		return Sequence(
			Keyword("function",false), FnName(), Function(false),
			push( new SetStmt( (Settable)pop(1), expr(pop()) ) )
		);
	}

	Rule FnName() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			push(null),  // marker
			Name(false),
			ZeroOrMore(
				'.', Spaces(false),
				makeVarExp(start.get()),
				NameExpr(false)
			),
			makeSettableVar(start.get())
		);
	}

	Rule LocalFunctionStmt() {
		return Sequence(
			Keyword("local",false), Keyword("function",false),
			Name(false),
			addSymbol( (String)pop() ),
			Function(false),
			push( new SetStmt( new SetLocalVar(symbolsSize()-1), expr(pop()) ) )
		);
	}

	Rule BreakStmt() {
		return Sequence(
			Keyword("break",false),
			frame.loops > 0,
			push( new BreakStmt() )
		);
	}

	Rule GenericForStmt() {
		Var<Integer> start = new Var<Integer>();
		Var<Integer> stackStart = new Var<Integer>(symbolsSize());
		Var<List<String>> names = new Var<List<String>>(new ArrayList<String>());
		return Sequence(
			start.set(currentIndex()),
			Keyword("for",false), NameList(false,names), Keyword("in",false), Expr(false), Keyword("do",false),
			addSymbols(names.get()),
			LoopBlock(), Keyword("end",false),
			push( new GenericForStmt( se(start.get()), stackStart.get(), symbolsSize() - stackStart.get(), expr(pop(1)), (Stmt)pop() ) ),
			popSymbols( symbolsSize() - stackStart.get() )
		);
	}

	Rule NumericForStmt() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Keyword("for",false), Name(false), '=', Spaces(false), Expr(false), Keyword("to",false), Expr(false),
			push( new ConstExpr(1) ),  // default step
			Optional(
				Keyword("step",false),
				drop(),
				Expr(false)
			),
			addSymbol( (String)pop(3) ),  // add "for" var to symbols
			Keyword("do",false), LoopBlock(), Keyword("end",false),
			push( new NumericForStmt( se(start.get()), symbolsSize()-1, expr(pop(3)), expr(pop(2)), expr(pop(1)), (Stmt)pop() ) ),
			popSymbols(1)
		);
	}

	Rule TryStmt() {
		return Sequence(
			Keyword("try",false), Block(),
			Keyword("catch",false), Name(false), addSymbol( (String)pop() ),
			Keyword("do",false), Block(), Keyword("end",false),
			push( new TryStmt( (Stmt)pop(1), symbolsSize()-1, (Stmt)pop() ) ),
			popSymbols(1)
		);
	}

	Rule DoStmt() {
		return Sequence(
			Keyword("do",false), Block(), Keyword("end",false)
		);
	}

	Rule LocalStmt(Var<List<Stmt>> stmts) {
		Var<List<String>> names = new Var<List<String>>(new ArrayList<String>());
		return Sequence(
			Keyword("local",false), NameList(false,names),
			Optional(
				'=', Spaces(false), ExpList(false),
				stmts.get().add( newSetLocalStmt(names.get().size()) )
			),
			addSymbols(names.get())
		);
	}

	Rule NameList(boolean inParens,Var<List<String>> names) {
		return Sequence(
			Name(inParens),
			names.get().add( (String)pop() ),
			ZeroOrMore(
				',', Spaces(inParens), Name(inParens),
				names.get().add( (String)pop() )
			)
		);
	}

	SetStmt newSetLocalStmt(int nVars) {
		Expressions values = (Expressions)pop();
		SetLocalVar[] vars = new SetLocalVar[nVars];
		int stackStart = symbolsSize();
		for( int i=0; i<vars.length; i++ ) {
			vars[i] = new SetLocalVar(stackStart+i);
		}
		return new SetStmt( vars, values );
	}

	Rule WhileStmt() {
		return Sequence(
			Keyword("while",false), Expr(false), Keyword("do",false), LoopBlock(), Keyword("end",false),
			push( new WhileStmt( expr(pop(1)), (Stmt)pop() ) )
		);
	}

	Rule RepeatStmt() {
		return Sequence(
			Keyword("repeat",false), LoopBlock(), Keyword("until",false), Expr(false),
			push( new RepeatStmt( (Stmt)pop(1), expr(pop()) ) )
		);
	}

	Rule LoopBlock() {
		return Sequence( incLoops(), Block(), decLoops() );
	}

	Rule IfStmt() {
		Var<Integer> n = new Var<Integer>(1);
		return Sequence(
			Keyword("if",false), Expr(false), Keyword("then",false), Block(),
			push(Stmt.EMPTY),
			ZeroOrMore(
				Keyword("elseif",false), drop(), Expr(false), Keyword("then",false), Block(),
				push(Stmt.EMPTY),
				n.set(n.get()+1)
			),
			Optional(
				Keyword("else",false), drop(), Block()
			),
			Keyword("end",false),
			buildIfStmt(n.get())
		);
	}

	boolean buildIfStmt(int n) {
		while( n-- > 0 ) {
			Stmt elseStmt = (Stmt)pop();
			Stmt thenStmt = (Stmt)pop();
			Expr cnd = expr(pop());
			push( new IfStmt(cnd,thenStmt,elseStmt) );
		}
		return true;
	}

	Rule SetStmt() {
		return Sequence(
			VarList(),
			'=', Spaces(false),
			ExpList(false),
			push( newSetStmt() )
		);
	}

	Rule ExpressionsStmt() {
		return Sequence(
			ExpList(false),
			push( new ExpressionsStmt((Expressions)pop()) )
		);
	}

	SetStmt newSetStmt() {
		Expressions values = (Expressions)pop();
		@SuppressWarnings("unchecked")
		List<Settable> vars = (List<Settable>)pop();
		return new SetStmt( vars.toArray(new Settable[0]), values );
	}

	Rule VarList() {
		Var<List<Settable>> vars = new Var<List<Settable>>(new ArrayList<Settable>());
		return Sequence(
			SettableVar(),
			vars.get().add( (Settable)pop() ),
			ZeroOrMore(
				',', Spaces(false), SettableVar(),
				vars.get().add( (Settable)pop() )
			),
			push(vars.get())
		);
	}

	Rule SettableVar() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Var(false),
			makeSettableVar(start.get())
		);
	}

	boolean makeSettableVar(int start) {
		Object obj2 = pop();
		if( obj2==null )
			return false;
		Object obj1 = pop();
		if( obj1!=null ) {
			Expr key = expr(obj2);
			Expr table = expr(obj1);
			return push( new SetTableEntry(se(start),table,key) );
		}
		String name = (String)obj2;
		int index = stackIndex(name);
		if( index != -1 )
			return push( new SetLocalVar(index) );
		index = upValueIndex(name);
		if( index != -1 )
			return push( new SetUpVar(index) );
		return push( new SetTableEntry( se(start), env(), new ConstExpr(name) ) );
	}

	@Cached
	Rule Expr(boolean inParens) {
		return FirstOf(
			VarArgs(inParens),
			OrExpr(inParens)
		);
	}

	@Cached
	Rule OrExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			AndExpr(inParens),
			ZeroOrMore( Keyword("or",inParens), AndExpr(inParens), push( new OrExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	@Cached
	Rule AndExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			RelExpr(inParens),
			ZeroOrMore( Keyword("and",inParens), RelExpr(inParens), push( new AndExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	@Cached
	Rule RelExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			ConcatExpr(inParens),
			ZeroOrMore(
				FirstOf(
					Sequence( "==", Spaces(inParens), ConcatExpr(inParens), push( new EqExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( "~=", Spaces(inParens), ConcatExpr(inParens), push( new NotExpr(se(start.get()),new EqExpr(se(start.get()),expr(pop(1)),expr(pop()))) ) ),
					Sequence( "<=", Spaces(inParens), ConcatExpr(inParens), push( new LeExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( ">=", Spaces(inParens), ConcatExpr(inParens), push( new LeExpr(se(start.get()),expr(pop()),expr(pop())) ) ),
					Sequence( "<", Spaces(inParens), ConcatExpr(inParens), push( new LtExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( ">", Spaces(inParens), ConcatExpr(inParens), push( new LtExpr(se(start.get()),expr(pop()),expr(pop())) ) )
				)
			)
		);
	}

	@Cached
	Rule ConcatExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			SumExpr(inParens),
			Optional( "..", Spaces(inParens), ConcatExpr(inParens), push( new ConcatExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	@Cached
	Rule SumExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			TermExpr(inParens),
			ZeroOrMore(
				FirstOf(
					Sequence( '+', Spaces(inParens), TermExpr(inParens), push( new AddExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( '-', TestNot('-'), Spaces(inParens), TermExpr(inParens), push( new SubExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	@Cached
	Rule TermExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			UnaryExpr(inParens),
			ZeroOrMore(
				FirstOf(
					Sequence( '*', Spaces(inParens), UnaryExpr(inParens), push( new MulExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( '/', Spaces(inParens), UnaryExpr(inParens), push( new DivExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( '%', Spaces(inParens), UnaryExpr(inParens), push( new ModExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	@Cached
	Rule UnaryExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			FirstOf(
				Sequence( '#', Spaces(inParens), PowExpr(inParens), push( new LenExpr(se(start.get()),expr(pop())) ) ),
				Sequence( '-', TestNot('-'), Spaces(inParens), PowExpr(inParens), push( new UnmExpr(se(start.get()),expr(pop())) ) ),
				Sequence( Keyword("not",inParens), PowExpr(inParens), push( new NotExpr(se(start.get()),expr(pop())) ) ),
				PowExpr(inParens)
			)
		);
	}

	@Cached
	Rule PowExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			SingleExpr(inParens),
			Optional( '^', Spaces(inParens), PowExpr(inParens), push( new PowExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	@Cached
	Rule SingleExpr(boolean inParens) {
		return FirstOf(
			FunctionExpr(inParens),
			TableExpr(inParens),
			VarExp(inParens),
			Literal(inParens)
		);
	}

	@Cached
	Rule FunctionExpr(boolean inParens) {
		return Sequence( "function", Spaces(inParens), Function(inParens) );
	}

	@Cached
	Rule Function(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		Var<List<String>> names = new Var<List<String>>(new ArrayList<String>());
		return Sequence(
			start.set(currentIndex()),
			'(', Spaces(true),
			action( frame = new Frame(frame) ),
			Optional(
				FirstOf(
					Sequence(
						NameList(true,names), addSymbols(names.get()),
						Optional( ',', Spaces(true), VarArgName() )
					),
					VarArgName()
				)
			),
			')', Spaces(inParens), Block(), Keyword("end",inParens),
			push( newChunk(start.get()) ),
			action( frame = frame.parent )
		);
	}

	Rule VarArgName() {
		return Sequence(
			"...", Spaces(true),
			action( frame.isVarArg = true )
		);
	}

	@Cached
	Rule VarArgs(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			"...", Spaces(inParens),
			frame.isVarArg,
			push( new VarArgs(se(start.get())) )
		);
	}

	@Cached
	Rule TableExpr(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		Var<List<TableExpr.Field>> fields = new Var<List<TableExpr.Field>>(new ArrayList<TableExpr.Field>());
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>(new ExpList.Builder());
		return Sequence(
			start.set(currentIndex()),
			'{', Spaces(true),
			Optional(
				Field(fields,builder),
				ZeroOrMore(
					FieldSep(),
					Field(fields,builder)
				),
				Optional( FieldSep() )
			),
			'}',
			Spaces(inParens),
			push( new TableExpr( se(start.get()), fields.get().toArray(new TableExpr.Field[0]), builder.get().build() ) )
		);
	}

	Rule FieldSep() {
		return Sequence( AnyOf(",;"), Spaces(true) );
	}

	Rule Field(Var<List<TableExpr.Field>> fields,Var<ExpList.Builder> builder) {
		return FirstOf(
			Sequence(
				FirstOf( SubExpr(true), NameExpr(true) ),
				'=', Spaces(true), Expr(true),
				fields.get().add( new TableExpr.Field( expr(pop(1)), expr(pop()) ) )
			),
			Sequence(
				Expr(true),
				addToExpList(builder.get())
			)
		);
	}

	static Expr expr(Object obj) {
		if( obj instanceof Expressions )
			return new ExpressionsExpr((Expressions)obj);
		return (Expr)obj;
	}

	@Cached
	Rule VarExp(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Var(inParens),
			makeVarExp(start.get())
		);
	}

	@Cached
	Rule Var(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>();
		return Sequence(
			start.set(currentIndex()),
			VarStart(inParens),
			ZeroOrMore(
				makeVarExp(start.get()),
				FirstOf(
					VarExt(inParens),
					Sequence(
						builder.set(new ExpList.Builder()),
						Args(inParens,start,builder)
					),
					Sequence(
						"->", Spaces(inParens),
						builder.set(new ExpList.Builder()),
						addToExpList(builder.get()),
						VarExpB(inParens),
						Args(inParens,start,builder)
					)
				)
			)
		);
	}

	@Cached
	Rule VarExpB(boolean inParens) {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			VarStart(inParens),
			ZeroOrMore(
				makeVarExp(start.get()),
				VarExt(inParens)
			),
			makeVarExp(start.get())
		);
	}

	@Cached
	Rule VarExt(boolean inParens) {
		return FirstOf(
			SubExpr(inParens),
			Sequence( '.', Spaces(inParens), NameExpr(inParens) )
		);
	}

	@Cached
	Rule VarStart(boolean inParens) {
		return FirstOf(
			Sequence(
				'(', Spaces(true), Expr(true), ')', Spaces(inParens),
				push(expr(pop())),
				push(null)  // marker
			),
			Sequence(
				push(null),  // marker
				Name(inParens)
			)
		);
	}

	Expr env() {
		int index = stackIndex(_ENV);
		if( index != -1 )
			return new GetLocalVar(null,index);
		index = upValueIndex(_ENV);
		if( index != -1 )
			return new GetUpVar(null,index);
		throw new RuntimeException("_ENV not found");
	}

	boolean makeVarExp(int start) {
		Object obj2 = pop();
		if( obj2==null )
			return true;
		Object obj1 = pop();
		if( obj1 != null )
			return push( new IndexExpr( se(start), expr(obj1), expr(obj2) ) );
		String name = (String)obj2;
		int index = stackIndex(name);
		if( index != -1 )
			return push( new GetLocalVar(se(start),index) );
		index = upValueIndex(name);
		if( index != -1 )
			return push( new GetUpVar(se(start),index) );
		return push( new IndexExpr( se(start), env(), new ConstExpr(name) ) );
	}

	// function should be on top of the stack
	Rule Args(boolean inParens,Var<Integer> start,Var<ExpList.Builder> builder) {
		return Sequence(
			FirstOf(
				Sequence(
					'(', Spaces(true), Optional(ExpList(true,builder)), ')', Spaces(inParens)
				),
				Sequence(
					TableExpr(inParens),
					addToExpList(builder.get())
				),
				Sequence(
					StringLiteral(inParens),
					push(new ConstExpr(pop())),
					addToExpList(builder.get())
				)
			),
			push( new FnCall( se(start.get()), expr(pop()), builder.get().build() ) ),
			push(null)  // marker
		);
	}

	@Cached
	Rule Expressions(boolean inParens) {
		return FirstOf(
			ExpList(inParens),
			push( ExpList.emptyExpList )
		);
	}

	@Cached
	Rule ExpList(boolean inParens) {
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>(new ExpList.Builder());
		return Sequence(
			ExpList(inParens,builder),
			push( builder.get().build() )
		);
	}

	Rule ExpList(boolean inParens,Var<ExpList.Builder> builder) {
		return Sequence(
			Expr(inParens),
			addToExpList(builder.get()),
			ZeroOrMore(
				',', Spaces(inParens), Expr(inParens),
				addToExpList(builder.get())
			)
		);
	}

	boolean addToExpList(ExpList.Builder bld) {
		Object obj = pop();
		if( obj instanceof Expressions ) {
			bld.add( (Expressions)obj );
		} else {
			bld.add( (Expr)obj );
		}
		return true;
	}

	boolean addToExpList(ExpList.Builder bld, Expr expr) {
		bld.add(expr);
		return true;
	}

	@Cached
	Rule SubExpr(boolean inParens) {
		return Sequence( '[', Spaces(true), Expr(true), ']', Spaces(inParens) );
	}

	@Cached
	Rule NameExpr(boolean inParens) {
		return Sequence(
			Name(inParens),
			push( new ConstExpr((String)pop()) )
		);
	}

	@Cached
	Rule Name(boolean inParens) {
		return Sequence(
			Sequence(
				NameFirstChar(),
				ZeroOrMore( NameChar() )
			),
			!keywords.contains(match()),
			push(match()),
			Spaces(inParens)
		);
	}

	Rule NameChar() {
		return FirstOf( NameFirstChar(), Digit() );
	}

	Rule NameFirstChar() {
		return FirstOf(
			CharRange('a', 'z'),
			CharRange('A', 'Z'),
			'_'
		);
	}

	Rule Keyword(String keyword,boolean inParens) {
		return Sequence(
			keyword,
			TestNot( NameChar() ),
			Spaces(inParens)
		);
	}

	static final Set<String> keywords = new HashSet<String>(Arrays.asList(
		"and",
		"break",
		"catch",
		"do",
		"else",
		"elseif",
		"end",
		"false",
		"for",
		"function",
		"goto",
		"if",
		"in",
		"local",
		"nil",
		"not",
		"or",
		"repeat",
		"return",
		"step",
		"then",
		"to",
		"true",
		"try",
		"until",
		"while"
	));

	@Cached
	Rule Literal(boolean inParens) {
		return Sequence(
			FirstOf(
				NilLiteral(inParens),
				BooleanLiteral(inParens),
				NumberLiteral(inParens),
				StringLiteral(inParens)
			),
			push(new ConstExpr(pop()))
		);
	}

	@Cached
	Rule NilLiteral(boolean inParens) {
		return Sequence( Keyword("nil",inParens), push(null) );
	}

	@Cached
	Rule BooleanLiteral(boolean inParens) {
		return FirstOf(
			Sequence( Keyword("true",inParens), push(true) ),
			Sequence( Keyword("false",inParens), push(false) )
		);
	}

	@Cached
	Rule NumberLiteral(boolean inParens) {
		return Sequence(
			FirstOf(
				Sequence(
					IgnoreCase("0x"),
					HexNumber()
				),
				Sequence(
					DecNumber(),
					push(Double.valueOf(match()))
				)
			),
			TestNot( NameChar() ),
			Spaces(inParens)
		);
	}

	Rule DecNumber() {
		return FirstOf(
			Sequence(
				Int(),
				Optional( '.', Optional(Int()) ),
				Exponent()
			),
			Sequence( '.', Int(), Exponent() )
		);
	}

	Rule Exponent() {
		return Optional(
			IgnoreCase('e'),
			Optional(AnyOf("+-")),
			Int()
		);
	}

	Rule Int() {
		return OneOrMore(Digit());
	}

	Rule Digit() {
		return CharRange('0', '9');
	}

	Rule HexNumber() {
		return FirstOf(
			Sequence(
				HexInt(),
				push( (double)Long.parseLong(match(),16) ),
				Optional( '.', Optional(HexDec()) ),
				HexExponent()
			),
			Sequence( push(0.0), '.', HexDec(), HexExponent() )
		);
	}

	Rule HexDec() {
		return Sequence(
			HexInt(),
			push( (Double)pop() + (double)Long.parseLong(match(),16) / Math.pow(16,matchLength()) )
		);
	}

	Rule HexExponent() {
		return Optional(
			IgnoreCase('p'),
			Sequence(
				Optional(AnyOf("+-")),
				HexInt()
			),
			push( (Double)pop() * Math.pow(2,(double)Long.parseLong(match())) )
		);
	}

	Rule HexInt() {
		return OneOrMore(Digit());
	}


	Rule HexDigit() {
		return FirstOf(
			Digit(),
			AnyOf("abcdefABCDEF")
		);
	}

	@Cached
	Rule StringLiteral(boolean inParens) {
		return Sequence(
			FirstOf(
				QuotedString('"'),
				QuotedString('\''),
				LongString()
			),
			Spaces(inParens)
		);
	}

	Rule LongString() {
		return Sequence(
			'[',
			ZeroOrMore('='),
			nEquals(matchLength()),
			'[',
			ZeroOrMore(
				TestNot(LongBracketsEnd()),
				ANY
			),
			push( match() ),
			LongBracketsEnd()
		);
	}

	Rule QuotedString(char quote) {
		StringBuilderVar buf = new StringBuilderVar();
		return Sequence(
			quote,
			ZeroOrMore(
				FirstOf(
					Sequence(
						NoneOf("\\\n"+quote),
						buf.append(matchedChar())
					),
					EscSeq(buf)
				)
			),
			quote,
			push( buf.getString() )
		);
	}

	Rule EscSeq(StringBuilderVar buf) {
		return Sequence(
			'\\',
			FirstOf(
				Sequence( 'a', buf.append('\u0007') ),
				Sequence( 'b', buf.append('\b') ),
				Sequence( 'f', buf.append('\f') ),
				Sequence( 'n', buf.append('\n') ),
				Sequence( 'r', buf.append('\r') ),
				Sequence( 't', buf.append('\t') ),
				Sequence( 'v', buf.append('\u000b') ),
				Sequence( '\\', buf.append('\\') ),
				Sequence( '"', buf.append('"') ),
				Sequence( '\'', buf.append('\'') ),
				Sequence(
					'x',
					Sequence( HexDigit(), HexDigit() ),
					buf.append( (char)Integer.parseInt(match(),16) )
				),
				Sequence(
					Sequence(
						Digit(),
						Optional(
							Digit(),
							Optional(
								Digit()
							)
						)
					),
					buf.append( (char)Integer.parseInt(match()) )
				)
			)
		);
	}

	@Cached
	Rule Spaces(boolean inParens) {
		return ZeroOrMore(
			FirstOf(
				AnyOf(" \t"),
				Comment(),
				Sequence( '\\', EndOfLine() ),
				Sequence( AnyOf("\r\n"), inParens )
			)
		);
	}

	Rule Comment() {
		return Sequence(
			"--[",
			ZeroOrMore('='),
			nEquals(matchLength()),
			'[',
			ZeroOrMore(
				TestNot(LongBracketsEnd()),
				ANY
			),
			LongBracketsEnd()
		);
	}

	Rule LongBracketsEnd() {
		return Sequence( ']', ZeroOrMore('='), nEquals==matchLength(), ']' );
	}

	static boolean action(Object obj) {
		return true;
	}

	// for debugging
	boolean print(Object o) {
		System.out.println(o);
		return true;
	}

}
