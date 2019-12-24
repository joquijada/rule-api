package com.exsoinn.ie.util.concurrent;


import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * An {@link ExecutorService} that executes the given command in the same thread where the executor was invoked, meaning
 * that the command will execute sequentially.
 * Because this is supposed to act as a non-parallel {@link Executor}, some methods simply throw {@link UnsupportedOperationException}
 * where appropriate.
 *
 * Created by QuijadaJ on 8/14/2017.
 */
public class SequentialTaskExecutor implements ExecutorService {
    public static final SequentialTaskExecutor SEQUENTIAL_TASK_EXECUTOR = new SequentialTaskExecutor();
    @Override
    public void execute(Runnable pCommand) {
        pCommand.run();
    }

    @Override
    public void shutdown() {
        return;
    }

    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTerminated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<?> submit(Runnable task) {
        throw new UnsupportedOperationException();
    }


    /**
     * See {@link this#invokeAll(Collection)}.
     * @param tasks
     * @param <T>
     * @return
     * @throws InterruptedException
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return invokeAll(tasks, 0, null);
    }


    /**
     * Simply iterate over each task in the collection, creating a <code>Future</code> out of each, run it,
     * and create a list of these <code>Future</code>'s, ready to obtain the results of each via
     * their <code>get</code> method.
     *
     * @param tasks
     * @param timeout
     * @param unit
     * @param <T>
     * @return
     * @throws InterruptedException
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        List<Future<T>> futures = tasks.stream().map(FutureTask::new).collect(Collectors.toList());
        futures.stream().forEach(e -> ((FutureTask<T>) e).run());
        return futures;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }
}
