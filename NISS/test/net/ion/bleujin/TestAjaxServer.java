package net.ion.bleujin;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import junit.framework.TestCase;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.radon.core.let.PathHandler;
import net.ion.radon311.provider.IJsonCompatable;

@Path("/hello")
public class TestAjaxServer extends TestCase {

	public void testBeanToJsonResponse() throws Exception {
		Radon radon = RadonConfiguration.newBuilder(9500).add(new PathHandler(TestAjaxServer.class)).startRadon() ;
		
		TinyClient.local(9500).sayHello("/hello/1.json");
		
		radon.stop().get() ;
	}
	
	

	@GET
	@Path("/{name}.txt")
	@Produces(MediaType.TEXT_PLAIN)
	public Employee empText(){
		return new Employee("bleujin", 20) ;
	}

	

	@GET
	@Path("/{name}.json")
	@Produces(MediaType.APPLICATION_JSON)
	public Employee empJson(){
		return new Employee("bleujin", 20) ;
	}


}

class Employee implements IJsonCompatable{
	private String name ;
	private int age ;
	
	public Employee(String name, int age){
		this.name = name ;
		this.age = age ;
	}
}