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
package tech.pegasys.pantheon.services;

import tech.pegasys.pantheon.api.services.PantheonEvents;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.eth.sync.BlockBroadcaster;

public class PantheonEventsImpl implements PantheonEvents {
  private final BlockBroadcaster blockBroadcaster;

  public PantheonEventsImpl(final BlockBroadcaster blockBroadcaster) {
    this.blockBroadcaster = blockBroadcaster;
  }

  @Override
  public Object addNewBlockPropagatedListener(final NewBlockPropagatedListener listener) {
    return blockBroadcaster.subscribePropagateNewBlocks(
        block -> dispatchNewBlockPropagatedMessage(block, listener));
  }

  @Override
  public void removeNewBlockPropagatedListener(final Object listenerIdentifier) {
    if (listenerIdentifier instanceof Long) {
      blockBroadcaster.unsubscribePropagateNewBlocks((Long) listenerIdentifier);
    }
  }

  private void dispatchNewBlockPropagatedMessage(
      final Block block, final NewBlockPropagatedListener listener) {
    listener.newBlockPropagated(block.getHeader());
  }
}
