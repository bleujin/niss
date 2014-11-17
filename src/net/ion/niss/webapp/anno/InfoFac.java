package net.ion.niss.webapp.anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Path;

import org.jboss.resteasy.util.FindAnnotation;

import net.ion.niss.webapp.Webapp;

public class InfoFac<T extends Webapp> {

	private Class<T> clz;

	public InfoFac(Class<T> clz) {
		this.clz = clz ;
	}

	public static <T extends Webapp> InfoFac<T> create(Class<T> clz) {
		return new InfoFac<T>(clz) ;
	}

	public <T> T visit(Info<T> builder) {
		Path clzAnno = clz.getAnnotation(Path.class) ;
		builder.classPath(clz, clzAnno) ;
		
		Method[] methods = AnnoUtil.findAnnoMethods(clz, Path.class) ;
		for (Method m : methods) {
			builder.methodPath(m, m.getAnnotation(Path.class), m.getDeclaredAnnotations(), m.getParameterAnnotations()) ;
		}
		return builder.build() ;
	}

}



class AnnoUtil {

	public static Field[] findFields(Class clazz, boolean recursively) {
		List<Field> fields = new LinkedList<Field>();
		Field[] result = clazz.getDeclaredFields();
		Collections.addAll(fields, result);

		Class superClass = clazz.getSuperclass();

		if (superClass != null && recursively) {
			Field[] declaredFieldsOfSuper = findFields(superClass, recursively);
			if (declaredFieldsOfSuper.length > 0)
				Collections.addAll(fields, declaredFieldsOfSuper);
		}

		return fields.toArray(new Field[fields.size()]);
	}

	public static Method[] findMethods(Class clazz) {
		List<Method> methods = new LinkedList<Method>();
		Method[] result = clazz.getDeclaredMethods();
		Collections.addAll(methods, result);

		return methods.toArray(new Method[methods.size()]);
	}


	public static Field[] findAnnoFields(Class clazz, Class<? extends Annotation> annotationClass, boolean recursively) {
		Field[] allFields = findFields(clazz, recursively);
		List<Field> result = new LinkedList<Field>();

		for (Field field : allFields) {
			if (field.isAnnotationPresent(annotationClass))
				result.add(field);
		}

		return result.toArray(new Field[result.size()]);
	}

	public static Method[] findAnnoMethods(Class clazz, Class<? extends Annotation> annotationClass) {
		Method[] methods = findMethods(clazz);
		List<Method> result = new LinkedList<Method>();

		for (Method field : methods) {
			if (field.isAnnotationPresent(annotationClass))
				result.add(field);
		}

		return result.toArray(new Method[result.size()]);
	}

}