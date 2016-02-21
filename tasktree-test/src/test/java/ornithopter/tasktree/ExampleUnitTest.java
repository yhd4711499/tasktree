package ornithopter.tasktree;


import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import ornithopter.demo.tasktree.tasks.HelloworldRxTask;
import ornithopter.demo.tasktree.tasks.HelloworldTask;
import ornithopter.demo.tasktree.tasks.ImportAssetsTask;
import ornithopter.demo.tasktree.tasks.LoginRxTask;
import ornithopter.demo.tasktree.tasks.LoginTask;
import ornithopter.demo.tasktree.tasks.TransferAssetsRxTask;
import ornithopter.demo.tasktree.tasks.TransferAssetsTask;
import ornithopter.tasktree.utils.MapUtil;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;


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
        HelloworldRxTask.build(name, 6).asObservable().subscribeOn(Schedulers.computation()).observeOn(Schedulers.newThread()).subscribe(new Subscriber<HelloworldRxTask.Result>() {
            @Override
            public void onStart() {
                System.out.println(String.format("[%s]: started.", Thread.currentThread()));
            }

            @Override
            public void onCompleted() {
                System.out.println(String.format("[%s]: completed.", Thread.currentThread()));
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(HelloworldRxTask.Result result) {
                System.out.println(String.format("[%s]: next.", Thread.currentThread()));
                if (result.completed) {
                    Assert.assertEquals(result.greetings, name);
                    signal.countDown();
                } else {
                    System.out.println(result.progress);
                }
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

        // map output [userInfo] to input [userInfo]
        Map<String, String> mapping = MapUtil.from(ImportAssetsTask.Keys.userInfo, LoginTask.Keys.userInfo);

        // append method will return the appending task,
        // which is ImportAssetsTask for this case.
        ImportAssetsTask importAssetsTask = loginTask.append(ImportAssetsTask.connect(mapping));
        importAssetsTask
                .onProgress(System.out::println)
                .onError(throwable -> signal.countDown())
                .onSuccess(signal::countDown);

        Executors.newCachedThreadPool().submit((Runnable) loginTask::execute);

        signal.await();
    }

    @Test
    public void testPrepend() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        TransferAssetsTask transferAssetsTask = new TransferAssetsTask();

        /*
         * Prepend two parallel tasks.
         *
         * When transferAssetsTask executed, these prepended tasks will be executed at first.
         *
         * Input of transferAssetsTask will be assigned thereafter according to mappings in TaskConnection.
         */

        transferAssetsTask.prepend(
                new TaskConnection<>(
                        LoginTask.build("google", "********").onProgress(System.out::println),
                        // map output [userInfo] to input [userInfoFrom]
                        TransferAssetsTask.Keys.userInfoFrom, LoginTask.Keys.userInfo   // mapping
                )
        );

        transferAssetsTask.prepend(
                new TaskConnection<>(
                        LoginTask.build("microsoft", "********").onProgress(System.out::println),
                        // map output [userInfo] to input [userInfoTo]
                        TransferAssetsTask.Keys.userInfoTo, LoginTask.Keys.userInfo
                )
        );

        transferAssetsTask
                .onProgress(System.out::println)
                .onError(throwable -> signal.countDown())
                .onSuccess(signal::countDown);

        Executors.newCachedThreadPool().submit((Runnable) transferAssetsTask::execute);

        signal.await();
    }

    @Test
    public void testPrependRx() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        /*
         * Prepend two parallel tasks.
         *
         * Here I'm using the native approach in RxJava to prepend those two login tasks.
         *
         * Input of transferAssetsTask will be assigned manually in flatMap.
         *
         * Note the subscribeOn on login tasks. These two tasks would execute synchronously without subscribeOn another thread.
         */

        Observable.combineLatest(
                LoginRxTask.build("google", "********").onProgress(System.out::println).asObservable().subscribeOn(Schedulers.io()),
                LoginRxTask.build("microsoft", "********").onProgress(System.out::println).asObservable().subscribeOn(Schedulers.io()),
                (result, result2) -> new LoginRxTask.Result[]{result, result2}
        )
            .flatMap(pair ->
                    TransferAssetsRxTask.build(pair[0].userInfo, pair[1].userInfo, null).onProgress(System.out::println).asObservable())
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .subscribe(new Subscriber<TransferAssetsRxTask.Result>() {
                @Override
                public void onCompleted() {
                    signal.countDown();
                }

                @Override
                public void onError(Throwable e) {
                    System.out.println("error");
                    signal.countDown();
                }

                @Override
                public void onNext(TransferAssetsRxTask.Result result) {
                }
            });
        signal.await();
    }

    @Test
    public void testPrependWithCancellation() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        TransferAssetsTask transferAssetsTask = new TransferAssetsTask();

        // prepend method will return the prepending task,
        // which is LoginTask in this case.
        LoginTask google = transferAssetsTask.prepend(
                new TaskConnection<>(
                        LoginTask.build("google", "********")
                            .onCancel(()-> System.out.println("login google canceled."))
                            .onProgress(System.out::println),
                        TransferAssetsTask.Keys.userInfoFrom, LoginTask.Keys.userInfo
                )
        );

        transferAssetsTask.prepend(
                new TaskConnection<>(
                        LoginTask.build("microsoft", "********").onProgress(System.out::println),
                        TransferAssetsTask.Keys.userInfoTo, LoginTask.Keys.userInfo
                )
        );

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