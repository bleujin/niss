package net.ion.niss.webapp.loaders;

public interface ResultHandler<T> {

	public T onSuccess(Object result, Object... args) ;
	public T onFail(Exception ex, Object... args) ;
}
