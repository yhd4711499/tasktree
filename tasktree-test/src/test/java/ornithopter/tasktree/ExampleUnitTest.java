package ornithopter.tasktree;


import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import ornithopter.demo.tasktree.HelloworldRxTask;
import ornithopter.demo.tasktree.HelloworldTask;
import ornithopter.demo.tasktree.ImportAssetsTask;
import ornithopter.demo.tasktree.LoginTask;
import ornithopter.demo.tasktree.TransferAssetsTask;
import ornithopter.tasktree.utils.MapUtil;
import rx.Subscriber;


/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void test() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldTask.build(name, 6)
                .onSuccess(s -> {
                    Assert.assertEquals(s, "Hello yhd. You're younger than me.");
                    signal.countDown();
                })
                .onError(throwable -> {
                    signal.countDown();
                    Assert.assertNull(throwable);
                })
                .onStarted(() -> System.out.println("HelloworldTask started."))
                .onProgress((progress) -> Assert.assertEquals(progress, "I'm thinking..."))
                .execute();
        signal.await();
    }

    @Test
    public void testRx() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
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
                Assert.assertEquals(result.greetings, name);
                signal.countDown();
            }
        });
        signal.await();
    }

    @Test
    public void testChained() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final String name = "yhd";
        HelloworldTask.build(name, 6)
                .onSuccess(s -> {
                    Assert.assertEquals(s, name);
                    HelloworldTask.build(name + " again", 6)
                            .onSuccess(s2 -> {
                                Assert.assertEquals(s2, name + " again");
                                signal.countDown();
                            })
                            .onStarted(() -> System.out.println("HelloworldTask started."))
                            .onProgress((progress) -> Assert.assertEquals(progress, "I'm thinking..."))
                            .execute();
                })
                .onStarted(() -> System.out.println("HelloworldTask started."))
                .onError(throwable -> signal.countDown())
                .onProgress((progress) -> Assert.assertEquals(progress, "I'm thinking..."))
                .execute();
        signal.await();
    }

    @Test
    public void testAppend() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        LoginTask loginTask = LoginTask.build("google", "********").onProgress(System.out::println);
        loginTask.append(ImportAssetsTask.connect(new HashMap<String, String>(){
            {
                put(ImportAssetsTask.Keys.userInfo, LoginTask.Keys.userInfo);
            }
        })).onProgress(System.out::println).onError(throwable -> signal.countDown()).onSuccess(signal::countDown);
        Executors.newCachedThreadPool().submit((Runnable) loginTask::execute);
        signal.await();
    }

    @Test
    public void testPrepend() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        TransferAssetsTask transferAssetsTask = new TransferAssetsTask();

        transferAssetsTask.prepend(
                new TaskConnection<>(LoginTask.build("google", "********").onProgress(System.out::println),
                MapUtil.from(TransferAssetsTask.Keys.userInfoFrom, LoginTask.Keys.userInfo)));

        transferAssetsTask.prepend(
                new TaskConnection<>(LoginTask.build("microsoft", "********").onProgress(System.out::println),
                MapUtil.from(TransferAssetsTask.Keys.userInfoTo, LoginTask.Keys.userInfo)));

        transferAssetsTask
                .onProgress(System.out::println)
                .onError(throwable -> signal.countDown())
                .onSuccess(signal::countDown);

        Executors.newCachedThreadPool().submit((Runnable) transferAssetsTask::execute);

        signal.await();
    }

    @Test
    public void testPrependWithCancellation() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        TransferAssetsTask transferAssetsTask = new TransferAssetsTask();

        LoginTask google = transferAssetsTask.prepend(
                new TaskConnection<>(LoginTask.build("google", "********")
                        .onCancel(()-> System.out.println("login google canceled."))
                        .onProgress(System.out::println),
                MapUtil.from(TransferAssetsTask.Keys.userInfoFrom, LoginTask.Keys.userInfo)));

        transferAssetsTask.prepend(new TaskConnection<>(LoginTask.build("microsoft", "********").onProgress(System.out::println),
                MapUtil.from(TransferAssetsTask.Keys.userInfoTo, LoginTask.Keys.userInfo)));

        transferAssetsTask
                .onProgress(System.out::println)
                .onError(throwable -> {
                    Assert.assertTrue(throwable instanceof IllegalArgumentException);
                    Assert.assertTrue(TransferAssetsTask.Keys.userInfoFrom.equals(throwable.getMessage()));
                    System.out.println("Transfer assets failed. Cause: " + throwable);
                    signal.countDown();
                })
                .onSuccess(signal::countDown);

        Executors.newCachedThreadPool().submit((Runnable) transferAssetsTask::execute);

        Thread.sleep(500);

        google.cancel(true);

        signal.await();
    }
}