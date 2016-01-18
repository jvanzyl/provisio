/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.nexus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Closeables;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

public class Command {
  private static final ImmutableSet<Integer> DEFAULT_SUCCESSFUL_EXIT_CODES = ImmutableSet.of(0);
  private static final File DEFAULT_DIRECTORY = new File(".").getAbsoluteFile();
  private static final Duration DEFAULT_TIME_LIMIT = new Duration(365, TimeUnit.DAYS);

  private final List<String> command;
  private final Set<Integer> successfulExitCodes;
  private final File directory;
  private final Map<String, String> environment;
  private final Duration timeLimit;
  private Process process;

  public Process getProcess() {
    return process;
  }

  public Command(String... command) {
    this(ImmutableList.copyOf(command), DEFAULT_SUCCESSFUL_EXIT_CODES, DEFAULT_DIRECTORY, ImmutableMap.<String, String>of(), DEFAULT_TIME_LIMIT);
  }

  public Command(List<String> command, Set<Integer> successfulExitCodes, File directory, Map<String, String> environment, Duration timeLimit) {
    Preconditions.checkNotNull(command, "command is null");
    Preconditions.checkArgument(!command.isEmpty(), "command is empty");
    Preconditions.checkNotNull(successfulExitCodes, "successfulExitCodes is null");
    Preconditions.checkArgument(!successfulExitCodes.isEmpty(), "successfulExitCodes is empty");
    Preconditions.checkNotNull(directory, "directory is null");
    Preconditions.checkNotNull(timeLimit, "timeLimit is null");

    this.command = ImmutableList.copyOf(command);

    // these have default so are required
    this.successfulExitCodes = ImmutableSet.copyOf(successfulExitCodes);
    this.directory = directory;
    this.environment = environment;
    this.timeLimit = timeLimit;
  }

  public List<String> getCommand() {
    return command;
  }

  public Command addArgs(String... args) {
    Preconditions.checkNotNull(args, "args is null");
    return addArgs(ImmutableList.copyOf(args));
  }

  public Command addArgs(Iterable<String> args) {
    Preconditions.checkNotNull(args, "args is null");
    ImmutableList.Builder<String> command = ImmutableList.<String>builder().addAll(this.command).addAll(args);
    return new Command(command.build(), successfulExitCodes, directory, environment, timeLimit);
  }

  public Map<String, String> getEnvironment() {
    return environment;
  }

  public Command addEnvironment(String name, String value) {
    Preconditions.checkNotNull(name, "name is null");
    Preconditions.checkNotNull(value, "value is null");
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().putAll(this.environment).put(name, value);
    return new Command(command, successfulExitCodes, directory, builder.build(), timeLimit);
  }

  public Command addEnvironment(Map<String, String> environment) {
    Preconditions.checkNotNull(environment, "environment is null");
    ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().putAll(this.environment).putAll(environment);
    return new Command(command, successfulExitCodes, directory, builder.build(), timeLimit);
  }

  public Set<Integer> getSuccessfulExitCodes() {
    return successfulExitCodes;
  }

  public Command setSuccessfulExitCodes(int... successfulExitCodes) {
    Preconditions.checkNotNull(successfulExitCodes, "successfulExitCodes is null");
    return setSuccessfulExitCodes(ImmutableSet.copyOf(Ints.asList(successfulExitCodes)));
  }

  public Command setSuccessfulExitCodes(Set<Integer> successfulExitCodes) {
    Preconditions.checkNotNull(successfulExitCodes, "successfulExitCodes is null");
    Preconditions.checkArgument(!successfulExitCodes.isEmpty(), "successfulExitCodes is empty");
    return new Command(command, successfulExitCodes, directory, environment, timeLimit);
  }

  public File getDirectory() {
    return directory;
  }

  public Command setDirectory(String directory) {
    Preconditions.checkNotNull(directory, "directory is null");
    return setDirectory(new File(directory));
  }

  public Command setDirectory(File directory) {
    Preconditions.checkNotNull(directory, "directory is null");
    return new Command(command, successfulExitCodes, directory, environment, timeLimit);
  }

  public Duration getTimeLimit() {
    return timeLimit;
  }

  public Command setTimeLimit(double value, TimeUnit timeUnit) {
    return setTimeLimit(new Duration(value, timeUnit));
  }

  public Command setTimeLimit(Duration timeLimit) {
    Preconditions.checkNotNull(timeLimit, "timeLimit is null");
    return new Command(command, successfulExitCodes, directory, environment, timeLimit);
  }

  public void stop() {
    process.destroy();
  }

  public void execute(Executor executor) throws CommandFailedException {
    Preconditions.checkNotNull(executor, "executor is null");
    Preconditions.checkNotNull(command, "command is null");

    ProcessBuilder processBuilder = new ProcessBuilder(getCommand());
    processBuilder.directory(getDirectory());
    processBuilder.redirectErrorStream(true);
    processBuilder.environment().putAll(getEnvironment());
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new CommandFailedException(this, "failed to start", e);
    }

    ProcessCallable processCallable = new ProcessCallable(executor, process);
    submit(executor, processCallable);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Command other = (Command) o;

    if (!command.equals(other.command)) {
      return false;
    }
    if (!directory.equals(other.directory)) {
      return false;
    }
    if (!successfulExitCodes.equals(other.successfulExitCodes)) {
      return false;
    }
    if (!timeLimit.equals(other.timeLimit)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = command.hashCode();
    result = 31 * result + successfulExitCodes.hashCode();
    result = 31 * result + directory.hashCode();
    result = 31 * result + timeLimit.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("Command");
    sb.append("{command=").append(command);
    sb.append(", successfulExitCodes=").append(successfulExitCodes);
    sb.append(", directory=").append(directory);
    sb.append(", timeLimit=").append(timeLimit);
    sb.append('}');
    return sb.toString();
  }

  private static class ProcessCallable implements Callable<Integer> {
    private final Executor executor;
    private Process process;

    public ProcessCallable(Executor executor, Process process) {
      this.executor = executor;
      this.process = process;
    }

    @Override
    public Integer call() throws CommandFailedException, InterruptedException {
      OutputProcessor outputProcessor = null;
      try {
        outputProcessor = new OutputProcessor(process, executor);
        outputProcessor.start();
        int exitCode = process.waitFor();
        return exitCode;
      } finally {
        try {
          process.destroy();
        } finally {
          if (outputProcessor != null) {
            outputProcessor.destroy();
          }
        }
      }
    }
  }

  private static class OutputProcessor {
    private final InputStream inputStream;
    private final Executor executor;
    private Future<Void> outputFuture;

    private OutputProcessor(Process process, Executor executor) {
      this.inputStream = process.getInputStream();
      this.executor = executor;
    }

    public void start() {
      outputFuture = submit(executor, new Callable<Void>() {
        @Override
        public Void call() throws IOException {
          BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
          Writer writer = new OutputStreamWriter(System.out);
          String lineSeparator = System.getProperty("line.separator");
          String line = null;
          try {
            while ((line = reader.readLine()) != null) {
              writer.write(line);
              writer.write(lineSeparator);
              writer.flush();
            }
          } catch (Exception e) {
            //
            // Do nothing
            //
            e.printStackTrace();
          }
          return null;
        }
      });
    }

    private void destroy() {
      // close input stream which will normally interrupt the reader
      Closeables.closeQuietly(inputStream);

      if (outputFuture != null) {
        outputFuture.cancel(true);
      }
    }
  }

  private static <T> ListenableFuture<T> submit(Executor executor, Callable<T> task) {
    ListenableFutureTask<T> future = ListenableFutureTask.create(task);
    executor.execute(future);
    return future;
  }
}
