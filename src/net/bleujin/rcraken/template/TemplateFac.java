package net.bleujin.rcraken.template;

import java.io.IOException;
import java.util.Map;
import net.bleujin.rcraken.Fqn;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

public class TemplateFac {
   private Map<String, String> dftTemplate = MapUtil.newMap();

   public TemplateFac() {
      this.dftTemplate.put("template", this.defaultTemplate());// 17
      this.dftTemplate.put("children", "[${foreach self.children() child ,}${child.toFlatJson()}${end}]");// 18
      this.dftTemplate.put("json", "${self.toFlatJson()}");// 19
   }// 20

   public TemplateNode newNode(ReadSession rsession, Fqn fqn, String templateName) {
      return new TemplateNode(this, rsession, fqn, templateName);// 23
   }

   public TemplateFac addTemplate(String name, String content) {
      this.dftTemplate.put(name, content);// 27
      return this;// 28
   }

   public String findTemplate(String templateName) {
      return StringUtil.coalesce(new String[]{(String)this.dftTemplate.get(templateName), (String)this.dftTemplate.get("template")});// 32
   }

   private String defaultTemplate() {
      try {
         return IOUtil.toStringWithClose(this.getClass().getResourceAsStream("./craken.tpl"));// 37
      } catch (IOException | NullPointerException var2) {// 38
         return "${self}";// 39
      }
   }
}
