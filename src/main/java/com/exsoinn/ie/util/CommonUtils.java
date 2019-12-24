package com.exsoinn.ie.util;

import com.exsoinn.ie.rule.*;
import com.exsoinn.ie.util.concurrent.CountingThread;
import com.exsoinn.ie.util.concurrent.DnbThreadFactory;
import com.exsoinn.ie.util.concurrent.SequentialTaskExecutor;
import com.exsoinn.util.DnbBusinessObject;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by QuijadaJ on 3/28/2017.
 */
public class CommonUtils {

    /*
     * Suppresses default constructor, ensuring non-instantiability.
     */
    private CommonUtils() {

    }


    /*
     * Public Methods
     */

    /**
     * Checks if a string is only white space characters.
     * @param pStr - The string to check
     * @return - True if the string is empty or if composed of only white space
     */
    public static boolean stringIsBlank(String pStr) {
        if(pStr != null && pStr.length() != 0) {
            pStr = pStr.replaceAll("\\s","");
            return (pStr.length()>0) ? false : true;
        } else {
            return true;
        }
    }


    /**
     * Checks if a string is empty, meaning that it's 0 length. Note that a white space only string
     * is not considered empty. If you need to check for blank string, then use method
     * {@link CommonUtils#stringIsBlank(String)}.
     * @param pStr - The string to check.
     * @return - True if the passed is string is <code>null</code> or 0 (zero) length.
     */
    public static boolean stringIsEmpty(String pStr) {
        return pStr == null || pStr.length() == 0;
    }

    /**
     *
     * @param pXml
     * @return
     */
    public static String convertXmlToJson(String pXml) {
        return JsonUtils.convertXmlToJson(pXml);
    }

    public static JSONObject convertXmlToJsonObject(String pXml) {
        return JsonUtils.convertXmlToJsonObject(pXml);
    }

    /**
     * Performs similar function to <code>Arrays.asList(T[])</code>, but this returns a modifiable
     * List instead.
     * @param pAry
     * @param <T>
     * @return
     */
    public static <T> List<T> buildListFromArray(T[] pAry) {
        List<T> newList = new ArrayList<>();

        for (T entry : pAry) {
            newList.add(entry);
        }

        return newList;
    }

    /**
     * Checks if the given {@code pPath} exists on disk
     * @param pPath - The path to check
     * @return
     */
    public static boolean pathExists(String pPath) {
        File pathObj = new File(pPath);
        return (pathObj.exists());
    }

    /**
     * Like {@link CommonUtils#pathExists(String)}, but in addition checks if the path is for a file
     * @param pPath - The path to check.
     * @return
     */
    public static boolean pathExistsAndIsFile(String pPath) {
        boolean exists = pathExists(pPath);
        File pathObj = new File(pPath);
        return exists && !pathObj.isDirectory();
    }



    public static StringBuilder extractStackTrace(Exception pExc) {
        StringBuilder out = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        pExc.printStackTrace(ps);
        out.append(baos.toString());

        return out;
    }


    /**
     * Uses the passed in {@param pExecutorSvc} and {@code List} of {@link FutureTask}'s to execute.
     * <strong>It is absolutely imperative</strong>, if necessary, that the operations that the passed in
     * {@code FutureTask}'s wrap
     * re-throw any caught exception wrapped in something suitable for runtime, such as a {@link RuntimeException}. This
     * applies for example if the wrapped tasks are <code>Runnable</code>'s. If any of the {@code FutureTask}'s had
     * problems, this method  will "unwrap" by doing {@link Exception#getCause()}, and return that cause back to caller
     * so that caller may do with it as it wishes.
     * Another nicety of this method, and added for convenience to caller, is that it will take care of NOT swallowing
     * {@link InterruptedException} if one was thrown (see "Java Concurrency In Practice", Goetz06 7.1.3).
     *
     * @param pExecutorSvc - The {@link ExecutorService} to use to run the {@code Runnable}'s
     * @param pOperations - The {@code Runnable}'s that encapsulate the operations to perform
     * @param pTimeout - Time budgeted in seconds that should be spent waiting for all operations to finish.
     * @return - The underlying cause of any caught exceptions inside any of the passed in {@code Runnable}'s, if any
     *   problems occurred.
     * @throws InterruptedException - This may get thrown because the call to wait for all {@code Runnable}'s to finish
     *   is blocking, namely via {@link ExecutorService#awaitTermination(long, TimeUnit)}.
     */
    public static <T> Throwable runOperationsAsynchronouslyAndWaitForAllToComplete(
            ExecutorService pExecutorSvc,
            Collection<? extends FutureTask<T>> pOperations, long pTimeout) throws InterruptedException, TimeoutException {


        /**
         * Create {@link Callable}'s from passed in {@link FutureTask}'s, a necessity to be able to use
         * {@link ExecutorService#invokeAll(CollectionSku, long, TimeUnit)}, which requires a
         * collection of {@link Callable}'s.
         * Note 4/12/2018: I remember here I had issue when using a Java AI utility method to create a Callable
         * for me - it didn't behave they way needed. So I opted by creating my own implementaton below as
         * an anonymous class (lambda). It was something like it was swallowing the error that FutureTask.get()
         * would throw or something like that.
         */
        Collection<Callable<Object>> callables = new ArrayList<>();
        for (FutureTask<T> r : pOperations) {
            callables.add(() -> {
                r.run();
                return r.get();
            });
        }

        /**
         * Execute all tasks with the specified "pTimeout". If one thread had issues, then the
         * "thrown.get()" call will NOT return null. Return the "thrown.get()" to caller so they can handle as they wish.
         * A NULL "thrown.get()" means there were no errors
         */
        List<Future<Object>> futures = pExecutorSvc.invokeAll(callables, pTimeout, TimeUnit.SECONDS);
        boolean timeoutOccurred = false;
        boolean interrupted = false;
        /*
         * The "thrown" below will hold exception, if any. We must use a synchronized variable to enable
         * safe publication of the thrown exception from runner thread back to the parent thread. Read section 3.5.3
         * and chapter 16 of "Java Concurrency In Practice" for more info.
         */
        final AtomicReference<Throwable> thrown = new AtomicReference<>(null);
        for (Future<Object> f : futures) {
            try {
                /**
                 * As per Java API doc, this is the way to find out if timeout occurred before all tasks
                 * completed.
                 * (See http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html#invokeAll-java.util.Collection-long-java.util.concurrent.TimeUnit-)
                 */
                if (f.isCancelled()) {
                    timeoutOccurred = true;
                    break;
                }

                /**
                 * IMPORTANT: Just because a tasks completed, it does not mean error might not have occurred. The only
                 * way to find out is by calling {@link Future#get}. Any exceptions thrown get wrapped into
                 * an ExecutionException, which gets re-thrown when {@link Future#get} gets
                 *   invoked. If we don't invoke {@link Future#get}, then we'll never know if there was any
                 *   exception, thus in fact swallowing the error, which will have obscure repercussions later on in
                 *   the code!!! Therefore it is critical we invoke {@link Future#get} as per below!!!
                 */
                f.get();
            } catch (Throwable e) {
                Throwable cause = e.getCause();
                thrown.set(cause);
                if (cause instanceof InterruptedException) {
                    interrupted = true;
                }
            } finally {
                if (interrupted) {
                    /**
                     * Restore thread interrupted status so that thread-owning code can handle appropriately, don't just
                     * swallow it (Java Concurrency In Practice, Goetz06 7.1.3)!!!
                     */
                    Thread.currentThread().interrupt();
                }

                if (timeoutOccurred) {
                    throw new TimeoutException("One or more tasks took longer to finish than the budgeted time"
                            + " (in seconds) of " + pTimeout);
                }
            }
        }

        return thrown.get();
    }


    /**
     *
     *
     * TODO: This method throws checked {@link RuleException}. Perhaps it should be part of Rule API util package,
     * TODO:   and not a common one.
     *
     * @param pExecutorSvc
     * @param pTasks
     * @param <T>
     * @return
     * @throws InterruptedException
     * @throws RuleException
     */
    public static <T extends DnbBusinessObject> T runTasksAsynchronouslyAndCancelOnFirstResult(
            ExecutorService pExecutorSvc,
            Collection<Callable<T>> pTasks) throws InterruptedException, RuleException {

        CompletionService<T> ecs = new ExecutorCompletionService<>(pExecutorSvc);
        int n = pTasks.size();
        List<Future<T>> futures = new ArrayList<>(n);
        T res = null;
        try {
            for (Callable<T> s : pTasks) {
                futures.add(ecs.submit(s));
            }

            for (int i = 0; i < n; i++) {
                try {
                    T r = ecs.take().get();
                    if (r != null && !r.isEmpty()) {
                        res = r;
                        break;
                    }
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuleException) {
                        throw (RuleException) cause;
                    }
                    throw launderThrowable(cause);
                }
            }
        } finally {
          // Cancel anything still running
            for (Future<T> f : futures) {
                f.cancel(true);
            }
        }

        return res;
    }


    /**
     * Ripped from Java Concurrency in practice, Goetz06, Page 98.
     *
     * This method is checking if the passed in {@link Throwable} is a checked exception or not. It
     * does so by checking if the argument is either an instance of {@link RuntimeException} or
     * {@link Error}. The calling code can use this method when for example it takes care of checking
     * for checked exceptions itself, delegating to this method to see if the exception is checked. "But
     * what are you high? Isn't calling code declaring checked exceptions in its method signature?"
     * you might be wondering. The checked exception might have been wrapped inside a another exception
     * type, and this helper method is recruited to help find out the wrapped exception type.
     * @param pThrowable
     * @return
     */
    public static RuntimeException launderThrowable(Throwable pThrowable) {
        if (pThrowable instanceof RuntimeException) {
            return (RuntimeException) pThrowable;
        } else if (pThrowable instanceof Error) {
            /**
             * Why do we cast to {@link Error}? Aha, first of all, we know we will not get {@link ClassCastException}
             * because we checked type in "else if" above. And the reason for doing so is that if we didn't cast,
             * we'd be forced to declare {@link Throwable} in the throws clause of this method, which we do
             * not want to do. Because {@link Error} is an unchecked exception as per Java docs (similar to
             * {@link RuleRuntimeException}), then we don't have to declare it in the <code>throws</code> clause
             * of this method.
             */
            throw (Error) pThrowable;
        } else {
            throw new IllegalStateException("Not unchecked", pThrowable);
        }
    }



    public static ExecutorService autoSizedExecutor() {
        return autoSizedExecutor(new DnbThreadFactory());
    }


    public static ExecutorService autoSizedExecutor(ThreadFactory pThreadFactory) {
        final int configuredPoolSize = Integer.valueOf(
                AbstractRulesEngine.fetchConfigurationPropertyValue(RuleConstants.CONFIG_PROP_THREAD_POOL_SIZE));
        final int poolSize = configuredPoolSize - CountingThread.runningThreads();
        final ExecutorService es;
        /**
         * Don't bother creating threads unnecessarily if we can't parallelize sufficiently. Cost of creating
         * threads must be offset by gains in running things in parallel
         */
        if (poolSize <= 2) {
            es =  SequentialTaskExecutor.SEQUENTIAL_TASK_EXECUTOR;
        } else {
            es = Executors.newFixedThreadPool(poolSize, pThreadFactory);
        }
        return es;
    }

    /**
     * Prints info message to both log file and to standard output. The reason for printing
     * to STDOUT is so that when maven build scripts execute, a copy of the messages are conveniently displayed
     * to developer in the console, thus eliminating the need for developer to go search the log files. Makes
     * development time quicker this way.
     */
    public static void infoToFileAndStdOut(String pMsg, Logger pLogger) {
        pLogger.info(pMsg);
        System.out.println(pMsg);
    }


    /**
     * Same as {@link CommonUtils#infoToFileAndStdOut(String, Logger)}, but for error messages
     */
    public static void errorToFileAndStdErr(String pMsg, Logger pLogger) {
        pLogger.error(pMsg);
        System.err.println(pMsg);
    }
}
