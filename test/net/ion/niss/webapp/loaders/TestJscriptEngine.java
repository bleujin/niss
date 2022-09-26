package net.ion.niss.webapp.loaders;

import java.io.File;
import java.io.IOException;

import javax.script.ScriptEngineManager;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

public class TestJscriptEngine extends TestCase{

	
	public void testInit() throws Exception {
		ScriptEngineManager manager = new ScriptEngineManager();

		manager.getEngineFactories().forEach(engine ->{
			Debug.line(engine.getEngineName(), engine.getEngineVersion(), engine.getEngineVersion(), engine.getScriptEngine()) ; 
		}); // -__-;;
	}
	
	
	public void testGraal() throws Exception {
		try (Context context = Context.create("js")) {
		    // 2 출력
		    context.eval("js", "print( Math.min(2, 3) )");
		} catch (Exception e) {
		    System.err.println();
		}
	}
	
	public void testGraalScriptFile() throws Exception {
		
		try (Context context = Context.create("js")) {
		    context.eval(Source.newBuilder("js", new File("./resource/script/graal_sample.js")).build()); 
		    Value accumulatorFunc = context.getBindings("js").getMember("accumulator"); // 컨텍스트의 바인딩 객체에서 "accumulator" 함수를 가져온다.
		    int result = accumulatorFunc.execute(1, 2).asInt();
		    Debug.debug(context, "result: " + result);
		} catch (IOException e) {
		    System.err.println(e);
		}
		
		try (Context context = Context.create("js")) {
		    context.eval(Source.newBuilder("js",  new File("./resource/script/graal_sample.js")).build());
		    
		    Value makeContractFunc = context.getBindings("js").getMember("makeContract"); // 컨텍스트의 바인딩 객체에서 "makeContract" 함수를 가져온다.
		    Value obj = makeContractFunc.execute("madplay", "010-1234-1234"); // 함수를 파라미터와 함께 실행시키고 결과를 `Value` 객체에 매핑한다.
		    
		    // 반환값의 key-value 구조를 스트림을 이용해 모두 출력한다.
		    obj.getMemberKeys().stream().forEach(key -> System.out.printf("%s: %s\n", key, obj.getMember(key)));
		} catch (IOException e) {
		    System.err.println(e);
		}
	}
	
	
}
