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
import luan.Lua;
import luan.LuaNumber;
import luan.LuaState;
import luan.LuaSource;


class LuaParser extends BaseParser<Object> {

	LuaSource source;

	LuaSource.Element se(int start) {
		return new LuaSource.Element(source,start,currentIndex());
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
	int parens = 0;
	Frame frame = new Frame();

	boolean nEquals(int n) {
		nEquals = n;
		return true;
	}

	boolean incParens() {
		parens++;
		return true;
	}

	boolean decParens() {
		parens--;
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
			Spaces(),
			FirstOf(
				Sequence( ExpList(), EOI ),
				Sequence(
					start.set(currentIndex()),
					action( frame.isVarArg = true ),
					Block(),
					EOI,
					push( newChunk(start.get()) )
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
				StmtSep(),
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

	Rule StmtSep() {
		return Sequence(
			FirstOf(
				';',
				Sequence(
					Optional( "--", ZeroOrMore(NoneOf("\r\n")) ),
					EndOfLine()
				)
			),
			Spaces()
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

	Rule ReturnStmt() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Keyword("return"), Expressions(),
			push( new ReturnStmt( se(start.get()), (Expressions)pop() ) )
		);
	}

	Rule FunctionStmt() {
		return Sequence(
			Keyword("function"), FnName(), Function(),
			push( new SetStmt( (Settable)pop(1), expr(pop()) ) )
		);
	}

	Rule FnName() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			push(null),  // marker
			Name(),
			ZeroOrMore(
				'.', Spaces(),
				makeVarExp(start.get()),
				NameExpr()
			),
			makeSettableVar(start.get())
		);
	}

	Rule LocalFunctionStmt() {
		return Sequence(
			Keyword("local"), Keyword("function"),
			Name(),
			addSymbol( (String)pop() ),
			Function(),
			push( new SetStmt( new SetLocalVar(symbolsSize()-1), expr(pop()) ) )
		);
	}

	Rule BreakStmt() {
		return Sequence(
			Keyword("break"),
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
			Keyword("for"), NameList(names), Keyword("in"), Expr(), Keyword("do"),
			addSymbols(names.get()),
			LoopBlock(), Keyword("end"),
			push( new GenericForStmt( se(start.get()), stackStart.get(), symbolsSize() - stackStart.get(), expr(pop(1)), (Stmt)pop() ) ),
			popSymbols( symbolsSize() - stackStart.get() )
		);
	}

	Rule NumericForStmt() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Keyword("for"), Name(), '=', Spaces(), Expr(), Keyword("to"), Expr(),
			push( new ConstExpr(new LuaNumber(1)) ),  // default step
			Optional(
				Keyword("step"),
				drop(),
				Expr()
			),
			addSymbol( (String)pop(3) ),  // add "for" var to symbols
			Keyword("do"), LoopBlock(), Keyword("end"),
			push( new NumericForStmt( se(start.get()), symbolsSize()-1, expr(pop(3)), expr(pop(2)), expr(pop(1)), (Stmt)pop() ) ),
			popSymbols(1)
		);
	}

	Rule TryStmt() {
		return Sequence(
			Keyword("try"), Block(),
			Keyword("catch"), Name(), addSymbol( (String)pop() ),
			Keyword("do"), Block(), Keyword("end"),
			push( new TryStmt( (Stmt)pop(1), symbolsSize()-1, (Stmt)pop() ) ),
			popSymbols(1)
		);
	}

	Rule DoStmt() {
		return Sequence(
			Keyword("do"), Block(), Keyword("end")
		);
	}

	Rule LocalStmt(Var<List<Stmt>> stmts) {
		Var<List<String>> names = new Var<List<String>>(new ArrayList<String>());
		return Sequence(
			Keyword("local"), NameList(names),
			Optional(
				'=', Spaces(), ExpList(),
				stmts.get().add( newSetLocalStmt(names.get().size()) )
			),
			addSymbols(names.get())
		);
	}

	Rule NameList(Var<List<String>> names) {
		return Sequence(
			Name(),
			names.get().add( (String)pop() ),
			ZeroOrMore(
				',', Spaces(), Name(),
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
			Keyword("while"), Expr(), Keyword("do"), LoopBlock(), Keyword("end"),
			push( new WhileStmt( expr(pop(1)), (Stmt)pop() ) )
		);
	}

	Rule RepeatStmt() {
		return Sequence(
			Keyword("repeat"), LoopBlock(), Keyword("until"), Expr(),
			push( new RepeatStmt( (Stmt)pop(1), expr(pop()) ) )
		);
	}

	Rule LoopBlock() {
		return Sequence( incLoops(), Block(), decLoops() );
	}

	Rule IfStmt() {
		Var<Integer> n = new Var<Integer>(1);
		return Sequence(
			Keyword("if"), Expr(), Keyword("then"), Block(),
			push(Stmt.EMPTY),
			ZeroOrMore(
				Keyword("elseif"), drop(), Expr(), Keyword("then"), Block(),
				push(Stmt.EMPTY),
				n.set(n.get()+1)
			),
			Optional(
				Keyword("else"), drop(), Block()
			),
			Keyword("end"),
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
			'=', Spaces(),
			ExpList(),
			push( newSetStmt() )
		);
	}

	Rule ExpressionsStmt() {
		return Sequence(
			ExpList(),
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
				',', Spaces(), SettableVar(),
				vars.get().add( (Settable)pop() )
			),
			push(vars.get())
		);
	}

	Rule SettableVar() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Var(),
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

	Rule Expr() {
		return FirstOf(
			VarArgs(),
			OrExpr()
		);
	}

	Rule OrExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			AndExpr(),
			ZeroOrMore( "or", Spaces(), AndExpr(), push( new OrExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule AndExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			RelExpr(),
			ZeroOrMore( "and", Spaces(), RelExpr(), push( new AndExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule RelExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			ConcatExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( "==", Spaces(), ConcatExpr(), push( new EqExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( "~=", Spaces(), ConcatExpr(), push( new NotExpr(se(start.get()),new EqExpr(se(start.get()),expr(pop(1)),expr(pop()))) ) ),
					Sequence( "<=", Spaces(), ConcatExpr(), push( new LeExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( ">=", Spaces(), ConcatExpr(), push( new LeExpr(se(start.get()),expr(pop()),expr(pop())) ) ),
					Sequence( "<", Spaces(), ConcatExpr(), push( new LtExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( ">", Spaces(), ConcatExpr(), push( new LtExpr(se(start.get()),expr(pop()),expr(pop())) ) )
				)
			)
		);
	}

	Rule ConcatExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			SumExpr(),
			Optional( "..", Spaces(), ConcatExpr(), push( new ConcatExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule SumExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			TermExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '+', Spaces(), TermExpr(), push( new AddExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( '-', TestNot('-'), Spaces(), TermExpr(), push( new SubExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	Rule TermExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			UnaryExpr(),
			ZeroOrMore(
				FirstOf(
					Sequence( '*', Spaces(), UnaryExpr(), push( new MulExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( '/', Spaces(), UnaryExpr(), push( new DivExpr(se(start.get()),expr(pop(1)),expr(pop())) ) ),
					Sequence( '%', Spaces(), UnaryExpr(), push( new ModExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
				)
			)
		);
	}

	Rule UnaryExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			FirstOf(
				Sequence( '#', Spaces(), PowExpr(), push( new LenExpr(se(start.get()),expr(pop())) ) ),
				Sequence( '-', TestNot('-'), Spaces(), PowExpr(), push( new UnmExpr(se(start.get()),expr(pop())) ) ),
				Sequence( "not", Spaces(), PowExpr(), push( new NotExpr(se(start.get()),expr(pop())) ) ),
				PowExpr()
			)
		);
	}

	Rule PowExpr() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			SingleExpr(),
			Optional( '^', Spaces(), PowExpr(), push( new PowExpr(se(start.get()),expr(pop(1)),expr(pop())) ) )
		);
	}

	Rule SingleExpr() {
		return FirstOf(
			FunctionExpr(),
			TableExpr(),
			VarExp(),
			LiteralExpr()
		);
	}

	Rule FunctionExpr() {
		return Sequence( "function", Spaces(), Function() );
	}

	Rule Function() {
		Var<Integer> start = new Var<Integer>();
		Var<List<String>> names = new Var<List<String>>(new ArrayList<String>());
		return Sequence(
			start.set(currentIndex()),
			'(', incParens(), Spaces(),
			action( frame = new Frame(frame) ),
			Optional(
				FirstOf(
					Sequence(
						NameList(names), addSymbols(names.get()),
						Optional( ',', Spaces(), VarArgName() )
					),
					VarArgName()
				)
			),
			')', decParens(), Spaces(), Block(), Keyword("end"),
			push( newChunk(start.get()) ),
			action( frame = frame.parent )
		);
	}

	Rule VarArgName() {
		return Sequence(
			"...", Spaces(),
			action( frame.isVarArg = true )
		);
	}

	Rule VarArgs() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			"...", Spaces(),
			frame.isVarArg,
			push( new VarArgs(se(start.get())) )
		);
	}

	Rule TableExpr() {
		Var<Integer> start = new Var<Integer>();
		Var<List<TableExpr.Field>> fields = new Var<List<TableExpr.Field>>(new ArrayList<TableExpr.Field>());
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>(new ExpList.Builder());
		return Sequence(
			start.set(currentIndex()),
			'{', incParens(), Spaces(),
			Optional(
				Field(fields,builder),
				ZeroOrMore(
					FieldSep(),
					Field(fields,builder)
				),
				Optional( FieldSep() )
			),
			'}', decParens(),
			Spaces(),
			push( new TableExpr( se(start.get()), fields.get().toArray(new TableExpr.Field[0]), builder.get().build() ) )
		);
	}

	Rule FieldSep() {
		return Sequence( AnyOf(",;"), Spaces() );
	}

	Rule Field(Var<List<TableExpr.Field>> fields,Var<ExpList.Builder> builder) {
		return FirstOf(
			Sequence(
				FirstOf( SubExpr(), NameExpr() ),
				'=', Spaces(), Expr(),
				fields.get().add( new TableExpr.Field( expr(pop(1)), expr(pop()) ) )
			),
			Sequence(
				Expr(),
				addToExpList(builder.get())
			)
		);
	}

	static Expr expr(Object obj) {
		if( obj instanceof Expressions )
			return new ExpressionsExpr((Expressions)obj);
		return (Expr)obj;
	}

	Rule VarExp() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			Var(),
			makeVarExp(start.get())
		);
	}

	Rule Var() {
		Var<Integer> start = new Var<Integer>();
		return Sequence(
			start.set(currentIndex()),
			FirstOf(
				Sequence(
					'(', incParens(), Spaces(), Expr(), ')', decParens(), Spaces(),
					push(expr(pop())),
					push(null)  // marker
				),
				Sequence(
					push(null),  // marker
					Name()
				)
			),
			ZeroOrMore(
				makeVarExp(start.get()),
				FirstOf(
					SubExpr(),
					Sequence( '.', Spaces(), NameExpr() ),
					Sequence(
						Args(start),
						push(null)  // marker
					)
				)
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
	Rule Args(Var<Integer> start) {
		return Sequence(
			FirstOf(
				Sequence(
					'(', incParens(), Spaces(), Expressions(), ')', decParens(), Spaces()
				),
				Sequence(
					TableExpr(),
					push( new ExpList.SingleExpList(expr(pop())) )
				),
				Sequence(
					StringLiteral(), Spaces(),
					push( new ExpList.SingleExpList(new ConstExpr(pop())) )
				)
			),
			push( new FnCall( se(start.get()), expr(pop(1)), (Expressions)pop() ) )
		);
	}

	Rule Expressions() {
		return FirstOf(
			ExpList(),
			push( ExpList.emptyExpList )
		);
	}

	Rule ExpList() {
		Var<ExpList.Builder> builder = new Var<ExpList.Builder>(new ExpList.Builder());
		return Sequence(
			Expr(),
			addToExpList(builder.get()),
			ZeroOrMore(
				',', Spaces(), Expr(),
				addToExpList(builder.get())
			),
			push( builder.get().build() )
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

	Rule SubExpr() {
		return Sequence( '[', incParens(), Spaces(), Expr(), ']', decParens(), Spaces() );
	}

	Rule NameExpr() {
		return Sequence(
			Name(),
			push( new ConstExpr((String)pop()) )
		);
	}

	Rule Name() {
		return Sequence(
			Sequence(
				NameFirstChar(),
				ZeroOrMore( NameChar() )
			),
			!keywords.contains(match()),
			push(match()),
			Spaces()
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

	Rule Keyword(String keyword) {
		return Sequence(
			keyword,
			TestNot( NameChar() ),
			Spaces()
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

	Rule LiteralExpr() {
		return Sequence(
			Literal(), Spaces(),
			push(new ConstExpr(pop()))
		);
	}

	Rule Literal() {
		return FirstOf(
			NilLiteral(),
			BooleanLiteral(),
			NumberLiteral(),
			StringLiteral()
		);
	}

	Rule NilLiteral() {
		return Sequence( "nil", push(null) );
	}

	Rule BooleanLiteral() {
		return FirstOf(
			Sequence( "true", push(true) ),
			Sequence( "false", push(false) )
		);
	}

	Rule NumberLiteral() {
		return Sequence(
			Number(),
			push(new LuaNumber((Double)pop()))
		);
	}

	Rule Number() {
		return FirstOf(
			Sequence(
				IgnoreCase("0x"),
				HexNumber()
			),
			Sequence(
				DecNumber(),
				push(Double.parseDouble(match()))
			)
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

	Rule StringLiteral() {
		return FirstOf(
			QuotedString('"'),
			QuotedString('\''),
			LongString()
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

	Rule Spaces() {
		return ZeroOrMore(
			FirstOf(
				AnyOf(" \t"),
				Comment(),
				Sequence( '\\', EndOfLine() ),
				Sequence( AnyOf("\r\n"), parens > 0 )
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
