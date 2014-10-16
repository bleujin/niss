package net.ion.niss.cloader;

import java.io.FileInputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import net.ion.framework.util.IOUtil;

public class SimpleBinClassLoader extends ClassLoader {

	private final String clzLocation ;
	public SimpleBinClassLoader(ClassLoader parent, String clzLocation) {
		super(parent);
		this.clzLocation = clzLocation ;
	}

	public static SimpleBinClassLoader create(String clzLocation) {
		return new SimpleBinClassLoader(SimpleBinClassLoader.class.getClassLoader(), clzLocation);
	}


	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {

		AccessControlContext acc = AccessController.getContext();
		try {
			return (Class) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws ClassNotFoundException {

					FileInputStream fi = null;
					try {
						String path = name.replace('.', '/');
						fi = new FileInputStream(clzLocation + path + ".class");
						byte[] classBytes = IOUtil.toByteArray(fi) ;
						return defineClass(name, classBytes, 0, classBytes.length);
					} catch (Exception e) {
						throw new ClassNotFoundException(name);
					} finally {
						IOUtil.close(fi);
					}
				}
			}, acc);
		} catch (java.security.PrivilegedActionException pae) {
			return super.findClass(name);
		}
	}

}