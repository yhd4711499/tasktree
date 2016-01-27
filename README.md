# tasktree

Concentrate on business!

## Getting started

**write your task as below**

```java
@Task(rx = false)
class Helloworld {
    @Input String name;

    @Input int age;

    @Output String greetings;

    @Inject
    TaskController<String> taskController;

    @Execution void execute() {
        taskController.progress("thinking...");
        greetings = "Hello " + name + ". You're ";
        greetings += (age > 26 ? "older" : "younger") + "than me";
        taskController.success();
    }
}
```

**use it**

```java
HelloworldTask.build(name, age)
  .onSuccess(greetings -> {
    System.out.println("Greetings from ornithopter: " + greetings);
  })
  .onStarted(() -> {
    System.out.println("HelloworldTask started.");
  })
  .onProgress((progress) -> {
    System.out.println("ornithopter is now " + progress);
  })
  .execute();
```

**how it works**

In `Helloworld`, fields annotated with `Input` will be asigned in `HelloworldTask.build(...)`. Fields with `Output` should be asigned in
method annotated with `Execution`.

In addition, you can declare a field with type `TaskController<?>` and annotate it with `Inject` to control the workflow of this task.

`HelloworldTask` is generated by APT and it wraps `Helloworld` as is't implementation to accomplish the task.

**RxJava supported**

Changing `@Task(rx = false)` to `@Task(rx = true)` makes `HelloworldTask` equiped with this powerful method: `asObservable`.
Then you can do whole bunch of things with RxJava!

```java
HelloworldRxTask.build(name, 6).asObservable().subscribe(new Subscriber<HelloworldRxTask.Result>() {
    @Override
    public void onStart() {
        System.out.println("HelloworldRxTask started.");
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(HelloworldRxTask.Result result) {
      System.out.println("Greetings from ornithopter: " + result.greetings);
    }
});
```

## How to build

You need both jdk7 and jdk8 to build this project.

JDK8 is used by `retrolambda` and it requires `JAVA8_HOME` to be set in the environment variables.

For OSX users, please refer to this link:
[http://stackoverflow.com/questions/135688/setting-environment-variables-in-os-x/588442#588442](http://stackoverflow.com/questions/135688/setting-environment-variables-in-os-x/588442#588442)

For now all demos are placed in Unit test cases. You need to switch the Test Artifact in the Build Variants view to run the `ExampleUnitTest` contained in module `tasktree-test`.

## Project structure

`tasktree-annotations`: only annotations contained.

`tasktree-compiler`: code generator.

`tasktree-core`: `TaskTree` implementations.

`tasktree-rx`: nothing but a place holder.

`tasktree-test`: unit tests for `tasktree-core` and `tasktree-compiler`.

## Note

This project is at it's early stage.
