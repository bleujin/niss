package net.ion.niss.webapp.loaders;

public interface ResultHandler<T> {

	public final static ResultHandler<Object> DEFAULT = new ResultHandler<Object>() {
		@Override
		public Object onSuccess(Object result, Object... args) {
			return result;
		}

		@Override
		public Object onFail(Exception ex, Object... args) {
			return ex;
		}
	};
	
	public T onSuccess(Object result, Object... args) ;
	public T onFail(Exception ex, Object... args) ;
}
