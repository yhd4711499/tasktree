package ornithopter.demo.tasktree.tasks;

import ornithopter.tasktree.TaskController;
import ornithopter.tasktree.annotations.Execution;
import ornithopter.tasktree.annotations.Inject;
import ornithopter.tasktree.annotations.Input;
import ornithopter.tasktree.annotations.Output;
import ornithopter.tasktree.annotations.Task;

/**
 * @author Ornithopter
 */
@Task(rx = true)
class HelloworldRx {
    @Input String name;

    @Input int age;

    @Output String greetings;

    @Inject TaskController<String> taskController;

    @Execution void execute() {
        taskController.progress("I'm thinking...");
        greetings = name;
        taskController.success();
    }
}