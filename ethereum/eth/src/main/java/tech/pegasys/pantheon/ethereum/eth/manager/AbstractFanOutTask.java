/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.eth.manager;

import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractFanOutTask<I, O> extends AbstractEthTask<List<O>> {

  private static class InFlightRequestData<I, O> {
    final Optional<I> previous;
    final I current;
    final CompletableFuture<O> future;

    private InFlightRequestData(
        final Optional<I> previous, final I current, final CompletableFuture<O> future) {
      this.previous = previous;
      this.current = current;
      this.future = future;
    }
  }

  static final int WAIT_NS = 100_000;

  private BlockingQueue<I> inboundQueue;
  private BlockingQueue<O> outboundQueue;
  private Queue<InFlightRequestData<I, O>> inFlightQueue;
  private List<O> results;

  private boolean shuttingDown = false;
  private AtomicReference<Throwable> processingException = new AtomicReference<>(null);

  protected AbstractFanOutTask(
      final BlockingQueue<I> inboundQueue,
      final int outboundBacklogSize,
      final LabelledMetric<OperationTimer> ethTasksTimer) {
    super(ethTasksTimer);
    this.inboundQueue = inboundQueue;
    outboundQueue = new LinkedBlockingQueue<>(outboundBacklogSize);
    inFlightQueue = new ArrayDeque<>(outboundBacklogSize);
    results = new ArrayList<>();
  }

  @Override
  protected void executeTask() {
    Optional<I> previousInput = Optional.empty();

    try {
      // Loop until an exception happened or we have no work and we are told to stop.
      while ((processingException.get() == null)
          && !(inboundQueue.isEmpty() && inFlightQueue.isEmpty() && shuttingDown)) {
        // take all current inbound items and start them working
        while (!inboundQueue.isEmpty()) {
          final I currentInput = inboundQueue.poll();
          final Optional<CompletableFuture<O>> future =
              startProcessing(currentInput, previousInput);
          if (future.isPresent()) {
            inFlightQueue.add(new InFlightRequestData<>(previousInput, currentInput, future.get()));
          }
          previousInput = Optional.of(currentInput);
        }

        // read all in flight processes and post process them
        while (!inFlightQueue.isEmpty()) {
          final InFlightRequestData<I, O> inFlightData = inFlightQueue.peek();
          if (!inFlightData.future.isDone()) {
            break;
          }
          final O futureResult;
          try {
            futureResult = inFlightData.future.get();
          } catch (final InterruptedException e) {
            // Try Again.
            continue;
          } catch (final ExecutionException e) {
            // Fail completely.
            result.get().completeExceptionally(e.getCause());
            return;
          }
          // Success. Take it out of the queue.
          inFlightQueue.poll();
          final Optional<O> output =
              finishProcessing(inFlightData.current, inFlightData.previous, futureResult);
          output.ifPresent(
              o -> {
                try {
                  outboundQueue.put(o);
                } catch (final InterruptedException e) {
                  processingException.compareAndSet(null, e);
                }
                results.add(o);
              });
        }

        // Sleep if inbound queue is empty.
        if (inboundQueue.isEmpty()) {
          if (inFlightQueue.isEmpty()) {
            // If both queues are empty park it for a fixed time
            LockSupport.parkNanos(WAIT_NS);
          } else if (!inFlightQueue.isEmpty()) {
            // If the inflight queue has items wait on the future.
            try {
              inFlightQueue.peek().future.get(WAIT_NS, TimeUnit.NANOSECONDS);
            } catch (final InterruptedException | TimeoutException e) {
              // Ignore these, these are expected.
            } catch (final ExecutionException e) {
              // Don't ignore this one, something actually went wrong.
              processingException.compareAndSet(null, e.getCause());
            }
          }
        }
      }
    } catch (final RuntimeException e) {
      processingException.compareAndSet(null, e);
    }

    if (processingException.get() == null) {
      result.get().complete(results);
    } else {
      result.get().completeExceptionally(processingException.get());
    }
  }

  public BlockingQueue<O> getOutboundQueue() {
    return outboundQueue;
  }

  public void shutdown() {
    this.shuttingDown = true;
  }

  protected abstract Optional<CompletableFuture<O>> startProcessing(
      I input, Optional<I> previousInput);

  protected abstract Optional<O> finishProcessing(
      I input, Optional<I> previousInput, O futureResult);
}
