package net.ion.bleujin;

import java.util.Locale;

import junit.framework.TestCase;
import net.ion.framework.mte.Engine;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class TestThymeleaf extends TestCase {

	public void testTemplateEngine() throws Exception {
		Engine engine = Engine.createDefaultEngine() ;
		
		String result = engine.transform("${name}", Locale.KOREA, MapUtil.chainKeyMap().put("name", "bleujin").toMap()) ;
		Debug.line(result);
	}
}
