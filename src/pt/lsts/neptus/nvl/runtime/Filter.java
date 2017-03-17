package pt.lsts.neptus.nvl.runtime;

public interface Filter<T> {
	boolean apply(T item);
}
