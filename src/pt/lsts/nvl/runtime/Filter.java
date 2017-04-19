package pt.lsts.nvl.runtime;

public interface Filter<T> {
	boolean apply(T item);
}
