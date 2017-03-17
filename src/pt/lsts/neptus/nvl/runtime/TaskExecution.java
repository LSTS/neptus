package pt.lsts.neptus.nvl.runtime;

public interface TaskExecution {

	boolean isDone();
	TaskState getState();
	
}
