package pt.lsts.nvl.runtime;

public interface TaskExecution {

	boolean isDone();
	TaskState getState();
	
}
