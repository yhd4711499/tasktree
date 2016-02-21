package ornithopter.demo.tasktree.tasks;

import org.jetbrains.annotations.Nullable;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Output;
import ornithopter.tasktree.annotations.Task;
import ornithopter.tasktree.functions.Func0;

/**
 * @author Ornithopter on 2015/11/15.
 */
@Task(rx = false)
class Helloworld {
    @Input String name;

    @Input int age;

    @Output String greetings;

    @Inject
    HelloTaskController taskController;

    @Execution void execute() {
        taskController.progress("I'm thinking...");
        greetings = "Hello " + name + ". You're ";
        greetings += (age > 26 ? "older" : "younger") + " than me.";
        taskController.success();
    }
}

class HelloTaskController extends TaskController<String> {

    /**
     * @param task          the task you want to control
     * @param cancelPending used as a delegate for rx task to check whether subscriber is unSubscribed or not.
     *                      passing null if this task is not a rx task.
     * @throws IllegalStateException if task has a taskController already.
     */
    public HelloTaskController(ornithopter.tasktree.Task task, @Nullable Func0<Boolean> cancelPending) throws IllegalStateException {
        super(task, cancelPending);
    }
}