package ornithopter.demo.tasktree;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Output;
import ornithopter.tasktree.annotations.Task;

/**
 * @author Ornithopter on 2015/11/15.
 */
@Task(rx = false)
class Helloworld {
    @Input String name;

    @Input int age;

    @Output String greetings;

    @Inject
    TaskController<String> taskController;

    @Execution void execute() {
        taskController.progress("I'm thinking...");
        greetings = "Hello " + name + ". You're ";
        greetings += (age > 26 ? "older" : "younger") + " than me.";
        taskController.success();
    }
}